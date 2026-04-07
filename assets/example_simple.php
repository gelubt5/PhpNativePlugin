<?php
/**
 * Example App using Simple PHP Layer
 * 
 * Copy this file and modify it to create your own app!
 * Much easier than writing all the JSON and callbacks manually.
 */

require_once 'simple.php';

class MyApp {
    
    /**
     * Main screen - shown when app starts
     */
    public function index() {
        return page("My First App", [
            
            // A simple label
            label("Welcome! 👋", ['size' => 20, 'center' => true]),
            
            spacer(20),
            
            // Status display (we'll update this)
            statusLabel("status", "Tap a button below"),
            
            spacer(20),
            
            // Some buttons
            button("👋 Say Hello", "sayHello", ['color' => Colors::SUCCESS]),
            button("📍 Get Location", "getLocation", ['color' => Colors::INFO]),
            button("🔋 Check Battery", "checkBattery", ['color' => Colors::WARNING]),
            button("📷 Scan QR Code", "scanQR", ['color' => Colors::PURPLE]),
            
            spacer(20),
            
            // Navigation example
            button("📝 Go to Form", "showForm", ['color' => Colors::GRAY]),
        ]);
    }
    
    /**
     * Simple button click handler
     */
    public function sayHello($params) {
        return setText("status", "👋 Hello from PHP!", Colors::SUCCESS);
    }
    
    /**
     * Request GPS - result goes to onLocation
     */
    public function getLocation($params) {
        return gps("onLocation");
    }
    
    /**
     * Receive GPS result
     */
    public function onLocation($params) {
        $lat = $params['lat'] ?? 'N/A';
        $lng = $params['lng'] ?? 'N/A';
        return setText("status", "📍 Location: $lat, $lng", Colors::INFO);
    }
    
    /**
     * Request battery - result goes to onBattery
     */
    public function checkBattery($params) {
        return battery("onBattery");
    }
    
    /**
     * Receive battery result
     */
    public function onBattery($params) {
        $level = $params['level'] ?? 0;
        $charging = $params['charging'] ? "⚡ Charging" : "🔋 Not charging";
        return setText("status", "$charging: {$level}%", Colors::WARNING);
    }
    
    /**
     * Scan barcode/QR
     */
    public function scanQR($params) {
        return scanBarcode("onScanned");
    }
    
    /**
     * Receive scan result
     */
    public function onScanned($params) {
        $code = $params['code'] ?? '';
        if (isEmpty($code)) {
            return setText("status", "❌ Scan cancelled", Colors::DANGER);
        }
        return setText("status", "📷 Scanned: $code", Colors::SUCCESS);
    }
    
    // =========================================================================
    // Form Example
    // =========================================================================
    
    /**
     * Show a form screen
     */
    public function showForm($params) {
        return page("Contact Form", [
            
            label("Enter your details:", ['color' => Colors::TEXT_MUTED]),
            
            spacer(10),
            
            input("name", "Your name"),
            input("email", "Your email"),
            input("message", "Your message", ['multiline' => true]),
            
            spacer(10),
            
            checkbox("subscribe", "Subscribe to newsletter"),
            
            spacer(20),
            
            button("✅ Submit", "onSubmit", ['color' => Colors::SUCCESS]),
            button("← Back", "index", ['color' => Colors::GRAY]),
            label("Items:", ['color' => Colors::TEXT_MUTED]),
            
            // Simple list with click handler
            simpleList(["Apple", "Banana", "Cherry", "Date"], "onItemClick"),
         
        ]);
    }
    
    /**
     * Handle list item click - receives the index of clicked item
     */
    public function onItemClick($params) {
        $index = $params['tag'] ?? -1;
        $items = ["Apple", "Banana", "Cherry", "Date"];
        $itemName = $items[$index] ?? "Unknown";
        return alert("You clicked item #$index: $itemName", "List Click");
    }
    
    /**
     * Handle form submission
     */
    public function onSubmit($params) {
        // Get the name field value, result goes to processForm
        return getText("name", "processForm");
    }
    
    /**
     * Process form after getting name value
     */
    public function processForm($params) {
        $name = $params['value'] ?? 'Guest';
        
        if (isEmpty($name)) {
            return alert("Please enter your name!", "Missing Info");
        }
        
        return alert("Thanks $name! Form submitted.", "Success");
    }
}
?>
