/*
    PhpNativePlugin - JavaScript Interface for DroidScript
    
    This file provides the JavaScript API that DroidScript apps use to interact
    with the PhpNativePlugin Java backend.
    
    Architecture Overview:
    - PHP 8 = "Brain" (business logic, UI decisions, data processing)
    - Java Plugin = "Native Executor" (renders UI, bridges PHP/DroidScript)
    - DroidScript = "Hardware Interface" (sensors, camera, GPS, permissions)
    
    Usage:
    app.LoadPlugin("PhpNativePlugin");
    var php = app.CreatePhpNative();
    php.Start();  // Starts the PHP app
*/

// Global reference for sensor callbacks from Java
var _phpPlugin = null;

// Add factory method to DroidScript's app object
app.CreatePhpNative = function(options) {
    return new PhpNativePlugin(options);
}

// Main plugin wrapper
function PhpNativePlugin(options) {
    var self = this;
    var dw = app.GetDisplayWidth();
    var dh = app.GetDisplayHeight();
    
    // Create the Java plugin instance
    this.plg = _CreatePlugin("com.phpnative.plugins.user.PhpNativePlugin", options);
    
    // Store global reference for sensor callbacks
    _phpPlugin = this;
    
    // Internal callbacks
    this._onPhpResponse = null;
    this._onSensorResult = null;
    this._onUiReady = null;
    this._onError = null;
    
    // =========================================================================
    // Core API
    // =========================================================================
    
    /**
     * Get plugin version
     * @returns {number} Version number
     */
    this.GetVersion = function() {
        return parseFloat(this.plg.Send("QueryVersion"));
    }
    
    /**
     * Check if PHP environment is ready
     * @returns {boolean} Ready state
     */
    this.IsReady = function() {
        return this.plg.Send("IsReady") === "true";
    }
    
    // =========================================================================
    // App Folder Management
    // =========================================================================
    
    /**
     * Set the DroidScript app and copy PHP files to its folder.
     * PHP will then run from /sdcard/DroidScript/<appName>/
     * @param {string} appName Name of the DroidScript app
     * @returns {object} {success, app, path, files} or {error}
     */
    this.SetApp = function(appName) {
        var result = this.plg.Send("SetApp", appName);
        try { return JSON.parse(result); }
        catch(e) { return {error: result}; }
    }
    
    /**
     * Get the current PHP scripts folder absolute path
     * @returns {string} Absolute path to PHP folder
     */
    this.GetAppPath = function() {
        return this.plg.Send("GetAppPath");
    }
    
    /**
     * Copy PHP files to a DroidScript app folder (without changing current app)
     * @param {string} appName Name of the DroidScript app
     * @returns {object} {files: [...]} or {error}
     */
    this.CopyPhpToApp = function(appName) {
        var result = this.plg.Send("CopyPhpToApp", appName);
        try { return JSON.parse(result); }
        catch(e) { return {error: result}; }
    }
    
    /**
     * Extract documentation files to a DroidScript app folder.
     * Copies: docs/, docs_examples/, DocRunner.js -> {appName}.js
     * @param {string} appName Name of the DroidScript app
     * @returns {object} {success, path, files: [...]} or {error}
     */
    this.ExtractDocs = function(appName) {
        var result = this.plg.Send("ExtractDocs", appName);
        try { return JSON.parse(result); }
        catch(e) { return {error: result}; }
    }
    
    /**
     * Debug plugin paths - shows where files are located
     * Use this to find where docs are actually stored on the device
     * @returns {object} {paths: {...}, m_plugDir, m_filesDir, extStorage}
     */
    this.DebugPaths = function() {
        var result = this.plg.Send("DebugPaths");
        try { return JSON.parse(result); }
        catch(e) { return {error: result}; }
    }
    
    /**
     * Get debug info about all relevant paths (for troubleshooting)
     * Returns: package, idePath, ideExists, apkPath, apkExists, legacyPath, legacyExists, etc.
     * @param {string} [appName] App name to test paths with
     * @returns {object} Path information
     */
    this.GetPathInfo = function(appName) {
        var result = this.plg.Send("GetPathInfo", appName || "");
        try { return JSON.parse(result); }
        catch(e) { return {error: result}; }
    }
    
    /**
     * Debug PHP execution - tests if PHP binary works and returns detailed info
     * @returns {object} Debug information about PHP paths, files, and test execution
     */
    this.DebugPhp = function() {
        var result = this.plg.Send("DebugPhp");
        try { return JSON.parse(result); }
        catch(e) { return {error: result, raw: result}; }
    }
    
    /**
     * Run PHP logic.php and return raw output for debugging
     * Shows exactly what PHP outputs (JSON or error)
     * @param {string} [method="index"] PHP method to call
     * @returns {object} {exitCode, rawOutput, error, ...}
     */
    this.RunPhpTest = function(method) {
        var result = this.plg.Send("RunPhpTest", method || "index");
        try { return JSON.parse(result); }
        catch(e) { return {error: "Parse failed", raw: result}; }
    }
    
    /**
     * Start the PHP application (calls logic.php with method)
     * @param {string} [method="index"] Initial PHP method to call
     */
    this.Start = function(method,entryFile) {
        method = method || "index";
        this.plg.Send("StartApp", method,entryFile);
    }
    
    /**
     * Check if app.php exists in the current app folder
     * @returns {boolean} True if app.php exists
     */
    this.HasAppPhp = function() {
        return this.plg.Send("HasAppPhp") === "true";
    }
    
    /**
     * Run app.php directly and render its UI output.
     * If app.php exists in the project folder, execute it and render the result.
     * app.php should contain a MyApp class with methods that return UI JSON.
     * @param {string} [method="index"] Method to call in MyApp class
     */
    this.RunApp = function(method) {
        method = method || "index";
        this.plg.Send("RunApp", method);
    }
    
    /**
     * Run any PHP file and render its UI output.
     * Searches for the file in: app dir, plugin assets, plugin dir
     * @param {string} filePath Path to PHP file (relative or absolute)
     * @param {string} [method="index"] Method to call
     */
    this.RunFile = function(filePath, method,appName) {
        method = method || "index";
        this.plg.Send("RunFile", filePath, method,appName);
    }
    
    /**
     * Debug: Run a PHP file and return raw output (doesn't render)
     * Use this to diagnose why examples show white screen
     * @param {string} filePath Path to PHP file
     * @param {string} [method="index"] Method to call
     * @returns {object} {file, includePath, method, exitCode, outputLength, output}
     */
    this.RunFileDebug = function(filePath, method) {
        method = method || "index";
        var result = this.plg.Send("RunFileDebug", filePath, method);
        try { return JSON.parse(result); }
        catch(e) { return {error: result}; }
    }
    
    /**
     * Auto-start: If app.php exists, run it. Otherwise use logic.php.
     * This is the recommended way to start the app.
     * @param {string} [method="index"] Initial method to call
     */
    this.AutoStart = function(method) {
        method = method || "index";
        if (this.HasAppPhp()) {
            this.RunApp(method);
        } else {
            this.Start(method);
        }
    }
    
    /**
     * Call a PHP method directly
     * @param {string} method PHP method name
     * @param {object} [params={}] Parameters to pass to PHP
     * @returns {string} JSON response from PHP
     */
    this.CallPhp = function(method, params) {
        var paramsJson = params ? JSON.stringify(params) : "{}";
        return this.plg.Send("CallPhp", method, paramsJson);
    }
    
    /**
     * Render UI from JSON (usually called automatically)
     * @param {string|object} json UI definition JSON
     */
    this.RenderUI = function(json) {
        var jsonStr = typeof json === "string" ? json : JSON.stringify(json);
        this.plg.Send("RenderUI", jsonStr);
    }
    
    /**
     * Update a specific view's attributes
     * @param {string} viewId View ID
     * @param {object} attrs Attributes to update
     */
    this.UpdateView = function(viewId, attrs) {
        this.plg.Send("UpdateView", viewId, JSON.stringify(attrs));
    }
    
    // =========================================================================
    // Overlay Management
    // =========================================================================
    
    /**
     * Show the native UI overlay
     */
    this.ShowOverlay = function() {
        this.plg.Send("ShowOverlay");
    }
    
    /**
     * Hide the native UI overlay (doesn't destroy it)
     */
    this.HideOverlay = function() {
        this.plg.Send("HideOverlay");
    }
    
    /**
     * Remove and destroy the native UI overlay
     */
    this.RemoveOverlay = function() {
        this.plg.Send("RemoveOverlay");
    }

    /**
     * Navigate back to the previous screen.
     * If at root screen, shows exit confirmation dialog.
     */
    this.GoBack = function() {
        this.plg.Send("GoBack");
    }

    /**
     * Set callback for when the user confirms exit via back button.
     * Called after the overlay is hidden.
     * @param {function} callback Function to call on exit
     */
    this.SetOnBack = function(callback) {
        this._onBack = callback;
        this.plg.Send("SetOnBack", "_phpPlugin._onBack");
    }
    
    // =========================================================================
    // View Accessors
    // =========================================================================
    
    /**
     * Get text from a view
     * @param {string} viewId View ID
     * @returns {string} Text content
     */
    this.GetViewText = function(viewId) {
        return this.plg.Send("GetViewText", viewId);
    }
    
    /**
     * Set text on a view
     * @param {string} viewId View ID
     * @param {string} text Text to set
     */
    this.SetViewText = function(viewId, text) {
        this.plg.Send("SetViewText", viewId, text);
    }
    
    // =========================================================================
    // Sensor Bridge (The "Puppeteer" Mechanism)
    // These methods request sensor data through DroidScript. The results
    // are automatically forwarded to PHP for processing.
    // =========================================================================
    
    /**
     * Request GPS location (result goes to PHP)
     * @param {string} [phpCallback="handle_gps"] PHP method to call with result
     */
    this.RequestLocation = function(phpCallback) {
        phpCallback = phpCallback || "handle_gps";
        this.plg.Send("InjectSensorCall", "location", phpCallback);
    }
    
    /**
     * Request battery status
     * @param {string} [phpCallback="handle_battery"] PHP method
     */
    this.RequestBattery = function(phpCallback) {
        phpCallback = phpCallback || "handle_battery";
        this.plg.Send("InjectSensorCall", "battery", phpCallback);
    }
    
    /**
     * Request accelerometer reading
     * @param {string} [phpCallback="handle_accelerometer"] PHP method
     */
    this.RequestAccelerometer = function(phpCallback) {
        phpCallback = phpCallback || "handle_accelerometer";
        this.plg.Send("InjectSensorCall", "accelerometer", phpCallback);
    }
    
    /**
     * Request compass/orientation reading
     * @param {string} [phpCallback="handle_compass"] PHP method
     */
    this.RequestCompass = function(phpCallback) {
        phpCallback = phpCallback || "handle_compass";
        this.plg.Send("InjectSensorCall", "compass", phpCallback);
    }
    
    /**
     * Scan barcode/QR code
     * @param {string} [phpCallback="handle_barcode"] PHP method
     */
    this.ScanBarcode = function(phpCallback) {
        phpCallback = phpCallback || "handle_barcode";
        this.plg.Send("InjectSensorCall", "barcode", phpCallback);
    }
    
    /**
     * Take a photo with camera
     * @param {string} [phpCallback="handle_camera"] PHP method
     */
    this.TakePhoto = function(phpCallback) {
        phpCallback = phpCallback || "handle_camera";
        this.plg.Send("InjectSensorCall", "camera", phpCallback);
    }
    
    /**
     * Get WiFi status
     * @param {string} [phpCallback="handle_wifi"] PHP method
     */
    this.RequestWifi = function(phpCallback) {
        phpCallback = phpCallback || "handle_wifi";
        this.plg.Send("InjectSensorCall", "wifi", phpCallback);
    }
    
    /**
     * Get Bluetooth status
     * @param {string} [phpCallback="handle_bluetooth"] PHP method
     */
    this.RequestBluetooth = function(phpCallback) {
        phpCallback = phpCallback || "handle_bluetooth";
        this.plg.Send("InjectSensorCall", "bluetooth", phpCallback);
    }
    
    /**
     * Start speech recognition
     * @param {string} [phpCallback="handle_speech"] PHP method
     */
    this.SpeechRecognition = function(phpCallback) {
        phpCallback = phpCallback || "handle_speech";
        this.plg.Send("InjectSensorCall", "speechrecognition", phpCallback);
    }
    
    /**
     * Vibrate the device
     * @param {string} [pattern="500"] Vibration pattern in ms
     */
    this.Vibrate = function(pattern) {
        pattern = pattern || "500";
        this.plg.Send("InjectSensorCall", "vibrate", "handle_vibrate");
    }
    
    /**
     * Text to speech
     * @param {string} text Text to speak
     */
    this.Speak = function(text) {
        // We need to call PHP first to let it decide what to say
        // or inject directly
        this.plg.Send("InjectSensorCall", "speech", "handle_speech");
    }
    
    // =========================================================================
    // Internal Sensor Result Handler (Called from Java)
    // =========================================================================
    
    /**
     * Internal method called by Java when a sensor returns data
     * @param {string} sensorType Type of sensor
     * @param {string} dataJson JSON data from sensor
     */
    this.OnSensorResult = function(sensorType, dataJson) {
        // Forward to Java which will call PHP
        this.plg.Send("OnInternalSensorResult", sensorType, dataJson);
        
        // Also call JS callback if set
        if (self._onSensorResult) {
            try {
                var data = JSON.parse(dataJson);
                self._onSensorResult(sensorType, data);
            } catch (e) {
                self._onSensorResult(sensorType, dataJson);
            }
        }
    }
    
    // =========================================================================
    // Callbacks
    // =========================================================================
    
    /**
     * Set callback for PHP responses
     * @param {function} callback function(response)
     */
    this.SetOnPhpResponse = function(callback) {
        this._onPhpResponse = callback;
        if (callback) {
            this.plg.Send("SetOnPhpResponse", callback.name);
        }
    }
    
    /**
     * Set callback for sensor results
     * @param {function} callback function(sensorType, data)
     */
    this.SetOnSensorResult = function(callback) {
        this._onSensorResult = callback;
        if (callback) {
            this.plg.Send("SetOnSensorResult", callback.name);
        }
    }
    
    /**
     * Set callback when PHP/UI is ready
     * @param {function} callback function()
     */
    this.SetOnReady = function(callback) {
        this._onUiReady = callback;
        if (callback) {
            this.plg.Send("SetOnUiReady", callback.name);
        }
    }
    
    /**
     * Set callback for errors
     * @param {function} callback function(errorMessage)
     */
    this.SetOnError = function(callback) {
        this._onError = callback;
        if (callback) {
            this.plg.Send("SetOnError", callback.name);
        }
    }
}

// =========================================================================
// PHP Action Helper Classes (Maps to ui_core.php)
// These allow building UI in JavaScript that matches PHP's fluent API
// =========================================================================

/**
 * Base UI Component class (mirrors PHP's Component)
 */
function PhpComponent(type) {
    this._type = type;
    this._attrs = {};
}

PhpComponent.prototype.toJSON = function() {
    var result = { type: this._type };
    for (var key in this._attrs) {
        result[key] = this._attrs[key];
    }
    return result;
};

// Dynamic attribute setter (fluent API)
PhpComponent.prototype.attr = function(name, value) {
    this._attrs[name] = value;
    return this;
};

// Common attributes as methods
["id", "text", "textSize", "textColor", "backgroundColor", "alpha", 
 "padding", "gravity", "textAllCaps", "enabled", "visibility",
 "action", "onLongClick", "onCheckedChange"].forEach(function(attr) {
    PhpComponent.prototype[attr] = function(value) {
        this._attrs[attr] = value;
        return this;
    };
});

/**
 * Create a TextView
 * @returns {PhpComponent}
 */
function TextView() {
    return new PhpComponent("TextView");
}

/**
 * Create a Button
 * @returns {PhpComponent}
 */
function Button() {
    return new PhpComponent("Button");
}

/**
 * Create a CheckBox
 * @returns {PhpComponent}
 */
function CheckBox() {
    return new PhpComponent("CheckBox");
}

/**
 * Create an EditText
 * @returns {PhpComponent}
 */
function EditText() {
    return new PhpComponent("EditText");
}

/**
 * Create an ImageView
 * @returns {PhpComponent}
 */
function ImageView() {
    return new PhpComponent("ImageView");
}

/**
 * Create a VerticalLayout with children
 * @param {Array} children Array of PhpComponent instances
 * @returns {object}
 */
function VerticalLayout(children) {
    return {
        type: "VerticalLayout",
        children: children.map(function(c) {
            return c.toJSON ? c.toJSON() : c;
        })
    };
}
