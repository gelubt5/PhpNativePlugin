# PhpNativePlugin for DroidScript

A sophisticated hybrid architecture plugin that enables **PHP 8** to act as the application brain while **Java** renders native Android UI **and handles all native functionality directly**.

## Architecture

```
┌─────────────────┐     ┌─────────────────────────────────────────┐
│     PHP 8       │◄───►│           Java Plugin                   │
│   "Brain"       │     │         "Full Native"                   │
│                 │     │                                         │
│ Business Logic  │     │ Native UI Rendering + All Native APIs:  │
│ Data Processing │     │ • Sensors (75 handlers)                 │
│ UI Decisions    │     │ • Camera, Audio, Media                  │
│                 │     │ • SMS, Phone, Notifications             │
│                 │     │ • WiFi, Bluetooth, Network              │
│                 │     │ • Clipboard, Flashlight, Vibrate        │
│                 │     │ • Files, Zip, Crypto, HTTP              │
└─────────────────┘     └─────────────────────────────────────────┘
                                     │
                                     ▼ (legacy only)
                        ┌─────────────────────────┐
                        │  DroidScript (optional) │
                        │  Custom JS handlers     │
                        └─────────────────────────┘
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

### Sensor Access (Two Methods)

**Method 1: Pure Java via `native()` (Recommended)**
```php
// Uses pure Java handlers - faster, no DroidScript
return native("battery", "onBattery");
return native("http", "onResponse", ["url" => "https://api.example.com"]);
return native("takephoto", "onPhoto");
```

**Method 2: DroidScript via `nativeCall()` (Legacy)**
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
                ->action("requestLocation"),
            (new Button())
                ->text("Get Battery")
                ->action("requestBattery"),
        ]))->padding(40);
    }

    // METHOD 1: Pure Java native call (recommended)
    public function requestBattery($params) {
        return native("battery", "handle_battery");
    }

    // METHOD 2: DroidScript sensor call (legacy)
    public function requestLocation($params) {
        return [
            "action" => "DS_SENSOR_CALL",
            "sensor" => "location",
            "callback" => "handle_gps"
        ];
    }

    // Handle battery result (from Java)
    public function handle_battery($params) {
        $level = $params['level'];
        $charging = $params['charging'] ? "Yes" : "No";
        
        return [
            "action" => "update",
            "target" => "status",
            "attributes" => [
                "text" => "Battery: $level%, Charging: $charging",
                "textColor" => "#4CAF50"
            ]
        ];
    }

    // Handle location result (from DroidScript)
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
4. **Native Request (Java)**: PHP returns `NATIVE_CALL` → Java executes directly in Java → Result sent to PHP callback
5. **Sensor Request (Legacy)**: PHP returns `DS_SENSOR_CALL` → Java injects JS into DroidScript → Result forwarded to PHP
6. **UI Update**: PHP returns update JSON → Java modifies native views

## Available Native Handlers (75 total)

### Pure Java Handlers (via `native()`)

| Category | Handlers |
|----------|----------|
| **Sensors** | `accelerometer`, `gyroscope`, `gravity`, `magneticfield`, `compass`, `light`, `proximity`, `pressure`, `humidity`, `temperature`, `stepcounter` |
| **Location** | `location`, `gps`, `lastlocation`, `locationenabled`, `geocode`, `reversegeocode` |
| **Battery** | `battery`, `powersavemode` |
| **Camera/Media** | `takephoto`, `recordvideo`, `pickimage`, `pickvideo` |
| **Audio** | `playaudio`, `pauseaudio`, `stopaudio`, `recordaudio`, `stoprecording`, `getvolume`, `setvolume`, `setringermode` |
| **Communication** | `sendsms`, `phonecall`, `opendial`, `sendemail` |
| **Network** | `wifi`, `wifiscan`, `bluetooth`, `networkinfo`, `http`, `download` |
| **System** | `clipboard_get`, `clipboard_set`, `vibrate`, `flashlight`, `notification`, `cancelnotification`, `keepscreenon`, `setbrightness` |
| **File System** | `readfile`, `writefile`, `deletefile`, `fileexists`, `listdir`, `mkdir`, `zipfile`, `zipfolder`, `unzip` |
| **Intents** | `openapp`, `openurl`, `opensettings`, `share`, `sendintent` |
| **Crypto** | `hash`, `encrypt`, `decrypt`, `base64encode`, `base64decode`, `randombytes` |

### DroidScript Sensors (Legacy via `nativeCall()`)

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
- ✅ **75 pure Java native handlers** - no JavaScript bridge needed
- ✅ PHP 8 OOP for business logic
- ✅ Full sensor, camera, audio, SMS, network access
- ✅ File operations, zip/unzip, encryption
- ✅ Legacy DroidScript support for custom handlers

## License

Free for personal and educational use.

## Credits

Based on DroidScript plugin architecture with PHP 8 CLI integration.
