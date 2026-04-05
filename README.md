# PhpNativePlugin for DroidScript

A sophisticated hybrid architecture plugin that enables **PHP 8** to act as the application brain while **Java** renders native Android UI and **DroidScript** provides hardware/sensor access.

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│     PHP 8       │◄───►│   Java Plugin   │◄───►│   DroidScript   │
│   "Brain"       │     │   "Executor"    │     │   "Hardware"    │
│                 │     │                 │     │                 │
│ Business Logic  │     │ Native UI       │     │ Sensors (GPS,   │
│ Data Processing │     │ Reflection      │     │  Camera, etc.)  │
│ UI Decisions    │     │ View Overlays   │     │ Permissions     │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

## Files

```
PhpNativePlugin/
├── PhpNativePlugin.java    # Main Java plugin class
├── PhpNativePlugin.inc     # JavaScript API wrapper
├── PhpNativePlugin.html    # Documentation
├── sample_app.js           # Example DroidScript app
├── README.md               # This file
└── assets/
    ├── arm64-v8a/
    │   └── libphp.so       # PHP 8 binary (64-bit)
    ├── armeabi-v7a/        # (add 32-bit binary here)
    ├── app.php             # Your PHP application
    ├── logic.php           # Entry point
    ├── router.php          # Request router
    └── ui_core.php         # UI component classes
```

## Building the Plugin

### Prerequisites
- Java JDK 8+
- Android SDK (for android.jar)

### Compile Java
```bash
# Set paths
ANDROID_JAR=/path/to/android-sdk/platforms/android-30/android.jar

# Compile
javac -source 1.8 -target 1.8 \
  -cp $ANDROID_JAR \
  -d out/ \
  PhpNativePlugin.java

# Create JAR
cd out
jar cvf ../PhpNativePlugin.jar com/
cd ..
```

### Create PPK Package
```bash
# Package into .ppk (ZIP format)
zip -r PhpNativePlugin.ppk \
  PhpNativePlugin.java \
  PhpNativePlugin.jar \
  PhpNativePlugin.inc \
  PhpNativePlugin.html \
  assets/
```

### Install in DroidScript
1. Copy `PhpNativePlugin.ppk` to device
2. Open DroidScript IDE
3. Go to Plugins → Install Plugin
4. Select the .ppk file

## Usage

### Basic Usage
```javascript
app.LoadPlugin("PhpNativePlugin");

var php;

function OnStart() {
    php = app.CreatePhpNative();
    php.SetOnReady(OnReady);
    php.SetOnError(OnError);
    php.Start();  // Calls PHP index() method
}

function OnReady() {
    app.ShowPopup("PHP Ready!");
}

function OnError(msg) {
    app.Alert(msg, "Error");
}
```

### Sensor Access (The "Puppeteer" Mechanism)
```javascript
// Request GPS - result goes to PHP handle_gps()
php.RequestLocation("handle_gps");

// Scan barcode - result goes to PHP handle_barcode()
php.ScanBarcode("handle_barcode");

// Get battery - result goes to PHP handle_battery()
php.RequestBattery("handle_battery");
```

### PHP Side (app.php)
```php
class MyApp {
    public function index() {
        return (new VerticalLayout([
            (new TextView())->text("Hello from PHP!"),
            (new Button())
                ->text("Get Location")
                ->action("requestLocation")
        ]))->padding(40);
    }

    // Request sensor through DroidScript
    public function requestLocation($params) {
        return [
            "action" => "DS_SENSOR_CALL",
            "sensor" => "location",
            "callback" => "handle_gps"
        ];
    }

    // Handle sensor result
    public function handle_gps($params) {
        $lat = $params['lat'];
        $lng = $params['lng'];
        
        return [
            "action" => "update",
            "target" => "status",
            "attributes" => [
                "text" => "Location: $lat, $lng",
                "textColor" => "#4CAF50"
            ]
        ];
    }
}
```

## Workflow

1. **Initialization**: DroidScript loads plugin → Java extracts PHP binary
2. **First Render**: Java calls PHP `index()` → PHP returns UI JSON → Java renders native Views
3. **User Interaction**: Button click → Java calls PHP method → PHP returns action
4. **Sensor Request**: PHP returns `DS_SENSOR_CALL` → Java injects JS into DroidScript
5. **Sensor Callback**: DroidScript gets data → Java forwards to PHP → PHP processes
6. **UI Update**: PHP returns update JSON → Java modifies native views

## Available Sensors

| Method | Sensor | PHP Data |
|--------|--------|----------|
| `RequestLocation(cb)` | GPS | `{lat, lng, speed, altitude}` |
| `RequestBattery(cb)` | Battery | `{level, charging}` |
| `RequestAccelerometer(cb)` | Motion | `{x, y, z}` |
| `RequestCompass(cb)` | Orientation | `{azimuth, pitch, roll}` |
| `ScanBarcode(cb)` | Camera | `{code}` |
| `TakePhoto(cb)` | Camera | `{file}` |
| `RequestWifi(cb)` | Network | `{ssid, ip, connected}` |
| `RequestBluetooth(cb)` | Bluetooth | `{enabled, paired}` |
| `SpeechRecognition(cb)` | Microphone | `{text}` |

## Key Advantages

- ✅ Edit PHP to add features - no APK recompilation
- ✅ Native Android UI performance (not WebView)
- ✅ Full access to DroidScript sensor APIs
- ✅ PHP 8 OOP for business logic
- ✅ Automatic JavaScript injection for hardware

## License

Free for personal and educational use.

## Credits

Based on DroidScript plugin architecture with PHP 8 CLI integration.
