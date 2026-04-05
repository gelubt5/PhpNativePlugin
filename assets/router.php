<?php
// router.php
// Dezactivăm orice output de erori/warning-uri către stdout care ar putea strica JSON-ul
error_reporting(E_ALL & ~E_NOTICE & ~E_WARNING);
ini_set('display_errors', 0);
ini_set('log_errors', 1);

class Router {
    public static function handle(object $app) {
        try {
            // Citim argumentele conform noii structuri din Java
            $options = getopt("", ["action:", "method:", "params:"]);

            // Suportăm atât --action (vechi) cât și --method (nou)
            $methodName = $options['method'] ?? ($options['action'] ?? 'index');
            $paramsJson = $options['params'] ?? '{}';
            $params = json_decode($paramsJson, true) ?? [];

            if (!method_id_exists($app, $methodName)) {
                echo json_encode(["error" => "Method '$methodName' not found"]);
                return;
            }

            // Executăm metoda
            $result = $app->$methodName($params);

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
}

function method_id_exists($obj, $method): bool {
    try {
        $reflection = new ReflectionClass($obj);
        return $reflection->hasMethod($method);
    } catch (Exception $e) {
        return false;
    }
}
?>