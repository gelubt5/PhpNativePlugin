/*
    PhpNativePlugin Sample App
    
    This is a sample DroidScript application that demonstrates how to use
    the PhpNativePlugin to create a hybrid PHP/Java/DroidScript application.
    
    Place this file in your DroidScript app folder along with the plugin.
*/

// Load the plugin
app.LoadPlugin("PhpNativePlugin");

var php;

function OnStart() {
    // Create a simple DroidScript layout (behind the PHP overlay)
    var lay = app.CreateLayout("Linear", "VCenter,FillXY");
    lay.SetBackgroundColor("#1a1a2e");
    
    var txt = app.CreateText("PhpNative Demo", 0.8);
    txt.SetTextSize(18);
    txt.SetTextColor("#444466");
    lay.AddChild(txt);
    
    var btnToggle = app.CreateButton("Toggle Overlay", 0.5);
    btnToggle.SetOnTouch(ToggleOverlay);
    lay.AddChild(btnToggle);
    
    app.AddLayout(lay);
    
    // Initialize the PHP plugin
    php = app.CreatePhpNative();
    
    // Set up callbacks
    php.SetOnReady(OnPhpReady);
    php.SetOnSensorResult(OnSensorResult);
    php.SetOnError(OnPhpError);
    
    // OPTION 1: Use default location (plugin's files directory)
    // php.Start();
    
    // OPTION 2: Copy PHP files to your app folder and run from there
    // This lets you edit PHP files directly in /sdcard/DroidScript/YourApp/
    var appName = app.GetAppName();  // Get current app name
    var result = php.SetApp(appName);
    console.log("SetApp result: " + JSON.stringify(result));
    
    if (result.success) {
        console.log("PHP path: " + result.path);
        console.log("Copied files: " + JSON.stringify(result.files));
    }
    
    // Show loading message
    app.ShowProgress("Initializing PHP...");
    
    // Start the PHP application
    php.Start();
}

function OnPhpReady() {
    app.HideProgress();
    app.ShowPopup("PHP Engine Ready!");
}

function OnSensorResult(sensorType, data) {
    console.log("Sensor [" + sensorType + "]: " + JSON.stringify(data));
}

function OnPhpError(message) {
    app.HideProgress();
    app.Alert(message, "PHP Error");
}

function ToggleOverlay() {
    // Toggle between showing and hiding the PHP overlay
    if (php._overlayVisible) {
        php.HideOverlay();
        php._overlayVisible = false;
        app.ShowPopup("Overlay Hidden");
    } else {
        php.ShowOverlay();
        php._overlayVisible = true;
        app.ShowPopup("Overlay Visible");
    }
}

// ============================================================================
// Optional: Direct PHP Calls from JavaScript
// ============================================================================

function CallCustomPhpMethod() {
    // Call any PHP method directly
    var response = php.CallPhp("customMethod", {
        param1: "value1",
        param2: 123
    });
    
    var result = JSON.parse(response);
    console.log("PHP Response:", result);
}

function RequestGpsFromJs() {
    // Request GPS through the plugin - result goes to PHP
    php.RequestLocation("handle_gps");
}

function ScanBarcodeFromJs() {
    // Request barcode scan - result goes to PHP
    php.ScanBarcode("handle_barcode");
}
