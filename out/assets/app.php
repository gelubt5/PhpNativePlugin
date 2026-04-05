<?php
/**
 * PhpNativePlugin - Sample Application
 * 
 * This demonstrates the hybrid architecture where PHP controls business logic,
 * Java renders native UI, and DroidScript provides sensor access.
 * 
 * Architecture Flow:
 * 1. User action → Java calls PHP method
 * 2. PHP returns UI JSON or DS_SENSOR_CALL action
 * 3. For sensors: Java injects JS into DroidScript
 * 4. Sensor result → Java → PHP → UI Update
 */

require_once 'ui_core.php';

class MyApp {
    
    /**
     * Main screen - demonstrates various UI components and sensor access
     */
    public function index() {
        return (new VerticalLayout([

            // Header
            (new TextView())
                ->text("PhpNative Demo")
                ->textSize(28)
                ->textColor("#4ec9b0")
                ->gravity("center"),

            (new TextView())
                ->text("PHP 8 + Java + DroidScript")
                ->textSize(14)
                ->textColor("#666666")
                ->gravity("center"),

            // Status label (updated by sensors)
            (new TextView())
                ->id("status_label")
                ->text("Status: Ready")
                ->textSize(16)
                ->textColor("#888888")
                ->padding(20),

            // Sensor buttons
            (new Button())
                ->text("📍 Get Location")
                ->id("btn_gps")
                ->action("requestLocation")
                ->textAllCaps(false)
                ->backgroundColor("#4CAF50"),

            (new Button())
                ->text("🔋 Check Battery")
                ->id("btn_battery")
                ->action("requestBattery")
                ->textAllCaps(false)
                ->backgroundColor("#FF9800"),

            (new Button())
                ->text("📷 Scan Barcode")
                ->id("btn_barcode")
                ->action("requestBarcode")
                ->textAllCaps(false)
                ->backgroundColor("#2196F3"),

            (new Button())
                ->text("🧭 Get Compass")
                ->id("btn_compass")
                ->action("requestCompass")
                ->textAllCaps(false)
                ->backgroundColor("#9C27B0"),

            // Interactive controls
            (new CheckBox())
                ->text("Enable Auto-Refresh")
                ->id("chk_auto")
                ->onCheckedChange("handleAutoRefresh"),

            (new Button())
                ->text("ℹ️ About")
                ->id("btn_about")
                ->action("showAbout")
                ->textAllCaps(false)
                ->backgroundColor("#607D8B"),

        ]))->padding(30)->gravity("center");
    }

    // =========================================================================
    // Sensor Request Methods
    // These return DS_SENSOR_CALL which tells Java to inject JS into DroidScript
    // =========================================================================

    /**
     * Request GPS location through DroidScript
     */
    public function requestLocation($params) {
        return [
            "action" => "DS_SENSOR_CALL",
            "sensor" => "location",
            "callback" => "handle_gps"
        ];
    }

    /**
     * Request battery status through DroidScript
     */
    public function requestBattery($params) {
        return [
            "action" => "DS_SENSOR_CALL",
            "sensor" => "battery",
            "callback" => "handle_battery"
        ];
    }

    /**
     * Request barcode scan through DroidScript
     */
    public function requestBarcode($params) {
        return [
            "action" => "DS_SENSOR_CALL",
            "sensor" => "barcode",
            "callback" => "handle_barcode"
        ];
    }

    /**
     * Request compass/orientation through DroidScript
     */
    public function requestCompass($params) {
        return [
            "action" => "DS_SENSOR_CALL",
            "sensor" => "compass",
            "callback" => "handle_compass"
        ];
    }

    // =========================================================================
    // Sensor Result Handlers
    // These receive data from DroidScript sensors and update the UI
    // =========================================================================

    /**
     * Handle GPS location result
     */
    public function handle_gps($params) {
        $lat = $params['lat'] ?? 'N/A';
        $lng = $params['lng'] ?? 'N/A';
        $alt = $params['altitude'] ?? '?';
        
        // Business logic: Check if location is valid
        if (is_numeric($lat) && is_numeric($lng)) {
            $text = sprintf("📍 Location: %.4f, %.4f\n🏔️ Altitude: %sm", $lat, $lng, $alt);
            $color = "#4CAF50";
        } else {
            $text = "⚠️ Could not get location";
            $color = "#F44336";
        }

        return [
            "action" => "update",
            "target" => "status_label",
            "attributes" => [
                "text" => $text,
                "textColor" => $color
            ]
        ];
    }

    /**
     * Handle battery status result
     */
    public function handle_battery($params) {
        $level = $params['level'] ?? 0;
        $charging = $params['charging'] ?? false;
        
        $icon = $charging ? "⚡" : "🔋";
        $status = $charging ? "Charging" : "Discharging";
        
        // Business logic: Warning if battery low
        if ($level < 20 && !$charging) {
            $color = "#F44336";
            $warning = " ⚠️ LOW!";
        } else {
            $color = "#4CAF50";
            $warning = "";
        }

        return [
            "action" => "update",
            "target" => "status_label",
            "attributes" => [
                "text" => "$icon Battery: {$level}% ($status)$warning",
                "textColor" => $color
            ]
        ];
    }

    /**
     * Handle barcode scan result
     */
    public function handle_barcode($params) {
        $code = $params['code'] ?? '';
        
        if (empty($code)) {
            return [
                "action" => "update",
                "target" => "status_label",
                "attributes" => [
                    "text" => "❌ Scan cancelled",
                    "textColor" => "#F44336"
                ]
            ];
        }

        // Business logic: Validate or process barcode
        $codeType = $this->detectBarcodeType($code);
        
        return [
            "action" => "update",
            "target" => "status_label",
            "attributes" => [
                "text" => "📷 Scanned ($codeType):\n$code",
                "textColor" => "#4CAF50"
            ]
        ];
    }

    /**
     * Handle compass result
     */
    public function handle_compass($params) {
        $azimuth = $params['azimuth'] ?? 0;
        $pitch = $params['pitch'] ?? 0;
        $roll = $params['roll'] ?? 0;
        
        // Convert azimuth to cardinal direction
        $direction = $this->azimuthToDirection($azimuth);
        
        return [
            "action" => "update",
            "target" => "status_label",
            "attributes" => [
                "text" => "🧭 Heading: {$direction} ({$azimuth}°)\n↕️ Pitch: {$pitch}° | ↔️ Roll: {$roll}°",
                "textColor" => "#9C27B0"
            ]
        ];
    }

    // =========================================================================
    // UI Event Handlers
    // =========================================================================

    /**
     * Handle auto-refresh checkbox change
     */
    public function handleAutoRefresh($params) {
        $enabled = $params['isChecked'] ?? false;
        
        return [
            "action" => "update",
            "target" => "status_label",
            "attributes" => [
                "text" => $enabled 
                    ? "✅ Auto-refresh enabled" 
                    : "⏸️ Auto-refresh disabled",
                "textColor" => $enabled ? "#4CAF50" : "#888888"
            ]
        ];
    }

    /**
     * Show about screen
     */
    public function showAbout() {
        return (new VerticalLayout([
            
            (new TextView())
                ->text("PhpNativePlugin")
                ->textSize(24)
                ->textColor("#4ec9b0")
                ->gravity("center"),

            (new TextView())
                ->text("Version 1.0")
                ->textSize(14)
                ->textColor("#888888")
                ->gravity("center"),

            (new TextView())
                ->id("about_text")
                ->text("A hybrid architecture plugin that combines:\n\n" .
                       "• PHP 8 for business logic\n" .
                       "• Java for native UI rendering\n" .
                       "• DroidScript for sensor access\n\n" .
                       "All without recompiling the APK!")
                ->textSize(14)
                ->textColor("#cccccc")
                ->padding(20),

            (new Button())
                ->text("← Back to Main")
                ->action("index")
                ->textAllCaps(false)
                ->backgroundColor("#607D8B"),

        ]))->padding(40)->gravity("center");
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Detect barcode type
     */
    private function detectBarcodeType($code) {
        if (strlen($code) === 13 && is_numeric($code)) return "EAN-13";
        if (strlen($code) === 12 && is_numeric($code)) return "UPC-A";
        if (strlen($code) === 8 && is_numeric($code)) return "EAN-8";
        if (preg_match('/^https?:\/\//', $code)) return "URL";
        if (preg_match('/^[A-Z0-9]{10,}$/', $code)) return "Code 39";
        return "Unknown";
    }

    /**
     * Convert azimuth angle to cardinal direction
     */
    private function azimuthToDirection($azimuth) {
        $directions = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'];
        $index = round($azimuth / 45) % 8;
        return $directions[$index];
    }
}
?>
