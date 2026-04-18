<?php
/**
 * server.php — Persistent PHP "Brain" server for PhpNativePlugin.
 *
 * Launched by Java as:
 *     <nativeLibraryDir>/libphp2.so -S 127.0.0.1:9045 -t <dir> server.php
 * or (preferred, because php -S doesn't give us raw socket control for
 *  long-lived request state):
 *     <nativeLibraryDir>/libphp2.so server.php
 * In the second form we run our own stream_socket_server loop below.
 *
 * Protocol (newline-free, Content-Length framed):
 *   Content-Length: <N>\r\n
 *   \r\n
 *   <N bytes of JSON>
 *
 * Request JSON:
 *   {"id": <int>, "method": "<string>", "params": <any>, "token": "<hmac>"}
 * Response JSON:
 *   {"id": <int>, "result": <any>, "error": null | {message, trace}}
 *
 * Internal control methods (prefixed with "__"):
 *   __ping__      → {"pong": true}
 *   __shutdown__  → graceful exit
 *   __reload__    → re-require app.php (dev mode)
 */
 
declare(strict_types=1);
 
// --- Error handling -------------------------------------------------------
// Route everything through Router so nothing bleeds into the socket.
error_reporting(E_ALL);
ini_set('display_errors', '0');
ini_set('log_errors', '1');
 
set_error_handler(static function (int $severity, string $msg, string $file, int $line): bool {
    if (!(error_reporting() & $severity)) {
        return false;
    }
    throw new ErrorException($msg, 0, $severity, $file, $line);
});
 
// --- Locate app ----------------------------------------------------------
$baseDir = dirname(__FILE__);
require_once $baseDir . '/ui_core.php';
require_once $baseDir . '/simple.php';
require_once $baseDir . '/router.php';
 
// app.php defines MyApp. We load it lazily so __reload__ can refresh it.
function _load_app(): object {
    static $app = null;
    if ($app === null) {
        require_once dirname(__FILE__) . '/app.php';
        $app = new MyApp();
    }
    return $app;
}
 
function _reload_app(): object {
    // No real way to "un-include" PHP, but we can re-instantiate
    global $app; // allow external tests to rebind if they need to
    $class = class_exists('MyApp') ? 'MyApp' : null;
    if ($class === null) {
        require_once dirname(__FILE__) . '/app.php';
    }
    return new MyApp();
}
 
// --- HMAC secret ----------------------------------------------------------
function _load_secret(string $baseDir): string {
    // Prefer env var path (app-private storage) for security
    $path = getenv('PHPNATIVE_SECRET_PATH');
    if ($path === false || $path === '') {
        // Fallback to script directory (legacy/development)
        $path = $baseDir . '/phpnative.key';
    }
    if (!is_file($path)) {
        // Java is supposed to write this before starting us. If it's
        // missing we refuse to start — unauthenticated local sockets are
        // a security hole.
        fwrite(STDERR, "[server.php] missing secret at $path\n");
        exit(2);
    }
    $secret = file_get_contents($path);
    if ($secret === false || strlen($secret) < 16) {
        fwrite(STDERR, "[server.php] secret too short\n");
        exit(3);
    }
    return $secret;
}
 
function _verify_token(string $secret, $id, string $method, $params, ?string $token): bool {
    if ($token === null) return false;
    // Use null byte delimiters (0x00) for unambiguous field separation
    // even if $method contains special characters.
    // Always JSON-encode params for consistent serialization with Java.
    $payload = (string)$id . "\x00" . $method . "\x00" . json_encode($params, JSON_UNESCAPED_UNICODE);
    $expected = hash_hmac('sha256', $payload, $secret);
    return hash_equals($expected, $token);
}
 
// --- Framing --------------------------------------------------------------
function _read_frame($stream): ?string {
    $headers = '';
    while (!feof($stream)) {
        $line = fgets($stream, 4096);
        if ($line === false) return null;
        $headers .= $line;
        if (str_ends_with($headers, "\r\n\r\n") || $headers === "\r\n") break;
        if (strlen($headers) > 8192) return null; // header flood guard
    }
    if (!preg_match('/Content-Length:\s*(\d+)/i', $headers, $m)) {
        return null;
    }
    $len = (int) $m[1];
    if ($len < 0 || $len > 16 * 1024 * 1024) return null; // 16 MiB cap
    $body = '';
    while (strlen($body) < $len && !feof($stream)) {
        $chunk = fread($stream, $len - strlen($body));
        if ($chunk === false || $chunk === '') return null;
        $body .= $chunk;
    }
    return strlen($body) === $len ? $body : null;
}
 
function _write_frame($stream, string $body): void {
    $hdr = 'Content-Length: ' . strlen($body) . "\r\n\r\n";
    fwrite($stream, $hdr . $body);
    fflush($stream);
}
 
// --- Dispatch -------------------------------------------------------------
function _handle_request(array $req, object $app): array {
    $id = $req['id'] ?? null;
    $method = (string) ($req['method'] ?? '');
    $params = $req['params'] ?? null;
 
    if ($method === '') {
        return ['id' => $id, 'result' => null, 'error' => ['message' => 'missing method']];
    }
 
    // Control methods
    if ($method === '__ping__') {
        return ['id' => $id, 'result' => ['pong' => true, 'pid' => getmypid()], 'error' => null];
    }
    if ($method === '__shutdown__') {
        return ['id' => $id, 'result' => ['bye' => true], 'error' => null, '__shutdown__' => true];
    }
    if ($method === '__reload__') {
        _reload_app();
        return ['id' => $id, 'result' => ['reloaded' => true], 'error' => null];
    }
 
    // App-level dispatch via Router (keeps the existing method-resolution
    // behaviour, including _get/_setState helpers).
    try {
        // Router::dispatch returns the raw value (string|array|Component).
        $result = Router::dispatch($app, $method, $params);
        // Normalise for wire: arrays/Components already json-friendly;
        // strings pass through.
        if ($result instanceof Component) {
            $result = $result->toArray();
        }
        return ['id' => $id, 'result' => $result, 'error' => null];
    } catch (Throwable $e) {
        return [
            'id' => $id,
            'result' => null,
            'error' => [
                'message' => $e->getMessage(),
                'class' => get_class($e),
                'file' => basename($e->getFile()),
                'line' => $e->getLine(),
            ],
        ];
    }
}
 
// --- Main loop ------------------------------------------------------------
function main(string $baseDir): void {
    $requestedPort = (int) (getenv('PHPNATIVE_PORT') ?: 0);
    $bind = 'tcp://127.0.0.1:' . $requestedPort;
 
    $secret = _load_secret($baseDir);
 
    $errno = 0;
    $errstr = '';
    $server = @stream_socket_server($bind, $errno, $errstr);
    if ($server === false) {
        fwrite(STDERR, "[server.php] bind failed: $errstr ($errno)\n");
        exit(1);
    }
    
    // Get the actual assigned port (important when requestedPort was 0)
    $localName = stream_socket_get_name($server, false); // e.g., "127.0.0.1:54321"
    $actualPort = (int) substr($localName, strrpos($localName, ':') + 1);
    
    // Ready marker — Java watches for this line on stdout.
    fwrite(STDOUT, "READY port=$actualPort pid=" . getmypid() . "\n");
    fflush(STDOUT);
 
    $app = _load_app();
    $shuttingDown = false;
 
    while (!$shuttingDown) {
        $client = @stream_socket_accept($server, 60);
        if ($client === false) {
            // Accept timeout — idle liveness check.
            continue;
        }
        stream_set_timeout($client, 30);
 
        try {
            $frame = _read_frame($client);
            if ($frame === null) {
                fclose($client);
                continue;
            }
            $req = json_decode($frame, true);
            if (!is_array($req)) {
                _write_frame($client, json_encode([
                    'id' => null,
                    'result' => null,
                    'error' => ['message' => 'invalid JSON'],
                ]));
                fclose($client);
                continue;
            }
 
            // Authenticate
            $ok = _verify_token(
                $secret,
                $req['id'] ?? 0,
                (string) ($req['method'] ?? ''),
                $req['params'] ?? null,
                $req['token'] ?? null
            );
            if (!$ok) {
                _write_frame($client, json_encode([
                    'id' => $req['id'] ?? null,
                    'result' => null,
                    'error' => ['message' => 'auth failed'],
                ]));
                fclose($client);
                continue;
            }
 
            $resp = _handle_request($req, $app);
            if (!empty($resp['__shutdown__'])) {
                $shuttingDown = true;
                unset($resp['__shutdown__']);
            }
            _write_frame($client, json_encode($resp, JSON_UNESCAPED_UNICODE));
        } catch (Throwable $e) {
            // Last-chance guard so one bad frame can't kill the loop.
            error_log('[server.php] loop error: ' . $e->getMessage());
        } finally {
            if (is_resource($client)) fclose($client);
        }
    }
 
    fclose($server);
    fwrite(STDOUT, "BYE\n");
}
 
main(__DIR__);