<?php
require_once '../simple.php';

class App {
    function index() {
        return page("Sensors Demo", [
            label("📱 Device Sensors", ['size' => 24, 'bold' => true, 'color' => '#4ec9b0', 'center' => true]),
            spacer(20),
            
            card("Location", [
                label("📍 GPS: --", ['id' => 'location']),
                button("Get Location", "getLocation", ['color' => '#2196F3']),
            ]),
            
            card("Battery", [
                label("🔋 Level: --", ['id' => 'battery']),
                button("Check Battery", "getBattery", ['color' => '#4CAF50']),
            ]),
            
            card("Compass", [
                label("🧭 Heading: --", ['id' => 'compass']),
                button("Read Compass", "getCompass", ['color' => '#FF9800']),
            ]),
            
            card("Barcode", [
                label("📷 Code: --", ['id' => 'barcode']),
                button("Scan Barcode", "scanCode", ['color' => '#9C27B0']),
            ]),
        ], ['padding' => 20]);
    }
    
    function getLocation($p) {
        return gps("onLocation");
    }
    
    function onLocation($p) {
        $lat = round($p['lat'], 6);
        $lng = round($p['lng'], 6);
        return updateView("location", [
            "text" => "📍 GPS: $lat, $lng"
        ]);
    }
    
    function getBattery($p) {
        return battery("onBattery");
    }
    
    function onBattery($p) {
        $level = $p['level'];
        $charging = $p['charging'] ? ' (Charging)' : '';
        return updateView("battery", [
            "text" => "🔋 Level: {$level}%{$charging}"
        ]);
    }
    
    function getCompass($p) {
        return compass("onCompass");
    }
    
    function onCompass($p) {
        $heading = round($p['azimuth']);
        $direction = $this->getDirection($heading);
        return updateView("compass", [
            "text" => "🧭 Heading: {$heading}° ({$direction})"
        ]);
    }
    
    private function getDirection($degrees) {
        $directions = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'];
        $index = round($degrees / 45) % 8;
        return $directions[$index];
    }
    
    function scanCode($p) {
        return scanBarcode("onBarcode");
    }
    
    function onBarcode($p) {
        $code = $p['code'];
        return batch([
            updateView("barcode", ["text" => "📷 Code: $code"]),
            toast("Scanned: $code")
        ]);
    }
}
