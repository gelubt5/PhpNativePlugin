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
    // Native Call Handler Registry
    // All sensor/native DroidScript logic lives here in JS.
    // PHP only receives results via callbacks.
    // Users can register custom handlers with RegisterNativeHandler().
    // =========================================================================
    
    this._nativeHandlers = {};
    
    /**
     * Execute a native call by type. Looks up the handler in the registry
     * and executes it with the given params. Results are sent back to PHP
     * via OnSensorResult().
     * @param {string} type The native call type (e.g., "location", "battery", "http")
     * @param {string} paramsJson JSON string of parameters
     */
    this.NativeCall = function(type, paramsJson) {
        var handler = self._nativeHandlers[type.toLowerCase()];
        if (handler) {
            var params = {};
            try { if (paramsJson) params = JSON.parse(paramsJson); } catch(e) {}
            try {
                handler(params);
            } catch(e) {
                self.OnSensorResult(type, JSON.stringify({error: e.message}));
            }
        } else {
            self.OnSensorResult(type, JSON.stringify({error: "Unknown native call type: " + type}));
        }
    };
    
    /**
     * Register a custom native call handler.
     * Use this to extend the plugin with your own sensor/native integrations.
     * 
     * The handler function receives a params object and should call
     * self.OnSensorResult(type, JSON.stringify(data)) when done.
     * 
     * @param {string} type Unique name for the native call
     * @param {function} handler Function(params) that performs the native operation
     * 
     * @example
     * // Register a custom "myGps" handler
     * php.RegisterNativeHandler("myGps", function(params) {
     *     var loc = app.CreateLocator('GPS');
     *     loc.SetOnChange(function(data) {
     *         _phpPlugin.OnSensorResult('myGps', JSON.stringify({lat: data.latitude}));
     *         loc.Stop();
     *     });
     *     loc.Start();
     * });
     * 
     * // Then from PHP: return nativeCall("myGps", [], "onMyGps");
     */
    this.RegisterNativeHandler = function(type, handler) {
        self._nativeHandlers[type.toLowerCase()] = handler;
    };
    
    /**
     * Called by native handlers when they have a result to send to PHP.
     * Routes the result back to Java which calls the PHP callback.
     * @param {string} type The native call type (matches what was registered)
     * @param {string} dataJson JSON string of result data
     */
    this.OnSensorResult = function(type, dataJson) {
        // Send result to Java which routes to PHP callback
        self.plg.Send("OnSensorResult", type, dataJson);
    };
    
    
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

    /**
     * Get the native View for embedding in DroidScript layouts.
     * Returns the raw Android FrameLayout that can be added to a DroidScript layout.
     * 
     * @example
     * var lay = app.CreateLayout("Linear", "FillXY");
     * php.AddTo(lay);  // Easier method
     * app.AddLayout(lay);
     * php.Start();
     * 
     * @returns {android.view.View} The native FrameLayout view
     */
    this.GetView = function() {
        // CreateObj creates the view synchronously and returns it
        var nativeView = this.plg.CreateObj("RootView");
        this._directView = nativeView;
        return nativeView;
    }
    
    /**
     * Add the PHP view to a DroidScript layout.
     * This is the easiest way to embed the PHP UI.
     * 
     * @param {object} layout A DroidScript layout (from app.CreateLayout)
     * 
     * @example
     * var lay = app.CreateLayout("Linear", "FillXY");
     * php.AddTo(lay);
     * app.AddLayout(lay);
     * php.Start();
     */
    this.AddTo = function(layout) {
        var phpView = this.GetView();
        // Add directly to the Android ViewGroup (bypasses DroidScript's AddChild)
        if (layout && layout.GetView) {
            layout.GetView().addView(phpView);
        }
    }

    // Helper methods for the view (call on php object, not the view)
    this.SetViewSize = function(w, h) {
        if (this._directView && this._directView.setLayoutParams) {
            var params = this._directView.getLayoutParams();
            if (w != null && w != -1) params.width = Math.round(w * app.GetDisplayWidth());
            if (h != null && h != -1) params.height = Math.round(h * app.GetDisplayHeight());
            this._directView.setLayoutParams(params);
        }
    }
    
    this.ShowView = function() {
        if (this._directView) this._directView.setVisibility(0);
    }
    
    this.HideView = function() {
        if (this._directView) this._directView.setVisibility(8);
    }
}
