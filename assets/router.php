<?php
// router.php
// Dezactivăm orice output de erori/warning-uri către stdout care ar putea strica JSON-ul
error_reporting(E_ALL & ~E_NOTICE & ~E_WARNING);
ini_set('display_errors', 0);
ini_set('log_errors', 1);

class Router {
    /**
     * Handle CLI-based method dispatch (for subprocess fallback).
     * Reads method and params from CLI arguments.
     */
    public static function handle(object $app) {
        try {
            // Citim argumentele conform noii structuri din Java
            $options = getopt("", ["action:", "method:", "params:"]);

            // Suportăm atât --action (vechi) cât și --method (nou)
            $methodName = $options['method'] ?? ($options['action'] ?? 'index');
            $paramsJson = $options['params'] ?? '{}';
            $params = json_decode($paramsJson, true) ?? [];

            // Use dispatch for actual method resolution
            $result = self::dispatch($app, $methodName, $params);

            // Mark output done (prevent simple.php autorun from producing duplicate output)
            $GLOBALS['_simple_php_did_output'] = true;

            // Returnăm JSON curat
            if (is_object($result) && method_exists($result, 'toArray')) {
                echo json_encode($result->toArray());
            } else {
                echo json_encode($result);
            }
        } catch (Throwable $e) {
            echo json_encode([
                "error" => "PHP Exception",
                "message" => $e->getMessage()
            ]);
        }
    }

    /**
     * Dispatch a method call on the app object.
     * Used by server.php worker and handle() CLI path.
     *
     * @param object $app The application object
     * @param string $method The method name to call
     * @param mixed $params Parameters to pass (array or other)
     * @return mixed The method result
     * @throws RuntimeException if method not found
     */
    public static function dispatch(object $app, string $method, $params = null) {
        // Normalize params to array
        if ($params === null) {
            $params = [];
        } elseif (!is_array($params)) {
            $params = ['value' => $params];
        }

        // Try app method first, then global function
        if (method_exists($app, $method)) {
            return $app->$method($params);
        } elseif (function_exists($method)) {
            // Fall back to global function (for callbacks like onViewPropertyResult)
            return $method($params);
        } else {
            throw new RuntimeException("Method '$method' not found on " . get_class($app) . " and no global function exists");
        }
    }
}
?>