/*
    PhpNativePlugin - DroidScript Plugin for Hybrid PHP/Java/DroidScript Architecture
    
    This plugin implements a sophisticated hybrid architecture where:
    - PHP 8 acts as the "Brain" (business logic, decisions, data processing)
    - Java Plugin acts as the "Native Executor" (UI rendering, process bridge, Reflection)
    - DroidScript acts as the "Hardware Interface" (sensors, camera, GPS, permissions)

    WORKFLOW:
    1. DroidScript loads the plugin and calls PHP for initial UI
    2. Java renders native Android UI from PHP JSON responses
    3. PHP can request sensor data via special "DS_SENSOR_CALL" actions
    4. Java injects JavaScript into DroidScript to access hardware
    5. Sensor callbacks return to Java, which forwards data to PHP
    6. PHP processes data and returns UI updates

    Package naming: Free plugins must end with '.user'
*/

package com.phpnative.plugins.user;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.NumberPicker;
import android.widget.AutoCompleteTextView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.SearchView;
import android.widget.CalendarView;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.VideoView;
import android.widget.Space;
import android.widget.Chronometer;
import android.widget.TextClock;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.GridLayout;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.RelativeLayout;
import android.widget.Toolbar;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.net.Uri;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

// Native functionality imports
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.Geocoder;
import android.location.Address;
import android.os.BatteryManager;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.os.Build;
import android.os.Environment;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.media.AudioAttributes;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.SpeechRecognizer;
import android.speech.RecognizerIntent;
import android.speech.RecognitionListener;
import android.telephony.SmsManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.ScanResult;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.app.DownloadManager;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraAccessException;
import android.provider.MediaStore;
import android.provider.Settings;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.Notification;
import android.app.PendingIntent;
import android.util.DisplayMetrics;
import android.content.pm.PackageManager;
import android.Manifest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import android.util.Base64;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PhpNativePlugin {
    public static final String TAG = "PhpNativePlugin";
    public static final float VERSION = 1.0f;
    public static final String VERSION_STRING = "1.0.0";
    
    // Subprocess timeout in seconds
    private static final long PHP_TIMEOUT_SECONDS = 30;
    
    // PHP Worker server configuration
    // Port 0 means OS will assign an available port (avoids collisions with multiple plugin instances)
    private static final int PHP_WORKER_CONNECT_TIMEOUT_MS = 5000;
    private static final int PHP_WORKER_READ_TIMEOUT_MS = 30000;
    private static final int PHP_WORKER_STARTUP_TIMEOUT_MS = 10000;
    
    // Activity result request codes
    private static final int REQUEST_TAKE_PHOTO = 1001;
    private static final int REQUEST_RECORD_VIDEO = 1002;
    private static final int REQUEST_PICK_IMAGE = 1003;
    private static final int REQUEST_PICK_VIDEO = 1004;
    private static final int REQUEST_PICK_AUDIO = 1005;
    private static final int REQUEST_PICK_FILE = 1006;
    
    // Permission request base code
    private static final int PERMISSION_REQUEST_CODE = 2000;

    // Properties to skip during reflection-based property extraction
    // Skips internal Android properties that are not useful for PHP, could have side effects,
    // or expose sensitive data
    private static final Set<String> SKIP_PROPERTIES = new HashSet<>(Arrays.asList(
        // Security-sensitive
        "password", "hint",
        // May allocate resources or have side effects
        "drawingCache", "drawingCacheBackgroundColor", "drawingCacheQuality",
        "rootWindowInsets", "windowSystemUiVisibility", "systemUiVisibility",
        // Internal/rarely useful
        "applicationWindowToken", "windowToken", "windowId",
        "accessibilityDelegate", "accessibilityNodeProvider",
        "autofillHints", "autofillId", "autofillType", "autofillValue",
        "clipBounds", "clipToOutline", "contentDescription",
        "filterTouchesWhenObscured", "fitsSystemWindows",
        "foreground", "foregroundGravity", "foregroundTintList", "foregroundTintMode",
        "handler", "hasExplicitFocusable", "hasFocus", "hasNestedScrollingParent",
        "hasOnClickListeners", "hasOnLongClickListeners", "hasOverlappingRendering",
        "hasPointerCapture", "hasTransientState", "hasWindowFocus",
        "importantForAccessibility", "importantForAutofill",
        "keepScreenOn", "keyDispatcherState", "labelFor",
        "layerType", "layoutDirection", "layoutParams",
        "locationInWindow", "locationOnScreen", "matrix",
        "measuredHeight", "measuredHeightAndState", "measuredState", "measuredWidth", "measuredWidthAndState",
        "nextClusterForwardId", "nextFocusDownId", "nextFocusForwardId",
        "nextFocusLeftId", "nextFocusRightId", "nextFocusUpId",
        "outlineProvider", "overlay", "overScrollMode",
        "paddingBottom", "paddingEnd", "paddingLeft", "paddingRight", "paddingStart", "paddingTop",
        "parent", "parentForAccessibility", "pivotX", "pivotY",
        "pointerIcon", "rawLayoutDirection", "resources",
        "revealOnFocusHint", "rootSurfaceControl", "rootView",
        "rotationX", "rotationY", "rotation",
        "saveEnabled", "saveFromParentEnabled", "scaleX", "scaleY",
        "scrollBarFadeDuration", "scrollBarSize", "scrollBarStyle",
        "scrollIndicators", "scrollX", "scrollY",
        "solidColor", "sourceLayoutResId", "stateListAnimator",
        "tag", "textAlignment", "textDirection",
        "tooltipText", "touchDelegate", "touchables",
        "transitionAlpha", "transitionName",
        "translationX", "translationY", "translationZ",
        "uniqueDrawingId", "verticalFadingEdgeLength", "verticalScrollbarPosition",
        "verticalScrollbarThumbDrawable", "verticalScrollbarTrackDrawable", "verticalScrollbarWidth",
        "viewTreeObserver", "windowAttachCount", "windowVisibility",
        "x", "y", "z"
    ));

    // Reflection handles to DroidScript host
    private Method m_callscript;
    private Method m_execscript;
    private Method m_getObject;

    private Object m_parent;
    private Context m_ctx;
    private Activity m_activity;
    private String m_plugDir;
    private String m_filesDir;
    private String m_appDir;        // DroidScript app folder (where PHP files run from)
    private String m_appName;       // Current app name
    private boolean m_phpReady = false;
    private String m_phpPath;
    private String m_entryFile; // Default PHP entry file
    
    // PHP Worker (long-running server for performance)
    private Process m_phpWorkerProcess;
    private String m_phpWorkerSecret;       // HMAC shared secret for authentication
    private File m_phpWorkerSecretFile;     // Exact path where secret was written (for cleanup)
    private int m_phpWorkerPort = 0;        // Dynamically assigned port (0 until worker reports READY)
    private boolean m_phpWorkerReady = false;
    private final AtomicInteger m_phpRequestId = new AtomicInteger(0);
    private final Object m_phpWorkerLock = new Object(); // Synchronization for worker state
    
    // Maximum response size from PHP worker (16 MiB, matching PHP side)
    private static final int MAX_RESPONSE_SIZE = 16 * 1024 * 1024;

    // UI Management
    private ViewGroup m_mainRootView;            // Main root view for direct rendering
    // Using ConcurrentHashMap for thread-safety across UI, executor, and sensor threads
    private Map<String, View> m_viewRegistry = new ConcurrentHashMap<>();
    private Map<String, android.widget.ArrayAdapter<String>> m_listAdapters = new ConcurrentHashMap<>();
    private Map<String, List<String>> m_listData = new ConcurrentHashMap<>();
    private Map<String, DrawerState> m_drawerStates = new ConcurrentHashMap<>();
    private Map<String, BottomNavState> m_bottomNavStates = new ConcurrentHashMap<>();
    private Handler m_mainHandler;
    private ExecutorService m_executor;

    // Callback function names
    private String m_OnPhpResponse;
    private String m_OnSensorResult;
    private String m_OnUiReady;
    private String m_OnError;

    // Pending sensor callbacks (sensor type -> PHP method to call with result)
    // Using ConcurrentHashMap for thread-safety as callbacks are added/removed from multiple threads
    private Map<String, String> m_pendingSensorCallbacks = new ConcurrentHashMap<>();

    // Navigation history stack: each entry is [method, dataJson]
    private ArrayList<String[]> m_screenHistory = new ArrayList<>();
    private String m_OnBack;

    // -------------------------------------------------------------------------
    // Native Call Handler System
    // -------------------------------------------------------------------------
    
    /**
     * Functional interface for native call handlers.
     * Handlers receive params JSON and callback name, execute native Android code,
     * and report results via reportNativeResult().
     */
    @FunctionalInterface
    private interface NativeHandler {
        void handle(JSONObject params, String callback);
    }
    
    // Native handler registry (thread-safe for concurrent access)
    private Map<String, NativeHandler> m_nativeHandlers = new ConcurrentHashMap<>();
    
    // Native functionality state
    private SensorManager m_sensorManager;
    private LocationManager m_locationManager;
    private TextToSpeech m_tts;
    private SpeechRecognizer m_speechRecognizer;
    private MediaPlayer m_mediaPlayer;
    private MediaRecorder m_mediaRecorder;
    private CameraManager m_cameraManager;
    private boolean m_ttsReady = false;
    
    // Permission request tracking
    private Map<Integer, Runnable> m_permissionCallbacks = new ConcurrentHashMap<>();
    private int m_permissionRequestId = 0;
    
    // Debug log file
    private String m_debugLogFile;
    private java.io.BufferedWriter m_debugLogWriter; // Cached writer for performance
    private final Object m_debugLogLock = new Object(); // Synchronization for debug logging
    
    // Pending photo target ImageView ID (for takephoto handler)
    private String m_pendingPhotoImageViewId;
    // Content URI for camera capture (MediaStore entry, copied to app folder then deleted)
    private Uri m_pendingPhotoUri;
    // Local file path where photo will be saved in app folder
    private String m_pendingPhotoPath;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public PhpNativePlugin() {
        Log.d(TAG, "Creating PhpNativePlugin");
    }
    
    /**
     * Logs a debug message to both logcat and a local file in app directory.
     * Uses a cached BufferedWriter for better performance under high-frequency logging.
     */
    private void debugLog(String message) {
        Log.d(TAG, message);
        if (m_debugLogFile != null) {
            synchronized (m_debugLogLock) {
                try {
                    // Lazily create the writer
                    if (m_debugLogWriter == null) {
                        m_debugLogWriter = new java.io.BufferedWriter(
                            new java.io.FileWriter(m_debugLogFile, true), 8192);
                    }
                    m_debugLogWriter.write(
                        new java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US)
                            .format(new java.util.Date()) + " " + message);
                    m_debugLogWriter.newLine();
                    m_debugLogWriter.flush();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to write debug log", e);
                    // Reset writer on error so it can be recreated
                    closeDebugLogWriter();
                }
            }
        }
    }
    
    /**
     * Close the debug log writer. Called during Release().
     */
    private void closeDebugLogWriter() {
        synchronized (m_debugLogLock) {
            if (m_debugLogWriter != null) {
                try {
                    m_debugLogWriter.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing debug log writer", e);
                }
                m_debugLogWriter = null;
            }
        }
    }

    public void Init(Context ctx, Object parent) throws Exception {
        Log.d(TAG, "Initializing PhpNativePlugin");

        m_ctx = ctx;
        m_parent = parent;
        m_activity = (Activity) ctx;
        m_mainHandler = new Handler(Looper.getMainLooper());
        m_executor = Executors.newSingleThreadExecutor();

        // Reflection methods to communicate with DroidScript
        m_callscript = parent.getClass().getMethod("CallScript", Bundle.class);
        m_execscript = parent.getClass().getMethod("ExecScript", String.class);
        m_getObject = parent.getClass().getMethod("GetObject", String.class);

        // Plugin directory for assets
        m_plugDir = m_ctx.getDir("Plugins", 0).getAbsolutePath() + "/phpnativeplugin";
        m_filesDir = m_ctx.getExternalFilesDir(null).getPath();

        // Initialize native systems
        initNativeSystems();
        
        // Register all native handlers
        registerAllNativeHandlers();

        // Initialize PHP environment
        initPhpEnvironment();
    }

    public void Release() {
        if (m_executor != null) {
            m_executor.shutdown();
        }
        // Clean up the root view
        if (m_mainRootView != null) {
            m_viewRegistry.clear();
            m_listAdapters.clear();
            m_listData.clear();
        }
        // Release native resources
        if (m_tts != null) {
            m_tts.stop();
            m_tts.shutdown();
            m_tts = null;
        }
        if (m_speechRecognizer != null) {
            m_speechRecognizer.destroy();
            m_speechRecognizer = null;
        }
        if (m_mediaPlayer != null) {
            m_mediaPlayer.release();
            m_mediaPlayer = null;
        }
        if (m_mediaRecorder != null) {
            try { m_mediaRecorder.release(); } catch (Exception e) {}
            m_mediaRecorder = null;
        }
        // Stop PHP worker server
        stopPhpWorker();
        // Close debug log writer
        closeDebugLogWriter();
    }

    // -------------------------------------------------------------------------
    // Activity Events
    // -------------------------------------------------------------------------

    public void OnResume() { }
    public void OnPause() { }
    public void OnConfig() { }
    public void OnNewIntent(Intent intent) { }
    
    /**
     * Handle permission request results.
     * Called when user grants/denies permissions from requestPermission().
     */
    public void OnPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // Check if this is one of our permission requests
        if (requestCode >= PERMISSION_REQUEST_CODE) {
            int requestId = requestCode - PERMISSION_REQUEST_CODE;
            Runnable callback = m_permissionCallbacks.remove(requestId);
            
            if (callback != null) {
                // Check if all permissions were granted
                boolean allGranted = true;
                if (grantResults != null) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            allGranted = false;
                            break;
                        }
                    }
                } else {
                    allGranted = false;
                }
                
                if (allGranted) {
                    callback.run();
                } else {
                    Log.w(TAG, "Permission denied for request " + requestId);
                }
            }
        }
    }
    
    public void OnActivityResult(int requestCode, int resultCode, Intent data) {
        // Handle file picker results
        String resultType = null;
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO: resultType = "takephoto_result"; break;
            case REQUEST_RECORD_VIDEO: resultType = "recordvideo_result"; break;
            case REQUEST_PICK_IMAGE: resultType = "pickimage_result"; break;
            case REQUEST_PICK_VIDEO: resultType = "pickvideo_result"; break;
            case REQUEST_PICK_AUDIO: resultType = "pickaudio_result"; break;
            case REQUEST_PICK_FILE: resultType = "pickfile_result"; break;
        }
        
        if (resultType != null) {
            JSONObject result = new JSONObject();
            try {
                if (resultCode == Activity.RESULT_OK) {
                    // Special handling for takephoto (REQUEST_TAKE_PHOTO):
                    // Copy raw bytes from MediaStore URI -> app folder file, delete MediaStore entry,
                    // then decode bitmap from local file.
                    if (requestCode == REQUEST_TAKE_PHOTO){
                        if (m_pendingPhotoUri != null && m_pendingPhotoPath != null) {
                        Uri photoUri = m_pendingPhotoUri;
                        String photoPath = m_pendingPhotoPath;
                        m_pendingPhotoUri = null;
                        m_pendingPhotoPath = null;
                        try {
                            Log.d(TAG, "takephoto result: copying from URI=" + photoUri + " to " + photoPath);
                            // Copy raw bytes from MediaStore to local file in app folder
                            InputStream is = m_ctx.getContentResolver().openInputStream(photoUri);
                            File localFile = new File(photoPath);
                            FileOutputStream fos = new FileOutputStream(localFile);
                            byte[] buf = new byte[8192];
                            int len;
                            long total = 0;
                            while ((len = is.read(buf)) > 0) {
                                fos.write(buf, 0, len);
                                total += len;
                            }
                            fos.close();
                            is.close();
                            Log.d(TAG, "takephoto result: copied " + total + " bytes to " + photoPath);
                            // Delete MediaStore entry - photo now lives in app folder
                            m_ctx.getContentResolver().delete(photoUri, null, null);
                            // Decode full-res bitmap from local file
                            Bitmap photo = BitmapFactory.decodeFile(photoPath);
                            Log.d(TAG, "takephoto result: decoded bitmap = " +
                                (photo != null ? photo.getWidth() + "x" + photo.getHeight() : "null"));
                            if (photo != null) {
                                // Display in ImageView if target was specified
                                if (m_pendingPhotoImageViewId != null) {
                                    final String viewId = m_pendingPhotoImageViewId;
                                    final Bitmap bmp = photo;
                                    m_pendingPhotoImageViewId = null;
                                    m_mainHandler.post(() -> {
                                        View v = m_viewRegistry.get(viewId);
                                        if (v instanceof ImageView) {
                                            ((ImageView) v).setImageBitmap(bmp);
                                            Log.d(TAG, "Photo displayed in ImageView: " + viewId
                                                + " (" + bmp.getWidth() + "x" + bmp.getHeight() + ")");
                                        } else {
                                            Log.w(TAG, "ImageView not found for id: " + viewId);
                                        }
                                    });
                                }
                                result.put("success", true);
                                result.put("path", photoPath);
                                result.put("width", photo.getWidth());
                                result.put("height", photo.getHeight());
                                result.put("size", localFile.length());
                            } else {
                                result.put("success", false);
                                result.put("error", "Failed to decode photo from " + photoPath + " (" + total + " bytes copied)");
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "takephoto result error", ex);
                            try { m_ctx.getContentResolver().delete(photoUri, null, null); } catch (Exception ignored) {}
                            result.put("success", false);
                            result.put("error", "Failed to read photo: " + ex.getMessage());
                        }
                      } else {
                        result.put("success", false);
                        result.put("error", "No pending photo URI/path");
                      }
                    }
                    // All other cases: get URI from data
                    else if (data != null) {
                        Uri uri = data.getData();
                        if (uri != null) {
                            result.put("uri", uri.toString());
                            String path = getPathFromUri(uri);
                            if (path != null) {
                                result.put("path", path);
                            }
                            String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE};
                            try (android.database.Cursor cursor = m_ctx.getContentResolver().query(uri, projection, null, null, null)) {
                                if (cursor != null && cursor.moveToFirst()) {
                                    int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                                    int sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
                                    if (nameIndex >= 0) result.put("name", cursor.getString(nameIndex));
                                    if (sizeIndex >= 0) result.put("size", cursor.getLong(sizeIndex));
                                }
                            }
                            result.put("success", true);
                        } else {
                            result.put("success", false);
                            result.put("error", "No URI returned");
                        }
                    } else {
                        result.put("success", false);
                        result.put("error", "No data returned");
                    }
                } else {
                    // Clean up pending MediaStore entry on cancel
                    if (requestCode == 1001 && m_pendingPhotoUri != null) {
                        try { m_ctx.getContentResolver().delete(m_pendingPhotoUri, null, null); } catch (Exception ignored) {}
                        m_pendingPhotoUri = null;
                        m_pendingPhotoPath = null;
                    }
                    result.put("success", false);
                    result.put("cancelled", true);
                }
            } catch (Exception e) {
                try {
                    result.put("success", false);
                    result.put("error", e.getMessage());
                } catch (Exception ignored) {}
            }
            
            // Get the stored callback and report result
            String nativeType = resultType.replace("_result", "");
            reportNativeResult(nativeType, result);
        }
    }
    
    /**
     * Try to get a file path from a content URI.
     * For Android 10+ scoped storage, MediaStore.MediaColumns.DATA may not work.
     * Falls back to copying content to app cache directory.
     */
    private String getPathFromUri(Uri uri) {
        if (uri == null) return null;
        
        // If it's already a file URI, just return the path
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        
        // Try to query for DATA column (works on Android 9 and below, deprecated on 10+)
        // On Android 10+ with scoped storage, this often returns null or inaccessible paths
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            String[] projection = {MediaStore.MediaColumns.DATA};
            try (android.database.Cursor cursor = m_ctx.getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                    if (columnIndex >= 0) {
                        String path = cursor.getString(columnIndex);
                        if (path != null && new File(path).exists()) {
                            return path;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        
        // For Android 10+ or when DATA column fails: copy content to cache directory
        // This is the recommended approach for scoped storage
        return copyUriToCache(uri);
    }
    
    /**
     * Copy content from a URI to the app's cache directory.
     * Returns the path to the cached file, or null on failure.
     */
    private String copyUriToCache(Uri uri) {
        if (uri == null) return null;
        
        try {
            // Get display name to preserve extension
            String fileName = "picked_file";
            try (android.database.Cursor cursor = m_ctx.getContentResolver().query(
                    uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        String name = cursor.getString(nameIndex);
                        if (name != null && !name.isEmpty()) {
                            fileName = name;
                        }
                    }
                }
            } catch (Exception ignored) {}
            
            // Create unique filename in cache dir
            File cacheDir = new File(m_ctx.getCacheDir(), "picked_files");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            
            // Add timestamp to avoid collisions
            String uniqueName = System.currentTimeMillis() + "_" + fileName;
            File destFile = new File(cacheDir, uniqueName);
            
            // Copy content
            try (InputStream in = m_ctx.getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(destFile)) {
                if (in == null) return null;
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            
            Log.d(TAG, "Copied URI content to cache: " + destFile.getAbsolutePath());
            return destFile.getAbsolutePath();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy URI to cache: " + e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // PHP Environment Setup
    // -------------------------------------------------------------------------

    private void initPhpEnvironment() {
        m_executor.execute(() -> {
            try {
                // Extract PHP binary from plugin assets
                extractPhpBinary();
                
                // Start the PHP worker server for better performance
                startPhpWorker();
           
                m_phpReady = true;
                Log.d(TAG, "PHP environment ready (worker=" + m_phpWorkerReady + ")");

                // Notify DroidScript that PHP is ready
                if (m_OnUiReady != null) {
                    m_mainHandler.post(() -> {
                        try {
                            Bundle b = new Bundle();
                            b.putString("cmd", m_OnUiReady);
                            CallScript(b);
                        } catch (Exception e) {
                            Log.e(TAG, "Error notifying UiReady", e);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize PHP environment", e);
                notifyError("PHP initialization failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Start the long-running PHP worker server.
     * This provides much better performance than spawning a new process per call.
     */
    private void startPhpWorker() {
        if (m_phpPath == null) {
            Log.w(TAG, "PHP binary not available, worker cannot start");
            return;
        }
        
        String scriptDir = getPhpScriptDir();
        File serverScript = new File(scriptDir, "server.php");
        if (!serverScript.exists()) {
            Log.w(TAG, "server.php not found, falling back to per-call subprocess");
            return;
        }
        
        try {
            // Generate and write the HMAC secret to app-private storage
            // (not world-readable like external storage on older Android)
            m_phpWorkerSecret = generateSecret();
            File privateDir = m_ctx.getFilesDir();
            m_phpWorkerSecretFile = new File(privateDir, "phpnative.key");
            try (FileOutputStream fos = new FileOutputStream(m_phpWorkerSecretFile)) {
                fos.write(m_phpWorkerSecret.getBytes(StandardCharsets.UTF_8));
            }
            Log.d(TAG, "Wrote HMAC secret to: " + m_phpWorkerSecretFile.getAbsolutePath());
            
            // Start the PHP worker process
            ProcessBuilder pb = new ProcessBuilder(
                m_phpPath,
                serverScript.getAbsolutePath()
            );
            // Port 0 tells PHP to let OS assign an available port
            pb.environment().put("PHPNATIVE_PORT", "0");
            // Pass secret path via env var so PHP reads from app-private location
            pb.environment().put("PHPNATIVE_SECRET_PATH", m_phpWorkerSecretFile.getAbsolutePath());
            pb.directory(new File(scriptDir));
            pb.redirectErrorStream(false); // Keep stderr separate for error logging
            
            Log.d(TAG, "Starting PHP worker: " + m_phpPath + " " + serverScript.getAbsolutePath());
            
            synchronized (m_phpWorkerLock) {
                m_phpWorkerProcess = pb.start();
            }
            
            // Start a thread to log stderr
            startWorkerErrorLogger();
            
            // Wait for the "READY" signal on stdout (includes assigned port)
            int assignedPort = waitForWorkerReady();
            if (assignedPort > 0) {
                m_phpWorkerPort = assignedPort;
                m_phpWorkerReady = true;
                Log.d(TAG, "PHP worker started successfully on port " + m_phpWorkerPort);
            } else {
                Log.e(TAG, "PHP worker failed to start within timeout");
                stopPhpWorker();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start PHP worker", e);
            m_phpWorkerReady = false;
        }
    }
    
    /**
     * Generate a random secret for HMAC authentication.
     */
    private String generateSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    /**
     * Start a thread to log PHP worker stderr output.
     */
    private void startWorkerErrorLogger() {
        Process process;
        synchronized (m_phpWorkerLock) {
            process = m_phpWorkerProcess;
        }
        if (process == null) return;
        
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Log.w(TAG, "[PHP worker stderr] " + line);
                }
            } catch (Exception e) {
                Log.d(TAG, "PHP worker stderr reader ended: " + e.getMessage());
            }
        }, "PhpWorkerErrorLogger").start();
    }
    
    /**
     * Wait for the PHP worker to output "READY port=XXXX pid=XXX" on stdout.
     * @return the assigned port number, or -1 if timeout/error
     */
    private int waitForWorkerReady() {
        Process process;
        synchronized (m_phpWorkerLock) {
            process = m_phpWorkerProcess;
        }
        if (process == null) return -1;
        
        long startTime = System.currentTimeMillis();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            // Set a read timeout by checking process liveness
            while (System.currentTimeMillis() - startTime < PHP_WORKER_STARTUP_TIMEOUT_MS) {
                if (!process.isAlive()) {
                    Log.e(TAG, "PHP worker process died during startup, exit=" + process.exitValue());
                    return -1;
                }
                // Try to read a line (we need the ready signal)
                // Use available() to avoid blocking forever
                if (reader.ready()) {
                    String line = reader.readLine();
                    Log.d(TAG, "[PHP worker stdout] " + line);
                    if (line != null && line.startsWith("READY")) {
                        // Parse port from "READY port=XXXX pid=YYYY"
                        java.util.regex.Matcher m = java.util.regex.Pattern
                            .compile("port=(\\d+)")
                            .matcher(line);
                        if (m.find()) {
                            return Integer.parseInt(m.group(1));
                        }
                        Log.w(TAG, "READY line missing port: " + line);
                        return -1;
                    }
                } else {
                    Thread.sleep(50);
                }
            }
            Log.e(TAG, "Timeout waiting for PHP worker READY signal");
        } catch (Exception e) {
            Log.e(TAG, "Error waiting for PHP worker ready", e);
        }
        return -1;
    }
    
    /**
     * Stop the PHP worker server gracefully.
     */
    private void stopPhpWorker() {
        synchronized (m_phpWorkerLock) {
            m_phpWorkerReady = false;
            if (m_phpWorkerProcess != null) {
                // Try graceful shutdown via __shutdown__ command (before resetting port)
                if (m_phpWorkerPort > 0) {
                    try {
                        sendWorkerCommand("__shutdown__", null);
                    } catch (Exception e) {
                        Log.d(TAG, "Graceful shutdown failed, forcing: " + e.getMessage());
                    }
                }
                m_phpWorkerPort = 0;
                
                // Give it a moment to exit gracefully
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {}
                
                // Force kill if still alive
                if (m_phpWorkerProcess.isAlive()) {
                    m_phpWorkerProcess.destroyForcibly();
                    Log.d(TAG, "PHP worker force-killed");
                }
                m_phpWorkerProcess = null;
            } else {
                m_phpWorkerPort = 0;
            }
        }
        
        // Clean up the secret file using the exact path that was written
        if (m_phpWorkerSecretFile != null && m_phpWorkerSecretFile.exists()) {
            if (m_phpWorkerSecretFile.delete()) {
                Log.d(TAG, "Deleted secret file: " + m_phpWorkerSecretFile.getAbsolutePath());
            }
            m_phpWorkerSecretFile = null;
        }
    }
    
    /**
     * Restart the PHP worker (e.g., after hot-reload during development).
     */
    private void restartPhpWorker() {
        stopPhpWorker();
        startPhpWorker();
    }

    /**
     * Get status information about the PHP worker.
     */
    private String getWorkerStatus() {
        JSONObject status = new JSONObject();
        try {
            status.put("ready", m_phpWorkerReady);
            status.put("port", m_phpWorkerPort);
            status.put("secretPresent", m_phpWorkerSecret != null && !m_phpWorkerSecret.isEmpty());
            status.put("secretPath", m_phpWorkerSecretFile != null ? m_phpWorkerSecretFile.getAbsolutePath() : null);
            status.put("processAlive", m_phpWorkerProcess != null && m_phpWorkerProcess.isAlive());
            status.put("requestCount", m_phpRequestId.get());
        } catch (JSONException e) {
            return "{\"error\": \"Failed to build status\"}";
        }
        return status.toString();
    }

    private void extractPhpBinary() throws IOException {
        // Use Android's native library directory - PHP binary extracted from jniLibs
        m_phpPath = m_ctx.getApplicationInfo().nativeLibraryDir + "/libphp2.so";
        
        File phpFile = new File(m_phpPath);
        if (phpFile.exists()) {
            Log.d(TAG, "Found PHP binary at: " + m_phpPath);
            Log.d(TAG, "PHP binary size: " + phpFile.length() + " bytes");
            Log.d(TAG, "PHP binary executable: " + phpFile.canExecute());
        } else {
            Log.w(TAG, "PHP binary not found at: " + m_phpPath);
            m_phpPath = null;
        }
    }















    private void copyFile(File src, File dest) throws IOException {
        try (InputStream is = new java.io.FileInputStream(src);
             OutputStream os = new FileOutputStream(dest)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
        }
    }

    // -------------------------------------------------------------------------
    // App Folder Management
    // -------------------------------------------------------------------------

    /**
     * Get the DroidScript app folder path dynamically.
     * IDE mode: /storage/emulated/0/Android/data/<packagename>/files/DroidScript/<appName>
     * APK mode: /storage/emulated/0/Android/data/<packagename>/assets
     */
    private String getDroidScriptAppPath(String appName) {
        String packageName = m_ctx.getPackageName();
        String basePath = "/storage/emulated/0/Android/data/" + packageName;
        
        // Try IDE mode first: files/DroidScript/<appName>
        String idePath = basePath + "/files/DroidScript/" + appName;
        File ideFolder = new File(idePath);
        if (ideFolder.exists() && ideFolder.isDirectory()) {
            Log.d(TAG, "Found IDE mode path: " + idePath);
            return idePath;
        }
        
        // Try APK mode: assets
        String apkPath = basePath + "/assets";
        File apkFolder = new File(apkPath);
        if (apkFolder.exists() && apkFolder.isDirectory()) {
            Log.d(TAG, "Found APK mode path: " + apkPath);
            return apkPath;
        }
        
        // Try creating IDE mode folder
        if (ideFolder.mkdirs()) {
            Log.d(TAG, "Created IDE mode path: " + idePath);
            return idePath;
        }
        
        // Fallback: try legacy /sdcard/DroidScript path
        String legacyPath = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() 
                          + "/DroidScript/" + appName;
        File legacyFolder = new File(legacyPath);
        if (legacyFolder.exists() || legacyFolder.mkdirs()) {
            Log.d(TAG, "Using legacy path: " + legacyPath);
            return legacyPath;
        }
        
        return null;
    }

    /**
     * Set the DroidScript app to use and copy PHP files to its folder.
     * Automatically detects IDE vs APK mode.
     * @param appName Name of the DroidScript app
     * @return JSON with status and path
     */
    private String setApp(String appName) {
        try {
            if (appName == null || appName.isEmpty()) {
                return "{\"error\": \"App name is required\"}";
            }

            m_appName = appName;
            String packageName = m_ctx.getPackageName();
            
            // Get the correct path for this app
            m_appDir = getDroidScriptAppPath(appName);
            
            if (m_appDir == null) {
                return "{\"error\": \"Could not determine app folder\", \"package\": \"" + packageName + "\"}";
            }
            
            File appFolder = new File(m_appDir);
            if (!appFolder.exists()) {
                if (!appFolder.mkdirs()) {
                    return "{\"error\": \"Could not create app folder\", \"path\": \"" + m_appDir + "\"}";
                }
            }

            Log.d(TAG, "App set to: " + m_appName + " at " + m_appDir);
            
            // Initialize debug log file
            m_debugLogFile = m_appDir + "/debug.log";
            try {
                // Clear previous log
                new java.io.FileWriter(m_debugLogFile, false).close();
                debugLog("=== Debug log started for " + m_appName + " ===");
            } catch (Exception e) {
                Log.e(TAG, "Failed to init debug log", e);
            }
            
            return "{\"success\": true, \"app\": \"" + m_appName + "\", \"path\": \"" + m_appDir + "\", \"package\": \"" + packageName + "\"}";
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting app: " + appName, e);
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    /**
     * Get the current app folder absolute path
     * @return Absolute path or empty string if not set
     */
    private String getAppPath() {
        if (m_appDir != null && !m_appDir.isEmpty()) {
            return m_appDir;
        }
        return m_filesDir;  // Fallback to default
    }

    /**
     * Get debug info about all relevant paths
     * @param appName App name to test
     * @return JSON with all path information
     */
    private String getPathInfo(String appName) {
        try {
            String packageName = m_ctx.getPackageName();
            String basePath = "/storage/emulated/0/Android/data/" + packageName;
            
            // Test IDE path
            String idePath = basePath + "/files/DroidScript/" + (appName != null ? appName : "TestApp");
            File ideFolder = new File(idePath);
            boolean ideExists = ideFolder.exists();
            
            // Test APK path
            String apkPath = basePath + "/assets";
            File apkFolder = new File(apkPath);
            boolean apkExists = apkFolder.exists();
            
            // Test legacy path
            String legacyPath = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() 
                              + "/DroidScript/" + (appName != null ? appName : "TestApp");
            File legacyFolder = new File(legacyPath);
            boolean legacyExists = legacyFolder.exists();
            
            // Current settings
            String currentAppDir = m_appDir != null ? m_appDir : "not set";
            String currentFilesDir = m_filesDir != null ? m_filesDir : "not set";
            String currentPlugDir = m_plugDir != null ? m_plugDir : "not set";
            
            StringBuilder json = new StringBuilder("{");
            json.append("\"package\": \"").append(packageName).append("\", ");
            json.append("\"idePath\": \"").append(idePath).append("\", ");
            json.append("\"ideExists\": ").append(ideExists).append(", ");
            json.append("\"apkPath\": \"").append(apkPath).append("\", ");
            json.append("\"apkExists\": ").append(apkExists).append(", ");
            json.append("\"legacyPath\": \"").append(legacyPath).append("\", ");
            json.append("\"legacyExists\": ").append(legacyExists).append(", ");
            json.append("\"currentAppDir\": \"").append(currentAppDir).append("\", ");
            json.append("\"currentFilesDir\": \"").append(currentFilesDir).append("\", ");
            json.append("\"currentPlugDir\": \"").append(currentPlugDir).append("\"");
            json.append("}");
            
            return json.toString();
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    /**
     * Copy PHP files to specified app folder
     * @param appName Name of the DroidScript app
     * @return JSON with copy results
     */
    private String copyPhpToApp(String appName) {
        try {
            if (appName == null || appName.isEmpty()) {
                return "{\"error\": \"App name is required\"}";
            }

            // Use dynamic path detection
            String targetDir = getDroidScriptAppPath(appName);
            
            if (targetDir == null) {
                return "{\"error\": \"Could not determine app folder for: " + appName + "\"}";
            }
            
            File targetFolder = new File(targetDir);
            if (!targetFolder.exists()) {
                if (!targetFolder.mkdirs()) {
                    return "{\"error\": \"Could not create app folder\", \"path\": \"" + targetDir + "\"}";
                }
            }

            String copyResult = copyPhpFilesToFolder(targetDir);
            return "{\"success\": true, \"path\": \"" + targetDir + "\", \"files\": " + copyResult + "}";
            
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    /**
     * Copy all PHP files from plugin to target folder
     * @param targetDir Target directory path
     * @return JSON array of copied files
     */
    private String copyPhpFilesToFolder(String targetDir) {
        StringBuilder copied = new StringBuilder("[");
        StringBuilder errors = new StringBuilder();
        String[] phpFiles = {"logic.php", "router.php", "ui_core.php", "app.php", "simple.php"};
        boolean first = true;
        
        // Try multiple source locations
        String[] sourcePaths = {
            m_plugDir,                          // Direct plugin folder
            m_plugDir + "/assets",              // Plugin assets subfolder
            m_ctx.getDir("Plugins", 0).getAbsolutePath() + "/phpnativeplugin/assets",
            m_ctx.getFilesDir().getAbsolutePath()  // Default files dir (fallback)
        };
        
        for (String fileName : phpFiles) {
            boolean fileCopied = false;
            
            for (String sourcePath : sourcePaths) {
                try {
                    File srcFile = new File(sourcePath, fileName);
                    File destFile = new File(targetDir, fileName);
                    
                    if (srcFile.exists() && srcFile.canRead()) {
                        copyFile(srcFile, destFile);
                        if (!first) copied.append(", ");
                        copied.append("\"" + fileName + "\"");
                        first = false;
                        fileCopied = true;
                        Log.d(TAG, "Copied " + fileName + " from " + sourcePath + " to " + targetDir);
                        break;  // File copied, move to next file
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to copy " + fileName + " from " + sourcePath + ": " + e.getMessage());
                }
            }
            
            if (!fileCopied) {
                errors.append(fileName).append(" not found; ");
                Log.w(TAG, "Could not find source for: " + fileName);
            }
        }
        
        copied.append("]");
        
        // Add debug info if no files copied
        if (first) {
            StringBuilder debug = new StringBuilder("{\"files\": [], \"searched\": [");
            for (int i = 0; i < sourcePaths.length; i++) {
                if (i > 0) debug.append(", ");
                File f = new File(sourcePaths[i]);
                debug.append("\"").append(sourcePaths[i]).append(" (exists:").append(f.exists()).append(")\"");
            }
            debug.append("], \"errors\": \"").append(errors.toString().replace("\"", "'")).append("\"}");
            return debug.toString();
        }
        
        return copied.toString();
    }

    /**
     * Extract documentation files to app folder.
     * Copies docs/, docs_examples/ folders and DocRunner.js -> {appName}.js
     * @param appName The target app name
     * @return JSON result
     */
    private String extractDocsToApp(String appName) {
        try {
            if (appName == null || appName.isEmpty()) {
                return "{\"error\": \"App name is required\"}";
            }

            // Get target app folder
            String targetDir = getDroidScriptAppPath(appName);
            if (targetDir == null) {
                return "{\"error\": \"Could not determine app folder for: " + appName + "\"}";
            }

            File targetFolder = new File(targetDir);
            if (!targetFolder.exists()) {
                if (!targetFolder.mkdirs()) {
                    return "{\"error\": \"Could not create app folder\"}";
                }
            }

            StringBuilder copied = new StringBuilder();
            copied.append("[");
            StringBuilder debug = new StringBuilder();
            boolean first = true;

            // Get external storage paths
            String extStorage = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
            
            // Source paths to check - comprehensive list with all variations
            String[] sourcePaths = {
                // Plugin internal dir
                m_plugDir,
                m_plugDir + "/assets",
                // Context Plugins dir
                m_ctx.getDir("Plugins", 0).getAbsolutePath() + "/phpnativeplugin",
                m_ctx.getDir("Plugins", 0).getAbsolutePath() + "/phpnativeplugin/assets",
                m_ctx.getDir("Plugins", 0).getAbsolutePath() + "/PhpNativePlugin",
                m_ctx.getDir("Plugins", 0).getAbsolutePath() + "/PhpNativePlugin/assets",
                // External storage paths - multiple variations
                extStorage + "/DroidScript/Plugins/phpnativeplugin",
                extStorage + "/DroidScript/Plugins/phpnativeplugin/assets",
                extStorage + "/DroidScript/Plugins/PhpNativePlugin",
                extStorage + "/DroidScript/Plugins/PhpNativePlugin/assets",
                "/sdcard/DroidScript/Plugins/phpnativeplugin",
                "/sdcard/DroidScript/Plugins/phpnativeplugin/assets",
                "/sdcard/DroidScript/Plugins/PhpNativePlugin",
                "/sdcard/DroidScript/Plugins/PhpNativePlugin/assets",
                // Files dir
                m_filesDir,
                m_filesDir + "/assets",
                // App external files
                m_ctx.getExternalFilesDir(null) + "/Plugins/phpnativeplugin",
                m_ctx.getExternalFilesDir(null) + "/Plugins/phpnativeplugin/assets"
            };

            // Log all paths being checked with contents
            for (String p : sourcePaths) {
                File f = new File(p);
                File docsInPath = new File(p, "docs");
                debug.append(p).append(":").append(f.exists() ? "Y" : "N");
                if (f.exists() && f.isDirectory()) {
                    String[] contents = f.list();
                    if (contents != null && contents.length > 0) {
                        debug.append("[");
                        for (int i = 0; i < Math.min(contents.length, 5); i++) {
                            if (i > 0) debug.append(",");
                            debug.append(contents[i]);
                        }
                        if (contents.length > 5) debug.append("...");
                        debug.append("]");
                    }
                }
                debug.append("; ");
            }
            Log.d(TAG, "ExtractDocs checking paths: " + debug.toString());

            // Copy docs/ folder
            for (String sourcePath : sourcePaths) {
                File docsDir = new File(sourcePath, "docs");
                if (docsDir.exists() && docsDir.isDirectory()) {
                    File targetDocs = new File(targetDir, "docs");
                    copyFolder(docsDir, targetDocs);
                    if (!first) copied.append(", ");
                    copied.append("\"docs/\"");
                    first = false;
                    Log.d(TAG, "Copied docs/ from " + sourcePath);
                    break;
                }
            }

            // Copy docs_examples/ folder
            for (String sourcePath : sourcePaths) {
                File examplesDir = new File(sourcePath, "docs_examples");
                if (examplesDir.exists() && examplesDir.isDirectory()) {
                    File targetExamples = new File(targetDir, "docs_examples");
                    copyFolder(examplesDir, targetExamples);
                    if (!first) copied.append(", ");
                    copied.append("\"docs_examples/\"");
                    first = false;
                    Log.d(TAG, "Copied docs_examples/ from " + sourcePath);
                    break;
                }
            }

            // Copy DocRunner.js as {appName}.js
            for (String sourcePath : sourcePaths) {
                File docRunner = new File(sourcePath, "DocRunner.js");
                if (docRunner.exists() && docRunner.canRead()) {
                    File targetJs = new File(targetDir, appName + ".js");
                    copyFile(docRunner, targetJs);
                    if (!first) copied.append(", ");
                    copied.append("\"" + appName + ".js\"");
                    first = false;
                    Log.d(TAG, "Copied DocRunner.js as " + appName + ".js");
                    break;
                }
            }

            copied.append("]");
            
            // Include debug info if no files were copied
            if (first) {
                return "{\"error\": \"No docs found\", \"searched\": \"" + debug.toString().replace("\"", "'") + "\", \"target\": \"" + targetDir + "\"}";
            }
            
            return "{\"success\": true, \"path\": \"" + targetDir + "\", \"files\": " + copied.toString() + "}";

        } catch (Exception e) {
            Log.e(TAG, "ExtractDocs failed", e);
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    /**
     * Recursively copy a folder
     */
    private void copyFolder(File src, File dest) throws IOException {
        if (!dest.exists()) {
            dest.mkdirs();
        }
        File[] files = src.listFiles();
        if (files != null) {
            for (File file : files) {
                File destFile = new File(dest, file.getName());
                if (file.isDirectory()) {
                    copyFolder(file, destFile);
                } else {
                    copyFile(file, destFile);
                }
            }
        }
    }

    /**
     * Debug: List all plugin paths and their contents
     */
    private String debugPluginPaths() {
        try {
            StringBuilder json = new StringBuilder("{\"paths\":{");
            String extStorage = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
            
            String[] paths = {
                m_plugDir,
                m_plugDir + "/assets",
                m_ctx.getDir("Plugins", 0).getAbsolutePath(),
                m_ctx.getDir("Plugins", 0).getAbsolutePath() + "/phpnativeplugin",
                m_ctx.getDir("Plugins", 0).getAbsolutePath() + "/PhpNativePlugin",
                extStorage + "/DroidScript/Plugins",
                extStorage + "/DroidScript/Plugins/phpnativeplugin",
                extStorage + "/DroidScript/Plugins/phpnativeplugin/assets",
                extStorage + "/DroidScript/Plugins/PhpNativePlugin",
                extStorage + "/DroidScript/Plugins/PhpNativePlugin/assets",
                m_filesDir
            };
            
            boolean first = true;
            for (String path : paths) {
                if (!first) json.append(",");
                first = false;
                
                json.append("\"").append(path.replace("\"", "'")).append("\":");
                File f = new File(path);
                if (!f.exists()) {
                    json.append("null");
                } else if (f.isDirectory()) {
                    json.append("[");
                    String[] contents = f.list();
                    if (contents != null) {
                        for (int i = 0; i < contents.length; i++) {
                            if (i > 0) json.append(",");
                            json.append("\"").append(contents[i]).append("\"");
                        }
                    }
                    json.append("]");
                } else {
                    json.append("\"file\"");
                }
            }
            
            json.append("},\"m_plugDir\":\"").append(m_plugDir).append("\"");
            json.append(",\"m_filesDir\":\"").append(m_filesDir).append("\"");
            json.append(",\"extStorage\":\"").append(extStorage).append("\"");
            json.append("}");
            
            return json.toString();
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Get the directory where PHP scripts should run from
     * @return App directory if set, otherwise default files directory
     */
    private String getPhpScriptDir() {
        if (m_appDir != null && !m_appDir.isEmpty()) {
            File appLogic = new File(m_appDir, "logic.php");
            if (appLogic.exists()) {
                return m_appDir;
            }
        }
        return m_filesDir;
    }

    // -------------------------------------------------------------------------
    // DroidScript Bridge Methods
    // -------------------------------------------------------------------------

    private void CallScript(Bundle b) throws Exception {
        m_callscript.invoke(m_parent, b);
    }

    public void ExecScript(String code) throws Exception {
        m_execscript.invoke(m_parent, code);
    }

    public Object GetObject(String id) throws Exception {
        return m_getObject.invoke(m_parent, id);
    }

    // -------------------------------------------------------------------------
    // Main Plugin API (Called from JavaScript)
    // -------------------------------------------------------------------------

    public String CallPlugin(Bundle b, Object obj) throws Exception {
        String cmd = b.getString("cmd");
        if (cmd == null) return null;

        switch (cmd) {
            case "QueryVersion":
                return String.valueOf(VERSION);
            
            case "SetOnPhpResponse":
                m_OnPhpResponse = b.getString("p1");
                break;
            
            case "SetOnSensorResult":
                m_OnSensorResult = b.getString("p1");
                break;
            
            case "SetOnUiReady":
                m_OnUiReady = b.getString("p1");
                break;
            
            case "SetOnError":
                m_OnError = b.getString("p1");
                break;
            
            case "CallPhp":
                return callPhp(b.getString("p1"), b.getString("p2"));
            
            case "RenderUI":
                renderUI(b.getString("p1"));
                break;
            
            case "UpdateView":
                updateView(b.getString("p1"), b.getString("p2"));
                break;
            
            // Overlay commands removed - only direct rendering is supported
            
            case "GoBack":
                m_mainHandler.post(() -> handleBackPress());
                break;

            case "SetOnBack":
                m_OnBack = b.getString("p1");
                break;
            
            case "InjectSensorCall":
                injectSensorCall(b.getString("p1"), b.getString("p2"));
                break;
            
            case "OnInternalSensorResult":
                handleSensorResult(b.getString("p1"), b.getString("p2"));
                break;
            
            case "OnSensorResult":
                // Result from JS native handler - route to PHP callback
                handleSensorResult(b.getString("p1"), b.getString("p2"));
                break;
            
            case "OnDsCallResult":
                // Result from dsCall() - route to PHP callback
                handleDsCallResult(b.getString("p1"), b.getString("p2"));
                break;
            
            case "StartApp":
                startPhpApp(b.getString("p1"),b.getString("p2"));
                break;
            
            case "RunApp":
                runAppPhp(b.getString("p1"));
                break;
            
            case "HasAppPhp":
                return String.valueOf(hasAppPhp());
            
            case "IsReady":
                return String.valueOf(m_phpReady);
            
            case "SetApp":
                return setApp(b.getString("p1"));
            
            case "GetAppPath":
                return getAppPath();
            
            case "CopyPhpToApp":
                return copyPhpToApp(b.getString("p1"));
            
            case "GetPathInfo":
                return getPathInfo(b.getString("p1"));
            
            case "GetViewText":
                return getViewText(b.getString("p1"));
            
            case "GetViewProperty":
                return getViewProperty(b.getString("p1"), b.getString("p2"));
            
            case "SetViewText":
                setViewText(b.getString("p1"), b.getString("p2"));
                break;
            
            case "DebugPhp":
                return debugPhp();
            
            case "RunPhpTest":
                return runPhpTest(b.getString("p1"));
            
            case "WorkerStatus":
                return getWorkerStatus();
            
            case "RestartWorker":
                m_executor.execute(this::restartPhpWorker);
                return "{\"status\": \"restarting\"}";
            
            case "StopWorker":
                stopPhpWorker();
                return "{\"status\": \"stopped\"}";
            
            case "StartWorker":
                m_executor.execute(this::startPhpWorker);
                return "{\"status\": \"starting\"}";
            
            case "ExtractDocs":
                return extractDocsToApp(b.getString("p1"));
            
            case "RunFile":
                runPhpFile(b.getString("p1"), b.getString("p2"), b.getString("p3"));
                break;
            
            case "RunFileDebug":
                return runPhpFileDebug(b.getString("p1"), b.getString("p2"));
            
            case "DebugPaths":
                return debugPluginPaths();
            
            case "GetRootView":
                // This is handled via CreateObject for returning actual View object
                // Trigger view creation on UI thread if needed
                ensureMainRootViewCreated();
                return "view_created";
            
            case "SetRenderMode":
                // Only direct rendering is supported now
                Log.d(TAG, "SetRenderMode called - only direct mode is supported");
                break;
            
            default:
                Log.w(TAG, "Unknown command: " + cmd);
                return "{\"error\": \"Unknown command: " + escapeJsonString(cmd) + "\"}";
        }
        return null;
    }

    public String CallPlugin(Bundle b) throws Exception {
        return CallPlugin(b, null);
    }

    public Object CreateObject(Bundle b) {
        String type = b.getString("type");
        
        // Return the root view for direct rendering
        if ("RootView".equals(type) || "GetRootView".equals(type)) {
            Log.d(TAG, "CreateObject: RootView requested, creating synchronously");
            
            // Create view synchronously on UI thread if needed
            if (m_mainRootView == null) {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    createMainRootViewInternal();
                } else {
                    // Must wait for UI thread to create view
                    final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                    m_mainHandler.post(() -> {
                        try {
                            createMainRootViewInternal();
                        } finally {
                            latch.countDown();
                        }
                    });
                    try {
                        latch.await(2000, java.util.concurrent.TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            
            Log.d(TAG, "CreateObject: Returning m_mainRootView=" + m_mainRootView);
            return m_mainRootView;
        }
        
        return null;
    }
    
    /**
     * Ensure the main root view is created (for direct rendering mode).
     * Must be called before CreateObject("RootView").
     * Thread-safe: works from both main thread and background threads.
     */
    private void ensureMainRootViewCreated() {
        if (m_mainRootView == null) {
            // Check if we're already on the main thread
            if (Looper.myLooper() == Looper.getMainLooper()) {
                // Already on main thread - create directly
                createMainRootViewInternal();
            } else {
                // On background thread - use CountDownLatch to wait for UI thread
                final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                m_mainHandler.post(() -> {
                    try {
                        createMainRootViewInternal();
                    } finally {
                        latch.countDown();
                    }
                });
                
                try {
                    latch.await(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    /**
     * Internal method to create the main root view. Must be called on UI thread.
     */
    private void createMainRootViewInternal() {
        if (m_mainRootView != null) return; // Double-check
        
        m_mainRootView = new FrameLayout(m_ctx);
        
        // Set layout params with explicit size
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        m_mainRootView.setLayoutParams(params);
        
        // Ensure view is visible and has minimum size
        m_mainRootView.setMinimumWidth(100);
        m_mainRootView.setMinimumHeight(100);
        m_mainRootView.setBackgroundColor(Color.parseColor("#FFFFFF"));
        m_mainRootView.setVisibility(View.VISIBLE);
        
        // Handle back button on this view
        m_mainRootView.setFocusableInTouchMode(true);
        m_mainRootView.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                handleBackPress();
                return true;
            }
            return false;
        });
        
        Log.d(TAG, "createMainRootViewInternal: created m_mainRootView");
    }

    /**
     * Get the root view wrapper for direct rendering mode.
     * This returns a FrameLayout that DroidScript can add to its own layout.
     * The PHP UI will be rendered into this container.
     */
    private Object getRootViewWrapper() {
        ensureMainRootViewCreated();
        Log.d(TAG, "getRootViewWrapper: returning m_mainRootView=" + m_mainRootView + 
              " visibility=" + (m_mainRootView != null ? m_mainRootView.getVisibility() : "null"));
        return m_mainRootView;
    }
    
    /**
     * Get the render container (m_mainRootView).
     */
    private ViewGroup getRenderContainer() {
        Log.d(TAG, "getRenderContainer: m_mainRootView=" + m_mainRootView);
        
        if (m_mainRootView != null) {
            return m_mainRootView;
        }
        // If no view yet, create it now
        Log.w(TAG, "getRenderContainer: m_mainRootView is null, creating now");
        ensureMainRootViewCreated();
        if (m_mainRootView != null) {
            return m_mainRootView;
        }
        Log.e(TAG, "getRenderContainer: Failed to create m_mainRootView!");
        return null;
    }

    // -------------------------------------------------------------------------
    // PHP Execution
    // -------------------------------------------------------------------------

    private String callPhp(String method, String paramsJson) {
        if (!m_phpReady) {
            return "{\"error\": \"PHP not ready\"}";
        }
         
        if (m_phpPath == null) {
            return "{\"error\": \"PHP binary not found. Use DebugPhp() to check paths.\"}";
        }
        
        // Sync view state to file before calling PHP (so PHP can read it)
        syncViewStateToFile();

        if (paramsJson == null || paramsJson.isEmpty()) {
            paramsJson = "{}";
        }
        
        // Try the persistent PHP worker first (much faster, ~5ms vs ~200ms)
        if (m_phpWorkerReady) {
            try {
                String result = sendWorkerCommand(method, paramsJson);
                if (result != null) {
                    return result;
                }
                // Worker call returned null, fall through to subprocess
                Log.w(TAG, "Worker returned null, falling back to subprocess");
            } catch (Exception e) {
                Log.w(TAG, "Worker call failed, falling back to subprocess: " + e.getMessage());
                // Worker might have died, mark it as not ready
                if (e instanceof SocketTimeoutException || e.getMessage().contains("Connection refused")) {
                    m_phpWorkerReady = false;
                    // Try to restart worker in background
                    m_executor.execute(this::restartPhpWorker);
                }
            }
        } else {
            // WARN: Using subprocess fallback - any PHP state (opcache, static vars) will be lost
            Log.w(TAG, "PHP worker not ready, using subprocess fallback (state will not persist between calls)");
        }
        
        // Fallback: spawn subprocess (slower but always works)
        return callPhpSubprocess(method, paramsJson);
    }
    
    /**
     * Call PHP via long-running worker server (fast path).
     * Uses TCP socket with Content-Length framing and HMAC authentication.
     * 
     * @param method PHP method to call
     * @param paramsJson JSON parameters string
     * @return JSON response string, or null on connection error
     * @throws Exception on communication errors
     */
    private String sendWorkerCommand(String method, String paramsJson) throws Exception {
        int requestId = m_phpRequestId.incrementAndGet();
        
        // Build request JSON
        JSONObject request = new JSONObject();
        request.put("id", requestId);
        request.put("method", method);
        
        // Parse params if it's a valid JSON string, otherwise wrap it
        Object params;
        if (paramsJson != null && !paramsJson.isEmpty()) {
            try {
                params = new JSONObject(paramsJson);
            } catch (Exception e) {
                try {
                    params = new JSONArray(paramsJson);
                } catch (Exception e2) {
                    params = paramsJson;
                }
            }
        } else {
            params = JSONObject.NULL;
        }
        request.put("params", params);
        
        // Generate HMAC token for authentication
        String token = generateHmacToken(requestId, method, params);
        request.put("token", token);
        
        String requestBody = request.toString();
        
        // Connect to worker
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", m_phpWorkerPort), PHP_WORKER_CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(PHP_WORKER_READ_TIMEOUT_MS);
            
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            
            // Send request with Content-Length framing
            String header = "Content-Length: " + requestBody.getBytes(StandardCharsets.UTF_8).length + "\r\n\r\n";
            out.write(header.getBytes(StandardCharsets.UTF_8));
            out.write(requestBody.getBytes(StandardCharsets.UTF_8));
            out.flush();
            
            // Read response
            String response = readFramedResponse(in);
            if (response == null) {
                throw new IOException("Empty response from PHP worker");
            }
            
            // Parse response
            JSONObject respJson = new JSONObject(response);
            
            // Check for errors
            if (!respJson.isNull("error")) {
                JSONObject error = respJson.optJSONObject("error");
                if (error != null) {
                    String errorMsg = error.optString("message", "Unknown error");
                    Log.e(TAG, "PHP worker error: " + errorMsg);
                    return "{\"error\": \"" + escapeJsonString(errorMsg) + "\"}";
                }
            }
            
            // Return the result
            Object result = respJson.opt("result");
            if (result == null || result == JSONObject.NULL) {
                return "{}";
            }
            if (result instanceof JSONObject || result instanceof JSONArray) {
                return result.toString();
            }
            return String.valueOf(result);
        }
    }
    
    /**
     * Generate HMAC-SHA256 token for request authentication.
     * Uses null-byte delimiters for unambiguous field separation.
     * Always JSON-encodes params to match PHP's json_encode().
     */
    private String generateHmacToken(int id, String method, Object params) {
        if (m_phpWorkerSecret == null) return "";
        try {
            // Always JSON-encode params for consistent serialization with PHP.
            // PHP does: json_encode($params, JSON_UNESCAPED_UNICODE)
            String paramsJson;
            if (params == null || params == JSONObject.NULL) {
                paramsJson = "null";
            } else if (params instanceof JSONObject || params instanceof JSONArray) {
                paramsJson = params.toString();
            } else if (params instanceof String) {
                // Wrap string in quotes for JSON compatibility
                paramsJson = new JSONObject().put("_", params).toString();
                // Extract just the JSON string value: {"_":"value"} -> "value"
                paramsJson = paramsJson.substring(5, paramsJson.length() - 1);
            } else if (params instanceof Number || params instanceof Boolean) {
                paramsJson = params.toString();
            } else {
                paramsJson = new JSONObject().put("v", params).optString("v", "null");
            }
            
            // Use null byte (0x00) delimiters for unambiguous field separation
            // even if method name contains special characters like |
            String payload = id + "\0" + method + "\0" + paramsJson;
            
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                m_phpWorkerSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "HMAC generation failed", e);
            return "";
        }
    }
    
    /**
     * Read a Content-Length framed response from input stream.
     * Uses byte-based reading (not char-based) since Content-Length is in bytes.
     */
    private String readFramedResponse(InputStream in) throws IOException {
        // Read headers byte-by-byte until we see \r\n\r\n
        ByteArrayOutputStream headerBuf = new ByteArrayOutputStream(256);
        int prev = 0, prevPrev = 0, prevPrevPrev = 0;
        int b;
        while ((b = in.read()) != -1) {
            headerBuf.write(b);
            // Check for \r\n\r\n sequence
            if (prevPrevPrev == '\r' && prevPrev == '\n' && prev == '\r' && b == '\n') {
                break;
            }
            // Header flood guard
            if (headerBuf.size() > 8192) {
                throw new IOException("Header too large");
            }
            prevPrevPrev = prevPrev;
            prevPrev = prev;
            prev = b;
        }
        
        String headerStr = headerBuf.toString(StandardCharsets.UTF_8.name());
        
        // Extract Content-Length
        int contentLength = -1;
        for (String line : headerStr.split("\r\n")) {
            if (line.toLowerCase().startsWith("content-length:")) {
                try {
                    contentLength = Integer.parseInt(line.substring(15).trim());
                } catch (NumberFormatException ignored) {}
                break;
            }
        }
        
        if (contentLength <= 0) {
            return null;
        }
        
        // Enforce size cap to prevent huge allocations
        if (contentLength > MAX_RESPONSE_SIZE) {
            throw new IOException("Response too large: " + contentLength + " bytes (max " + MAX_RESPONSE_SIZE + ")");
        }
        
        // Read body as bytes (Content-Length is byte count, not char count)
        byte[] bodyBytes = new byte[contentLength];
        int totalRead = 0;
        while (totalRead < contentLength) {
            int read = in.read(bodyBytes, totalRead, contentLength - totalRead);
            if (read == -1) break;
            totalRead += read;
        }
        
        if (totalRead != contentLength) {
            throw new IOException("Incomplete response: got " + totalRead + " of " + contentLength + " bytes");
        }
        
        return new String(bodyBytes, StandardCharsets.UTF_8);
    }
    
    /**
     * Call PHP via subprocess (slow fallback path).
     * Spawns a new PHP process for each call.
     */
    private String callPhpSubprocess(String method, String paramsJson) {
        Process process = null;
        try {
            // Use app directory if set, otherwise default
            String scriptDir = getPhpScriptDir();
            File scriptFile = new File(scriptDir, m_entryFile != null ? m_entryFile : "logic.php");
            if (!scriptFile.exists()) {
                return "{\"error\": \"logic.php not found in " + scriptDir + "\"}";
            }

            ProcessBuilder pb = new ProcessBuilder(
                m_phpPath,
                scriptFile.getAbsolutePath(),
                "--method=" + method,
                "--params=" + paramsJson
            );
            pb.redirectErrorStream(true);
            pb.directory(new File(scriptDir));  // Set working directory
            
            Log.d(TAG, "Executing PHP subprocess: " + m_phpPath + " --method=" + method);
            
            process = pb.start();
            StringBuilder output = new StringBuilder();
            
            // Use try-with-resources to ensure reader is closed
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (!firstLine) {
                        output.append("\n");
                    }
                    output.append(line);
                    firstLine = false;
                }
            }
            
            // Wait with timeout to prevent hanging on infinite loops
            boolean finished = process.waitFor(PHP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                Log.e(TAG, "PHP process timed out after " + PHP_TIMEOUT_SECONDS + " seconds");
                return "{\"error\": \"PHP process timed out after " + PHP_TIMEOUT_SECONDS + " seconds\"}";
            }
            
            int exitCode = process.exitValue();
            String result = output.toString().trim();
            Log.d(TAG, "PHP exit code: " + exitCode + ", output length: " + result.length());
            
            // Extract JSON from output (in case of warnings/notices before JSON)
            int jsonStart = result.indexOf("{");
            if (jsonStart > 0) {
                Log.w(TAG, "Non-JSON prefix: " + result.substring(0, jsonStart));
                result = result.substring(jsonStart);
            } else if (jsonStart < 0) {
                Log.e(TAG, "No JSON found in PHP output");
                return "{\"error\": \"PHP returned non-JSON: " + escapeJsonString(result.substring(0, Math.min(100, result.length()))) + "\"}";
            }

            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "PHP call interrupted: " + method, e);
            return "{\"error\": \"PHP call interrupted\"}";
        } catch (Exception e) {
            Log.e(TAG, "PHP call failed: " + method, e);
            return "{\"error\": \"" + escapeJsonString(e.getMessage()) + "\"}";
        } finally {
            // Ensure process is cleaned up
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
    
    /**
     * Safely escape a string for JSON embedding.
     * Handles backslashes, quotes, newlines, and control characters.
     */
    private String escapeJsonString(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Debug PHP execution - test if PHP binary works and return diagnostic info
     */
    private String debugPhp() {
        StringBuilder debug = new StringBuilder();
        debug.append("{");
        
        // 1. Native library directory info
        String nativeLibDir = m_ctx.getApplicationInfo().nativeLibraryDir;
        debug.append("\"nativeLibraryDir\": \"").append(nativeLibDir).append("\", ");
        
        // 1b. Check PHP binary
        debug.append("\"phpPath\": ").append(m_phpPath != null ? "\"" + m_phpPath + "\"" : "null").append(", ");
        File phpBinary = m_phpPath != null ? new File(m_phpPath) : null;
        debug.append("\"phpExists\": ").append(phpBinary != null && phpBinary.exists()).append(", ");
        debug.append("\"phpExecutable\": ").append(phpBinary != null && phpBinary.canExecute()).append(", ");
        debug.append("\"phpReadable\": ").append(phpBinary != null && phpBinary.canRead()).append(", ");
        debug.append("\"phpSize\": ").append(phpBinary != null && phpBinary.exists() ? phpBinary.length() : 0).append(", ");
        
        // 1c. List files in native lib dir
        debug.append("\"nativeLibs\": [");
        File nativeDir = new File(nativeLibDir);
        if (nativeDir.exists() && nativeDir.isDirectory()) {
            String[] libs = nativeDir.list();
            if (libs != null) {
                for (int i = 0; i < libs.length; i++) {
                    if (i > 0) debug.append(", ");
                    debug.append("\"").append(libs[i]).append("\"");
                }
            }
        }
        debug.append("], ");
        
        // 2. Check script directory
        String scriptDir = getPhpScriptDir();
        debug.append("\"scriptDir\": \"").append(scriptDir).append("\", ");
        File scriptFolder = new File(scriptDir);
        debug.append("\"scriptDirExists\": ").append(scriptFolder.exists()).append(", ");
        
        // 3. List files in script directory
        debug.append("\"scriptsFound\": [");
        if (scriptFolder.exists() && scriptFolder.isDirectory()) {
            String[] files = scriptFolder.list();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    if (i > 0) debug.append(", ");
                    debug.append("\"").append(files[i]).append("\"");
                }
            }
        }
        debug.append("], ");
        
        // 4. Check individual PHP files
        String[] phpFiles = {"logic.php", "router.php", "ui_core.php", "app.php"};
        debug.append("\"phpFiles\": {");
        for (int i = 0; i < phpFiles.length; i++) {
            if (i > 0) debug.append(", ");
            File f = new File(scriptDir, phpFiles[i]);
            debug.append("\"").append(phpFiles[i]).append("\": {");
            debug.append("\"exists\": ").append(f.exists()).append(", ");
            debug.append("\"size\": ").append(f.exists() ? f.length() : 0);
            debug.append("}");
        }
        debug.append("}, ");
        
        // 5. Test PHP execution with simple command
        debug.append("\"phpTest\": ");
        if (m_phpPath == null) {
            debug.append("{\"success\": false, \"error\": \"phpPath is null - binary not found\"}");
        } else {
            Process process = null;
            try {
                ProcessBuilder pb = new ProcessBuilder(m_phpPath, "-v");
                pb.redirectErrorStream(true);
                process = pb.start();
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append(" ");
                    }
                }
                boolean finished = process.waitFor(PHP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    debug.append("{\"success\": false, \"error\": \"PHP version check timed out\"}");
                } else {
                    int exitCode = process.exitValue();
                    String phpVersion = escapeJsonString(output.toString().trim());
                    debug.append("{\"success\": true, \"exitCode\": ").append(exitCode);
                    debug.append(", \"output\": \"").append(phpVersion.substring(0, Math.min(100, phpVersion.length()))).append("\"}");
                }
            } catch (Exception e) {
                debug.append("{\"success\": false, \"error\": \"").append(escapeJsonString(e.getMessage())).append("\"}");
            } finally {
                if (process != null && process.isAlive()) {
                    process.destroyForcibly();
                }
            }
        }
        debug.append(", ");
        
        // 6. Test actual PHP script execution
        debug.append("\"logicTest\": ");
        File logicFile = new File(scriptDir, "logic.php");
        if (m_phpPath == null) {
            debug.append("{\"success\": false, \"error\": \"phpPath is null\"}");
        } else if (logicFile.exists()) {
            Process logicProcess = null;
            try {
                ProcessBuilder pb = new ProcessBuilder(m_phpPath, logicFile.getAbsolutePath(), "--method=index");
                pb.redirectErrorStream(true);
                pb.directory(new File(scriptDir));
                logicProcess = pb.start();
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(logicProcess.getInputStream()))) {
                    String line;
                    boolean firstLine = true;
                    while ((line = reader.readLine()) != null) {
                        if (!firstLine) output.append("\n");
                        output.append(line);
                        firstLine = false;
                    }
                }
                boolean finished = logicProcess.waitFor(PHP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!finished) {
                    logicProcess.destroyForcibly();
                    debug.append("{\"success\": false, \"error\": \"logic.php test timed out\"}");
                } else {
                    int exitCode = logicProcess.exitValue();
                    String result = escapeJsonString(output.toString().trim());
                    debug.append("{\"success\": true, \"exitCode\": ").append(exitCode);
                    debug.append(", \"outputLength\": ").append(result.length());
                    debug.append(", \"output\": \"").append(result.substring(0, Math.min(500, result.length()))).append("\"}");
                }
            } catch (Exception e) {
                debug.append("{\"success\": false, \"error\": \"").append(escapeJsonString(e.getMessage())).append("\"}");
            } finally {
                if (logicProcess != null && logicProcess.isAlive()) {
                    logicProcess.destroyForcibly();
                }
            }
        } else {
            debug.append("{\"success\": false, \"error\": \"logic.php not found\"}");
        }
        debug.append(", ");
        
        // 7. PHP ready state
        debug.append("\"phpReady\": ").append(m_phpReady).append(", ");
        
        // 8. App directory info
        debug.append("\"appDir\": \"").append(m_appDir != null ? m_appDir : "null").append("\", ");
        debug.append("\"appName\": \"").append(m_appName != null ? m_appName : "null").append("\", ");
        debug.append("\"filesDir\": \"").append(m_filesDir != null ? m_filesDir : "null").append("\", ");
        debug.append("\"plugDir\": \"").append(m_plugDir != null ? m_plugDir : "null").append("\"");
        
        debug.append("}");
        return debug.toString();
    }

    /**
     * Run PHP logic.php with specified method and return raw output for debugging
     * @param method PHP method to call (default: index)
     * @return JSON with raw PHP output, exit code, and any errors
     */
    private String runPhpTest(String method) {
        StringBuilder result = new StringBuilder();
        result.append("{");
        
        if (method == null || method.isEmpty()) {
            method = "index";
        }
        
        result.append("\"method\": \"").append(method).append("\", ");
        
        // Check PHP path
        result.append("\"phpPath\": ").append(m_phpPath != null ? "\"" + m_phpPath + "\"" : "null").append(", ");
        if (m_phpPath == null) {
            result.append("\"error\": \"PHP binary not found\"}");
            return result.toString();
        }
        
        File phpBinary = new File(m_phpPath);
        result.append("\"phpExists\": ").append(phpBinary.exists()).append(", ");
        result.append("\"phpExecutable\": ").append(phpBinary.canExecute()).append(", ");
        
        // Check script directory
        String scriptDir = getPhpScriptDir();
        result.append("\"scriptDir\": \"").append(scriptDir).append("\", ");
        
        File logicFile = new File(scriptDir, "logic.php");
        result.append("\"logicPhpExists\": ").append(logicFile.exists()).append(", ");
        
        if (!logicFile.exists()) {
            result.append("\"error\": \"logic.php not found in ").append(scriptDir).append("\"}");
            return result.toString();
        }
        
        // Execute PHP
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(
                m_phpPath,
                logicFile.getAbsolutePath(),
                "--method=" + method
            );
            pb.redirectErrorStream(true);
            pb.directory(new File(scriptDir));
            
            result.append("\"command\": \"").append(escapeJsonString(m_phpPath + " " + logicFile.getAbsolutePath() + " --method=" + method)).append("\", ");
            
            process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (!firstLine) output.append("\n");
                    output.append(line);
                    firstLine = false;
                }
            }
            
            boolean finished = process.waitFor(PHP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                result.append("\"error\": \"PHP process timed out after ").append(PHP_TIMEOUT_SECONDS).append(" seconds\"}");
                return result.toString();
            }
            
            int exitCode = process.exitValue();
            String rawOutput = output.toString();
            result.append("\"exitCode\": ").append(exitCode).append(", ");
            result.append("\"outputLength\": ").append(rawOutput.length()).append(", ");
            result.append("\"rawOutput\": \"").append(escapeJsonString(rawOutput)).append("\"");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.append("\"error\": \"PHP execution interrupted\", ");
            result.append("\"exception\": \"InterruptedException\"");
        } catch (Exception e) {
            result.append("\"error\": \"").append(escapeJsonString(e.getMessage())).append("\", ");
            result.append("\"exception\": \"").append(e.getClass().getName()).append("\"");
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
        
        result.append("}");
        return result.toString();
    }

    /**
     * Execute a list of actions after the view hierarchy has been laid out.
     * Uses ViewTreeObserver.OnGlobalLayoutListener instead of magic postDelayed timings.
     * 
     * @param view The view to attach the layout listener to
     * @param actions List of JSON actions to execute after layout
     */
    private void executeAfterLayout(View view, List<JSONObject> actions) {
        if (view == null || actions == null || actions.isEmpty()) return;
        
        view.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Remove listener to prevent multiple calls
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                
                // Execute all queued post-render actions
                Log.d(TAG, "Layout complete, executing " + actions.size() + " post-render actions");
                for (int i = 0; i < actions.size(); i++) {
                    try {
                        JSONObject action = actions.get(i);
                        Log.d(TAG, "POST-LAYOUT[" + i + "] executing");
                        processPhpResponse(action.toString());
                    } catch (Exception e) {
                        Log.e(TAG, "POST-LAYOUT: error executing action " + i, e);
                    }
                }
            }
        });
    }

    private void processPhpResponse(String jsonResponse) {
        debugLog("processPhpResponse INPUT (first 300): " + (jsonResponse != null ? jsonResponse.substring(0, Math.min(300, jsonResponse.length())) : "null"));
        try {
            JSONObject response = new JSONObject(jsonResponse);
            String action = response.optString("action", "");
            debugLog("processPhpResponse: action='" + action + "' hasType=" + response.has("type") + " hasChildren=" + response.has("children"));

            // Check for special DroidScript sensor call action (legacy - uses JS)
            if ("DS_SENSOR_CALL".equals(action)) {
                String sensor = response.optString("sensor");
                String callback = response.optString("callback", "handle_sensor_result");
                JSONObject paramsObj = response.optJSONObject("params");
                String paramsJson = paramsObj != null ? paramsObj.toString() : "{}";
                
                // Store the PHP callback for when sensor returns
                m_pendingSensorCallbacks.put(sensor, callback);
                
                // Execute via JS native call handler registry
                try {
                    ExecScript("_phpPlugin.NativeCall('" + escapeJs(sensor) + "', '" + escapeJs(paramsJson) + "');");
                    Log.d(TAG, "Native call dispatched: " + sensor);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to dispatch native call: " + sensor, e);
                    notifyError("Native call failed: " + e.getMessage());
                }
            }
            // Native call action - pure Java implementation (no DroidScript)
            else if ("NATIVE_CALL".equals(action)) {
                String type = response.optString("type", response.optString("sensor", ""));
                String callback = response.optString("callback", "handle_native_result");
                JSONObject params = response.optJSONObject("params");
                if (params == null) params = new JSONObject();
                
                debugLog("NATIVE_CALL: type=" + type + ", callback=" + callback + ", params=" + params.toString());
                
                // Store callback and dispatch to handler
                m_pendingSensorCallbacks.put(type, callback);
                
                NativeHandler handler = m_nativeHandlers.get(type.toLowerCase());
                if (handler != null) {
                    final JSONObject finalParams = params;
                    m_executor.execute(() -> {
                        try {
                            handler.handle(finalParams, callback);
                        } catch (Exception e) {
                            Log.e(TAG, "Native handler error: " + type, e);
                            reportNativeResult(type, createErrorJson("Handler error: " + e.getMessage()));
                        }
                    });
                } else {
                    Log.e(TAG, "Unknown native call type: " + type);
                    reportNativeResult(type, createErrorJson("Unknown native call type: " + type));
                }
            }
            // Check for UI render action
           
            // Check for view update action
            else if ("update".equals(action)) {
                String target = response.optString("target");
                JSONObject attrs = response.optJSONObject("attributes");
                if (target != null && attrs != null) {
                    m_mainHandler.post(() -> updateViewInternal(target, attrs));
                }
            }
            // Check for multiple view updates action
            else if ("update_many".equals(action)) {
                JSONObject updates = response.optJSONObject("updates");
                if (updates != null) {
                    m_mainHandler.post(() -> {
                        Iterator<String> keys = updates.keys();
                        while (keys.hasNext()) {
                            String viewId = keys.next();
                            JSONObject attrs = updates.optJSONObject(viewId);
                            if (attrs != null) {
                                updateViewInternal(viewId, attrs);
                            }
                        }
                    });
                }
            }

            else if("ALERT".equals(action)) {
                String message = response.optString("message", "No message");
                m_mainHandler.post(() -> {
                    try {
                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(m_ctx);
                        builder.setTitle(response.optString("title", "PHP Alert"));
                        builder.setMessage(message);
                        builder.setPositiveButton("OK", null);
                        builder.show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error showing alert dialog", e);
                    }
                });
            }

            else if ("TOAST".equals(action)) {
                String message = response.optString("message", "No message");
                m_mainHandler.post(() -> {
                    try {
                        android.widget.Toast.makeText(m_ctx, message, android.widget.Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error showing toast", e);
                    }
                });
            }

            else if("NAVIGATE".equals(action)) {
                String destination = response.optString("screen");
                JSONObject data = response.optJSONObject("data");
                String dataJson = data != null ? data.toString() : "{}";
                
                Log.d(TAG, "Navigate to: " + destination + " with data: " + dataJson);

                // Push to navigation history
                m_screenHistory.add(new String[]{destination, dataJson});
                
                // Call the PHP method (screen) with the data and render the result
                if (!m_executor.isShutdown()) {
                    m_executor.execute(() -> {
                        String phpResponse = callPhp(destination, dataJson);
                        // Process the response (will render new UI if it returns a layout)
                        m_mainHandler.post(() -> processPhpResponse(phpResponse));
                    });
                }
            }

            // Handle GO_BACK action from PHP
            else if("GO_BACK".equals(action)) {
                m_mainHandler.post(() -> handleBackPress());
            }
            
            // ListView actions
            else if ("list_set_items".equals(action)) {
                handleListSetItems(response);
            }
            else if ("list_add_item".equals(action)) {
                handleListAddItem(response);
            }
            else if ("list_add_items".equals(action)) {
                handleListAddItems(response);
            }
            else if ("list_insert_item".equals(action)) {
                handleListInsertItem(response);
            }
            else if ("list_remove_item".equals(action)) {
                handleListRemoveItem(response);
            }
            else if ("list_update_item".equals(action)) {
                handleListUpdateItem(response);
            }
            else if ("list_scroll".equals(action)) {
                handleListScroll(response, false);
            }
            else if ("list_smooth_scroll".equals(action)) {
                handleListScroll(response, true);
            }
            
            // Drawer actions
            else if ("drawer_open".equals(action)) {
                handleDrawerAction(response, "open");
            }
            else if ("drawer_close".equals(action)) {
                handleDrawerAction(response, "close");
            }
            else if ("drawer_toggle".equals(action)) {
                handleDrawerAction(response, "toggle");
            }
            
            // Navigation actions
            else if ("nav_set_items".equals(action)) {
                handleNavSetItems(response);
            }
            else if ("bottom_nav_select".equals(action)) {
                handleBottomNavSelect(response);
            }
            else if ("bottom_nav_badge".equals(action)) {
                handleBottomNavBadge(response);
            }
            
            // Tab actions
            else if ("tab_select".equals(action)) {
                handleTabSelect(response);
            }
            else if ("tab_set_items".equals(action)) {
                handleTabSetItems(response);
            }
            
            // Dialog/Popup actions
            else if ("SNACKBAR".equals(action)) {
                handleSnackbar(response);
            }
            else if ("DIALOG".equals(action)) {
                handleDialog(response);
            }
            else if ("LIST_DIALOG".equals(action)) {
                handleListDialog(response);
            }
            else if ("DATE_PICKER_DIALOG".equals(action)) {
                handleDatePickerDialog(response);
            }
            else if ("TIME_PICKER_DIALOG".equals(action)) {
                handleTimePickerDialog(response);
            }
            else if ("INPUT_DIALOG".equals(action)) {
                handleInputDialog(response);
            }
            else if ("BOTTOM_SHEET".equals(action)) {
                handleBottomSheet(response);
            }
            else if ("DISMISS_DIALOG".equals(action)) {
                handleDismissDialog();
            }
            
            // Animation actions
            else if ("ANIMATE".equals(action)) {
                handleAnimate(response);
            }
            else if ("ANIMATE_SET".equals(action)) {
                handleAnimateSet(response);
            }
            
            // Clipboard/Share/URL
            else if ("CLIPBOARD_COPY".equals(action)) {
                handleClipboardCopy(response);
            }
            else if ("SHARE".equals(action)) {
                handleShare(response);
            }
            else if ("OPEN_URL".equals(action)) {
                handleOpenUrl(response);
            }
            
            // Dynamic view manipulation
            else if ("REMOVE_VIEW".equals(action)) {
                handleRemoveView(response);
            }
            else if ("ADD_VIEW".equals(action)) {
                handleAddView(response);
            }
            else if ("REPLACE_CHILDREN".equals(action)) {
                handleReplaceChildren(response);
            }
            else if ("SCROLL_TO".equals(action)) {
                handleScrollTo(response);
            }
            
            // Call PHP method action - useful for onInit callbacks
            // Note: does NOT re-render if the method returns a layout (to avoid clearing current UI)
            else if ("callMethod".equals(action)) {
                String method = response.optString("method", "");
                JSONObject params = response.optJSONObject("params");
                String paramsJson = params != null ? params.toString() : "{}";
                
                if (!method.isEmpty()) {
                    Log.d(TAG, "callMethod: " + method + " with params: " + paramsJson);
                    m_executor.execute(() -> {
                        String phpResponse = callPhp(method, paramsJson);
                        // Only process non-render actions (updates, native calls, etc.)
                        // Skip if response would trigger a full re-render
                        try {
                            JSONObject resp = new JSONObject(phpResponse);
                            String respAction = resp.optString("action", "");
                            // Skip processing if it's a layout that would replace current UI
                            if (!resp.has("type") && !resp.has("children") && !"render".equals(respAction)) {
                                m_mainHandler.post(() -> processPhpResponse(phpResponse));
                            } else {
                                Log.d(TAG, "callMethod: skipping re-render from " + method);
                            }
                        } catch (Exception e) {
                            // If not valid JSON, try processing anyway
                            m_mainHandler.post(() -> processPhpResponse(phpResponse));
                        }
                    });
                }
            }
            
            // Batch multiple actions
            else if ("batch".equals(action)) {
                JSONArray batchActions = response.optJSONArray("actions");
                Log.d(TAG, "BATCH: processing " + (batchActions != null ? batchActions.length() : 0) + " actions");
                
                if (batchActions != null && batchActions.length() > 0) {
                    // Process actions in order using ViewTreeObserver for proper timing
                    // Actions BEFORE first render: immediate
                    // First render: immediate with layout listener  
                    // Actions AFTER render: after layout completes
                    
                    int firstRenderIndex = -1;
                    
                    // Find first render action
                    for (int i = 0; i < batchActions.length(); i++) {
                        try {
                            JSONObject item = batchActions.getJSONObject(i);
                            String itemAction = item.optString("action", "");
                            // NATIVE_CALL has "type" but is NOT a render - exclude it
                            boolean isRender = !"NATIVE_CALL".equals(itemAction) && 
                                (item.has("type") || item.has("children") || "render".equals(itemAction));
                            if (isRender) {
                                firstRenderIndex = i;
                                break;
                            }
                        } catch (Exception e) {}
                    }
                    
                    Log.d(TAG, "BATCH: firstRenderIndex=" + firstRenderIndex);
                    
                    // Collect post-render actions to execute after layout
                    final List<JSONObject> postRenderActions = new ArrayList<>();
                    final int renderIdx = firstRenderIndex;
                    
                    // Process each action with appropriate timing
                    for (int i = 0; i < batchActions.length(); i++) {
                        try {
                            final JSONObject item = batchActions.getJSONObject(i);
                            final int index = i;
                            String itemAction = item.optString("action", "");
                            // NATIVE_CALL has "type" but is NOT a render - exclude it
                            boolean isRender = !"NATIVE_CALL".equals(itemAction) && 
                                (item.has("type") || item.has("children") || "render".equals(itemAction));
                            if ("REPLACE_CHILDREN".equals(itemAction)) {
                                Log.d(TAG, "BATCH[" + i + "] processing REPLACE_CHILDREN immediately");
                                m_mainHandler.post(() -> handleReplaceChildren(item));
                                continue;
                            }
                            if (renderIdx < 0) {
                                // No render in batch - process everything immediately
                                Log.d(TAG, "BATCH[" + i + "] immediate (no render in batch)");
                                processPhpResponse(item.toString());
                            } else if (i < renderIdx) {
                                // Before render - process immediately
                                Log.d(TAG, "BATCH[" + i + "] immediate (before render)");
                                processPhpResponse(item.toString());
                            } else if (isRender) {
                                // This is a render - process immediately and setup layout listener
                                Log.d(TAG, "BATCH[" + i + "] render with layout listener");
                                final String renderJson = item.toString();
                                m_mainHandler.post(() -> {
                                    renderUI(renderJson);
                                    // After render, use ViewTreeObserver to wait for layout
                                    if (m_mainRootView != null && !postRenderActions.isEmpty()) {
                                        executeAfterLayout(m_mainRootView, postRenderActions);
                                    }
                                });
                            } else {
                                // After render - queue for execution after layout
                                Log.d(TAG, "BATCH[" + i + "] queued for post-layout");
                                postRenderActions.add(item);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "BATCH: error processing item " + i, e);
                        }
                    }
                }
            }  else if ("render".equals(action) || response.has("type") || response.has("children")) {
                Log.d(TAG, ">>> ROUTING TO renderUI! action=" + action + " type=" + response.optString("type", "none"));
                m_mainHandler.post(() -> renderUI(jsonResponse));
            }

        } catch (Exception e) {
            Log.w(TAG, "Could not parse PHP response: " + e.getMessage());
        }
    }
    
    // Shared state file path
    private static final String VIEW_STATE_FILE = "view_state.json";
    
    // Properties to skip when syncing (internal Android properties, not useful for PHP)
   
    
    /**
     * Sync ALL registered view properties to a shared JSON file.
     * PHP can read this file to get view values synchronously.
     * 
     * THREADING: This method is safe to call from any thread.
     * - If called from UI thread: collects state and writes to file directly
     * - If called from background: marshals state collection to UI thread, waits, then writes
     */
    private void syncViewStateToFile() {
        try {
            final String stateJson;
            
            if (Looper.myLooper() == Looper.getMainLooper()) {
                // Already on UI thread - collect directly
                stateJson = collectViewStateJson();
            } else {
                // On background thread - collect state on UI thread synchronously
                final String[] stateHolder = new String[1];
                final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                
                m_mainHandler.post(() -> {
                    try {
                        stateHolder[0] = collectViewStateJson();
                    } finally {
                        latch.countDown();
                    }
                });
                
                // Wait for UI thread to complete collection (with timeout)
                if (!latch.await(1, TimeUnit.SECONDS)) {
                    Log.w(TAG, "Timeout waiting for UI thread to collect view state");
                    return;
                }
                stateJson = stateHolder[0];
            }
            
            if (stateJson == null) return;
            
            // Write to file (safe from any thread)
            writeViewStateToFile(stateJson);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to sync view state", e);
        }
    }
    
    /**
     * Collect view state as JSON string. MUST be called from UI thread.
     * Uses reflection to discover and sync all readable properties.
     * 
     * @return JSON string with all view states, or null on error
     */
    private String collectViewStateJson() {
        try {
            JSONObject state = new JSONObject();
            
            for (Map.Entry<String, View> entry : m_viewRegistry.entrySet()) {
                String viewId = entry.getKey();
                View view = entry.getValue();
                
                JSONObject viewState = extractAllProperties(view);
                
                // Add ListView-specific data
                if (view instanceof android.widget.ListView) {
                    List<String> listData = m_listData.get(viewId);
                    if (listData != null) {
                        viewState.put("items", new JSONArray(listData));
                        viewState.put("count", listData.size());
                    }
                    android.widget.ListView lv = (android.widget.ListView) view;
                    viewState.put("selectedPosition", lv.getCheckedItemPosition());
                }
                
                // Add DrawerLayout-specific state
                DrawerState drawerState = m_drawerStates.get(viewId);
                if (drawerState != null) {
                    viewState.put("isOpen", drawerState.isOpen);
                    viewState.put("drawerWidth", drawerState.drawerWidth);
                }
                
                // Add BottomNav-specific state
                BottomNavState bottomNavState = m_bottomNavStates.get(viewId);
                if (bottomNavState != null) {
                    viewState.put("selectedItemId", bottomNavState.selectedId);
                }
                
                state.put(viewId, viewState);
            }
            
            return state.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to collect view state", e);
            return null;
        }
    }
    
    /**
     * Write view state JSON to file. Safe to call from any thread.
     * 
     * @param stateJson JSON string to write
     */
    private void writeViewStateToFile(String stateJson) {
        try {
            String scriptDir = getPhpScriptDir();
            File stateFile = new File(scriptDir, VIEW_STATE_FILE);
            
            try (FileOutputStream fos = new FileOutputStream(stateFile)) {
                fos.write(stateJson.getBytes("UTF-8"));
            }
            
            Log.d(TAG, "Synced view state to: " + stateFile.getAbsolutePath() + " (" + m_viewRegistry.size() + " views)");
        } catch (Exception e) {
            Log.e(TAG, "Failed to write view state file", e);
        }
    }
    
    /**
     * Extract ALL readable properties from a view using reflection.
     * Scans for getter methods (get*, is*) and extracts their values.
     */
    private JSONObject extractAllProperties(View view) {
        JSONObject props = new JSONObject();
        if (view == null) return props;
        
        Set<String> processedProps = new HashSet<>();
        
        // Scan all methods in the view's class hierarchy
        for (Method method : view.getClass().getMethods()) {
            try {
                String methodName = method.getName();
                
                // Skip methods with parameters (not getters)
                if (method.getParameterCount() > 0) continue;
                
                // Skip void return type
                if (method.getReturnType() == void.class) continue;
                
                // Extract property name from getter method
                String propName = null;
                if (methodName.startsWith("get") && methodName.length() > 3) {
                    propName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
                } else if (methodName.startsWith("is") && methodName.length() > 2) {
                    propName = methodName.substring(2, 3).toLowerCase() + methodName.substring(3);
                }
                
                if (propName == null) continue;
                
                // Skip already processed and blacklisted properties
                if (processedProps.contains(propName)) continue;
                if (SKIP_PROPERTIES.contains(propName)) continue;
                
                processedProps.add(propName);
                
                // Get the value
                Object value = method.invoke(view);
                if (value == null) continue;
                
                // Convert to JSON-compatible type
                Object jsonValue = convertToJsonValue(value);
                if (jsonValue != null) {
                    props.put(propName, jsonValue);
                }
                
            } catch (Exception ignored) {
                // Skip methods that throw exceptions
            }
        }
        
        return props;
    }
    
    /**
     * Convert a value to a JSON-compatible type.
     * Returns null if the value cannot be represented in JSON.
     */                                                               ///////////////////////////////////////////////
    private Object convertToJsonValue(Object value) {
        if (value == null) return null;
        
        // Primitives and wrappers
        if (value instanceof Boolean || value instanceof Integer || 
            value instanceof Long || value instanceof Float || 
            value instanceof Double || value instanceof String) {
            return value;
        }
        
        // CharSequence (includes SpannableString, etc.)
        if (value instanceof CharSequence) {
            return value.toString();
        }
        
        // Arrays of primitives
        if (value instanceof int[]) {
            JSONArray arr = new JSONArray();
            for (int v : (int[]) value) arr.put(v);
            return arr;
        }
        if (value instanceof float[]) {
            JSONArray arr = new JSONArray();
            for (float v : (float[]) value) {
                try { arr.put((double) v); } catch (Exception ignored) {}
            }
            return arr;
        }
        
        // Enums - return name
        if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        }
        
        // Skip complex objects (Views, Drawables, etc.)
        return null;
    }

    /**
     * Get a specific property value from a view using reflection.
     * Tries getter patterns: getProperty(), property(), isProperty()
     */
    private Object getPropertyValue(View view, String property) {
        if (view == null || property == null) return null;
        
        String capProperty = property.substring(0, 1).toUpperCase() + property.substring(1);
        String[] methodNames = {
            "get" + capProperty,  // getUrl(), getText()
            property,              // url(), text()
            "is" + capProperty    // isChecked(), isEnabled()
        };
        
        for (String methodName : methodNames) {
            try {
                Method method = view.getClass().getMethod(methodName);
                Object result = method.invoke(view);
                return convertToJsonValue(result);
            } catch (NoSuchMethodException ignored) {
                // Try next pattern
            } catch (Exception e) {
                Log.w(TAG, "Error getting property " + property + ": " + e.getMessage());
            }
        }
        
        return null;
    }

    // -------------------------------------------------------------------------
    // DroidScript Sensor Injection (The "Puppeteer" Mechanism)
    // -------------------------------------------------------------------------

    private void injectSensorCall(String sensorType, String phpCallback) {
        if (phpCallback != null && !phpCallback.isEmpty()) {
            m_pendingSensorCallbacks.put(sensorType, phpCallback);
        }
        // Execute via JS native call handler registry
        try {
            ExecScript("_phpPlugin.NativeCall('" + escapeJs(sensorType) + "', '{}');");
            Log.d(TAG, "Native call dispatched: " + sensorType);
        } catch (Exception e) {
            Log.e(TAG, "Failed to dispatch native call: " + sensorType, e);
            notifyError("Native call failed: " + e.getMessage());
        }
    }

    /**
     * Escape a string for safe embedding in JavaScript string literals.
     * Handles backslashes, quotes, newlines, and Unicode line/paragraph separators
     * that can break out of JS strings.
     */
    private String escapeJs(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\'': sb.append("\\'"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\0': sb.append("\\0"); break;
                case '\u2028': sb.append("\\u2028"); break; // Line separator
                case '\u2029': sb.append("\\u2029"); break; // Paragraph separator
                case '<':
                    // Escape </script> to prevent breaking out of script tags
                    if (i + 1 < s.length() && s.charAt(i + 1) == '/') {
                        sb.append("<\\/");
                        i++; // Skip the /
                    } else {
                        sb.append(c);
                    }
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Sensor Result Callback (Called from DroidScript via injected JS)
    // -------------------------------------------------------------------------

    private void handleSensorResult(String sensorType, String dataJson) {
        Log.d(TAG, "Sensor result: " + sensorType + " = " + dataJson);

        // Get the PHP callback method for this sensor
        String phpCallback = m_pendingSensorCallbacks.remove(sensorType);
        if (phpCallback == null) {
            phpCallback = "handle_" + sensorType.toLowerCase();
        }

        // Call PHP with the sensor data
        final String finalCallback = phpCallback;
        m_executor.execute(() -> {
            String response = callPhp(finalCallback, dataJson);
            
            // Process the response for UI updates or actions
            if (response != null && !response.isEmpty()) {
                m_mainHandler.post(() -> processPhpResponse(response));
            }
            
            // Notify JS if callback is set
            if (m_OnSensorResult != null) {
                m_mainHandler.post(() -> {
                    try {
                        Bundle b = new Bundle();
                        b.putString("cmd", m_OnSensorResult);
                        b.putString("p1", sensorType);
                        b.putString("p2", response);
                        CallScript(b);
                    } catch (Exception e) {
                        Log.e(TAG, "Error notifying sensor result", e);
                    }
                });
            }
        });
    }

    /**
     * Handle result from dsCall() - route to PHP callback
     * @param callback PHP method name to call
     * @param resultJson JSON result from DroidScript code
     */
    private void handleDsCallResult(String callback, String resultJson) {
        debugLog("DS call result: callback=" + callback + ", result=" + resultJson);
        
        if (callback == null || callback.isEmpty()) {
            return;
        }
        
        // Call PHP with the result
        m_executor.execute(() -> {
            String response = callPhp(callback, resultJson);
            
            // Process the response for UI updates or actions
            if (response != null && !response.isEmpty()) {
                m_mainHandler.post(() -> processPhpResponse(response));
            }
        });
    }

    // -------------------------------------------------------------------------
    // Native Call Infrastructure
    // -------------------------------------------------------------------------
    
    /**
     * Initialize native Android systems (SensorManager, LocationManager, etc.)
     */
    private void initNativeSystems() {
        m_sensorManager = (SensorManager) m_ctx.getSystemService(Context.SENSOR_SERVICE);
        m_locationManager = (LocationManager) m_ctx.getSystemService(Context.LOCATION_SERVICE);
        m_cameraManager = (CameraManager) m_ctx.getSystemService(Context.CAMERA_SERVICE);
        
        // Initialize TTS
        m_tts = new TextToSpeech(m_ctx, status -> {
            if (status == TextToSpeech.SUCCESS) {
                m_tts.setLanguage(Locale.getDefault());
                m_ttsReady = true;
                Log.d(TAG, "TTS initialized");
            }
        });
    }
    
    /**
     * Report native call result back to PHP via callback
     */
    private void reportNativeResult(String type, JSONObject data) {
        handleSensorResult(type, data.toString());
    }
    
    /**
     * Create error JSON object
     */
    private JSONObject createErrorJson(String message) {
        JSONObject json = new JSONObject();
        try {
            json.put("error", message);
        } catch (Exception e) {}
        return json;
    }
    
    /**
     * Check if permission is granted
     */
    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT >= 23) {
            return m_ctx.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Pre-Marshmallow: permissions granted at install time
    }
    
    /**
     * Request permissions and run callback on grant
     */
    private void requestPermission(String[] permissions, Runnable onGranted, Runnable onDenied) {
        // Check if all permissions already granted
        boolean allGranted = true;
        for (String perm : permissions) {
            if (!hasPermission(perm)) {
                allGranted = false;
                break;
            }
        }
        
        if (allGranted) {
            if (onGranted != null) onGranted.run();
            return;
        }
        
        // Request permissions - store callback for onRequestPermissionsResult
        int requestId = ++m_permissionRequestId;
        m_permissionCallbacks.put(requestId, onGranted);
        if (Build.VERSION.SDK_INT >= 23) {
            m_activity.requestPermissions(permissions, PERMISSION_REQUEST_CODE + requestId);
        } else if (onGranted != null) {
            onGranted.run(); // Pre-Marshmallow: permissions already granted
        }
    }
    
    // -------------------------------------------------------------------------
    // Native Handler Registration
    // -------------------------------------------------------------------------
    
    /**
     * Register all built-in native handlers for sensors, device APIs, etc.
     * These run pure Java code without DroidScript.
     */
    /**
     * Registers all native handler functions that can be called from PHP/JavaScript.
     * 
     * This comprehensive method sets up handlers for various Android device capabilities
     * and features, organized into logical categories:
     * 
     * MOTION SENSORS:
     * - accelerometer: Reads accelerometer x, y, z values
     * - gyroscope: Reads gyroscope rotation values in x, y, z axes
     * - gravity: Reads gravity sensor data
     * - magneticfield: Reads magnetic field sensor values
     * - compass/orientation: Calculates azimuth, pitch, roll from rotation vector
     * 
     * ENVIRONMENT SENSORS:
     * - light: Reads ambient light level in lux
     * - proximity: Detects object proximity distance and near/far state
     * - pressure: Reads atmospheric pressure in hPa
     * - humidity: Reads relative humidity percentage
     * - temperature: Reads ambient temperature in Celsius
     * - stepcounter: Counts steps (requires API 29+ with ACTIVITY_RECOGNITION permission)
     * 
     * LOCATION/GPS:
     * - location/gps: Requests fine location permission and starts continuous location updates
     * - lastlocation: Returns last known GPS or network location
     * - locationenabled: Checks if GPS and network location providers are enabled
     * - geocode: Converts address string to latitude/longitude coordinates
     * - reversegeocode: Converts latitude/longitude to address, city, country, postal code
     * 
     * BATTERY & POWER:
     * - battery: Returns battery level, percentage, charging status, health, and temperature
     * - powersavemode: Checks if power save mode is currently enabled
     * 
     * DEVICE & SCREEN INFO:
     * - deviceinfo: Returns device model, manufacturer, brand, OS version, build fingerprint
     * - screeninfo: Returns screen dimensions, density, and DPI information
     * 
     * NETWORK - WiFi & Bluetooth:
     * - wifi: Returns WiFi enabled status, connected SSID, BSSID, signal strength (RSSI), link speed, and IP address
     * - wifiscan: Scans for available WiFi networks with SSID, BSSID, signal level, and frequency
     * - bluetooth: Returns Bluetooth availability, enabled status, device name, address, and paired devices list
     * - networkinfo: Checks network connectivity status, type (WiFi/cellular), metering, and VPN usage
     * 
     * HTTP REQUESTS:
     * - http: Performs HTTP requests (GET, POST, PUT, PATCH) with custom headers, body, and timeout
     * - download: Downloads files via DownloadManager with progress notifications
     * 
     * AUDIO - MediaPlayer, Recording, Volume:
     * - playaudio: Plays audio file with status callbacks for playing and completion
     * - pauseaudio: Pauses currently playing audio
     * - stopaudio: Stops audio playback and releases resources
     * - recordaudio: Records audio to M4A format with microphone permission
     * - stoprecording: Stops audio recording
     * - getvolume: Returns current volume levels for music, ring, alarm, and notification streams
     * - setvolume: Sets volume level for specified audio stream
     * - setringermode: Changes ringer mode (normal, silent, vibrate)
     * 
     * TEXT-TO-SPEECH & SPEECH RECOGNITION:
     * - tts/speech: Converts text to speech with completion callbacks
     * - speechrecognition: Starts voice recognition with results and alternatives
     * 
     * COMMUNICATION - SMS, Phone, Email:
     * - sendsms: Sends SMS message to phone number (requires SEND_SMS permission)
     * - phonecall: Initiates phone call to number (requires CALL_PHONE permission)
     * - opendial: Opens phone dialer with pre-filled number
     * - sendemail: Opens email composer with recipient, subject, and body
     * 
     * SYSTEM - Clipboard, Vibration, Flashlight, Notifications:
     * - clipboard_get: Reads text from system clipboard
     * - clipboard_set: Writes text to system clipboard
     * - vibrate: Vibrates device with duration or custom pattern (API 26+ support)
     * - flashlight: Controls device flashlight/torch (requires API 23+)
     * - notification: Shows system notification with title and message
     * - cancelnotification: Cancels notification by ID
     * - keepscreenon: Prevents screen from turning off
     * - setbrightness: Sets screen brightness level (0.0 to 1.0)
     * 
     * FILE SYSTEM:
     * - readfile: Reads file content as text with size and modification metadata
     * - writefile: Writes or appends text to file, creating parent directories as needed
     * - deletefile: Deletes file from filesystem
     * - fileexists: Checks file existence, type (file/directory), size, and modification time
     * - listdir: Lists directory contents with file info (name, path, type, size, modified)
     * - mkdir: Creates directory tree
     * - zipfile: Compresses single file to ZIP archive
     * - zipfolder: Compresses entire directory tree to ZIP archive
     * - unzip: Extracts ZIP archive to specified directory with file list
     * 
     * INTENTS - Open Apps, URLs, Share:
     * - openapp: Launches app by package name via intent
     * - openurl: Opens URL in browser
     * - opensettings: Opens system settings (WiFi, Bluetooth, Location, App Details, or General)
     * - share: Opens share dialog with text and subject
     * - sendintent: Sends custom intent with action, data, type, and extras
     * 
     * CRYPTO - Hash, Encrypt, Decrypt:
     * - hash: Generates hash (SHA-256 default) of text
     * - encrypt: Encrypts text with AES-256-CBC using password-derived key
     * - decrypt: Decrypts AES-256-CBC encrypted text
     * - base64encode: Encodes text to Base64
     * - base64decode: Decodes Base64 text
     * - randombytes: Generates cryptographically secure random bytes
     * 
     * CAMERA - Take Photo, Record Video, Pick Image:
     * - takephoto: Launches camera to capture photo
     * - recordvideo: Launches camera to record video with optional duration limit
     * - pickimage: Opens gallery to select image
     * - pickvideo: Opens gallery to select video
     * 
     * Each handler stores results via reportNativeResult() callback with corresponding handler name.
     * Permission-required handlers use requestPermission() with success/failure callbacks.
     * Sensor handlers use continuous event listeners via readSensor() method.
     */
    private void registerAllNativeHandlers() {
        
        // =================================================================
        // MOTION SENSORS
        // =================================================================
        
        m_nativeHandlers.put("accelerometer", (params, callback) -> {
            readSensor(Sensor.TYPE_ACCELEROMETER, "accelerometer", event -> {
                JSONObject data = new JSONObject();
                try {
                    data.put("x", event.values[0]);
                    data.put("y", event.values[1]);
                    data.put("z", event.values[2]);
                } catch (Exception e) {}
                return data;
            });
        });
        
        m_nativeHandlers.put("gyroscope", (params, callback) -> {
            readSensor(Sensor.TYPE_GYROSCOPE, "gyroscope", event -> {
                JSONObject data = new JSONObject();
                try {
                    data.put("x", event.values[0]);
                    data.put("y", event.values[1]);
                    data.put("z", event.values[2]);
                } catch (Exception e) {}
                return data;
            });
        });
        
        m_nativeHandlers.put("gravity", (params, callback) -> {
            readSensor(Sensor.TYPE_GRAVITY, "gravity", event -> {
                JSONObject data = new JSONObject();
                try {
                    data.put("x", event.values[0]);
                    data.put("y", event.values[1]);
                    data.put("z", event.values[2]);
                } catch (Exception e) {}
                return data;
            });
        });
        
        m_nativeHandlers.put("magneticfield", (params, callback) -> {
            readSensor(Sensor.TYPE_MAGNETIC_FIELD, "magneticfield", event -> {
                JSONObject data = new JSONObject();
                try {
                    data.put("x", event.values[0]);
                    data.put("y", event.values[1]);
                    data.put("z", event.values[2]);
                } catch (Exception e) {}
                return data;
            });
        });
        
        m_nativeHandlers.put("compass", (params, callback) -> {
            readSensor(Sensor.TYPE_ROTATION_VECTOR, "compass", event -> {
                JSONObject data = new JSONObject();
                try {
                    float[] rotationMatrix = new float[9];
                    float[] orientation = new float[3];
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                    SensorManager.getOrientation(rotationMatrix, orientation);
                    data.put("azimuth", Math.toDegrees(orientation[0]));
                    data.put("pitch", Math.toDegrees(orientation[1]));
                    data.put("roll", Math.toDegrees(orientation[2]));
                } catch (Exception e) {}
                return data;
            });
        });
        m_nativeHandlers.put("orientation", m_nativeHandlers.get("compass"));
        
        // =================================================================
        // ENVIRONMENT SENSORS
        // =================================================================
        
        m_nativeHandlers.put("light", (params, callback) -> {
            readSensor(Sensor.TYPE_LIGHT, "light", event -> {
                JSONObject data = new JSONObject();
                try { data.put("lux", event.values[0]); } catch (Exception e) {}
                return data;
            });
        });
        
        m_nativeHandlers.put("proximity", (params, callback) -> {
            readSensor(Sensor.TYPE_PROXIMITY, "proximity", event -> {
                JSONObject data = new JSONObject();
                try {
                    float distance = event.values[0];
                    data.put("distance", distance);
                    data.put("near", distance < 5);
                } catch (Exception e) {}
                return data;
            });
        });
        
        m_nativeHandlers.put("pressure", (params, callback) -> {
            readSensor(Sensor.TYPE_PRESSURE, "pressure", event -> {
                JSONObject data = new JSONObject();
                try { data.put("hPa", event.values[0]); } catch (Exception e) {}
                return data;
            });
        });
        
        m_nativeHandlers.put("humidity", (params, callback) -> {
            readSensor(Sensor.TYPE_RELATIVE_HUMIDITY, "humidity", event -> {
                JSONObject data = new JSONObject();
                try { data.put("percent", event.values[0]); } catch (Exception e) {}
                return data;
            });
        });
        
        m_nativeHandlers.put("temperature", (params, callback) -> {
            readSensor(Sensor.TYPE_AMBIENT_TEMPERATURE, "temperature", event -> {
                JSONObject data = new JSONObject();
                try { data.put("celsius", event.values[0]); } catch (Exception e) {}
                return data;
            });
        });
        
        m_nativeHandlers.put("stepcounter", (params, callback) -> {
            if (Build.VERSION.SDK_INT >= 29 && !hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)) {
                requestPermission(new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                    () -> readStepCounter("stepcounter"), null);
            } else {
                readStepCounter("stepcounter");
            }
        });
        
        // =================================================================
        // LOCATION / GPS
        // =================================================================
        
        m_nativeHandlers.put("location", (params, callback) -> {
            requestPermission(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                () -> readLocation("location"), 
                () -> reportNativeResult("location", createErrorJson("Location permission denied")));
        });
        m_nativeHandlers.put("gps", m_nativeHandlers.get("location"));
        
        m_nativeHandlers.put("lastlocation", (params, callback) -> {
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                reportNativeResult("lastlocation", createErrorJson("Location permission required"));
                return;
            }
            try {
                Location loc = m_locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc == null) loc = m_locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (loc != null) {
                    JSONObject data = locationToJson(loc);
                    reportNativeResult("lastlocation", data);
                } else {
                    reportNativeResult("lastlocation", createErrorJson("No last known location"));
                }
            } catch (SecurityException e) {
                reportNativeResult("lastlocation", createErrorJson(e.getMessage()));
            }
        });
        
        m_nativeHandlers.put("locationenabled", (params, callback) -> {
            JSONObject data = new JSONObject();
            try {
                data.put("gps", m_locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
                data.put("network", m_locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
            } catch (Exception e) {}
            reportNativeResult("locationenabled", data);
        });
        
        m_nativeHandlers.put("geocode", (params, callback) -> {
            String address = params.optString("address", "");
            if (address.isEmpty()) {
                reportNativeResult("geocode", createErrorJson("Address required"));
                return;
            }
            try {
                Geocoder geocoder = new Geocoder(m_ctx, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(address, 1);
                JSONObject data = new JSONObject();
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    data.put("lat", addr.getLatitude());
                    data.put("lng", addr.getLongitude());
                    data.put("address", addr.getAddressLine(0));
                } else {
                    data.put("error", "Address not found");
                }
                reportNativeResult("geocode", data);
            } catch (Exception e) {
                reportNativeResult("geocode", createErrorJson(e.getMessage()));
            }
        });
        
        m_nativeHandlers.put("reversegeocode", (params, callback) -> {
            double lat = params.optDouble("lat", 0);
            double lng = params.optDouble("lng", 0);
            try {
                Geocoder geocoder = new Geocoder(m_ctx, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                JSONObject data = new JSONObject();
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    data.put("address", addr.getAddressLine(0));
                    data.put("city", addr.getLocality());
                    data.put("country", addr.getCountryName());
                    data.put("postalCode", addr.getPostalCode());
                } else {
                    data.put("error", "Location not found");
                }
                reportNativeResult("reversegeocode", data);
            } catch (Exception e) {
                reportNativeResult("reversegeocode", createErrorJson(e.getMessage()));
            }
        });
        
        // =================================================================
        // BATTERY & POWER
        // =================================================================
        
        m_nativeHandlers.put("battery", (params, callback) -> {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = m_ctx.registerReceiver(null, filter);
            JSONObject data = new JSONObject();
            try {
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
                int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                int temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                
                data.put("level", level);
                data.put("scale", scale);
                data.put("percent", level * 100 / (float) scale);
                data.put("charging", status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);
                data.put("status", batteryStatusToString(status));
                data.put("health", batteryHealthToString(health));
                data.put("plugged", plugged == BatteryManager.BATTERY_PLUGGED_USB ? "usb" : plugged == BatteryManager.BATTERY_PLUGGED_AC ? "ac" : "none");
                data.put("temperature", temperature / 10.0); // tenths of degree Celsius
            } catch (Exception e) {}
            reportNativeResult("battery", data);
        });
        
        m_nativeHandlers.put("powersavemode", (params, callback) -> {
            PowerManager pm = (PowerManager) m_ctx.getSystemService(Context.POWER_SERVICE);
            JSONObject data = new JSONObject();
            try {
                data.put("enabled", pm.isPowerSaveMode());
            } catch (Exception e) {}
            reportNativeResult("powersavemode", data);
        });
        
        // =================================================================
        // DEVICE & SCREEN INFO
        // =================================================================
        
        m_nativeHandlers.put("deviceinfo", (params, callback) -> {
            JSONObject data = new JSONObject();
            try {
                data.put("model", Build.MODEL);
                data.put("manufacturer", Build.MANUFACTURER);
                data.put("brand", Build.BRAND);
                data.put("device", Build.DEVICE);
                data.put("product", Build.PRODUCT);
                data.put("sdk", Build.VERSION.SDK_INT);
                data.put("osVersion", Build.VERSION.RELEASE);
                data.put("id", Build.ID);
                data.put("fingerprint", Build.FINGERPRINT);
            } catch (Exception e) {}
            reportNativeResult("deviceinfo", data);
        });
        
        m_nativeHandlers.put("screeninfo", (params, callback) -> {
            DisplayMetrics metrics = new DisplayMetrics();
            ((WindowManager) m_ctx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
            JSONObject data = new JSONObject();
            try {
                data.put("width", metrics.widthPixels);
                data.put("height", metrics.heightPixels);
                data.put("density", metrics.density);
                data.put("densityDpi", metrics.densityDpi);
                data.put("scaledDensity", metrics.scaledDensity);
            } catch (Exception e) {}
            reportNativeResult("screeninfo", data);
        });
        
        // =================================================================
        // NETWORK - WiFi & Bluetooth
        // =================================================================
        
        m_nativeHandlers.put("wifi", (params, callback) -> {
            WifiManager wm = (WifiManager) m_ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            JSONObject data = new JSONObject();
            try {
                data.put("enabled", wm.isWifiEnabled());
                WifiInfo info = wm.getConnectionInfo();
                if (info != null) {
                    data.put("ssid", info.getSSID());
                    data.put("bssid", info.getBSSID());
                    data.put("rssi", info.getRssi());
                    data.put("linkSpeed", info.getLinkSpeed());
                    int ip = info.getIpAddress();
                    data.put("ip", String.format("%d.%d.%d.%d", ip & 0xff, (ip >> 8) & 0xff, (ip >> 16) & 0xff, (ip >> 24) & 0xff));
                }
            } catch (Exception e) {}
            reportNativeResult("wifi", data);
        });
        
        m_nativeHandlers.put("wifiscan", (params, callback) -> {
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                reportNativeResult("wifiscan", createErrorJson("Location permission required for WiFi scan"));
                return;
            }
            WifiManager wm = (WifiManager) m_ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            List<ScanResult> results = wm.getScanResults();
            JSONObject data = new JSONObject();
            try {
                JSONArray networks = new JSONArray();
                for (ScanResult r : results) {
                    JSONObject net = new JSONObject();
                    net.put("ssid", r.SSID);
                    net.put("bssid", r.BSSID);
                    net.put("rssi", r.level);
                    net.put("frequency", r.frequency);
                    networks.put(net);
                }
                data.put("networks", networks);
            } catch (Exception e) {}
            reportNativeResult("wifiscan", data);
        });
        
        m_nativeHandlers.put("bluetooth", (params, callback) -> {
            BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
            JSONObject data = new JSONObject();
            try {
                if (bt == null) {
                    data.put("available", false);
                } else {
                    data.put("available", true);
                    data.put("enabled", bt.isEnabled());
                    data.put("name", bt.getName());
                    data.put("address", bt.getAddress());
                    JSONArray paired = new JSONArray();
                    if (Build.VERSION.SDK_INT < 31 || hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                        for (BluetoothDevice device : bt.getBondedDevices()) {
                            JSONObject d = new JSONObject();
                            d.put("name", device.getName());
                            d.put("address", device.getAddress());
                            paired.put(d);
                        }
                    }
                    data.put("paired", paired);
                }
            } catch (Exception e) {}
            reportNativeResult("bluetooth", data);
        });
        
        m_nativeHandlers.put("networkinfo", (params, callback) -> {
            ConnectivityManager cm = (ConnectivityManager) m_ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            JSONObject data = new JSONObject();
            try {
                Network network = cm.getActiveNetwork();
                NetworkCapabilities caps = cm.getNetworkCapabilities(network);
                if (caps != null) {
                    data.put("connected", true);
                    data.put("wifi", caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
                    data.put("cellular", caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
                    data.put("metered", !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED));
                    data.put("vpn", caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
                } else {
                    data.put("connected", false);
                }
            } catch (Exception e) {}
            reportNativeResult("networkinfo", data);
        });
        
        // =================================================================
        // HTTP REQUESTS
        // =================================================================
        
        m_nativeHandlers.put("http", (params, callback) -> {
            String method = params.optString("method", "GET");
            String urlStr = params.optString("url", "");
            String body = params.optString("body", "");
            JSONObject headers = params.optJSONObject("headers");
            int timeout = params.optInt("timeout", 30000);
            
            new Thread(() -> {
                JSONObject data = new JSONObject();
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(urlStr);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod(method);
                    conn.setConnectTimeout(timeout);
                    conn.setReadTimeout(timeout);
                    
                    // Set headers
                    if (headers != null) {
                        Iterator<String> keys = headers.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            conn.setRequestProperty(key, headers.optString(key));
                        }
                    }
                    
                    // Write body for POST/PUT/PATCH
                    if (!body.isEmpty() && (method.equals("POST") || method.equals("PUT") || method.equals("PATCH"))) {
                        conn.setDoOutput(true);
                        try (OutputStream os = conn.getOutputStream()) {
                            os.write(body.getBytes("UTF-8"));
                        }
                    }
                    
                    int status = conn.getResponseCode();
                    data.put("status", status);
                    
                    // Read response - use ByteArrayOutputStream to preserve binary and newlines
                    InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
                    if (is != null) {
                        try (InputStream responseStream = is;
                             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = responseStream.read(buffer)) != -1) {
                                baos.write(buffer, 0, len);
                            }
                            // Convert to string - assumes UTF-8 for text responses
                            data.put("body", baos.toString("UTF-8"));
                        }
                    } else {
                        data.put("body", "");
                    }
                    
                    // Response headers - store all values for multi-value headers (e.g., Set-Cookie)
                    JSONObject respHeaders = new JSONObject();
                    for (Map.Entry<String, List<String>> entry : conn.getHeaderFields().entrySet()) {
                        if (entry.getKey() != null) {
                            List<String> values = entry.getValue();
                            if (values.size() == 1) {
                                respHeaders.put(entry.getKey(), values.get(0));
                            } else {
                                respHeaders.put(entry.getKey(), new JSONArray(values));
                            }
                        }
                    }
                    data.put("headers", respHeaders);
                    
                } catch (Exception e) {
                    try { 
                        data.put("error", e.getMessage()); 
                        data.put("errorType", e.getClass().getSimpleName());
                    } catch (Exception ex) {
                        Log.e(TAG, "Failed to set error in HTTP response", ex);
                    }
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
                reportNativeResult("http", data);
            }).start();
        });
        
        m_nativeHandlers.put("download", (params, callback) -> {
            String urlStr = params.optString("url", "");
            String destPath = params.optString("dest", "");
            String title = params.optString("title", "Download");
            
            if (destPath.isEmpty()) {
                destPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() 
                    + "/" + UUID.randomUUID().toString();
            }
            
            try {
                DownloadManager dm = (DownloadManager) m_ctx.getSystemService(Context.DOWNLOAD_SERVICE);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(urlStr));
                request.setTitle(title);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationUri(Uri.fromFile(new File(destPath)));
                long downloadId = dm.enqueue(request);
                
                JSONObject data = new JSONObject();
                data.put("downloadId", downloadId);
                data.put("dest", destPath);
                reportNativeResult("download", data);
            } catch (Exception e) {
                reportNativeResult("download", createErrorJson(e.getMessage()));
            }
        });
        
        // =================================================================
        // AUDIO - MediaPlayer, Recording, Volume
        // =================================================================
        
        m_nativeHandlers.put("playaudio", (params, callback) -> {
            String file = params.optString("file", "");
            if (file.isEmpty()) {
                reportNativeResult("playaudio", createErrorJson("File required"));
                return;
            }
            m_mainHandler.post(() -> {
                try {
                    if (m_mediaPlayer != null) {
                        m_mediaPlayer.release();
                    }
                    m_mediaPlayer = new MediaPlayer();
                    m_mediaPlayer.setAudioAttributes(
                        new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    );
                    
                    // Handle both file paths and content:// URIs
                    if (file.startsWith("content://") || file.startsWith("file://")) {
                        m_mediaPlayer.setDataSource(m_ctx, Uri.parse(file));
                    } else {
                        m_mediaPlayer.setDataSource(file);
                    }
                    
                    m_mediaPlayer.setOnPreparedListener(mp -> {
                        mp.start();
                        JSONObject data = new JSONObject();
                        try {
                            data.put("status", "playing");
                            data.put("duration", mp.getDuration());
                        } catch (Exception e) {}
                        reportNativeResult("playaudio", data);
                    });
                    m_mediaPlayer.setOnCompletionListener(mp -> {
                        JSONObject data = new JSONObject();
                        try { data.put("status", "complete"); } catch (Exception e) {}
                        reportNativeResult("playaudio", data);
                    });
                    m_mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                        reportNativeResult("playaudio", createErrorJson("Playback error: " + what + "/" + extra));
                        return true;
                    });
                    m_mediaPlayer.prepareAsync();
                } catch (Exception e) {
                    reportNativeResult("playaudio", createErrorJson(e.getMessage()));
                }
            });
        });
        
        m_nativeHandlers.put("pauseaudio", (params, callback) -> {
            m_mainHandler.post(() -> {
                JSONObject data = new JSONObject();
                try {
                    if (m_mediaPlayer != null && m_mediaPlayer.isPlaying()) {
                        m_mediaPlayer.pause();
                        data.put("status", "paused");
                        data.put("position", m_mediaPlayer.getCurrentPosition());
                    } else {
                        data.put("status", "not_playing");
                    }
                } catch (Exception e) {
                    data = createErrorJson(e.getMessage());
                }
                reportNativeResult("pauseaudio", data);
            });
        });
        
        m_nativeHandlers.put("resumeaudio", (params, callback) -> {
            m_mainHandler.post(() -> {
                JSONObject data = new JSONObject();
                try {
                    if (m_mediaPlayer != null && !m_mediaPlayer.isPlaying()) {
                        m_mediaPlayer.start();
                        data.put("status", "playing");
                        data.put("position", m_mediaPlayer.getCurrentPosition());
                        data.put("duration", m_mediaPlayer.getDuration());
                    } else if (m_mediaPlayer != null && m_mediaPlayer.isPlaying()) {
                        data.put("status", "already_playing");
                    } else {
                        data.put("status", "no_player");
                    }
                } catch (Exception e) {
                    data = createErrorJson(e.getMessage());
                }
                reportNativeResult("resumeaudio", data);
            });
        });
        
        m_nativeHandlers.put("stopaudio", (params, callback) -> {
            m_mainHandler.post(() -> {
                JSONObject data = new JSONObject();
                try {
                    if (m_mediaPlayer != null) {
                        m_mediaPlayer.stop();
                        m_mediaPlayer.release();
                        m_mediaPlayer = null;
                        data.put("status", "stopped");
                    } else {
                        data.put("status", "no_player");
                    }
                } catch (Exception e) {
                    data = createErrorJson(e.getMessage());
                }
                reportNativeResult("stopaudio", data);
            });
        });
        
        m_nativeHandlers.put("seekaudio", (params, callback) -> {
            int position = params.optInt("position", 0);
            m_mainHandler.post(() -> {
                JSONObject data = new JSONObject();
                try {
                    if (m_mediaPlayer != null) {
                        m_mediaPlayer.seekTo(position);
                        data.put("status", "seeked");
                        data.put("position", position);
                    } else {
                        data.put("status", "no_player");
                    }
                } catch (Exception e) {
                    data = createErrorJson(e.getMessage());
                }
                reportNativeResult("seekaudio", data);
            });
        });
        
        m_nativeHandlers.put("getaudioposition", (params, callback) -> {
            m_mainHandler.post(() -> {
                JSONObject data = new JSONObject();
                try {
                    if (m_mediaPlayer != null) {
                        data.put("position", m_mediaPlayer.getCurrentPosition());
                        data.put("duration", m_mediaPlayer.getDuration());
                        data.put("playing", m_mediaPlayer.isPlaying());
                    } else {
                        data.put("status", "no_player");
                    }
                } catch (Exception e) {
                    data = createErrorJson(e.getMessage());
                }
                reportNativeResult("getaudioposition", data);
            });
        });
        
        m_nativeHandlers.put("recordaudio", (params, callback) -> {
            final String requestedFile = params.optString("file", "");
            debugLog("RECORDAUDIO: handler called, requestedFile=" + requestedFile);
            
            requestPermission(new String[]{Manifest.permission.RECORD_AUDIO}, () -> {
                debugLog("RECORDAUDIO: permission granted, starting recording setup");
                try {
                    // Generate file path inside callback to avoid timing issues
                    String file = requestedFile;
                    if (file.isEmpty()) {
                        String basePath = (m_appDir != null) ? m_appDir : m_filesDir;
                        file = basePath + "/recording_" + System.currentTimeMillis() + ".m4a";
                    }
                    debugLog("RECORDAUDIO: file path = " + file);
                    
                    if (m_mediaRecorder != null) {
                        debugLog("RECORDAUDIO: releasing existing recorder");
                        m_mediaRecorder.release();
                        m_mediaRecorder = null;
                    }
                    m_mediaRecorder = new MediaRecorder();
                    m_mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    m_mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                    m_mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                    m_mediaRecorder.setOutputFile(file);
                    m_mediaRecorder.prepare();
                    m_mediaRecorder.start();
                    debugLog("RECORDAUDIO: started recording to " + file);
                    
                    JSONObject data = new JSONObject();
                    data.put("status", "recording");
                    data.put("file", file);
                    reportNativeResult("recordaudio", data);
                } catch (Exception e) {
                    reportNativeResult("recordaudio", createErrorJson(e.getMessage()));
                }
            }, () -> reportNativeResult("recordaudio", createErrorJson("Microphone permission denied")));
        });
        
        m_nativeHandlers.put("stoprecording", (params, callback) -> {
            JSONObject data = new JSONObject();
            try {
                if (m_mediaRecorder != null) {
                    m_mediaRecorder.stop();
                    m_mediaRecorder.release();
                    m_mediaRecorder = null;
                    data.put("status", "stopped");
                } else {
                    data.put("status", "no_recorder");
                }
            } catch (Exception e) {
                data = createErrorJson(e.getMessage());
            }
            reportNativeResult("stoprecording", data);
        });
        
        m_nativeHandlers.put("getvolume", (params, callback) -> {
            AudioManager am = (AudioManager) m_ctx.getSystemService(Context.AUDIO_SERVICE);
            JSONObject data = new JSONObject();
            try {
                data.put("music", am.getStreamVolume(AudioManager.STREAM_MUSIC));
                data.put("musicMax", am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                data.put("ring", am.getStreamVolume(AudioManager.STREAM_RING));
                data.put("ringMax", am.getStreamMaxVolume(AudioManager.STREAM_RING));
                data.put("alarm", am.getStreamVolume(AudioManager.STREAM_ALARM));
                data.put("notification", am.getStreamVolume(AudioManager.STREAM_NOTIFICATION));
            } catch (Exception e) {}
            reportNativeResult("getvolume", data);
        });
        
        m_nativeHandlers.put("setvolume", (params, callback) -> {
            AudioManager am = (AudioManager) m_ctx.getSystemService(Context.AUDIO_SERVICE);
            String stream = params.optString("stream", "music");
            int level = params.optInt("level", -1);
            int streamType = stream.equals("ring") ? AudioManager.STREAM_RING :
                            stream.equals("alarm") ? AudioManager.STREAM_ALARM :
                            stream.equals("notification") ? AudioManager.STREAM_NOTIFICATION : AudioManager.STREAM_MUSIC;
            
            JSONObject data = new JSONObject();
            try {
                if (level >= 0) {
                    am.setStreamVolume(streamType, level, 0);
                    data.put("done", true);
                    data.put("level", level);
                } else {
                    data.put("error", "Invalid level");
                }
            } catch (Exception e) {
                data = createErrorJson(e.getMessage());
            }
            reportNativeResult("setvolume", data);
        });
        
        m_nativeHandlers.put("setringermode", (params, callback) -> {
            AudioManager am = (AudioManager) m_ctx.getSystemService(Context.AUDIO_SERVICE);
            String mode = params.optString("mode", "normal");
            int ringerMode = mode.equals("silent") ? AudioManager.RINGER_MODE_SILENT :
                            mode.equals("vibrate") ? AudioManager.RINGER_MODE_VIBRATE : AudioManager.RINGER_MODE_NORMAL;
            JSONObject data = new JSONObject();
            try {
                am.setRingerMode(ringerMode);
                data.put("done", true);
                data.put("mode", mode);
            } catch (Exception e) {
                data = createErrorJson(e.getMessage());
            }
            reportNativeResult("setringermode", data);
        });
        
        // =================================================================
        // TEXT-TO-SPEECH & SPEECH RECOGNITION
        // =================================================================
        
        m_nativeHandlers.put("tts", (params, callback) -> {
            m_nativeHandlers.get("speech").handle(params, callback);
        });
        
        m_nativeHandlers.put("speech", (params, callback) -> {
            String text = params.optString("text", "");
            if (text.isEmpty()) {
                reportNativeResult("speech", createErrorJson("Text required"));
                return;
            }
            if (!m_ttsReady) {
                reportNativeResult("speech", createErrorJson("TTS not ready"));
                return;
            }
            
            String utteranceId = UUID.randomUUID().toString();
            m_tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) {}
                @Override public void onDone(String id) {
                    JSONObject data = new JSONObject();
                    try { data.put("done", true); } catch (Exception e) {}
                    reportNativeResult("speech", data);
                }
                @Override public void onError(String id) {
                    reportNativeResult("speech", createErrorJson("TTS error"));
                }
            });
            m_tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        });
        
        m_nativeHandlers.put("speechrecognition", (params, callback) -> {
            requestPermission(new String[]{Manifest.permission.RECORD_AUDIO}, () -> {
                m_mainHandler.post(() -> {
                    try {
                        if (m_speechRecognizer != null) {
                            m_speechRecognizer.destroy();
                        }
                        m_speechRecognizer = SpeechRecognizer.createSpeechRecognizer(m_ctx);
                        m_speechRecognizer.setRecognitionListener(new RecognitionListener() {
                            @Override public void onReadyForSpeech(Bundle params) {}
                            @Override public void onBeginningOfSpeech() {}
                            @Override public void onRmsChanged(float rmsdB) {}
                            @Override public void onBufferReceived(byte[] buffer) {}
                            @Override public void onEndOfSpeech() {}
                            @Override public void onError(int error) {
                                reportNativeResult("speechrecognition", createErrorJson("Recognition error: " + error));
                            }
                            @Override public void onResults(Bundle results) {
                                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                                JSONObject data = new JSONObject();
                                try {
                                    data.put("text", matches != null && !matches.isEmpty() ? matches.get(0) : "");
                                    JSONArray alternatives = new JSONArray();
                                    if (matches != null) {
                                        for (String m : matches) alternatives.put(m);
                                    }
                                    data.put("alternatives", alternatives);
                                } catch (Exception e) {}
                                reportNativeResult("speechrecognition", data);
                            }
                            @Override public void onPartialResults(Bundle partialResults) {}
                            @Override public void onEvent(int eventType, Bundle params) {}
                        });
                        
                        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
                        m_speechRecognizer.startListening(intent);
                    } catch (Exception e) {
                        reportNativeResult("speechrecognition", createErrorJson(e.getMessage()));
                    }
                });
            }, () -> reportNativeResult("speechrecognition", createErrorJson("Microphone permission denied")));
        });
        
        // =================================================================
        // COMMUNICATION - SMS, Phone, Email
        // =================================================================
        
        m_nativeHandlers.put("sendsms", (params, callback) -> {
            String phone = params.optString("phone", "");
            String message = params.optString("message", "");
            
            requestPermission(new String[]{Manifest.permission.SEND_SMS}, () -> {
                try {
                    SmsManager sms = SmsManager.getDefault();
                    sms.sendTextMessage(phone, null, message, null, null);
                    JSONObject data = new JSONObject();
                    data.put("sent", true);
                    data.put("phone", phone);
                    reportNativeResult("sendsms", data);
                } catch (Exception e) {
                    reportNativeResult("sendsms", createErrorJson(e.getMessage()));
                }
            }, () -> reportNativeResult("sendsms", createErrorJson("SMS permission denied")));
        });
        
        m_nativeHandlers.put("phonecall", (params, callback) -> {
            String number = params.optString("number", "");
            requestPermission(new String[]{Manifest.permission.CALL_PHONE}, () -> {
                try {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:" + number));
                    m_activity.startActivity(callIntent);
                    JSONObject data = new JSONObject();
                    data.put("calling", true);
                    data.put("number", number);
                    reportNativeResult("phonecall", data);
                } catch (Exception e) {
                    reportNativeResult("phonecall", createErrorJson(e.getMessage()));
                }
            }, () -> reportNativeResult("phonecall", createErrorJson("Call permission denied")));
        });
        
        m_nativeHandlers.put("opendial", (params, callback) -> {
            String number = params.optString("number", "");
            try {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                dialIntent.setData(Uri.parse("tel:" + number));
                m_activity.startActivity(dialIntent);
                JSONObject data = new JSONObject();
                data.put("opened", true);
                reportNativeResult("opendial", data);
            } catch (Exception e) {
                reportNativeResult("opendial", createErrorJson(e.getMessage()));
            }
        });
        
        m_nativeHandlers.put("sendemail", (params, callback) -> {
            String recipient = params.optString("recipient", "");
            String subject = params.optString("subject", "");
            String body = params.optString("body", "");
            try {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:"));
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{recipient});
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                emailIntent.putExtra(Intent.EXTRA_TEXT, body);
                m_activity.startActivity(Intent.createChooser(emailIntent, "Send email"));
                JSONObject data = new JSONObject();
                data.put("sent", true);
                reportNativeResult("sendemail", data);
            } catch (Exception e) {
                reportNativeResult("sendemail", createErrorJson(e.getMessage()));
            }
        });
        
        // =================================================================
        // SYSTEM - Clipboard, Vibration, Flashlight, Notifications
        // =================================================================
        
        m_nativeHandlers.put("clipboard_get", (params, callback) -> {
            m_mainHandler.post(() -> {
                ClipboardManager clipboard = (ClipboardManager) m_ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                JSONObject data = new JSONObject();
                try {
                    ClipData clip = clipboard.getPrimaryClip();
                    if (clip != null && clip.getItemCount() > 0) {
                        data.put("text", clip.getItemAt(0).getText().toString());
                        data.put("hasText", true);
                    } else {
                        data.put("text", "");
                        data.put("hasText", false);
                    }
                } catch (Exception e) {
                    data = createErrorJson(e.getMessage());
                }
                reportNativeResult("clipboard_get", data);
            });
        });
        
        m_nativeHandlers.put("clipboard_set", (params, callback) -> {
            String text = params.optString("text", "");
            m_mainHandler.post(() -> {
                ClipboardManager clipboard = (ClipboardManager) m_ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("text", text);
                clipboard.setPrimaryClip(clip);
                JSONObject data = new JSONObject();
                try { data.put("done", true); } catch (Exception e) {}
                reportNativeResult("clipboard_set", data);
            });
        });
        
        m_nativeHandlers.put("vibrate", (params, callback) -> {
            long duration = params.optLong("duration", 500);
            String patternStr = params.optString("pattern", "");
            
            Vibrator vibrator = (Vibrator) m_ctx.getSystemService(Context.VIBRATOR_SERVICE);
            JSONObject data = new JSONObject();
            try {
                if (!patternStr.isEmpty()) {
                    // Parse pattern like "100,200,100,200"
                    String[] parts = patternStr.split(",");
                    long[] pattern = new long[parts.length];
                    for (int i = 0; i < parts.length; i++) {
                        pattern[i] = Long.parseLong(parts[i].trim());
                    }
                    if (Build.VERSION.SDK_INT >= 26) {
                        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
                    } else {
                        vibrator.vibrate(pattern, -1);
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= 26) {
                        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(duration);
                    }
                }
                data.put("done", true);
            } catch (Exception e) {
                data = createErrorJson(e.getMessage());
            }
            reportNativeResult("vibrate", data);
        });
        
        m_nativeHandlers.put("flashlight", (params, callback) -> {
            boolean on = params.optBoolean("on", true);
            JSONObject data = new JSONObject();
            try {
                if (Build.VERSION.SDK_INT >= 23) {
                    String cameraId = m_cameraManager.getCameraIdList()[0];
                    m_cameraManager.setTorchMode(cameraId, on);
                    data.put("on", on);
                } else {
                    data.put("error", "Requires API 23+");
                }
            } catch (CameraAccessException e) {
                data = createErrorJson(e.getMessage());
            } catch (Exception e) {
                data = createErrorJson(e.getMessage());
            }
            reportNativeResult("flashlight", data);
        });
        
        m_nativeHandlers.put("notification", (params, callback) -> {
            String title = params.optString("title", "Notification");
            String message = params.optString("message", "");
            int id = params.optInt("id", (int) System.currentTimeMillis());
            
            m_mainHandler.post(() -> {
                try {
                    NotificationManager nm = (NotificationManager) m_ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                    String channelId = "php_native_channel";
                    
                    if (Build.VERSION.SDK_INT >= 26) {
                        NotificationChannel channel = new NotificationChannel(channelId, "PHP Native", NotificationManager.IMPORTANCE_DEFAULT);
                        nm.createNotificationChannel(channel);
                    }
                    
                    Notification.Builder builder;
                    if (Build.VERSION.SDK_INT >= 26) {
                        builder = new Notification.Builder(m_ctx, channelId);
                    } else {
                        builder = new Notification.Builder(m_ctx);
                    }
                    builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true);
                    
                    nm.notify(id, builder.build());
                    
                    JSONObject data = new JSONObject();
                    data.put("shown", true);
                    data.put("id", id);
                    reportNativeResult("notification", data);
                } catch (Exception e) {
                    reportNativeResult("notification", createErrorJson(e.getMessage()));
                }
            });
        });
        
        m_nativeHandlers.put("cancelnotification", (params, callback) -> {
            int id = params.optInt("id", 0);
            NotificationManager nm = (NotificationManager) m_ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(id);
            JSONObject data = new JSONObject();
            try { data.put("done", true); } catch (Exception e) {}
            reportNativeResult("cancelnotification", data);
        });
        
        m_nativeHandlers.put("keepscreenon", (params, callback) -> {
            boolean keep = params.optBoolean("keep", true);
            m_mainHandler.post(() -> {
                try {
                    if (keep) {
                        m_activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    } else {
                        m_activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                    JSONObject data = new JSONObject();
                    data.put("done", true);
                    data.put("keep", keep);
                    reportNativeResult("keepscreenon", data);
                } catch (Exception e) {
                    reportNativeResult("keepscreenon", createErrorJson(e.getMessage()));
                }
            });
        });
        
        m_nativeHandlers.put("setbrightness", (params, callback) -> {
            float level = (float) params.optDouble("level", 0.5);
            m_mainHandler.post(() -> {
                try {
                    WindowManager.LayoutParams lp = m_activity.getWindow().getAttributes();
                    lp.screenBrightness = level;
                    m_activity.getWindow().setAttributes(lp);
                    JSONObject data = new JSONObject();
                    data.put("done", true);
                    data.put("level", level);
                    reportNativeResult("setbrightness", data);
                } catch (Exception e) {
                    reportNativeResult("setbrightness", createErrorJson(e.getMessage()));
                }
            });
        });
        
        // =================================================================
        // FILE SYSTEM
        // =================================================================
        
        m_nativeHandlers.put("readfile", (params, callback) -> {
            String path = params.optString("path", "");
            try {
                File file = new File(path);
                StringBuilder content = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                reader.close();
                JSONObject data = new JSONObject();
                data.put("content", content.toString());
                data.put("size", file.length());
                data.put("modified", file.lastModified());
                reportNativeResult("readfile", data);
            } catch (Exception e) {
                reportNativeResult("readfile", createErrorJson(e.getMessage()));
            }
        });
        
        m_nativeHandlers.put("writefile", (params, callback) -> {
            String path = params.optString("path", "");
            String content = params.optString("content", "");
            boolean append = params.optBoolean("append", false);
            try {
                File file = new File(path);
                file.getParentFile().mkdirs();
                BufferedWriter writer = new BufferedWriter(new FileWriter(file, append));
                writer.write(content);
                writer.close();
                JSONObject data = new JSONObject();
                data.put("success", true);
                data.put("path", path);
                data.put("size", file.length());
                reportNativeResult("writefile", data);
            } catch (Exception e) {
                reportNativeResult("writefile", createErrorJson(e.getMessage()));
            }
        });
        
        m_nativeHandlers.put("deletefile", (params, callback) -> {
            String path = params.optString("path", "");
            JSONObject data = new JSONObject();
            try {
                File file = new File(path);
                boolean deleted = file.delete();
                data.put("deleted", deleted);
                data.put("path", path);
            } catch (Exception e) {
                data = createErrorJson(e.getMessage());
            }
            reportNativeResult("deletefile", data);
        });
        
        m_nativeHandlers.put("fileexists", (params, callback) -> {
            String path = params.optString("path", "");
            JSONObject data = new JSONObject();
            try {
                File file = new File(path);
                data.put("exists", file.exists());
                data.put("isFile", file.isFile());
                data.put("isDirectory", file.isDirectory());
                if (file.exists()) {
                    data.put("size", file.length());
                    data.put("modified", file.lastModified());
                }
            } catch (Exception e) {
                data = createErrorJson(e.getMessage());
            }
            reportNativeResult("fileexists", data);
        });
        
        m_nativeHandlers.put("listdir", (params, callback) -> {
            String path = params.optString("path", "");
            JSONObject data = new JSONObject();
            try {
                File dir = new File(path);
                JSONArray files = new JSONArray();
                File[] list = dir.listFiles();
                if (list != null) {
                    for (File f : list) {
                        JSONObject fileInfo = new JSONObject();
                        fileInfo.put("name", f.getName());
                        fileInfo.put("path", f.getAbsolutePath());
                        fileInfo.put("isDir", f.isDirectory());
                        fileInfo.put("size", f.length());
                        fileInfo.put("modified", f.lastModified());
                        files.put(fileInfo);
                    }
                }
                data.put("files", files);
                data.put("path", path);
            } catch (Exception e) {
                data = createErrorJson(e.getMessage());
            }
            reportNativeResult("listdir", data);
        });
        
        m_nativeHandlers.put("mkdir", (params, callback) -> {
            String path = params.optString("path", "");
            JSONObject data = new JSONObject();
            try {
                File dir = new File(path);
                boolean created = dir.mkdirs();
                data.put("created", created || dir.exists());
                data.put("path", path);
            } catch (Exception e) {
                data = createErrorJson(e.getMessage());
            }
            reportNativeResult("mkdir", data);
        });
        
        m_nativeHandlers.put("zipfile", (params, callback) -> {
            String sourcePath = params.optString("source", "");
            String destPath = params.optString("dest", sourcePath + ".zip");
            
            try {
                File sourceFile = new File(sourcePath);
                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destPath));
                zos.putNextEntry(new ZipEntry(sourceFile.getName()));
                FileInputStream fis = new FileInputStream(sourceFile);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                fis.close();
                zos.closeEntry();
                zos.close();
                
                JSONObject data = new JSONObject();
                data.put("success", true);
                data.put("zipPath", destPath);
                data.put("size", new File(destPath).length());
                reportNativeResult("zipfile", data);
            } catch (Exception e) {
                reportNativeResult("zipfile", createErrorJson(e.getMessage()));
            }
        });
        
        m_nativeHandlers.put("zipfolder", (params, callback) -> {
            String sourcePath = params.optString("source", "");
            String destPath = params.optString("dest", sourcePath + ".zip");
            
            try {
                File sourceDir = new File(sourcePath);
                final int[] fileCount = {0};
                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destPath));
                zipDirectory(sourceDir, sourceDir.getName(), zos, fileCount);
                zos.close();
                
                JSONObject data = new JSONObject();
                data.put("success", true);
                data.put("zipPath", destPath);
                data.put("fileCount", fileCount[0]);
                data.put("size", new File(destPath).length());
                reportNativeResult("zipfolder", data);
            } catch (Exception e) {
                reportNativeResult("zipfolder", createErrorJson(e.getMessage()));
            }
        });
        
        m_nativeHandlers.put("unzip", (params, callback) -> {
            String zipPath = params.optString("source", "");
            String destPath = params.optString("dest", "");
            if (destPath.isEmpty()) {
                destPath = new File(zipPath).getParent();
            }
            
            try {
                File destDir = new File(destPath);
                destDir.mkdirs();
                JSONArray extractedFiles = new JSONArray();
                
                ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath));
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    File outFile = new File(destDir, entry.getName());
                    if (entry.isDirectory()) {
                        outFile.mkdirs();
                    } else {
                        outFile.getParentFile().mkdirs();
                        FileOutputStream fos = new FileOutputStream(outFile);
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                        extractedFiles.put(outFile.getAbsolutePath());
                    }
                    zis.closeEntry();
                }
                zis.close();
                
                JSONObject data = new JSONObject();
                data.put("success", true);
                data.put("destPath", destPath);
                data.put("files", extractedFiles);
                reportNativeResult("unzip", data);
            } catch (Exception e) {
                reportNativeResult("unzip", createErrorJson(e.getMessage()));
            }
        });
        
        // =================================================================
        // INTENTS - Open Apps, URLs, Share
        // =================================================================
        
        m_nativeHandlers.put("openapp", (params, callback) -> {
            String packageName = params.optString("package", "");
            try {
                Intent launchIntent = m_ctx.getPackageManager().getLaunchIntentForPackage(packageName);
                if (launchIntent != null) {
                    m_activity.startActivity(launchIntent);
                    JSONObject data = new JSONObject();
                    data.put("opened", true);
                    data.put("package", packageName);
                    reportNativeResult("openapp", data);
                } else {
                    reportNativeResult("openapp", createErrorJson("App not found: " + packageName));
                }
            } catch (Exception e) {
                reportNativeResult("openapp", createErrorJson(e.getMessage()));
            }
        });
        
        m_nativeHandlers.put("openurl", (params, callback) -> {
            String url = params.optString("url", "");
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                m_activity.startActivity(browserIntent);
                JSONObject data = new JSONObject();
                data.put("opened", true);
                reportNativeResult("openurl", data);
            } catch (Exception e) {
                reportNativeResult("openurl", createErrorJson(e.getMessage()));
            }
        });
        
        m_nativeHandlers.put("opensettings", (params, callback) -> {
            String setting = params.optString("setting", "");
            try {
                Intent intent;
                switch (setting) {
                    case "wifi": intent = new Intent(Settings.ACTION_WIFI_SETTINGS); break;
                    case "bluetooth": intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS); break;
                    case "location": intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS); break;
                    case "app": 
                        intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + m_ctx.getPackageName()));
                        break;
                    default: intent = new Intent(Settings.ACTION_SETTINGS);
                }
                m_activity.startActivity(intent);
                JSONObject data = new JSONObject();
                data.put("opened", true);
                reportNativeResult("opensettings", data);
            } catch (Exception e) {
                reportNativeResult("opensettings", createErrorJson(e.getMessage()));
            }
        });
        
        m_nativeHandlers.put("share", (params, callback) -> {
            String text = params.optString("text", "");
            String subject = params.optString("subject", "");
            try {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, text);
                if (!subject.isEmpty()) {
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                }
                m_activity.startActivity(Intent.createChooser(shareIntent, "Share"));
                JSONObject data = new JSONObject();
                data.put("shared", true);
                reportNativeResult("share", data);
            } catch (Exception e) {
                reportNativeResult("share", createErrorJson(e.getMessage()));
            }
        });
        
        m_nativeHandlers.put("sendintent", (params, callback) -> {
            String action = params.optString("action", "");
            String data = params.optString("data", "");
            String type = params.optString("type", "");
            JSONObject extras = params.optJSONObject("extras");
            
            try {
                Intent intent = new Intent(action);
                if (!data.isEmpty()) {
                    intent.setData(Uri.parse(data));
                }
                if (!type.isEmpty()) {
                    intent.setType(type);
                }
                if (extras != null) {
                    Iterator<String> keys = extras.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        intent.putExtra(key, extras.optString(key));
                    }
                }
                m_activity.startActivity(intent);
                JSONObject result = new JSONObject();
                result.put("sent", true);
                reportNativeResult("sendintent", result);
            } catch (Exception e) {
                reportNativeResult("sendintent", createErrorJson(e.getMessage()));
            }
        });
        
        // =================================================================
        // CRYPTO - Hash, Encrypt, Decrypt
        // =================================================================
        
        m_nativeHandlers.put("hash", (params, callback) -> {
            String text = params.optString("text", "");
            String algorithm = params.optString("algorithm", "SHA-256");
            try {
                MessageDigest digest = MessageDigest.getInstance(algorithm);
                byte[] hash = digest.digest(text.getBytes("UTF-8"));
                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) {
                    hexString.append(String.format("%02x", b));
                }
                JSONObject data = new JSONObject();
                data.put("result", hexString.toString());
                data.put("algorithm", algorithm);
                reportNativeResult("hash", data);
            } catch (Exception e) {
                reportNativeResult("hash", createErrorJson(e.getMessage()));
            }
        });
        
        m_nativeHandlers.put("encrypt", (params, callback) -> {
            String text = params.optString("text", "");
            String password = params.optString("password", "");
            try {
                // Derive key from password using SHA-256
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] keyBytes = digest.digest(password.getBytes("UTF-8"));
                SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
                
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                byte[] iv = new byte[16];
                new SecureRandom().nextBytes(iv);
                cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
                
                byte[] encrypted = cipher.doFinal(text.getBytes("UTF-8"));
                
                // Combine IV + encrypted data
                byte[] combined = new byte[iv.length + encrypted.length];
                System.arraycopy(iv, 0, combined, 0, iv.length);
                System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
                
                JSONObject data = new JSONObject();
                data.put("result", Base64.encodeToString(combined, Base64.NO_WRAP));
                reportNativeResult("encrypt", data);
            } catch (Exception e) {
                reportNativeResult("encrypt", createErrorJson(e.getMessage()));
            }
        });
        
        m_nativeHandlers.put("decrypt", (params, callback) -> {
            String encrypted = params.optString("text", "");
            String password = params.optString("password", "");
            try {
                byte[] combined = Base64.decode(encrypted, Base64.NO_WRAP);
                
                // Extract IV and encrypted data
                byte[] iv = new byte[16];
                byte[] encryptedBytes = new byte[combined.length - 16];
                System.arraycopy(combined, 0, iv, 0, 16);
                System.arraycopy(combined, 16, encryptedBytes, 0, encryptedBytes.length);
                
                // Derive key from password
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] keyBytes = digest.digest(password.getBytes("UTF-8"));
                SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
                
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
                
                byte[] decrypted = cipher.doFinal(encryptedBytes);
                
                JSONObject data = new JSONObject();
                data.put("result", new String(decrypted, "UTF-8"));
                reportNativeResult("decrypt", data);
            } catch (Exception e) {
                reportNativeResult("decrypt", createErrorJson(e.getMessage()));
            }
        });
        
        m_nativeHandlers.put("base64encode", (params, callback) -> {
            String text = params.optString("text", "");
            JSONObject data = new JSONObject();
            try {
                data.put("result", Base64.encodeToString(text.getBytes("UTF-8"), Base64.NO_WRAP));
            } catch (Exception e) {
                data = createErrorJson(e.getMessage());
            }
            reportNativeResult("base64encode", data);
        });
        
        m_nativeHandlers.put("base64decode", (params, callback) -> {
            String text = params.optString("text", "");
            JSONObject data = new JSONObject();
            try {
                data.put("result", new String(Base64.decode(text, Base64.NO_WRAP), "UTF-8"));
            } catch (Exception e) {
                data = createErrorJson(e.getMessage());
            }
            reportNativeResult("base64decode", data);
        });
        
        m_nativeHandlers.put("randombytes", (params, callback) -> {
            int length = params.optInt("length", 16);
            JSONObject data = new JSONObject();
            try {
                byte[] bytes = new byte[length];
                new SecureRandom().nextBytes(bytes);
                data.put("result", Base64.encodeToString(bytes, Base64.NO_WRAP));
                data.put("length", length);
            } catch (Exception e) {
                data = createErrorJson(e.getMessage());
            }
            reportNativeResult("randombytes", data);
        });
        
        // =================================================================
        // CAMERA - Take Photo, Record Video, Pick Image
        // =================================================================
        
        m_nativeHandlers.put("takephoto", (params, callback) -> {
            // Optional: pass "imageview": "myImageId" to display photo directly in an ImageView
            m_pendingPhotoImageViewId = params.optString("imageview", null);
            requestPermission(new String[]{Manifest.permission.CAMERA}, () -> {
                m_mainHandler.post(() -> {
                    try {
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        // Prepare local file path in app folder
                        String folder = (m_appDir != null && !m_appDir.isEmpty()) ? m_appDir
                            : m_ctx.getExternalFilesDir(null).getAbsolutePath();
                        String fileName = "photo_" + System.currentTimeMillis() + ".jpg";
                        m_pendingPhotoPath = folder + "/" + fileName;
                        Log.d(TAG, "takephoto: target file = " + m_pendingPhotoPath);
                        // Create MediaStore entry so camera gets a content:// URI it can write to
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                        m_pendingPhotoUri = m_ctx.getContentResolver().insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                        Log.d(TAG, "takephoto: MediaStore URI = " + m_pendingPhotoUri);
                        if (m_pendingPhotoUri != null) {
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, m_pendingPhotoUri);
                            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            m_pendingSensorCallbacks.put("takephoto_result", callback);
                            m_activity.startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                        } else {
                            reportNativeResult("takephoto", createErrorJson("Failed to create MediaStore entry"));
                        }
                    } catch (Exception e) {
                        reportNativeResult("takephoto", createErrorJson(e.getMessage()));
                    }
                });
            }, () -> reportNativeResult("takephoto", createErrorJson("Camera permission denied")));
        });
        
        m_nativeHandlers.put("recordvideo", (params, callback) -> {
            requestPermission(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, () -> {
                m_mainHandler.post(() -> {
                    try {
                        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                        int duration = params.optInt("duration", 0);
                        if (duration > 0) {
                            takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, duration);
                        }
                        takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
                        m_pendingSensorCallbacks.put("recordvideo_result", callback);
                        m_activity.startActivityForResult(takeVideoIntent, REQUEST_RECORD_VIDEO);
                    } catch (Exception e) {
                        reportNativeResult("recordvideo", createErrorJson(e.getMessage()));
                    }
                });
            }, () -> reportNativeResult("recordvideo", createErrorJson("Camera/audio permission denied")));
        });
        
        m_nativeHandlers.put("pickimage", (params, callback) -> {
            m_mainHandler.post(() -> {
                try {
                    Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    m_pendingSensorCallbacks.put("pickimage_result", callback);
                    m_activity.startActivityForResult(pickIntent, REQUEST_PICK_IMAGE);
                } catch (Exception e) {
                    reportNativeResult("pickimage", createErrorJson(e.getMessage()));
                }
            });
        });
        
        m_nativeHandlers.put("pickvideo", (params, callback) -> {
            m_mainHandler.post(() -> {
                try {
                    Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                    m_pendingSensorCallbacks.put("pickvideo_result", callback);
                    m_activity.startActivityForResult(pickIntent, REQUEST_PICK_VIDEO);
                } catch (Exception e) {
                    reportNativeResult("pickvideo", createErrorJson(e.getMessage()));
                }
            });
        });
        
        m_nativeHandlers.put("pickaudio", (params, callback) -> {
            m_mainHandler.post(() -> {
                try {
                    Intent pickIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    pickIntent.setType("audio/*");
                    pickIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    m_pendingSensorCallbacks.put("pickaudio_result", callback);
                    m_activity.startActivityForResult(Intent.createChooser(pickIntent, "Select Audio"), REQUEST_PICK_AUDIO);
                } catch (Exception e) {
                    reportNativeResult("pickaudio", createErrorJson(e.getMessage()));
                }
            });
        });
        
        m_nativeHandlers.put("pickfile", (params, callback) -> {
            String mimeType = params.optString("type", "*/*");
            m_mainHandler.post(() -> {
                try {
                    Intent pickIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    pickIntent.setType(mimeType);
                    pickIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    m_pendingSensorCallbacks.put("pickfile_result", callback);
                    m_activity.startActivityForResult(Intent.createChooser(pickIntent, "Select File"), REQUEST_PICK_FILE);
                } catch (Exception e) {
                    reportNativeResult("pickfile", createErrorJson(e.getMessage()));
                }
            });
        });
        
        Log.d(TAG, "Registered " + m_nativeHandlers.size() + " native handlers");
    }
    
    // -------------------------------------------------------------------------
    // Native Handler Helper Methods
    // -------------------------------------------------------------------------
    
    /**
     * Functional interface for sensor data parsing
     */
    @FunctionalInterface
    private interface SensorDataParser {
        JSONObject parse(SensorEvent event);
    }
    
    /**
     * Read a single value from a sensor and report result
     */
    private void readSensor(int sensorType, String resultType, SensorDataParser parser) {
        Sensor sensor = m_sensorManager.getDefaultSensor(sensorType);
        if (sensor == null) {
            reportNativeResult(resultType, createErrorJson("Sensor not available"));
            return;
        }
        
        SensorEventListener listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                m_sensorManager.unregisterListener(this);
                JSONObject data = parser.parse(event);
                reportNativeResult(resultType, data);
            }
            @Override
            public void onAccuracyChanged(Sensor s, int accuracy) {}
        };
        
        m_sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
    
    /**
     * Read step counter sensor
     */
    private void readStepCounter(String resultType) {
        Sensor sensor = m_sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (sensor == null) {
            reportNativeResult(resultType, createErrorJson("Step counter not available"));
            return;
        }
        
        SensorEventListener listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                m_sensorManager.unregisterListener(this);
                JSONObject data = new JSONObject();
                try { data.put("steps", (long) event.values[0]); } catch (Exception e) {}
                reportNativeResult(resultType, data);
            }
            @Override
            public void onAccuracyChanged(Sensor s, int accuracy) {}
        };
        
        m_sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
    
    /**
     * Read current location using standard Android LocationManager
     */
    private void readLocation(String resultType) {
        try {
            LocationListener listener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    m_locationManager.removeUpdates(this);
                    JSONObject data = locationToJson(location);
                    reportNativeResult(resultType, data);
                }
                @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override public void onProviderEnabled(String provider) {}
                @Override public void onProviderDisabled(String provider) {}
            };
            
            // Try GPS first, then network
            if (m_locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                m_locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, Looper.getMainLooper());
            } else if (m_locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                m_locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper());
            } else {
                reportNativeResult(resultType, createErrorJson("No location provider available"));
            }
        } catch (SecurityException e) {
            reportNativeResult(resultType, createErrorJson("Location permission required"));
        } catch (Exception e) {
            reportNativeResult(resultType, createErrorJson("Location error: " + e.getMessage()));
        }
    }
    
    /**
     * Convert Location to JSON
     */
    private JSONObject locationToJson(Location loc) {
        JSONObject data = new JSONObject();
        try {
            data.put("lat", loc.getLatitude());
            data.put("lng", loc.getLongitude());
            data.put("altitude", loc.getAltitude());
            data.put("accuracy", loc.getAccuracy());
            data.put("speed", loc.getSpeed());
            data.put("bearing", loc.getBearing());
            data.put("provider", loc.getProvider());
            data.put("time", loc.getTime());
        } catch (Exception e) {}
        return data;
    }
    
    /**
     * Recursively zip a directory
     */
    private void zipDirectory(File dir, String baseName, ZipOutputStream zos, int[] fileCount) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectory(file, baseName + "/" + file.getName(), zos, fileCount);
            } else {
                zos.putNextEntry(new ZipEntry(baseName + "/" + file.getName()));
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                fis.close();
                zos.closeEntry();
                fileCount[0]++;
            }
        }
    }
    
    /**
     * Convert battery status int to string
     */
    private String batteryStatusToString(int status) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING: return "charging";
            case BatteryManager.BATTERY_STATUS_DISCHARGING: return "discharging";
            case BatteryManager.BATTERY_STATUS_FULL: return "full";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING: return "not_charging";
            default: return "unknown";
        }
    }
    
    /**
     * Convert battery health int to string
     */
    private String batteryHealthToString(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "good";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "overheat";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "dead";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "over_voltage";
            case BatteryManager.BATTERY_HEALTH_COLD: return "cold";
            default: return "unknown";
        }
    }

    // -------------------------------------------------------------------------
    // Native UI Rendering (Direct Mode Only)
    // -------------------------------------------------------------------------

    /**
     * Ensure the render container (m_mainRootView) is created and ready.
     * MUST be called on the main (UI) thread.
     * Note: View is never auto-attached to the activity. User must call GetView()
     * in JS and add it to a DroidScript layout.
     */
    private void ensureRenderContainerCreated() {
        if (m_mainRootView == null) {
            Log.d(TAG, "ensureRenderContainerCreated: Creating m_mainRootView");
            createMainRootViewInternal();
        }
        // Never auto-attach - DroidScript manages the view placement via GetView()
    }
    
    /**
     * Hide the main root view.
     */
    private void hideRootView() {
        m_mainHandler.post(() -> {
            if (m_mainRootView != null) {
                m_mainRootView.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Handle back button press.
     * If there is navigation history, go back to the previous screen.
     * If at the root screen, show exit confirmation dialog.
     */
    private void handleBackPress() {
        Log.d(TAG, "handleBackPress: history size=" + m_screenHistory.size());

        if (m_screenHistory.size() > 1) {
            // Pop current screen and navigate to previous
            navigateBack();
        } else {
            // At root screen - show exit confirmation
            showExitConfirmDialog();
        }
    }

    /**
     * Navigate back to the previous screen in the history stack.
     */
    private void navigateBack() {
        if (m_screenHistory.size() <= 1) {
            // Already at root, nothing to go back to
            return;
        }

        // Remove current screen
        m_screenHistory.remove(m_screenHistory.size() - 1);

        // Get previous screen
        String[] prev = m_screenHistory.get(m_screenHistory.size() - 1);
        String method = prev[0];
        String dataJson = prev[1];

        Log.d(TAG, "navigateBack to: " + method + " data: " + dataJson);

        if (!m_executor.isShutdown()) {
            m_executor.execute(() -> {
                String phpResponse = callPhp(method, dataJson);
                m_mainHandler.post(() -> processPhpResponse(phpResponse));
            });
        }
    }

    /**
     * Show exit confirmation dialog. On confirm, hide view and clear history.
     */
    private void showExitConfirmDialog() {
        m_mainHandler.post(() -> {
            try {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(m_ctx);
                builder.setTitle("Exit");
                builder.setMessage("Close this app?");
                builder.setPositiveButton("Yes", (dialog, which) -> {
                    m_screenHistory.clear();
                    hideRootView();
                    // Notify JS if callback is set
                    if (m_OnBack != null && !m_OnBack.isEmpty()) {
                        try {
                            m_execscript.invoke(m_parent, m_OnBack + "()");
                        } catch (Exception e) {
                            Log.e(TAG, "Error calling OnBack callback", e);
                        }
                    }
                });
                builder.setNegativeButton("No", null);
                builder.show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing exit confirm dialog", e);
            }
        });
    }

    private void renderUI(String jsonStr) {
        m_mainHandler.post(() -> {
            try {
                // Log raw input for debugging
                Log.d(TAG, "renderUI input (first 200 chars): " + (jsonStr != null ? jsonStr.substring(0, Math.min(200, jsonStr.length())) : "null"));
                
                if (jsonStr == null || jsonStr.isEmpty()) {
                    notifyError("UI render failed: Empty JSON");
                    return;
                }
                
                // Try to find JSON start
                String cleanJson = jsonStr.trim();
                int jsonStart = cleanJson.indexOf("{");
                if (jsonStart > 0) {
                    Log.w(TAG, "Non-JSON prefix found: " + cleanJson.substring(0, jsonStart));
                    cleanJson = cleanJson.substring(jsonStart);
                } else if (jsonStart < 0) {
                    notifyError("UI render failed: No JSON object found in: " + cleanJson.substring(0, Math.min(100, cleanJson.length())));
                    return;
                }
                
                JSONObject root = new JSONObject(cleanJson);
                
                // Check for update action
                if ("update".equals(root.optString("action"))) {
                    String target = root.optString("target");
                    JSONObject attrs = root.optJSONObject("attributes");
                    if (target != null && attrs != null) {
                        updateViewInternal(target, attrs);
                    }
                    return;
                }

                // Create render container if needed (synchronous — we're already on the main thread)
                ensureRenderContainerCreated();
                
                // Get the render container
                ViewGroup container = getRenderContainer();
                if (container == null) {
                    notifyError("UI render failed: No render container available");
                    return;
                }

                // Clear existing views
                container.removeAllViews();
                m_viewRegistry.clear();

                // Create ScrollView wrapper
                // ScrollView scrollView = new ScrollView(m_ctx);
                // scrollView.setFillViewport(true);
                // scrollView.setLayoutParams(new FrameLayout.LayoutParams(
                //     ViewGroup.LayoutParams.MATCH_PARENT,
                //     ViewGroup.LayoutParams.MATCH_PARENT
                // ));

                // Process the root element - it should be a layout
                View rootView = processComponentRecursive(root);
                
                if (rootView != null) {
                    // Apply default background if not set
                    if (rootView.getBackground() == null) {
                        rootView.setBackgroundColor(Color.WHITE);
                    }
                    container.addView(rootView);
                
                } else {
                    // Fallback: create default layout
                    LinearLayout fallbackLayout = new LinearLayout(m_ctx);
                    fallbackLayout.setOrientation(LinearLayout.VERTICAL);
                    fallbackLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
                    fallbackLayout.setBackgroundColor(Color.WHITE);
                    fallbackLayout.setLayoutParams(new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ));
                    container.addView(fallbackLayout);
                }

                Log.d(TAG, "UI rendered with " + m_viewRegistry.size() + " views in direct mode");
                
                // Force layout refresh to ensure view is drawn
                container.requestLayout();
                container.invalidate();
                
                // Sync view state after render
                syncViewStateToFile();

            } catch (Exception e) {
                Log.e(TAG, "Failed to render UI: " + jsonStr, e);
                notifyError("UI render failed: " + e.getMessage());
            }
        });
    }

    /**
     * Recursively processes a JSON component and its children.
     * Creates the view, applies attributes, sets up event listeners, 
     * and if it's a ViewGroup, recursively processes all children.
     *
     * @param item The JSONObject describing the component.
     * @return The created View (or ViewGroup with children).
     */
    private View processComponentRecursive(JSONObject item) {
        String type = item.optString("type", "");
        if (type.isEmpty()) return null;

        // Handle special composite components
        if ("DrawerLayout".equals(type)) {
            return processDrawerLayout(item);
        }
        if ("NavigationDrawer".equals(type)) {
            return processNavigationDrawer(item);
        }
        if ("TopAppBar".equals(type)) {
            return processTopAppBar(item);
        }
        if ("BottomNavBar".equals(type)) {
            return processBottomNavBar(item);
        }
        if ("TabLayout".equals(type)) {
            return processTabLayout(item);
        }
        if ("TextInputLayout".equals(type)) {
            return processTextInputLayout(item);
        }
        if ("FloatingActionButton".equals(type)) {
            return processFloatingActionButton(item);
        }

        View view = createComponent(type);
        if (view == null) return null;

        // Register view by ID if present
        String viewId = item.optString("id", null);
        if (viewId != null && !viewId.isEmpty()) {
            m_viewRegistry.put(viewId, view);
        }

        // Setup event listeners
        setupEventListeners(view, item, viewId);

        // Apply attributes (including layout params, padding, etc.)
        applyAttributes(view, item);

        // If it's a ViewGroup (layout), process children recursively
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            JSONArray children = item.optJSONArray("children");
            
            if (children != null && children.length() > 0) {
                for (int i = 0; i < children.length(); i++) {
                    try {
                        JSONObject childItem = children.getJSONObject(i);
                        View childView = processComponentRecursive(childItem);
                        
                        if (childView != null) {
                            viewGroup.addView(childView);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing child at index " + i, e);
                    }
                }
            }
        }

        return view;
    }

    /**
     * @deprecated Use processComponentRecursive instead
     */
    private void processComponent(JSONObject item, ViewGroup parent) {
        View view = processComponentRecursive(item);
        if (view != null) {
            parent.addView(view);
        }
    }

    private View createComponent(String type) {
        // Handle full class names
        if (type.contains(".")) {
            try {
                Class<?> cls = Class.forName(type);
                Constructor<?> ctor = cls.getConstructor(Context.class);
                return (View) ctor.newInstance(m_ctx);
            } catch (Exception e) {
                Log.w(TAG, "Could not create: " + type);
            }
        }

        // Handle simple type names
        switch (type) {
            case "TextView":
                return new TextView(m_ctx);
            case "Button":
                Button btn = new Button(m_ctx);
                btn.setAllCaps(false);
                return btn;
            case "EditText":
                return new EditText(m_ctx);
            case "CheckBox":
                return new CheckBox(m_ctx);
            case "Switch":
                return new Switch(m_ctx);
            case "ImageView":
                return new ImageView(m_ctx);
            case "ProgressBar":
                return new ProgressBar(m_ctx, null, android.R.attr.progressBarStyleHorizontal);
            case "SeekBar":
                return new android.widget.SeekBar(m_ctx);
            case "RatingBar":
                android.widget.RatingBar rb = new android.widget.RatingBar(m_ctx);
                rb.setStepSize(0.5f);
                return rb;
            case "Spinner":
                return new android.widget.Spinner(m_ctx);
            case "ListView":
                return new android.widget.ListView(m_ctx);
            case "ToggleButton":
                return new ToggleButton(m_ctx);
            case "RadioButton":
                return new RadioButton(m_ctx);
            case "RadioGroup":
                RadioGroup rg = new RadioGroup(m_ctx);
                rg.setOrientation(RadioGroup.VERTICAL);
                return rg;
            case "NumberPicker":
                NumberPicker np = new NumberPicker(m_ctx);
                np.setMinValue(0);
                np.setMaxValue(100);
                return np;
            case "AutoCompleteTextView":
                return new AutoCompleteTextView(m_ctx);
            case "MultiAutoCompleteTextView":
                return new MultiAutoCompleteTextView(m_ctx);
            case "SearchView":
                return new SearchView(m_ctx);
            case "CalendarView":
                return new CalendarView(m_ctx);
            case "DatePicker":
                return new DatePicker(m_ctx);
            case "TimePicker":
                return new TimePicker(m_ctx);
            case "VideoView":
                return new VideoView(m_ctx);
            case "Space":
                return new Space(m_ctx);
            case "Chronometer":
                return new Chronometer(m_ctx);
            case "TextClock":
                return new TextClock(m_ctx);
            case "Chip":
                // Chip as a styled Button (Material Chip requires Material lib)
                Button chipBtn = new Button(m_ctx);
                chipBtn.setAllCaps(false);
                chipBtn.setMinHeight(dpToPx(32));
                chipBtn.setMinimumHeight(dpToPx(32));
                chipBtn.setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4));
                chipBtn.setTextSize(14);
                // Give it a chip-like rounded shape
                GradientDrawable chipBg = new GradientDrawable();
                chipBg.setShape(GradientDrawable.RECTANGLE);
                chipBg.setCornerRadius(dpToPx(16));
                chipBg.setColor(Color.parseColor("#E0E0E0"));
                chipBtn.setBackground(chipBg);
                return chipBtn;
            case "ChipGroup":
                // Use a horizontal-wrapping FlowLayout (approximate with horizontal LinearLayout)
                LinearLayout chipGroup = new LinearLayout(m_ctx);
                chipGroup.setOrientation(LinearLayout.HORIZONTAL);
                // Enable wrapping would need FlexboxLayout; we use horizontal for now
                return chipGroup;
            case "FloatingActionButton":
                // FAB as a styled circular Button
                Button fabBtn = new Button(m_ctx);
                fabBtn.setAllCaps(false);
                int fabSize = dpToPx(56);
                fabBtn.setMinHeight(fabSize);
                fabBtn.setMinWidth(fabSize);
                fabBtn.setMinimumHeight(fabSize);
                fabBtn.setMinimumWidth(fabSize);
                fabBtn.setGravity(Gravity.CENTER);
                fabBtn.setTextSize(24);
                fabBtn.setElevation(dpToPx(6));
                GradientDrawable fabBg = new GradientDrawable();
                fabBg.setShape(GradientDrawable.OVAL);
                fabBg.setColor(Color.parseColor("#FF4081"));
                fabBtn.setBackground(fabBg);
                fabBtn.setTextColor(Color.WHITE);
                return fabBtn;
            case "TextInputLayout":
                // Approximate with a LinearLayout containing hint TextView + EditText
                LinearLayout tilLayout = new LinearLayout(m_ctx);
                tilLayout.setOrientation(LinearLayout.VERTICAL);
                return tilLayout;
            case "TabLayout":
                // Tab bar as horizontal LinearLayout with equal-weight items
                LinearLayout tabBar = new LinearLayout(m_ctx);
                tabBar.setOrientation(LinearLayout.HORIZONTAL);
                tabBar.setBackgroundColor(Color.parseColor("#1976D2"));
                tabBar.setElevation(dpToPx(4));
                LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(48)
                );
                tabBar.setLayoutParams(tabParams);
                return tabBar;
            case "Toolbar":
                LinearLayout toolbarLayout = new LinearLayout(m_ctx);
                toolbarLayout.setOrientation(LinearLayout.HORIZONTAL);
                toolbarLayout.setGravity(Gravity.CENTER_VERTICAL);
                toolbarLayout.setBackgroundColor(Color.parseColor("#333333"));
                toolbarLayout.setElevation(dpToPx(4));
                toolbarLayout.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
                LinearLayout.LayoutParams tlParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(56)
                );
                toolbarLayout.setLayoutParams(tlParams);
                return toolbarLayout;
            case "SwipeRefreshLayout":
                // Approximate with a FrameLayout
                FrameLayout swipeFrame = new FrameLayout(m_ctx);
                return swipeFrame;
            case "GridLayout":
                GridLayout gl = new GridLayout(m_ctx);
                gl.setColumnCount(2);
                return gl;
            case "TableLayout":
                return new TableLayout(m_ctx);
            case "TableRow":
                return new TableRow(m_ctx);
            case "VerticalLayout":
            case "LinearLayout":
                LinearLayout ll = new LinearLayout(m_ctx);
                ll.setOrientation(LinearLayout.VERTICAL);
                return ll;
            case "HorizontalLayout":
                LinearLayout hl = new LinearLayout(m_ctx);
                hl.setOrientation(LinearLayout.HORIZONTAL);
                return hl;
            case "FrameLayout":
            case "StackLayout":
                return new FrameLayout(m_ctx);
            case "RelativeLayout":
                return new RelativeLayout(m_ctx);
            case "ScrollView":
                ScrollView sv = new ScrollView(m_ctx);
                sv.setFillViewport(true);
                return sv;
            case "HorizontalScrollView":
                return new HorizontalScrollView(m_ctx);
            case "CardView":
                // Card as a styled LinearLayout with elevation and corners
                LinearLayout cardLayout = new LinearLayout(m_ctx);
                cardLayout.setOrientation(LinearLayout.VERTICAL);
                cardLayout.setElevation(dpToPx(4));
                GradientDrawable cardBg = new GradientDrawable();
                cardBg.setShape(GradientDrawable.RECTANGLE);
                cardBg.setCornerRadius(dpToPx(8));
                cardBg.setColor(Color.WHITE);
                cardLayout.setBackground(cardBg);
                return cardLayout;
            case "DrawerLayout":
                // Custom drawer layout (FrameLayout that we manage)
                return createDrawerLayout();
            case "NavigationDrawer":
                // Navigation drawer content (LinearLayout with items)
                return createNavigationDrawerView();
            case "TopAppBar":
                // Top app bar / toolbar
                return createTopAppBar();
            case "BottomNavBar":
                // Bottom navigation bar
                return createBottomNavBar();
            default:
                // Try to find in common packages
                String[] packages = {
                    "android.widget.",
                    "android.view.",
                    "androidx.appcompat.widget.",
                    "com.google.android.material.button.",
                    "com.google.android.material.switchmaterial.",
                    "com.google.android.material.checkbox.",
                    "com.google.android.material.textfield.",
                    "com.google.android.material.card.",
                    "android.webkit."
                };
                for (String pkg : packages) {
                    try {
                        Class<?> cls = Class.forName(pkg + type);
                        Constructor<?> ctor = cls.getConstructor(Context.class);
                        return (View) ctor.newInstance(m_ctx);
                    } catch (Exception ignored) {}
                }
                Log.w(TAG, "Unknown component type: " + type);
                TextView errorView = new TextView(m_ctx);
                errorView.setText("Unknown: " + type);
                errorView.setTextColor(Color.RED);
                return errorView;
        }
    }

    /**
     * Configures event listeners for a specific View based on the properties defined in a JSONObject.
     * <p>
     * This method iterates through the keys of the provided JSON object. If a key is named "action",
     * it binds a click listener. If a key starts with "on" (e.g., "onLongClick", "onCheckedChange"),
     * it binds the corresponding event to the specified PHP backend method.
     * </p>
     *
     * @param view      The Android View component to which the listeners will be attached.
     * @param item      The JSONObject containing property definitions and event mapping strings.
     * @param viewId    The unique identifier for the view, used to track the event source in the backend.
     */
    private void setupEventListeners(View view, JSONObject item, String viewId) {
        debugLog("SETUP_EVENTS: viewId=" + viewId + " view=" + view.getClass().getSimpleName() + " hasAction=" + item.has("action") + " hasOnClick=" + item.has("onClick"));
        
        Iterator<String> keys = item.keys();
        boolean hasAction = item.has("action");
        
        while (keys.hasNext()) {
            String key = keys.next();
            String phpMethod = item.optString(key);

            if (key.equals("action")) {
                bindEvent(view, "onClick", phpMethod, viewId);;
            } else if (key.startsWith("on")) {
                // Skip onClick if "action" already handled it
                if (key.equals("onClick") && hasAction) {
                    continue;
                }
                bindEvent(view, key, phpMethod, viewId);
            }
        }
    }

    /**
     * Binds an Android view event to a PHP method using reflection and dynamic proxies.
     * <p>
     * This method searches for the appropriate listener setter (e.g., "setOnClickListener")
     * based on the provided event name. It creates a dynamic proxy for the listener interface
     * which, when triggered, gathers event metadata and arguments, sends them to the
     * PHP backend via callPhp, and handles any subsequent UI actions returned.
     * </p>
     *
     * @param view       The Android View component to attach the listener to.
     * @param eventName  The name of the event (e.g., "onClick" or "onCheckedChanged").
     * @param phpMethod  The specific PHP function/method to be called on the server side.
     * @param viewId     The unique identifier of the view, passed back to PHP for context.
     */
    private void bindEvent(View view, String eventName, String phpMethod, String viewId) {
        debugLog("BIND_EVENT: eventName=" + eventName + " viewId=" + viewId + " phpMethod=" + phpMethod + " view=" + view.getClass().getSimpleName());
        
        // Derive the setter method name from the event name
        // e.g., "onClick" -> "setOnClickListener", "onLongClick" -> "setOnLongClickListener"
        String suffix = eventName.startsWith("on") ? eventName.substring(2) : eventName;
        String setterName = "setOn" + suffix.substring(0, 1).toUpperCase() + suffix.substring(1) + "Listener";

        // Search for the setter method in the view's class hierarchy
        for (Method method : view.getClass().getMethods()) {
            if (method.getName().equals(setterName) && method.getParameterTypes().length == 1) {
                Class<?> listenerInterface = method.getParameterTypes()[0];

                // Create a dynamic proxy that implements the listener interface
                Object proxy = Proxy.newProxyInstance(
                    listenerInterface.getClassLoader(),
                    new Class<?>[]{listenerInterface},
                    (proxyInstance, m, args) -> {
                        // Skip Object methods (toString, hashCode, equals)
                        if (m.getDeclaringClass() == Object.class) {
                            if (m.getName().equals("toString")) return listenerInterface.getName() + "@proxy";
                            if (m.getName().equals("hashCode")) return System.identityHashCode(proxyInstance);
                            if (m.getName().equals("equals")) return proxyInstance == args[0];
                            return null;
                        }
                        
                        // Debug: Log every event invocation
                        debugLog("EVENT FIRED: " + eventName + " method=" + m.getName() + " viewId=" + viewId + " phpMethod=" + phpMethod + " thread=" + Thread.currentThread().getName());
                        
                        // Build the parameters JSON to send to PHP
                        JSONObject params = new JSONObject();
                        try {
                            params.put("targetId", viewId);
                            params.put("event", eventName);
                            params.put("method", m.getName());

                            // Extract useful data from listener method arguments
                            if (args != null) {
                                for (int i = 0; i < args.length; i++) {
                                    extractArgumentData(params, args[i], i);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error building event params for " + eventName, e);
                        }

                        // Execute PHP call on background thread
                        if (m_executor != null && !m_executor.isShutdown()) {
                            debugLog("EVENT CALLING PHP: " + phpMethod + " params=" + params.toString());
                            m_executor.execute(() -> {
                                try {
                                    String response = callPhp(phpMethod, params.toString());
                                    debugLog("EVENT PHP RESPONSE: " + (response != null ? response.substring(0, Math.min(200, response.length())) : "null"));
                                    if (response != null && !response.isEmpty()) {
                                        m_mainHandler.post(() -> processPhpResponse(response));
                                    }
                                } catch (Exception ex) {
                                    Log.e(TAG, "Error executing PHP call for event " + eventName, ex);
                                }
                            });
                        } else {
                            Log.w(TAG, "Executor not available for event " + eventName + " on " + viewId);
                        }

                        // Return appropriate value based on method return type
                        Class<?> returnType = m.getReturnType();
                        if (returnType == boolean.class) {
                            return true; // Consume the event by default
                        } else if (returnType == void.class) {
                            return null;
                        } else if (returnType.isPrimitive()) {
                            // Return default primitive values
                            if (returnType == int.class) return 0;
                            if (returnType == long.class) return 0L;
                            if (returnType == float.class) return 0f;
                            if (returnType == double.class) return 0.0;
                            if (returnType == char.class) return '\0';
                            if (returnType == byte.class) return (byte) 0;
                            if (returnType == short.class) return (short) 0;
                        }
                        return null;
                    }
                );

                // Invoke the setter to attach the proxy listener
                try {
                    method.invoke(view, proxy);
                    Log.d(TAG, "Bound event " + eventName + " via " + setterName);
                } catch (Exception e) {
                    Log.e(TAG, "Error binding event " + setterName, e);
                }
                return;
            }
        }

        // Setter not found - log warning
        Log.w(TAG, "Could not find setter " + setterName + " for event " + eventName + " on " + view.getClass().getSimpleName());
    }

    /**
     * Extracts useful data from event listener arguments and adds them to the params JSON.
     * Handles common Android types like CompoundButton, MotionEvent, View, etc.
     *
     * @param params  The JSONObject to populate with extracted data.
     * @param arg     The argument from the listener callback.
     * @param index   The index of the argument in the callback signature.
     */
    private void extractArgumentData(JSONObject params, Object arg, int index) {
        if (arg == null) return;

        try {
            // Handle primitive wrappers and strings directly
            if (arg instanceof Boolean) {
                params.put("arg" + index, arg);
                // Common case: second arg is often isChecked for checkboxes
                if (index == 1) {
                    params.put("isChecked", arg);
                }
            } else if (arg instanceof Number) {
                params.put("arg" + index, arg);
                // Common cases based on position
                if (index == 1) {
                    params.put("position", arg); // For AdapterView clicks
                }
                if (index == 2) {
                    params.put("id", arg); // For AdapterView clicks
                }
            } else if (arg instanceof String || arg instanceof CharSequence) {
                params.put("arg" + index, arg.toString());
                params.put("text", arg.toString());
            }
            // Handle CompoundButton (CheckBox, Switch, ToggleButton)
            else if (arg instanceof android.widget.CompoundButton) {
                android.widget.CompoundButton cb = (android.widget.CompoundButton) arg;
                params.put("isChecked", cb.isChecked());
                params.put("text", cb.getText().toString());
            }
            // Handle View
            else if (arg instanceof View) {
                View v = (View) arg;
                params.put("viewId", v.getId());
                params.put("visibility", v.getVisibility());
                params.put("enabled", v.isEnabled());
                params.put("clickable", v.isClickable());
                params.put("focusable", v.isFocusable());
                params.put("focused", v.isFocused());
                params.put("selected", v.isSelected());
                params.put("alpha", v.getAlpha());
                params.put("x", v.getX());
                params.put("y", v.getY());
                params.put("width", v.getWidth());
                params.put("height", v.getHeight());
                params.put("translationX", v.getTranslationX());
                params.put("translationY", v.getTranslationY());
                params.put("scaleX", v.getScaleX());
                params.put("scaleY", v.getScaleY());
                params.put("rotation", v.getRotation());
                if (v.getTag() != null) {
                    params.put("tag", v.getTag().toString());
                }
                if (v instanceof TextView) {
                    TextView tv = (TextView) v;
                    params.put("text", tv.getText().toString());
                    params.put("hint", tv.getHint() != null ? tv.getHint().toString() : "");
                    params.put("textSize", tv.getTextSize());
                    params.put("currentTextColor", tv.getCurrentTextColor());
                }
                if (v instanceof android.widget.ImageView) {
                    // ImageView doesn't expose drawable URL, but we can get content description
                    android.widget.ImageView iv = (android.widget.ImageView) v;
                    if (iv.getContentDescription() != null) {
                        params.put("contentDescription", iv.getContentDescription().toString());
                    }
                }
                if (v instanceof android.widget.ProgressBar) {
                    android.widget.ProgressBar pb = (android.widget.ProgressBar) v;
                    params.put("progress", pb.getProgress());
                    params.put("max", pb.getMax());
                }
            }
            // Handle MotionEvent
            else if (arg instanceof android.view.MotionEvent) {
                android.view.MotionEvent event = (android.view.MotionEvent) arg;
                params.put("touchAction", event.getAction());
                params.put("x", event.getX());
                params.put("y", event.getY());
                params.put("rawX", event.getRawX());
                params.put("rawY", event.getRawY());
                params.put("pressure", event.getPressure());
            }
            // Handle KeyEvent
            else if (arg instanceof android.view.KeyEvent) {
                android.view.KeyEvent event = (android.view.KeyEvent) arg;
                params.put("keyCode", event.getKeyCode());
                params.put("keyAction", event.getAction());
                params.put("keyChar", String.valueOf((char) event.getUnicodeChar()));
            }
            // Handle DragEvent
            else if (arg instanceof android.view.DragEvent) {
                android.view.DragEvent event = (android.view.DragEvent) arg;
                params.put("dragAction", event.getAction());
                params.put("x", event.getX());
                params.put("y", event.getY());
            }
            // Handle SeekBar
            else if (arg instanceof android.widget.SeekBar) {
                android.widget.SeekBar seekBar = (android.widget.SeekBar) arg;
                params.put("progress", seekBar.getProgress());
                params.put("max", seekBar.getMax());
            }
            // Handle RatingBar
            else if (arg instanceof android.widget.RatingBar) {
                android.widget.RatingBar ratingBar = (android.widget.RatingBar) arg;
                params.put("rating", ratingBar.getRating());
                params.put("numStars", ratingBar.getNumStars());
            }
            // Handle AdapterView for spinners/lists
            else if (arg instanceof android.widget.AdapterView) {
                android.widget.AdapterView<?> adapterView = (android.widget.AdapterView<?>) arg;
                int selectedPos = adapterView.getSelectedItemPosition();
                params.put("selectedPosition", selectedPos);
                Object selectedItem = adapterView.getSelectedItem();
                if (selectedItem != null) {
                    params.put("selectedItem", selectedItem.toString());
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error extracting argument data at index " + index, e);
        }
    }

    /**
     * Applies all non-reserved attributes from a JSON object to an Android View using reflection.
     * <p>
     * This method iterates through all keys in the JSON object and attempts to invoke
     * the corresponding setter method on the view. Reserved keys (type, children, action,
     * id, target, attributes, and event handlers starting with "on") are skipped.
     * Layout params (width, height, margin, weight, gravity) are handled specially.
     * </p>
     *
     * @param view The Android View to apply attributes to.
     * @param json The JSONObject containing attribute key-value pairs.
     */
    private void applyAttributes(View view, JSONObject json) {
        // First, handle layout params
        applyLayoutParams(view, json);
        
        // Handle border property (creates GradientDrawable background)
        if (json.has("border")) {
            applyBorder(view, json);
        }
        
        // Handle WebView settings
        if (json.has("settings") && view instanceof android.webkit.WebView) {
            applyWebViewSettings((android.webkit.WebView) view, json.optJSONObject("settings"));
        }
        
        // Handle ListView items
        if (view instanceof android.widget.ListView && json.has("items")) {
            String viewId = json.optString("id", null);
            setupListView((android.widget.ListView) view, viewId, json);
        }
        
        // Handle Spinner items
        if (view instanceof android.widget.Spinner && json.has("items")) {
            setupSpinner((android.widget.Spinner) view, json);
        }
        
        // Handle NumberPicker range
        if (view instanceof NumberPicker) {
            setupNumberPicker((NumberPicker) view, json);
        }
        
        // Handle AutoCompleteTextView suggestions
        if (view instanceof AutoCompleteTextView && json.has("suggestions")) {
            setupAutoComplete((AutoCompleteTextView) view, json);
        }
        
        // Handle RatingBar special properties
        if (view instanceof android.widget.RatingBar) {
            setupRatingBar((android.widget.RatingBar) view, json);
        }
        
        // Handle VideoView source
        if (view instanceof VideoView && json.has("videoUri")) {
            setupVideoView((VideoView) view, json);
        }
        
        // Handle GridLayout column/row count
        if (view instanceof GridLayout) {
            if (json.has("columnCount")) {
                ((GridLayout) view).setColumnCount(json.optInt("columnCount", 2));
            }
            if (json.has("rowCount")) {
                ((GridLayout) view).setRowCount(json.optInt("rowCount", -1));
            }
        }
        
        // Handle Chronometer
        if (view instanceof Chronometer) {
            setupChronometer((Chronometer) view, json);
        }
        
        // Then handle regular view attributes
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            // Skip reserved keys and layout params (handled separately)
            if (key.equals("type") || key.equals("children") || key.equals("action") ||
                key.equals("id") || key.equals("target") || key.equals("attributes") ||
                key.equals("border") || key.equals("settings") || key.equals("items") ||
                key.equals("itemLayout") || key.startsWith("on") || isLayoutParam(key)) continue;

            try {
                Object value = json.get(key);
                // Try setter first (e.g., setText), then direct method (e.g., loadUrl)
                String setterName = "set" + key.substring(0, 1).toUpperCase() + key.substring(1);
                if (!invokeMethod(view, setterName, value)) {
                    // Setter didn't exist, try direct method name (e.g., loadUrl for WebView)
                    invokeMethod(view, key, value);
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not apply attribute: " + key);
            }
        }
    }

    /**
     * Applies settings to a WebView's WebSettings object.
     * Supports settings like: javaScriptEnabled, domStorageEnabled, allowFileAccess, etc.
     *
     * @param webView The WebView to configure.
     * @param settings JSONObject containing setting key-value pairs.
     */
    private void applyWebViewSettings(android.webkit.WebView webView, JSONObject settings) {
        if (settings == null) return;
        
        android.webkit.WebSettings webSettings = webView.getSettings();
        Iterator<String> keys = settings.keys();
        
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                Object value = settings.get(key);
                String setterName = "set" + key.substring(0, 1).toUpperCase() + key.substring(1);
                
                // Find and invoke the setter method on WebSettings
                for (Method method : webSettings.getClass().getMethods()) {
                    if (method.getName().equals(setterName) && method.getParameterTypes().length == 1) {
                        Class<?> paramType = method.getParameterTypes()[0];
                        Object convertedValue = convertSettingsValue(value, paramType);
                        if (convertedValue != null) {
                            method.invoke(webSettings, convertedValue);
                            Log.d(TAG, "Applied WebView setting: " + key + " = " + value);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not apply WebView setting: " + key + " - " + e.getMessage());
            }
        }
    }

    /**
     * Converts a value to the target type for WebSettings.
     */
    private Object convertSettingsValue(Object value, Class<?> targetType) {
        try {
            if (targetType == boolean.class || targetType == Boolean.class) {
                if (value instanceof Boolean) return value;
                return Boolean.parseBoolean(value.toString());
            }
            if (targetType == int.class || targetType == Integer.class) {
                if (value instanceof Number) return ((Number) value).intValue();
                return Integer.parseInt(value.toString());
            }
            if (targetType == String.class) {
                return value.toString();
            }
            if (targetType.isInstance(value)) {
                return value;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     * Applies border styling to a View using GradientDrawable.
     * Border property can be:
     * - Object: { width: 2, color: "#000", radius: 10, background: "#FFF" }
     * - String: "2 #000000" (width and color)
     * - Number: border width in dp (uses default black color)
     *
     * @param view The View to apply the border to.
     * @param json The JSONObject containing the border property.
     */
    private void applyBorder(View view, JSONObject json) {
        try {
            Object borderVal = json.get("border");
            
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            
            // Default values
            int borderWidth = dpToPx(1);
            int borderColor = Color.BLACK;
            float cornerRadius = 0f;
            int backgroundColor = Color.TRANSPARENT;
            
            if (borderVal instanceof JSONObject) {
                JSONObject border = (JSONObject) borderVal;
                
                // Border width
                if (border.has("width")) {
                    borderWidth = dpToPx(border.optInt("width", 1));
                }
                
                // Border color
                if (border.has("color")) {
                    try {
                        borderColor = Color.parseColor(border.optString("color", "#000000"));
                    } catch (Exception e) {
                        borderColor = Color.BLACK;
                    }
                }
                
                // Corner radius
                if (border.has("radius")) {
                    cornerRadius = dpToPx(border.optInt("radius", 0));
                }
                
                // Background color
                if (border.has("background")) {
                    try {
                        backgroundColor = Color.parseColor(border.optString("background", "#FFFFFF"));
                    } catch (Exception e) {
                        backgroundColor = Color.WHITE;
                    }
                }
            } else if (borderVal instanceof String) {
                // Parse string format: "width color" e.g., "2 #000000"
                String[] parts = ((String) borderVal).trim().split("\\s+");
                if (parts.length >= 1) {
                    try {
                        borderWidth = dpToPx(Integer.parseInt(parts[0]));
                    } catch (NumberFormatException e) {
                        // If first part is color, use default width
                        try {
                            borderColor = Color.parseColor(parts[0]);
                        } catch (Exception ignored) {}
                    }
                }
                if (parts.length >= 2) {
                    try {
                        borderColor = Color.parseColor(parts[1]);
                    } catch (Exception ignored) {}
                }
            } else if (borderVal instanceof Number) {
                // Just border width
                borderWidth = dpToPx(((Number) borderVal).intValue());
            }
            
            // Apply the drawable properties
            drawable.setColor(backgroundColor);
            drawable.setStroke(borderWidth, borderColor);
            if (cornerRadius > 0) {
                drawable.setCornerRadius(cornerRadius);
            }
            
            view.setBackground(drawable);
            
            // Add padding so the content doesn't overlap the border stroke
            int pad = borderWidth;
            view.setPadding(
                Math.max(view.getPaddingLeft(), pad),
                Math.max(view.getPaddingTop(), pad),
                Math.max(view.getPaddingRight(), pad),
                Math.max(view.getPaddingBottom(), pad)
            );
            
        } catch (Exception e) {
            Log.w(TAG, "Could not apply border: " + e.getMessage());
        }
    }

    /**
     * Checks if an attribute key is a layout parameter or padding.
     */
    private boolean isLayoutParam(String key) {
        return key.equals("width") || key.equals("height") || 
               key.equals("weight") || key.equals("layoutGravity") ||
               key.equals("margin") || key.equals("marginLeft") || key.equals("marginRight") ||
               key.equals("marginTop") || key.equals("marginBottom") ||
               key.equals("marginStart") || key.equals("marginEnd") ||
               key.equals("padding") || key.equals("paddingLeft") || key.equals("paddingRight") ||
               key.equals("paddingTop") || key.equals("paddingBottom") ||
               key.equals("paddingStart") || key.equals("paddingEnd") ||
               key.equals("paddingHorizontal") || key.equals("paddingVertical") ||
               key.equals("minWidth") || key.equals("minHeight") ||
               key.equals("maxWidth") || key.equals("maxHeight") ||
               key.equals("layoutRow") || key.equals("layoutColumn") ||
               key.equals("layoutRowSpan") || key.equals("layoutColumnSpan");
    }

    /**
     * Applies layout parameters (width, height, margin, weight, gravity) to a View.
     * Supports special values: "match_parent", "wrap_content", or numeric dp values.
     *
     * @param view The View to apply layout params to.
     * @param json The JSONObject containing layout param values.
     */
    private void applyLayoutParams(View view, JSONObject json) {
        ViewGroup.LayoutParams existingParams = view.getLayoutParams();
        
        // Determine the appropriate LayoutParams type based on parent
        ViewGroup.MarginLayoutParams params;
        if (existingParams instanceof LinearLayout.LayoutParams) {
            params = (LinearLayout.LayoutParams) existingParams;
        } else if (existingParams instanceof FrameLayout.LayoutParams) {
            params = (FrameLayout.LayoutParams) existingParams;
        } else if (existingParams instanceof ViewGroup.MarginLayoutParams) {
            params = (ViewGroup.MarginLayoutParams) existingParams;
        } else {
            // Create new LinearLayout params as default
            params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // Width (use screen width as reference for percentages)
        if (json.has("width")) {
            Object widthVal = json.opt("width");
            if (widthVal instanceof Number) {
                double num = ((Number) widthVal).doubleValue();
                if (num > 0 && num <= 1) {
                    params.width = (int) (num * getScreenWidth());
                } else if (num == -1) {
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                } else if (num == -2) {
                    params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                } else {
                    params.width = dpToPx((int) num);
                }
            } else {
                params.width = parseLayoutDimension(json.optString("width"));
            }
        }

        // Height (use screen height as reference for percentages)
        if (json.has("height")) {
            Object heightVal = json.opt("height");
            if (heightVal instanceof Number) {
                double num = ((Number) heightVal).doubleValue();
                if (num > 0 && num <= 1) {
                    params.height = (int) (num * getScreenHeight());
                } else if (num == -1) {
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                } else if (num == -2) {
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                } else {
                    params.height = dpToPx((int) num);
                }
            } else {
                params.height = parseLayoutDimensionHeight(json.optString("height"));
            }
        }

        // Weight (LinearLayout only)
        if (json.has("weight") && params instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) params).weight = (float) json.optDouble("weight", 0);
        }

        // Layout Gravity
        if (json.has("layoutGravity")) {
            int gravity = parseGravity(json.optString("layoutGravity"));
            if (params instanceof LinearLayout.LayoutParams) {
                ((LinearLayout.LayoutParams) params).gravity = gravity;
            } else if (params instanceof FrameLayout.LayoutParams) {
                ((FrameLayout.LayoutParams) params).gravity = gravity;
            }
        }

        // Margins - single value for all sides
        if (json.has("margin")) {
            int margin = parseDimensionValue(json.opt("margin"), getScreenWidth());
            params.setMargins(margin, margin, margin, margin);
        }

        // Individual margins (horizontal uses screen width, vertical uses screen height)
        int marginLeft = json.has("marginLeft") ? parseDimensionValue(json.opt("marginLeft"), getScreenWidth()) : params.leftMargin;
        int marginTop = json.has("marginTop") ? parseDimensionValue(json.opt("marginTop"), getScreenHeight()) : params.topMargin;
        int marginRight = json.has("marginRight") ? parseDimensionValue(json.opt("marginRight"), getScreenWidth()) : params.rightMargin;
        int marginBottom = json.has("marginBottom") ? parseDimensionValue(json.opt("marginBottom"), getScreenHeight()) : params.bottomMargin;
        
        // marginStart/marginEnd (API 17+)
        if (json.has("marginStart")) {
            marginLeft = parseDimensionValue(json.opt("marginStart"), getScreenWidth());
        }
        if (json.has("marginEnd")) {
            marginRight = parseDimensionValue(json.opt("marginEnd"), getScreenWidth());
        }
        
        params.setMargins(marginLeft, marginTop, marginRight, marginBottom);

        // Min/Max dimensions (set directly on view)
        if (json.has("minWidth")) {
            view.setMinimumWidth(dpToPx(json.optInt("minWidth")));
        }
        if (json.has("minHeight")) {
            view.setMinimumHeight(dpToPx(json.optInt("minHeight")));
        }

        view.setLayoutParams(params);

        // Padding - handled separately from LayoutParams (set directly on view)
        applyPadding(view, json);
    }

    /**
     * Applies padding to a View from JSON attributes.
     * Supports: padding (all sides), paddingLeft/Right/Top/Bottom, paddingStart/End,
     * paddingHorizontal, paddingVertical.
     * Values can be integers (dp), decimals 0-1 (percentage), or strings with suffixes.
     *
     * @param view The View to apply padding to.
     * @param json The JSONObject containing padding values.
     */
    private void applyPadding(View view, JSONObject json) {
        int screenW = getScreenWidth();
        int screenH = getScreenHeight();
        
        // Get existing padding as defaults
        int paddingLeft = view.getPaddingLeft();
        int paddingTop = view.getPaddingTop();
        int paddingRight = view.getPaddingRight();
        int paddingBottom = view.getPaddingBottom();

        // Single value for all sides
        if (json.has("padding")) {
            int padding = parseDimensionValue(json.opt("padding"), screenW);
            paddingLeft = paddingTop = paddingRight = paddingBottom = padding;
        }

        // Horizontal/Vertical shortcuts
        if (json.has("paddingHorizontal")) {
            int h = parseDimensionValue(json.opt("paddingHorizontal"), screenW);
            paddingLeft = paddingRight = h;
        }
        if (json.has("paddingVertical")) {
            int v = parseDimensionValue(json.opt("paddingVertical"), screenH);
            paddingTop = paddingBottom = v;
        }

        // Individual paddings (horizontal uses width, vertical uses height as reference)
        if (json.has("paddingLeft")) {
            paddingLeft = parseDimensionValue(json.opt("paddingLeft"), screenW);
        }
        if (json.has("paddingTop")) {
            paddingTop = parseDimensionValue(json.opt("paddingTop"), screenH);
        }
        if (json.has("paddingRight")) {
            paddingRight = parseDimensionValue(json.opt("paddingRight"), screenW);
        }
        if (json.has("paddingBottom")) {
            paddingBottom = parseDimensionValue(json.opt("paddingBottom"), screenH);
        }

        // Start/End (RTL support, API 17+)
        if (json.has("paddingStart")) {
            paddingLeft = parseDimensionValue(json.opt("paddingStart"), screenW);
        }
        if (json.has("paddingEnd")) {
            paddingRight = parseDimensionValue(json.opt("paddingEnd"), screenW);
        }

        view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
    }

    /**
     * Gets the screen width in pixels.
     */
    private int getScreenWidth() {
        android.util.DisplayMetrics metrics = m_ctx.getResources().getDisplayMetrics();
        return metrics.widthPixels;
    }

    /**
     * Gets the screen height in pixels.
     */
    private int getScreenHeight() {
        android.util.DisplayMetrics metrics = m_ctx.getResources().getDisplayMetrics();
        return metrics.heightPixels;
    }

    /**
     * Parses a dimension value that can be:
     * - Integer (treated as dp)
     * - Fractional 0-1 (treated as percentage of reference size)
     * - String with suffix: "dp", "px", "%"
     *
     * @param value The value from JSON (can be int, double, or String).
     * @param referenceSize The reference size for percentage calculations (e.g., screen width).
     * @return The dimension in pixels.
     */
    private int parseDimensionValue(Object value, int referenceSize) {
        if (value == null) return 0;
        
        // Handle numeric types directly
        if (value instanceof Number) {
            double num = ((Number) value).doubleValue();
            // Fractional value 0-1 = percentage
            if (num > 0 && num <= 1) {
                return (int) (num * referenceSize);
            }
            // Otherwise treat as dp
            return dpToPx((int) num);
        }
        
        // Handle string values
        String strVal = value.toString().toLowerCase().trim();
        if (strVal.isEmpty()) return 0;
        
        try {
            // Check for percentage suffix
            if (strVal.endsWith("%")) {
                float pct = Float.parseFloat(strVal.replace("%", "").trim());
                return (int) ((pct / 100f) * referenceSize);
            }
            
            // Check for px suffix (raw pixels)
            if (strVal.endsWith("px")) {
                return Integer.parseInt(strVal.replace("px", "").trim());
            }
            
            // Remove dp suffix if present
            String numStr = strVal.replace("dp", "").trim();
            double num = Double.parseDouble(numStr);
            
            // Fractional value 0-1 = percentage
            if (num > 0 && num <= 1) {
                return (int) (num * referenceSize);
            }
            
            // Otherwise treat as dp
            return dpToPx((int) num);
            
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Parses a layout dimension value.
     * Supports: "match_parent", "wrap_content", "fill_parent", numeric dp values,
     * or fractional values 0-1 (percentage of screen).
     *
     * @param value The dimension value as string.
     * @return The parsed dimension in pixels or LayoutParams constant.
     */
    private int parseLayoutDimension(String value) {
        if (value == null || value.isEmpty()) {
            return ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        
        String lower = value.toLowerCase().trim();
        switch (lower) {
            case "match_parent":
            case "fill_parent":
            case "-1":
                return ViewGroup.LayoutParams.MATCH_PARENT;
            case "wrap_content":
            case "-2":
                return ViewGroup.LayoutParams.WRAP_CONTENT;
            default:
                // Check for percentage suffix
                if (lower.endsWith("%")) {
                    try {
                        float pct = Float.parseFloat(lower.replace("%", "").trim());
                        // Use screen width as reference for width-like dimensions
                        return (int) ((pct / 100f) * getScreenWidth());
                    } catch (NumberFormatException e) {
                        return ViewGroup.LayoutParams.WRAP_CONTENT;
                    }
                }
                
                // Try to parse as numeric value
                try {
                    // Remove dp/px suffix if present
                    String numStr = lower.replace("dp", "").replace("px", "").trim();
                    double num = Double.parseDouble(numStr);
                    
                    // Fractional value 0-1 = percentage of screen
                    if (num > 0 && num <= 1) {
                        return (int) (num * getScreenWidth());
                    }
                    
                    // Otherwise treat as dp
                    return dpToPx((int) num);
                } catch (NumberFormatException e) {
                    return ViewGroup.LayoutParams.WRAP_CONTENT;
                }
        }
    }

    /**
     * Parses a layout dimension value for height specifically.
     * Uses screen height as reference for percentage calculations.
     *
     * @param value The dimension value as string.
     * @return The parsed dimension in pixels or LayoutParams constant.
     */
    private int parseLayoutDimensionHeight(String value) {
        if (value == null || value.isEmpty()) {
            return ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        
        String lower = value.toLowerCase().trim();
        switch (lower) {
            case "match_parent":
            case "fill_parent":
            case "-1":
                return ViewGroup.LayoutParams.MATCH_PARENT;
            case "wrap_content":
            case "-2":
                return ViewGroup.LayoutParams.WRAP_CONTENT;
            default:
                // Check for percentage suffix
                if (lower.endsWith("%")) {
                    try {
                        float pct = Float.parseFloat(lower.replace("%", "").trim());
                        return (int) ((pct / 100f) * getScreenHeight());
                    } catch (NumberFormatException e) {
                        return ViewGroup.LayoutParams.WRAP_CONTENT;
                    }
                }
                
                // Try to parse as numeric value
                try {
                    String numStr = lower.replace("dp", "").replace("px", "").trim();
                    double num = Double.parseDouble(numStr);
                    
                    // Fractional value 0-1 = percentage of screen height
                    if (num > 0 && num <= 1) {
                        return (int) (num * getScreenHeight());
                    }
                    
                    return dpToPx((int) num);
                } catch (NumberFormatException e) {
                    return ViewGroup.LayoutParams.WRAP_CONTENT;
                }
        }
    }

    /**
     * Parses a gravity string into Android Gravity constant.
     * Supports: "center", "left", "right", "top", "bottom", "start", "end", and combinations.
     *
     * @param gravityStr The gravity string (e.g., "center", "top|left", "center_horizontal").
     * @return The parsed Gravity constant.
     */
    private int parseGravity(String gravityStr) {
        if (gravityStr == null || gravityStr.isEmpty()) {
            return Gravity.NO_GRAVITY;
        }

        String lower = gravityStr.toLowerCase().trim();
        int gravity = Gravity.NO_GRAVITY;

        // Handle pipe-separated values
        String[] parts = lower.split("\\|");
        for (String part : parts) {
            part = part.trim().replace("_", "");
            switch (part) {
                case "center":
                    gravity |= Gravity.CENTER;
                    break;
                case "centerhorizontal":
                    gravity |= Gravity.CENTER_HORIZONTAL;
                    break;
                case "centervertical":
                    gravity |= Gravity.CENTER_VERTICAL;
                    break;
                case "left":
                case "start":
                    gravity |= Gravity.START;
                    break;
                case "right":
                case "end":
                    gravity |= Gravity.END;
                    break;
                case "top":
                    gravity |= Gravity.TOP;
                    break;
                case "bottom":
                    gravity |= Gravity.BOTTOM;
                    break;
                case "fill":
                    gravity |= Gravity.FILL;
                    break;
                case "fillhorizontal":
                    gravity |= Gravity.FILL_HORIZONTAL;
                    break;
                case "fillvertical":
                    gravity |= Gravity.FILL_VERTICAL;
                    break;
            }
        }

        return gravity == Gravity.NO_GRAVITY ? Gravity.START : gravity;
    }

    /**
     * Attempts to invoke a specific method on a View using reflection.
     * It scans the view's available methods to find a match by name that accepts a single parameter,
     * then attempts to convert the provided value to the required parameter type before execution.
     *
     * @param view       The target View object on which the method will be called.
     * @param methodName The name of the method to invoke (e.g., "setText", "setBackgroundColor", or "loadUrl").
     * @param value      The raw value to be passed to the method, which will be converted to the appropriate type.
     * @return true if the method was found and invoked successfully, false otherwise.
     */
    private boolean invokeMethod(View view, String methodName, Object value) {
        for (Method method : view.getClass().getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterTypes().length == 1) {
                try {
                    Class<?> paramType = method.getParameterTypes()[0];
                    Object convertedValue = convertValue(value, paramType, methodName);
                    if (convertedValue != null) {
                        method.invoke(view, convertedValue);
                        // Sync view state after property change (debounced on next frame)
                        m_mainHandler.post(() -> syncViewStateToFile());
                        return true;
                    }
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    /**
     * Converts a value to the target type required by a setter method.
     * Handles common types including colors, primitives, strings,
     * orientation, gravity, and other Android-specific values.
     *
     * @param value      The value to convert.
     * @param targetType The target parameter type.
     * @param methodName The method name (used to detect special setters).
     * @return The converted value, or null if conversion failed.
     */
    private Object convertValue(Object value, Class<?> targetType, String methodName) {
        try {
            String strValue = value.toString().toLowerCase().trim();
            String originalValue = value.toString().trim();
            
            // Handle "new ClassName()" pattern - instantiate objects dynamically
            if (originalValue.startsWith("new ") && originalValue.endsWith("()")) {
                String className = originalValue.substring(4, originalValue.length() - 2).trim();
                return instantiateClass(className, targetType);
            }
            
            // Handle color methods
            if (methodName.toLowerCase().contains("color") && value instanceof String) {
                return Color.parseColor((String) value);
            }
            
            // Handle ColorStateList for tint methods (backgroundTintList, foregroundTintList, etc.)
            if (methodName.toLowerCase().contains("tintlist") && value instanceof String) {
                int color = Color.parseColor((String) value);
                return ColorStateList.valueOf(color);
            }
            
            // Handle orientation (for LinearLayout)
            if (methodName.equals("setOrientation")) {
                if (strValue.equals("vertical") || strValue.equals("1")) {
                    return LinearLayout.VERTICAL;
                } else if (strValue.equals("horizontal") || strValue.equals("0")) {
                    return LinearLayout.HORIZONTAL;
                }
            }
            
            // Handle gravity methods
            if (methodName.toLowerCase().contains("gravity")) {
                return parseGravity(value.toString());
            }
            
            // Handle visibility
            if (methodName.equals("setVisibility")) {
                switch (strValue) {
                    case "visible":
                    case "0":
                        return View.VISIBLE;
                    case "invisible":
                    case "4":
                        return View.INVISIBLE;
                    case "gone":
                    case "8":
                        return View.GONE;
                }
            }
            
            // Handle scaleType for ImageView
            if (methodName.equals("setScaleType") && value instanceof String) {
                switch (strValue) {
                    case "center": return ImageView.ScaleType.CENTER;
                    case "centercrop": return ImageView.ScaleType.CENTER_CROP;
                    case "centerinside": return ImageView.ScaleType.CENTER_INSIDE;
                    case "fitcenter": return ImageView.ScaleType.FIT_CENTER;
                    case "fitend": return ImageView.ScaleType.FIT_END;
                    case "fitstart": return ImageView.ScaleType.FIT_START;
                    case "fitxy": return ImageView.ScaleType.FIT_XY;
                    case "matrix": return ImageView.ScaleType.MATRIX;
                }
            }
            
            // Handle inputType for EditText
            if (methodName.equals("setInputType")) {
                int inputType = 0;
                if (strValue.contains("text")) inputType |= android.text.InputType.TYPE_CLASS_TEXT;
                if (strValue.contains("number")) inputType |= android.text.InputType.TYPE_CLASS_NUMBER;
                if (strValue.contains("phone")) inputType |= android.text.InputType.TYPE_CLASS_PHONE;
                if (strValue.contains("email")) inputType |= android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
                if (strValue.contains("password")) inputType |= android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;
                if (strValue.contains("multiline")) inputType |= android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE;
                if (inputType == 0) inputType = android.text.InputType.TYPE_CLASS_TEXT;
                return inputType;
            }
            
            // Standard type conversions
            if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(value.toString());
            }
            if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(value.toString());
            }
            if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(value.toString());
            }
            if (targetType == CharSequence.class || targetType == String.class) {
                return value.toString();
            }
            if (targetType.isInstance(value)) {
                return value;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     * Instantiates a class by name using reflection.
     * Searches common Android packages if the class name is not fully qualified.
     *
     * @param className The class name (simple or fully qualified).
     * @param targetType The expected target type (used for validation).
     * @return A new instance of the class, or null if instantiation failed.
     */
    private Object instantiateClass(String className, Class<?> targetType) {
        // Packages to search for simple class names
        String[] packages = {
            "",  // Fully qualified name
            "android.webkit.",
            "android.widget.",
            "android.view.",
            "android.graphics.",
            "android.graphics.drawable.",
            "android.text.",
            "android.text.method.",
            "android.content.",
            "android.os.",
            "androidx.appcompat.widget.",
            "com.google.android.material."
        };

        for (String pkg : packages) {
            try {
                String fullClassName = pkg.isEmpty() && className.contains(".") ? className : pkg + className;
                Class<?> cls = Class.forName(fullClassName);
                
                // Check if the class is assignable to the target type
                if (targetType.isAssignableFrom(cls)) {
                    // Try no-arg constructor first
                    try {
                        Constructor<?> ctor = cls.getConstructor();
                        return ctor.newInstance();
                    } catch (NoSuchMethodException e) {
                        // Try constructor with Context
                        try {
                            Constructor<?> ctorCtx = cls.getConstructor(Context.class);
                            return ctorCtx.newInstance(m_ctx);
                        } catch (NoSuchMethodException e2) {
                            // No suitable constructor found
                        }
                    }
                }
            } catch (ClassNotFoundException ignored) {
                // Try next package
            } catch (Exception e) {
                Log.w(TAG, "Could not instantiate " + className + ": " + e.getMessage());
            }
        }
        
        Log.w(TAG, "Could not find or instantiate class: " + className);
        return null;
    }

    private void updateView(String viewId, String attrsJson) {
        m_mainHandler.post(() -> {
            try {
                JSONObject attrs = new JSONObject(attrsJson);
                updateViewInternal(viewId, attrs);
            } catch (Exception e) {
                Log.e(TAG, "Failed to update view: " + viewId, e);
            }
        });
    }

    private void updateViewInternal(String viewId, JSONObject attrs) {
        View view = m_viewRegistry.get(viewId);
        if (view != null) {
            applyAttributes(view, attrs);
            // Sync view state after property change
            syncViewStateToFile();
        } else {
            Log.w(TAG, "View not found: " + viewId);
        }
    }

    private String getViewText(String viewId) {
        View view = m_viewRegistry.get(viewId);
        if (view instanceof TextView) {
            return ((TextView) view).getText().toString();
        }
        return "";
    }

    /**
     * Gets a property value from a view using reflection.
     * Tries getter methods: getProperty(), property(), isProperty()
     *
     * @param viewId The view ID to get property from.
     * @param property The property name (e.g., "url", "text", "checked").
     * @return JSON with the property value or error.
     */
    private String getViewProperty(String viewId, String property) {
        View view = m_viewRegistry.get(viewId);
        if (view == null) {
            return "{\"error\": \"View not found: " + viewId + "\"}";
        }
        
        if (property == null || property.isEmpty()) {
            return "{\"error\": \"Property name required\"}";
        }
        
        // Try different getter patterns
        String capProperty = property.substring(0, 1).toUpperCase() + property.substring(1);
        String[] methodNames = {
            "get" + capProperty,  // getUrl(), getText()
            property,              // url(), text()
            "is" + capProperty    // isChecked(), isEnabled()
        };
        
        for (String methodName : methodNames) {
            try {
                Method method = view.getClass().getMethod(methodName);
                Object result = method.invoke(view);
                return formatPropertyResult(property, result);
            } catch (NoSuchMethodException ignored) {
                // Try next pattern
            } catch (Exception e) {
                return "{\"error\": \"Failed to get " + property + ": " + e.getMessage() + "\"}";
            }
        }
        
        return "{\"error\": \"Property not found: " + property + " on " + view.getClass().getSimpleName() + "\"}";
    }
    
    /**
     * Formats a property result as JSON.
     */
    private String formatPropertyResult(String property, Object value) {
        if (value == null) {
            return "{\"" + property + "\": null}";
        }
        
        if (value instanceof Boolean || value instanceof Number) {
            return "{\"" + property + "\": " + value + "}";
        }
        
        // String or other - escape for JSON
        String strVal = value.toString()
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
        return "{\"" + property + "\": \"" + strVal + "\"}";
    }

    private void setViewText(String viewId, String text) {
        m_mainHandler.post(() -> {
            View view = m_viewRegistry.get(viewId);
            if (view instanceof TextView) {
                ((TextView) view).setText(text);
                // Sync view state after text change
                syncViewStateToFile();
            }
        });
    }

    // -------------------------------------------------------------------------
    // Start PHP App
    // -------------------------------------------------------------------------

    private void startPhpApp(String initialMethod,String entryFile) {
        if (initialMethod == null || initialMethod.isEmpty()) {
            initialMethod = "index";
        }

        final String method = initialMethod;
  m_entryFile = entryFile != null && !entryFile.isEmpty() ? entryFile : null;

        // Clear history and push initial screen
        m_screenHistory.clear();
        m_screenHistory.add(new String[]{method, "{}"});

        m_executor.execute(() -> {
            String uiJson = callPhp(method, null);
            Log.d(TAG, "startPhpApp result: " + (uiJson != null ? uiJson.substring(0, Math.min(300, uiJson.length())) : "null"));
            
            if (uiJson != null && !uiJson.isEmpty()) {
                // Check if it's an error
                if (uiJson.contains("\"error\"")) {
                    notifyError("PHP error: " + uiJson);
                    return;
                }
                
                m_mainHandler.post(() -> {
                    processPhpResponse(uiJson);
                });
            } else {
                notifyError("PHP returned empty response");
            }
        });
    }

    /**
     * Check if app.php exists in the current app folder
     */
    private boolean hasAppPhp() {
        String scriptDir = getPhpScriptDir();
        File appPhp = new File(scriptDir, "app.php");
        return appPhp.exists();
    }

    /**
     * Run app.php directly and render its UI output.
     * This executes app.php which auto-instantiates MyApp class and calls the method.
     * @param method Method name to call (default: index)
     * @param entryFile PHP file to execute (default: app.php)
     */
    private void runAppPhp(String method) {
        if (!m_phpReady) {
            notifyError("PHP not ready");
            return;
        }

        if (method == null || method.isEmpty()) {
            method = "index";
        }

        final String finalMethod = method;

        // Clear history and push initial screen
        m_screenHistory.clear();
        m_screenHistory.add(new String[]{finalMethod, "{}"});

        m_executor.execute(() -> {
            try {
                String scriptDir = getPhpScriptDir();
                File appPhp = new File(scriptDir, "app.php");
                
                if (!appPhp.exists()) {
                    Log.w(TAG, "app.php not found in " + scriptDir);
                    notifyError("app.php not found in " + scriptDir);
                    return;
                }

                ProcessBuilder pb = new ProcessBuilder(
                    m_phpPath,
                    appPhp.getAbsolutePath(),
                    "--method=" + finalMethod
                );
                pb.redirectErrorStream(true);
                pb.directory(new File(scriptDir));

                Process process = pb.start();
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
                );

                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }

                process.waitFor();
                String uiJson = output.toString().trim();
                
                Log.d(TAG, "app.php output: " + uiJson.substring(0, Math.min(200, uiJson.length())));

                if (uiJson != null && !uiJson.isEmpty() && !uiJson.contains("\"error\"")) {
                    m_mainHandler.post(() -> {
                        renderUI(uiJson);
                    });
                } else {
                    notifyError("app.php returned error: " + uiJson);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error running app.php", e);
                notifyError("Error running app.php: " + e.getMessage());
            }
        });
    }

    /**
     * Run any PHP file and render its UI output.
     * @param filePath Path to PHP file (relative to plugin assets or absolute)
     * @param method Method to call (default: index)
     */
    private void runPhpFile(String filePath, String method,String appName) {
        if (!m_phpReady) {
            notifyError("PHP not ready");
            return;
        }

        if (filePath == null || filePath.isEmpty()) {
            notifyError("File path is required");
            return;
        }

        if (method == null || method.isEmpty()) {
            method = "index";
        }

        final String finalMethod = method;
        final String finalPath = filePath;
        
        m_executor.execute(() -> {
            try {
                // Try to find the PHP file in various locations
                File phpFile = null;
                String extStorage = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
                String targetDir = getDroidScriptAppPath(appName);
                if (targetDir == null) {
                    notifyError("Could not determine app folder for: " + appName);
                    return;
                }
                String[] searchPaths = {
                    targetDir + "/" + finalPath,
                };
                
                for (String searchPath : searchPaths) {
                    File f = new File(searchPath);
                    if (f.exists() && f.canRead()) {
                        phpFile = f;
                        break;
                    }
                }
                
                if (phpFile == null || !phpFile.exists()) {
                    notifyError("PHP file not found: " + finalPath);
                    return;
                }

                // Build include path - include parent folder (for simple.php) and script dir
                String includePath = phpFile.getParentFile().getAbsolutePath();
                File parentDir = phpFile.getParentFile().getParentFile();
                if (parentDir != null && parentDir.exists()) {
                    includePath = parentDir.getAbsolutePath() + ":" + includePath;
                }
                // Also add main script dir where simple.php/ui_core.php typically are
                String scriptDir = getPhpScriptDir();
                if (scriptDir != null && !includePath.contains(scriptDir)) {
                    includePath = scriptDir + ":" + includePath;
                }

                ProcessBuilder pb = new ProcessBuilder(
                    m_phpPath,
                    "-d", "include_path=" + includePath,
                    phpFile.getAbsolutePath(),
                    "--method=" + finalMethod
                );
                pb.redirectErrorStream(true);
                pb.directory(phpFile.getParentFile());

                Process process = pb.start();
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
                );

                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }

                process.waitFor();
                String uiJson = output.toString().trim();
                
                Log.d(TAG, "PHP file output (" + uiJson.length() + " chars): " + uiJson.substring(0, Math.min(500, uiJson.length())));

                if (uiJson == null || uiJson.isEmpty()) {
                    notifyError("PHP returned empty output. File: " + phpFile.getAbsolutePath() + ", Include: " + includePath);
                } else if (uiJson.contains("\"error\"")) {
                    notifyError("PHP error: " + uiJson);
                } else {
                    m_mainHandler.post(() -> {
                        renderUI(uiJson);
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error running PHP file", e);
                notifyError("Error running PHP file: " + e.getMessage());
            }
        });
    }

    /**
     * Run any PHP file and return debug info (synchronous).
     * @param filePath Path to PHP file
     * @param method Method to call
     * @return JSON with debug info and raw output
     */
    private String runPhpFileDebug(String filePath, String method) {
        try {
            if (!m_phpReady) {
                return "{\"error\": \"PHP not ready\"}";
            }

            if (filePath == null || filePath.isEmpty()) {
                return "{\"error\": \"File path is required\"}";
            }

            if (method == null || method.isEmpty()) {
                method = "index";
            }

            // Try to find the PHP file in various locations
            File phpFile = null;
            String extStorage = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
            StringBuilder searched = new StringBuilder();
            
            String[] searchPaths = {
                filePath,
                m_appDir + "/" + filePath,
                getPhpScriptDir() + "/" + filePath,
                m_plugDir + "/assets/" + filePath,
                m_plugDir + "/" + filePath,
                extStorage + "/DroidScript/Plugins/phpnativeplugin/assets/" + filePath,
                extStorage + "/DroidScript/Plugins/PhpNativePlugin/assets/" + filePath,
                m_filesDir + "/" + filePath
            };
            
            for (String searchPath : searchPaths) {
                File f = new File(searchPath);
                searched.append(searchPath).append(":").append(f.exists() ? "Y" : "N").append("; ");
                if (f.exists() && f.canRead()) {
                    phpFile = f;
                    break;
                }
            }
            
            if (phpFile == null || !phpFile.exists()) {
                return "{\"error\": \"File not found\", \"searched\": \"" + searched.toString().replace("\"", "'") + "\"}";
            }

            // Build include path
            String includePath = phpFile.getParentFile().getAbsolutePath();
            File parentDir = phpFile.getParentFile().getParentFile();
            if (parentDir != null && parentDir.exists()) {
                includePath = parentDir.getAbsolutePath() + ":" + includePath;
            }
            String scriptDir = getPhpScriptDir();
            if (scriptDir != null && !includePath.contains(scriptDir)) {
                includePath = scriptDir + ":" + includePath;
            }

            ProcessBuilder pb = new ProcessBuilder(
                m_phpPath,
                "-d", "include_path=" + includePath,
                phpFile.getAbsolutePath(),
                "--method=" + method
            );
            pb.redirectErrorStream(true);
            pb.directory(phpFile.getParentFile());

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            String rawOutput = output.toString().trim();
            
            // Escape for JSON
            String escapedOutput = rawOutput.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
            
            return "{\"file\": \"" + phpFile.getAbsolutePath() + "\", \"includePath\": \"" + includePath + "\", \"method\": \"" + method + "\", \"exitCode\": " + exitCode + ", \"outputLength\": " + rawOutput.length() + ", \"output\": \"" + escapedOutput.substring(0, Math.min(1000, escapedOutput.length())) + "\"}";

        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private int dpToPx(int dp) {
        float density = m_ctx.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void notifyError(String message) {
        if (m_OnError != null) {
            m_mainHandler.post(() -> {
                try {
                    Bundle b = new Bundle();
                    b.putString("cmd", m_OnError);
                    b.putString("p1", message);
                    CallScript(b);
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying error", e);
                }
            });
        }
    }

    // -------------------------------------------------------------------------
    // ListView Management
    // -------------------------------------------------------------------------
    
    /**
     * Set up a ListView with items and click handlers.
     */
    private void setupListView(android.widget.ListView listView, String viewId, JSONObject json) {
        try {
            JSONArray itemsArray = json.optJSONArray("items");
            if (itemsArray == null) return;
            
            // Create list data
            List<String> items = new ArrayList<>();
            for (int i = 0; i < itemsArray.length(); i++) {
                Object item = itemsArray.get(i);
                if (item instanceof JSONObject) {
                    // Complex item - use "title" field or stringify
                    JSONObject itemObj = (JSONObject) item;
                    items.add(itemObj.optString("title", itemObj.toString()));
                } else {
                    items.add(item.toString());
                }
            }
            
            // Store data for later access
            if (viewId != null) {
                m_listData.put(viewId, items);
            }
            
            // Create adapter
            String itemLayout = json.optString("itemLayout", "simple");
            int layoutRes = getListItemLayout(itemLayout);
            
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                m_ctx, layoutRes, items
            );
            listView.setAdapter(adapter);
            
            // Store adapter for later updates
            if (viewId != null) {
                m_listAdapters.put(viewId, adapter);
            }
            
            // Set up click handler
            String onItemClick = json.optString("onItemClick", null);
            if (onItemClick != null) {
                final String clickHandler = onItemClick;
                listView.setOnItemClickListener((parent, view, position, id) -> {
                    String itemText = items.get(position);
                    JSONObject params = new JSONObject();
                    try {
                        params.put("position", position);
                        params.put("item", itemText);
                        params.put("viewId", viewId);
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating click params", e);
                    }
                    
                    m_executor.execute(() -> {
                        String response = callPhp(clickHandler, params.toString());
                        m_mainHandler.post(() -> processPhpResponse(response));
                    });
                });
            }
            
            // Set up long click handler
            String onItemLongClick = json.optString("onItemLongClick", null);
            if (onItemLongClick != null) {
                final String longClickHandler = onItemLongClick;
                listView.setOnItemLongClickListener((parent, view, position, id) -> {
                    String itemText = items.get(position);
                    JSONObject params = new JSONObject();
                    try {
                        params.put("position", position);
                        params.put("item", itemText);
                        params.put("viewId", viewId);
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating long click params", e);
                    }
                    
                    m_executor.execute(() -> {
                        String response = callPhp(longClickHandler, params.toString());
                        m_mainHandler.post(() -> processPhpResponse(response));
                    });
                    return true;
                });
            }
            
            // Handle dividers
            boolean showDividers = json.optBoolean("showDividers", true);
            if (!showDividers) {
                listView.setDivider(null);
                listView.setDividerHeight(0);
            }
            
            // If ListView is in a ScrollView, we need to calculate total height
            // This is a workaround for the ListView-in-ScrollView problem
            boolean expandHeight = json.optBoolean("expandHeight", true);
            if (expandHeight) {
                // Post to run after adapter is set
                listView.post(() -> setListViewHeightBasedOnChildren(listView));
            }
            
            Log.d(TAG, "ListView setup complete: " + viewId + " with " + items.size() + " items");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up ListView", e);
        }
    }
    
    /**
     * Utility method to set ListView height when inside a ScrollView.
     * Measures all items and sets a fixed height.
     */
    private void setListViewHeightBasedOnChildren(android.widget.ListView listView) {
        android.widget.ListAdapter adapter = listView.getAdapter();
        if (adapter == null) return;
        
        int totalHeight = 0;
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);
        
        for (int i = 0; i < adapter.getCount(); i++) {
            View listItem = adapter.getView(i, null, listView);
            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += listItem.getMeasuredHeight();
        }
        
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (adapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
        
        Log.d(TAG, "Set ListView height to: " + params.height + "px for " + adapter.getCount() + " items");
    }
    
    private int getListItemLayout(String layout) {
        switch (layout) {
            case "two_line":
                return android.R.layout.simple_list_item_2;
            case "checkbox":
                return android.R.layout.simple_list_item_multiple_choice;
            case "radio":
                return android.R.layout.simple_list_item_single_choice;
            case "simple":
            default:
                return android.R.layout.simple_list_item_1;
        }
    }
    
    private void handleListSetItems(JSONObject response) {
        String viewId = response.optString("target");
        JSONArray itemsArray = response.optJSONArray("items");
        
        m_mainHandler.post(() -> {
            try {
                android.widget.ArrayAdapter<String> adapter = m_listAdapters.get(viewId);
                List<String> data = m_listData.get(viewId);
                
                if (adapter != null && data != null) {
                    data.clear();
                    if (itemsArray != null) {
                        for (int i = 0; i < itemsArray.length(); i++) {
                            Object item = itemsArray.get(i);
                            if (item instanceof JSONObject) {
                                data.add(((JSONObject) item).optString("title", item.toString()));
                            } else {
                                data.add(item.toString());
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    recalculateListViewHeight(viewId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting list items", e);
            }
        });
    }
    
    private void handleListAddItem(JSONObject response) {
        String viewId = response.optString("target");
        Object item = response.opt("item");
        
        m_mainHandler.post(() -> {
            try {
                android.widget.ArrayAdapter<String> adapter = m_listAdapters.get(viewId);
                List<String> data = m_listData.get(viewId);
                
                if (adapter != null && data != null && item != null) {
                    String itemText = (item instanceof JSONObject) 
                        ? ((JSONObject) item).optString("title", item.toString())
                        : item.toString();
                    data.add(itemText);
                    adapter.notifyDataSetChanged();
                    recalculateListViewHeight(viewId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error adding list item", e);
            }
        });
    }
    
    private void handleListAddItems(JSONObject response) {
        String viewId = response.optString("target");
        JSONArray itemsArray = response.optJSONArray("items");
        
        m_mainHandler.post(() -> {
            try {
                android.widget.ArrayAdapter<String> adapter = m_listAdapters.get(viewId);
                List<String> data = m_listData.get(viewId);
                
                if (adapter != null && data != null && itemsArray != null) {
                    for (int i = 0; i < itemsArray.length(); i++) {
                        Object item = itemsArray.get(i);
                        String itemText = (item instanceof JSONObject) 
                            ? ((JSONObject) item).optString("title", item.toString())
                            : item.toString();
                        data.add(itemText);
                    }
                    adapter.notifyDataSetChanged();
                    recalculateListViewHeight(viewId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error adding list items", e);
            }
        });
    }
    
    private void handleListInsertItem(JSONObject response) {
        String viewId = response.optString("target");
        int position = response.optInt("position", 0);
        Object item = response.opt("item");
        
        m_mainHandler.post(() -> {
            try {
                android.widget.ArrayAdapter<String> adapter = m_listAdapters.get(viewId);
                List<String> data = m_listData.get(viewId);
                
                if (adapter != null && data != null && item != null) {
                    String itemText = (item instanceof JSONObject) 
                        ? ((JSONObject) item).optString("title", item.toString())
                        : item.toString();
                    if (position >= 0 && position <= data.size()) {
                        data.add(position, itemText);
                        adapter.notifyDataSetChanged();
                        recalculateListViewHeight(viewId);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error inserting list item", e);
            }
        });
    }
    
    private void handleListRemoveItem(JSONObject response) {
        String viewId = response.optString("target");
        int position = response.optInt("position", -1);
        
        m_mainHandler.post(() -> {
            try {
                android.widget.ArrayAdapter<String> adapter = m_listAdapters.get(viewId);
                List<String> data = m_listData.get(viewId);
                
                if (adapter != null && data != null && position >= 0 && position < data.size()) {
                    data.remove(position);
                    adapter.notifyDataSetChanged();
                    recalculateListViewHeight(viewId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error removing list item", e);
            }
        });
    }
    
    private void handleListUpdateItem(JSONObject response) {
        String viewId = response.optString("target");
        int position = response.optInt("position", -1);
        Object item = response.opt("item");
        
        m_mainHandler.post(() -> {
            try {
                android.widget.ArrayAdapter<String> adapter = m_listAdapters.get(viewId);
                List<String> data = m_listData.get(viewId);
                
                if (adapter != null && data != null && item != null 
                    && position >= 0 && position < data.size()) {
                    String itemText = (item instanceof JSONObject) 
                        ? ((JSONObject) item).optString("title", item.toString())
                        : item.toString();
                    data.set(position, itemText);
                    adapter.notifyDataSetChanged();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating list item", e);
            }
        });
    }
    
    private void handleListScroll(JSONObject response, boolean smooth) {
        String viewId = response.optString("target");
        int position = response.optInt("position", 0);
        
        m_mainHandler.post(() -> {
            View view = m_viewRegistry.get(viewId);
            if (view instanceof android.widget.ListView) {
                android.widget.ListView listView = (android.widget.ListView) view;
                if (smooth) {
                    listView.smoothScrollToPosition(position);
                } else {
                    listView.setSelection(position);
                }
            }
        });
    }
    
    /**
     * Recalculate ListView height after items change.
     */
    private void recalculateListViewHeight(String viewId) {
        View view = m_viewRegistry.get(viewId);
        if (view instanceof android.widget.ListView) {
            android.widget.ListView listView = (android.widget.ListView) view;
            listView.post(() -> setListViewHeightBasedOnChildren(listView));
        }
    }

    // =========================================================================
    // DRAWER LAYOUT IMPLEMENTATION
    // =========================================================================
    
    /**
     * State holder for drawer layouts
     */
    private static class DrawerState {
        FrameLayout container;
        View drawerView;
        View contentView;
        View overlay;
        boolean isOpen = false;
        int drawerWidth = 280; // dp
        String viewId;
    }
    
    /**
     * State holder for bottom nav bars
     */
    private static class BottomNavState {
        LinearLayout container;
        List<View> itemViews = new ArrayList<>();
        List<JSONObject> items = new ArrayList<>();
        String selectedId;
        String onItemSelected;
        String viewId;
    }
    
    private FrameLayout createDrawerLayout() {
        FrameLayout container = new FrameLayout(m_ctx);
        container.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        return container;
    }
    
    private View processDrawerLayout(JSONObject item) {
        String viewId = item.optString("id", "drawer_" + System.currentTimeMillis());
        int drawerWidth = item.optInt("drawerWidth", 280);
        
        // Create container
        FrameLayout container = createDrawerLayout();
        m_viewRegistry.put(viewId, container);
        
        DrawerState state = new DrawerState();
        state.container = container;
        state.viewId = viewId;
        state.drawerWidth = drawerWidth;
        m_drawerStates.put(viewId, state);
        
        // Process main content
        JSONObject contentJson = item.optJSONObject("content");
        if (contentJson != null) {
            View contentView = processComponentRecursive(contentJson);
            if (contentView != null) {
                contentView.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ));
                container.addView(contentView);
                state.contentView = contentView;
            }
        }
        
        // Create overlay (semi-transparent background when drawer is open)
        View overlay = new View(m_ctx);
        overlay.setBackgroundColor(Color.parseColor("#80000000"));
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        overlay.setVisibility(View.GONE);
        overlay.setOnClickListener(v -> closeDrawer(viewId));
        container.addView(overlay);
        state.overlay = overlay;
        
        // Process drawer content
        JSONObject drawerJson = item.optJSONObject("drawer");
        if (drawerJson != null) {
            View drawerView = processComponentRecursive(drawerJson);
            if (drawerView != null) {
                FrameLayout.LayoutParams drawerParams = new FrameLayout.LayoutParams(
                    dpToPx(drawerWidth),
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.START
                );
                drawerView.setLayoutParams(drawerParams);
                drawerView.setTranslationX(-dpToPx(drawerWidth)); // Start hidden
                drawerView.setBackgroundColor(Color.WHITE);
                drawerView.setElevation(dpToPx(16));
                container.addView(drawerView);
                state.drawerView = drawerView;
            }
        }
        
        return container;
    }
    
    private void openDrawer(String viewId) {
        DrawerState state = m_drawerStates.get(viewId);
        if (state == null || state.isOpen) return;
        
        state.isOpen = true;
        state.overlay.setVisibility(View.VISIBLE);
        state.overlay.animate().alpha(1f).setDuration(250).start();
        state.drawerView.animate().translationX(0).setDuration(250).start();
    }
    
    private void closeDrawer(String viewId) {
        DrawerState state = m_drawerStates.get(viewId);
        if (state == null || !state.isOpen) return;
        
        state.isOpen = false;
        state.overlay.animate().alpha(0f).setDuration(250).withEndAction(() -> 
            state.overlay.setVisibility(View.GONE)
        ).start();
        state.drawerView.animate().translationX(-dpToPx(state.drawerWidth)).setDuration(250).start();
    }
    
    private void toggleDrawer(String viewId) {
        DrawerState state = m_drawerStates.get(viewId);
        if (state == null) return;
        
        if (state.isOpen) {
            closeDrawer(viewId);
        } else {
            openDrawer(viewId);
        }
    }
    
    private void handleDrawerAction(JSONObject response, String action) {
        String viewId = response.optString("target");
        m_mainHandler.post(() -> {
            switch (action) {
                case "open": openDrawer(viewId); break;
                case "close": closeDrawer(viewId); break;
                case "toggle": toggleDrawer(viewId); break;
            }
        });
    }

    // =========================================================================
    // NAVIGATION DRAWER IMPLEMENTATION
    // =========================================================================
    
    private LinearLayout createNavigationDrawerView() {
        LinearLayout layout = new LinearLayout(m_ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.WHITE);
        return layout;
    }
    
    private View processNavigationDrawer(JSONObject item) {
        String viewId = item.optString("id", "nav_drawer_" + System.currentTimeMillis());
        String onItemSelected = item.optString("onItemSelected", null);
        String selectedItem = item.optString("selectedItem", null);
        
        LinearLayout container = createNavigationDrawerView();
        m_viewRegistry.put(viewId, container);
        
        // Process header if present
        JSONObject headerJson = item.optJSONObject("header");
        if (headerJson != null) {
            View headerView = processComponentRecursive(headerJson);
            if (headerView != null) {
                container.addView(headerView);
            }
        }
        
        // Process menu items
        JSONArray itemsArray = item.optJSONArray("items");
        if (itemsArray != null) {
            for (int i = 0; i < itemsArray.length(); i++) {
                try {
                    Object menuItem = itemsArray.get(i);
                    
                    if ("divider".equals(menuItem)) {
                        // Add divider
                        View divider = new View(m_ctx);
                        divider.setBackgroundColor(Color.parseColor("#E0E0E0"));
                        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1)
                        );
                        dividerParams.setMargins(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
                        divider.setLayoutParams(dividerParams);
                        container.addView(divider);
                    } else if (menuItem instanceof JSONObject) {
                        JSONObject itemObj = (JSONObject) menuItem;
                        View menuItemView = createNavDrawerItem(itemObj, viewId, onItemSelected, 
                            itemObj.optString("id", "").equals(selectedItem));
                        container.addView(menuItemView);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing nav drawer item", e);
                }
            }
        }
        
        return container;
    }
    
    private View createNavDrawerItem(JSONObject item, String drawerId, String onItemSelected, boolean selected) {
        String itemId = item.optString("id", "");
        String title = item.optString("title", "Menu Item");
        String icon = item.optString("icon", null);
        
        LinearLayout itemLayout = new LinearLayout(m_ctx);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemLayout.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        
        // Set selected state
        if (selected) {
            itemLayout.setBackgroundColor(Color.parseColor("#E3F2FD"));
        } else {
            // Add ripple effect
            TypedValue outValue = new TypedValue();
            m_ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            itemLayout.setBackgroundResource(outValue.resourceId);
        }
        
        // Icon placeholder (text-based for now)
        if (icon != null) {
            TextView iconView = new TextView(m_ctx);
            iconView.setText(getIconChar(icon));
            iconView.setTextSize(24);
            iconView.setTextColor(selected ? Color.parseColor("#1976D2") : Color.parseColor("#757575"));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                dpToPx(24), dpToPx(24)
            );
            iconParams.setMarginEnd(dpToPx(32));
            iconView.setLayoutParams(iconParams);
            iconView.setGravity(Gravity.CENTER);
            itemLayout.addView(iconView);
        }
        
        // Title
        TextView titleView = new TextView(m_ctx);
        titleView.setText(title);
        titleView.setTextSize(14);
        titleView.setTextColor(selected ? Color.parseColor("#1976D2") : Color.parseColor("#212121"));
        if (selected) {
            titleView.setTypeface(null, Typeface.BOLD);
        }
        itemLayout.addView(titleView);
        
        // Click handler
        itemLayout.setOnClickListener(v -> {
            if (onItemSelected != null) {
                JSONObject params = new JSONObject();
                try {
                    params.put("itemId", itemId);
                    params.put("title", title);
                    params.put("drawerId", drawerId);
                } catch (Exception e) {}
                
                // Close drawer first
                for (DrawerState state : m_drawerStates.values()) {
                    if (state.isOpen) {
                        closeDrawer(state.viewId);
                    }
                }
                
                m_executor.execute(() -> {
                    String response = callPhp(onItemSelected, params.toString());
                    m_mainHandler.post(() -> processPhpResponse(response));
                });
            }
        });
        
        return itemLayout;
    }
    
    private void handleNavSetItems(JSONObject response) {
        // TODO: Implement dynamic nav drawer item updates
        Log.d(TAG, "handleNavSetItems not fully implemented yet");
    }

    // =========================================================================
    // TOP APP BAR IMPLEMENTATION
    // =========================================================================
    
    private LinearLayout createTopAppBar() {
        LinearLayout appBar = new LinearLayout(m_ctx);
        appBar.setOrientation(LinearLayout.HORIZONTAL);
        appBar.setGravity(Gravity.CENTER_VERTICAL);
        appBar.setBackgroundColor(Color.parseColor("#1976D2")); // Primary blue
        appBar.setElevation(dpToPx(4));
        appBar.setPadding(dpToPx(4), 0, dpToPx(4), 0);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(56)
        );
        appBar.setLayoutParams(params);
        
        return appBar;
    }
    
    private View processTopAppBar(JSONObject item) {
        String viewId = item.optString("id", "app_bar_" + System.currentTimeMillis());
        String title = item.optString("title", "");
        String subtitle = item.optString("subtitle", null);
        String navIcon = item.optString("navigationIcon", null);
        String onNavClick = item.optString("onNavigationClick", null);
        String onActionClick = item.optString("onActionClick", null);
        String bgColor = item.optString("backgroundColor", "#1976D2");
        String titleColor = item.optString("titleColor", "#FFFFFF");
        int elevation = item.optInt("elevation", 4);
        
        LinearLayout appBar = createTopAppBar();
        appBar.setBackgroundColor(Color.parseColor(bgColor));
        appBar.setElevation(dpToPx(elevation));
        m_viewRegistry.put(viewId, appBar);
        
        // Navigation icon (hamburger menu, back arrow, etc.)
        if (navIcon != null) {
            TextView navButton = new TextView(m_ctx);
            navButton.setText(getIconChar(navIcon));
            navButton.setTextSize(24);
            navButton.setTextColor(Color.parseColor(titleColor));
            navButton.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
            navButton.setGravity(Gravity.CENTER);
            
            if (onNavClick != null) {
                final String clickHandler = onNavClick;
                navButton.setOnClickListener(v -> {
                    // Special handling for drawer toggle
                    if ("onToggleDrawer".equals(clickHandler)) {
                        for (DrawerState state : m_drawerStates.values()) {
                            toggleDrawer(state.viewId);
                            return;
                        }
                    }
                    
                    m_executor.execute(() -> {
                        String response = callPhp(clickHandler, "{}");
                        m_mainHandler.post(() -> processPhpResponse(response));
                    });
                });
            }
            appBar.addView(navButton);
        }
        
        // Title and subtitle container
        LinearLayout titleContainer = new LinearLayout(m_ctx);
        titleContainer.setOrientation(LinearLayout.VERTICAL);
        titleContainer.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ));
        titleContainer.setPadding(dpToPx(16), 0, dpToPx(16), 0);
        
        // Title
        TextView titleView = new TextView(m_ctx);
        titleView.setText(title);
        titleView.setTextSize(20);
        titleView.setTextColor(Color.parseColor(titleColor));
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setId(View.generateViewId());
        titleContainer.addView(titleView);
        m_viewRegistry.put(viewId + "_title", titleView);
        
        // Subtitle
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView subtitleView = new TextView(m_ctx);
            subtitleView.setText(subtitle);
            subtitleView.setTextSize(14);
            subtitleView.setTextColor(Color.parseColor("#B3FFFFFF"));
            titleContainer.addView(subtitleView);
            m_viewRegistry.put(viewId + "_subtitle", subtitleView);
        }
        
        appBar.addView(titleContainer);
        
        // Action items
        JSONArray actions = item.optJSONArray("actions");
        if (actions != null) {
            for (int i = 0; i < actions.length(); i++) {
                try {
                    JSONObject action = actions.getJSONObject(i);
                    String actionId = action.optString("id", "action_" + i);
                    String actionIcon = action.optString("icon", "more_vert");
                    
                    TextView actionButton = new TextView(m_ctx);
                    actionButton.setText(getIconChar(actionIcon));
                    actionButton.setTextSize(24);
                    actionButton.setTextColor(Color.parseColor(titleColor));
                    actionButton.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
                    actionButton.setGravity(Gravity.CENTER);
                    
                    if (onActionClick != null) {
                        final String handler = onActionClick;
                        final String aid = actionId;
                        actionButton.setOnClickListener(v -> {
                            JSONObject params = new JSONObject();
                            try {
                                params.put("actionId", aid);
                            } catch (Exception e) {}
                            
                            m_executor.execute(() -> {
                                String response = callPhp(handler, params.toString());
                                m_mainHandler.post(() -> processPhpResponse(response));
                            });
                        });
                    }
                    
                    appBar.addView(actionButton);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing app bar action", e);
                }
            }
        }
        
        return appBar;
    }

    // =========================================================================
    // BOTTOM NAV BAR IMPLEMENTATION
    // =========================================================================
    
    private LinearLayout createBottomNavBar() {
        LinearLayout navBar = new LinearLayout(m_ctx);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setBackgroundColor(Color.WHITE);
        navBar.setElevation(dpToPx(8));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(56)
        );
        navBar.setLayoutParams(params);
        
        return navBar;
    }
    
    private View processBottomNavBar(JSONObject item) {
        String viewId = item.optString("id", "bottom_nav_" + System.currentTimeMillis());
        String onItemSelected = item.optString("onItemSelected", null);
        String selectedItem = item.optString("selectedItem", null);
        String bgColor = item.optString("backgroundColor", "#FFFFFF");
        String selectedColor = item.optString("selectedColor", "#1976D2");
        String unselectedColor = item.optString("unselectedColor", "#757575");
        boolean showLabels = item.optBoolean("showLabels", true);
        
        LinearLayout navBar = createBottomNavBar();
        navBar.setBackgroundColor(Color.parseColor(bgColor));
        m_viewRegistry.put(viewId, navBar);
        
        BottomNavState state = new BottomNavState();
        state.container = navBar;
        state.viewId = viewId;
        state.onItemSelected = onItemSelected;
        state.selectedId = selectedItem;
        m_bottomNavStates.put(viewId, state);
        
        // Process nav items
        JSONArray itemsArray = item.optJSONArray("items");
        if (itemsArray != null) {
            for (int i = 0; i < itemsArray.length(); i++) {
                try {
                    JSONObject navItem = itemsArray.getJSONObject(i);
                    state.items.add(navItem);
                    
                    String itemId = navItem.optString("id", "item_" + i);
                    String itemTitle = navItem.optString("title", "");
                    String itemIcon = navItem.optString("icon", "circle");
                    boolean isSelected = itemId.equals(selectedItem);
                    
                    View itemView = createBottomNavItem(viewId, itemId, itemTitle, itemIcon, 
                        isSelected, selectedColor, unselectedColor, showLabels, onItemSelected);
                    
                    LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.MATCH_PARENT, 1f
                    );
                    itemView.setLayoutParams(itemParams);
                    navBar.addView(itemView);
                    state.itemViews.add(itemView);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error processing bottom nav item", e);
                }
            }
        }
        
        return navBar;
    }
    
    private View createBottomNavItem(String navId, String itemId, String title, String icon,
            boolean selected, String selectedColor, String unselectedColor, 
            boolean showLabel, String onItemSelected) {
        
        LinearLayout itemLayout = new LinearLayout(m_ctx);
        itemLayout.setOrientation(LinearLayout.VERTICAL);
        itemLayout.setGravity(Gravity.CENTER);
        itemLayout.setPadding(0, dpToPx(8), 0, dpToPx(8));
        
        // Clickable background
        TypedValue outValue = new TypedValue();
        m_ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        itemLayout.setBackgroundResource(outValue.resourceId);
        
        String color = selected ? selectedColor : unselectedColor;
        
        // Icon
        TextView iconView = new TextView(m_ctx);
        iconView.setText(getIconChar(icon));
        iconView.setTextSize(24);
        iconView.setTextColor(Color.parseColor(color));
        iconView.setGravity(Gravity.CENTER);
        iconView.setTag("icon");
        itemLayout.addView(iconView);
        
        // Label
        if (showLabel) {
            TextView labelView = new TextView(m_ctx);
            labelView.setText(title);
            labelView.setTextSize(12);
            labelView.setTextColor(Color.parseColor(color));
            labelView.setGravity(Gravity.CENTER);
            labelView.setTag("label");
            itemLayout.addView(labelView);
        }
        
        // Store item ID for selection updates
        itemLayout.setTag(itemId);
        
        // Click handler
        itemLayout.setOnClickListener(v -> {
            // Update selection state
            selectBottomNavItem(navId, itemId);
            
            // Call PHP handler
            if (onItemSelected != null) {
                JSONObject params = new JSONObject();
                try {
                    params.put("itemId", itemId);
                    params.put("title", title);
                    params.put("navId", navId);
                } catch (Exception e) {}
                
                m_executor.execute(() -> {
                    String response = callPhp(onItemSelected, params.toString());
                    m_mainHandler.post(() -> processPhpResponse(response));
                });
            }
        });
        
        return itemLayout;
    }
    
    private void selectBottomNavItem(String navId, String itemId) {
        BottomNavState state = m_bottomNavStates.get(navId);
        if (state == null) return;
        
        state.selectedId = itemId;
        
        String selectedColor = "#1976D2";
        String unselectedColor = "#757575";
        
        for (int i = 0; i < state.itemViews.size(); i++) {
            View itemView = state.itemViews.get(i);
            boolean isSelected = itemId.equals(itemView.getTag());
            String color = isSelected ? selectedColor : unselectedColor;
            
            if (itemView instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) itemView;
                for (int j = 0; j < group.getChildCount(); j++) {
                    View child = group.getChildAt(j);
                    if (child instanceof TextView) {
                        ((TextView) child).setTextColor(Color.parseColor(color));
                    }
                }
            }
        }
    }
    
    private void handleBottomNavSelect(JSONObject response) {
        String navId = response.optString("target");
        String itemId = response.optString("itemId");
        m_mainHandler.post(() -> selectBottomNavItem(navId, itemId));
    }
    
    private void handleBottomNavBadge(JSONObject response) {
        // TODO: Implement badge support
        Log.d(TAG, "handleBottomNavBadge not fully implemented yet");
    }

    // =========================================================================
    // TAB LAYOUT IMPLEMENTATION
    // =========================================================================
    
    private Map<String, TabState> m_tabStates = new HashMap<>();
    
    private static class TabState {
        LinearLayout container;
        List<View> tabViews = new ArrayList<>();
        List<JSONObject> tabs = new ArrayList<>();
        int selectedIndex = 0;
        String onTabSelected;
        String viewId;
        String selectedColor = "#FFFFFF";
        String unselectedColor = "#80FFFFFF";
    }
    
    private View processTabLayout(JSONObject item) {
        String viewId = item.optString("id", "tab_" + System.currentTimeMillis());
        String onTabSelected = item.optString("onTabSelected", null);
        int selectedTab = item.optInt("selectedTab", 0);
        String bgColor = item.optString("backgroundColor", "#1976D2");
        String selectedColor = item.optString("selectedColor", "#FFFFFF");
        String unselectedColor = item.optString("unselectedColor", "#80FFFFFF");
        
        LinearLayout tabBar = new LinearLayout(m_ctx);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setBackgroundColor(Color.parseColor(bgColor));
        tabBar.setElevation(dpToPx(4));
        tabBar.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(48)
        ));
        
        m_viewRegistry.put(viewId, tabBar);
        
        TabState state = new TabState();
        state.container = tabBar;
        state.viewId = viewId;
        state.onTabSelected = onTabSelected;
        state.selectedIndex = selectedTab;
        state.selectedColor = selectedColor;
        state.unselectedColor = unselectedColor;
        m_tabStates.put(viewId, state);
        
        JSONArray tabsArray = item.optJSONArray("tabs");
        if (tabsArray != null) {
            for (int i = 0; i < tabsArray.length(); i++) {
                try {
                    JSONObject tab = tabsArray.getJSONObject(i);
                    state.tabs.add(tab);
                    
                    String text = tab.optString("text", "Tab " + i);
                    String icon = tab.optString("icon", null);
                    boolean isSelected = (i == selectedTab);
                    
                    View tabView = createTabItem(viewId, i, text, icon, isSelected,
                        selectedColor, unselectedColor, onTabSelected);
                    
                    LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.MATCH_PARENT, 1f
                    );
                    tabView.setLayoutParams(tabParams);
                    tabBar.addView(tabView);
                    state.tabViews.add(tabView);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing tab", e);
                }
            }
        }
        
        return tabBar;
    }
    
    private View createTabItem(String tabLayoutId, int index, String text, String icon,
            boolean selected, String selectedColor, String unselectedColor, String onTabSelected) {
        
        LinearLayout tabItem = new LinearLayout(m_ctx);
        tabItem.setOrientation(LinearLayout.VERTICAL);
        tabItem.setGravity(Gravity.CENTER);
        tabItem.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        
        TypedValue outValue = new TypedValue();
        m_ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        tabItem.setBackgroundResource(outValue.resourceId);
        
        String color = selected ? selectedColor : unselectedColor;
        
        if (icon != null) {
            TextView iconView = new TextView(m_ctx);
            iconView.setText(getIconChar(icon));
            iconView.setTextSize(18);
            iconView.setTextColor(Color.parseColor(color));
            iconView.setGravity(Gravity.CENTER);
            iconView.setTag("icon");
            tabItem.addView(iconView);
        }
        
        TextView labelView = new TextView(m_ctx);
        labelView.setText(text.toUpperCase());
        labelView.setTextSize(12);
        labelView.setTextColor(Color.parseColor(color));
        labelView.setGravity(Gravity.CENTER);
        labelView.setTag("label");
        tabItem.addView(labelView);
        
        // Selection indicator
        if (selected) {
            View indicator = new View(m_ctx);
            indicator.setBackgroundColor(Color.parseColor(selectedColor));
            LinearLayout.LayoutParams indParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(2)
            );
            indicator.setLayoutParams(indParams);
            indicator.setTag("indicator");
            tabItem.addView(indicator);
        }
        
        tabItem.setTag(index);
        
        tabItem.setOnClickListener(v -> {
            selectTab(tabLayoutId, index);
            
            if (onTabSelected != null) {
                JSONObject params = new JSONObject();
                try {
                    params.put("index", index);
                    params.put("text", text);
                    params.put("tabLayoutId", tabLayoutId);
                } catch (Exception e) {}
                
                m_executor.execute(() -> {
                    String response = callPhp(onTabSelected, params.toString());
                    m_mainHandler.post(() -> processPhpResponse(response));
                });
            }
        });
        
        return tabItem;
    }
    
    private void selectTab(String tabLayoutId, int index) {
        TabState state = m_tabStates.get(tabLayoutId);
        if (state == null) return;
        
        state.selectedIndex = index;
        
        for (int i = 0; i < state.tabViews.size(); i++) {
            View tabView = state.tabViews.get(i);
            boolean isSelected = (i == index);
            String color = isSelected ? state.selectedColor : state.unselectedColor;
            
            if (tabView instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) tabView;
                // Remove existing indicator
                View existingIndicator = group.findViewWithTag("indicator");
                if (existingIndicator != null) {
                    group.removeView(existingIndicator);
                }
                
                for (int j = 0; j < group.getChildCount(); j++) {
                    View child = group.getChildAt(j);
                    if (child instanceof TextView) {
                        ((TextView) child).setTextColor(Color.parseColor(color));
                    }
                }
                
                if (isSelected) {
                    View indicator = new View(m_ctx);
                    indicator.setBackgroundColor(Color.parseColor(state.selectedColor));
                    LinearLayout.LayoutParams indParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(2)
                    );
                    indicator.setLayoutParams(indParams);
                    indicator.setTag("indicator");
                    group.addView(indicator);
                }
            }
        }
    }
    
    private void handleTabSelect(JSONObject response) {
        String viewId = response.optString("target");
        int index = response.optInt("index", 0);
        m_mainHandler.post(() -> selectTab(viewId, index));
    }
    
    private void handleTabSetItems(JSONObject response) {
        Log.d(TAG, "handleTabSetItems: " + response.toString());
    }

    // =========================================================================
    // TEXT INPUT LAYOUT IMPLEMENTATION
    // =========================================================================
    
    private View processTextInputLayout(JSONObject item) {
        String viewId = item.optString("id", "til_" + System.currentTimeMillis());
        String hint = item.optString("hint", "");
        String helperText = item.optString("helperText", null);
        String errorText = item.optString("errorText", null);
        String inputType = item.optString("inputType", null);
        boolean counterEnabled = item.optBoolean("counterEnabled", false);
        int counterMax = item.optInt("counterMaxLength", 0);
        
        LinearLayout container = new LinearLayout(m_ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        m_viewRegistry.put(viewId, container);
        
        // Floating label
        TextView labelView = new TextView(m_ctx);
        labelView.setText(hint);
        labelView.setTextSize(12);
        labelView.setTextColor(Color.parseColor("#1976D2"));
        container.addView(labelView);
        
        // EditText
        EditText editText = new EditText(m_ctx);
        editText.setHint(hint);
        editText.setTextSize(16);
        if (inputType != null) {
            int iType = parseInputType(inputType);
            editText.setInputType(iType);
        }
        editText.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        container.addView(editText);
        m_viewRegistry.put(viewId + "_input", editText);
        
        // Helper/Error text
        if (helperText != null || errorText != null) {
            TextView helperView = new TextView(m_ctx);
            if (errorText != null && !errorText.isEmpty()) {
                helperView.setText(errorText);
                helperView.setTextColor(Color.parseColor("#F44336"));
            } else if (helperText != null) {
                helperView.setText(helperText);
                helperView.setTextColor(Color.parseColor("#757575"));
            }
            helperView.setTextSize(12);
            helperView.setPadding(0, dpToPx(4), 0, 0);
            container.addView(helperView);
            m_viewRegistry.put(viewId + "_helper", helperView);
        }
        
        // Counter
        if (counterEnabled && counterMax > 0) {
            TextView counterView = new TextView(m_ctx);
            counterView.setText("0/" + counterMax);
            counterView.setTextSize(12);
            counterView.setTextColor(Color.parseColor("#757575"));
            counterView.setGravity(Gravity.END);
            container.addView(counterView);
            
            final int maxLen = counterMax;
            editText.addTextChangedListener(new android.text.TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                public void afterTextChanged(android.text.Editable s) {
                    counterView.setText(s.length() + "/" + maxLen);
                    counterView.setTextColor(s.length() > maxLen ? 
                        Color.parseColor("#F44336") : Color.parseColor("#757575"));
                }
            });
        }
        
        // Setup event listeners from JSON
        setupEventListeners(editText, item, viewId);
        
        return container;
    }
    
    private int parseInputType(String type) {
        if (type == null) return android.text.InputType.TYPE_CLASS_TEXT;
        String lower = type.toLowerCase();
        int inputType = 0;
        if (lower.contains("text")) inputType |= android.text.InputType.TYPE_CLASS_TEXT;
        if (lower.contains("number")) inputType |= android.text.InputType.TYPE_CLASS_NUMBER;
        if (lower.contains("phone")) inputType |= android.text.InputType.TYPE_CLASS_PHONE;
        if (lower.contains("email")) inputType |= android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
        if (lower.contains("password")) inputType |= android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;
        if (lower.contains("multiline")) inputType |= android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE;
        if (lower.contains("decimal")) inputType |= android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;
        if (inputType == 0) inputType = android.text.InputType.TYPE_CLASS_TEXT;
        return inputType;
    }

    // =========================================================================
    // FAB IMPLEMENTATION
    // =========================================================================
    
    private View processFloatingActionButton(JSONObject item) {
        String viewId = item.optString("id", "fab_" + System.currentTimeMillis());
        String icon = item.optString("icon", "add");
        String bgColor = item.optString("backgroundColor", "#FF4081");
        String iconColor = item.optString("iconColor", "#FFFFFF");
        
        Button fab = new Button(m_ctx);
        fab.setAllCaps(false);
        fab.setText(getIconChar(icon));
        fab.setTextSize(24);
        fab.setTextColor(Color.parseColor(iconColor));
        fab.setGravity(Gravity.CENTER);
        
        int fabSize = dpToPx(56);
        fab.setMinHeight(fabSize);
        fab.setMinWidth(fabSize);
        fab.setMinimumHeight(fabSize);
        fab.setMinimumWidth(fabSize);
        fab.setElevation(dpToPx(6));
        
        GradientDrawable fabBg = new GradientDrawable();
        fabBg.setShape(GradientDrawable.OVAL);
        fabBg.setColor(Color.parseColor(bgColor));
        fab.setBackground(fabBg);
        
        m_viewRegistry.put(viewId, fab);
        setupEventListeners(fab, item, viewId);
        
        return fab;
    }

    // =========================================================================
    // NEW COMPONENT SETUP HELPERS
    // =========================================================================
    
    private void setupSpinner(android.widget.Spinner spinner, JSONObject json) {
        try {
            JSONArray itemsArray = json.optJSONArray("items");
            if (itemsArray == null) return;
            
            List<String> items = new ArrayList<>();
            for (int i = 0; i < itemsArray.length(); i++) {
                items.add(itemsArray.getString(i));
            }
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                m_ctx, android.R.layout.simple_spinner_item, items
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            
            // Set initial selection
            int selected = json.optInt("selectedPosition", 0);
            if (selected >= 0 && selected < items.size()) {
                spinner.setSelection(selected);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Spinner", e);
        }
    }
    
    private void setupNumberPicker(NumberPicker np, JSONObject json) {
        if (json.has("minValue")) np.setMinValue(json.optInt("minValue", 0));
        if (json.has("maxValue")) np.setMaxValue(json.optInt("maxValue", 100));
        if (json.has("value")) np.setValue(json.optInt("value", 0));
        if (json.has("wrapSelectorWheel")) np.setWrapSelectorWheel(json.optBoolean("wrapSelectorWheel", true));
        
        // Display values
        JSONArray displayValues = json.optJSONArray("displayedValues");
        if (displayValues != null) {
            String[] values = new String[displayValues.length()];
            for (int i = 0; i < displayValues.length(); i++) {
                values[i] = displayValues.optString(i);
            }
            np.setDisplayedValues(values);
        }
    }
    
    private void setupAutoComplete(AutoCompleteTextView actv, JSONObject json) {
        try {
            JSONArray suggestionsArray = json.optJSONArray("suggestions");
            if (suggestionsArray == null) return;
            
            List<String> suggestions = new ArrayList<>();
            for (int i = 0; i < suggestionsArray.length(); i++) {
                suggestions.add(suggestionsArray.getString(i));
            }
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                m_ctx, android.R.layout.simple_dropdown_item_1line, suggestions
            );
            actv.setAdapter(adapter);
            
            int threshold = json.optInt("completionThreshold", 1);
            actv.setThreshold(threshold);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up AutoCompleteTextView", e);
        }
    }
    
    private void setupRatingBar(android.widget.RatingBar ratingBar, JSONObject json) {
        if (json.has("numStars")) ratingBar.setNumStars(json.optInt("numStars", 5));
        if (json.has("rating")) ratingBar.setRating((float) json.optDouble("rating", 0));
        if (json.has("stepSize")) ratingBar.setStepSize((float) json.optDouble("stepSize", 0.5));
        if (json.has("isIndicator")) ratingBar.setIsIndicator(json.optBoolean("isIndicator", false));
    }
    
    private void setupVideoView(VideoView videoView, JSONObject json) {
        String uri = json.optString("videoUri", null);
        if (uri != null) {
            videoView.setVideoURI(Uri.parse(uri));
            if (json.optBoolean("autoPlay", false)) {
                videoView.start();
            }
        }
    }
    
    private void setupChronometer(Chronometer chronometer, JSONObject json) {
        if (json.has("format")) chronometer.setFormat(json.optString("format"));
        if (json.optBoolean("start", false)) chronometer.start();
    }

    // =========================================================================
    // DIALOG / SNACKBAR / POPUP HANDLERS
    // =========================================================================
    
    private android.app.Dialog m_currentDialog = null;
    
    private void handleSnackbar(JSONObject response) {
        String message = response.optString("message", "");
        String actionText = response.optString("actionText", null);
        String actionCallback = response.optString("actionCallback", null);
        int durationVal = response.optInt("duration", 0);
        
        m_mainHandler.post(() -> {
            try {
                // Use a Toast as fallback (Snackbar requires Material lib + CoordinatorLayout)
                // For proper Snackbar, we'd need the Material Components library
                if (actionText != null && actionCallback != null) {
                    // Show a toast + run callback on tap
                    android.widget.Toast toast = android.widget.Toast.makeText(m_ctx, 
                        message + " [" + actionText + "]", android.widget.Toast.LENGTH_LONG);
                    toast.show();
                    // Note: Real Snackbar with action button requires Material lib
                } else {
                    int duration = durationVal == 0 ? android.widget.Toast.LENGTH_SHORT : android.widget.Toast.LENGTH_LONG;
                    android.widget.Toast.makeText(m_ctx, message, duration).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error showing snackbar", e);
            }
        });
    }
    
    private void handleDialog(JSONObject response) {
        String title = response.optString("title", "");
        String message = response.optString("message", "");
        String positiveText = response.optString("positiveText", "OK");
        String positiveCallback = response.optString("positiveCallback", null);
        String negativeText = response.optString("negativeText", null);
        String negativeCallback = response.optString("negativeCallback", null);
        
        m_mainHandler.post(() -> {
            try {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(m_ctx);
                builder.setTitle(title);
                builder.setMessage(message);
                
                builder.setPositiveButton(positiveText, (d, which) -> {
                    if (positiveCallback != null) {
                        m_executor.execute(() -> {
                            String resp = callPhp(positiveCallback, "{}");
                            m_mainHandler.post(() -> processPhpResponse(resp));
                        });
                    }
                });
                
                if (negativeText != null) {
                    builder.setNegativeButton(negativeText, (d, which) -> {
                        if (negativeCallback != null) {
                            m_executor.execute(() -> {
                                String resp = callPhp(negativeCallback, "{}");
                                m_mainHandler.post(() -> processPhpResponse(resp));
                            });
                        }
                    });
                }
                
                m_currentDialog = builder.show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing dialog", e);
            }
        });
    }
    
    private void handleListDialog(JSONObject response) {
        String title = response.optString("title", "");
        JSONArray itemsArray = response.optJSONArray("items");
        String callback = response.optString("callback", null);
        
        m_mainHandler.post(() -> {
            try {
                if (itemsArray == null) return;
                
                String[] items = new String[itemsArray.length()];
                for (int i = 0; i < itemsArray.length(); i++) {
                    items[i] = itemsArray.optString(i);
                }
                
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(m_ctx);
                builder.setTitle(title);
                builder.setItems(items, (d, which) -> {
                    if (callback != null) {
                        JSONObject params = new JSONObject();
                        try {
                            params.put("index", which);
                            params.put("item", items[which]);
                        } catch (Exception e) {}
                        
                        m_executor.execute(() -> {
                            String resp = callPhp(callback, params.toString());
                            m_mainHandler.post(() -> processPhpResponse(resp));
                        });
                    }
                });
                
                m_currentDialog = builder.show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing list dialog", e);
            }
        });
    }
    
    private void handleDatePickerDialog(JSONObject response) {
        String callback = response.optString("callback", null);
        String initialDate = response.optString("initialDate", null);
        
        m_mainHandler.post(() -> {
            try {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                
                if (initialDate != null) {
                    String[] parts = initialDate.split("-");
                    if (parts.length == 3) {
                        cal.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
                    }
                }
                
                DatePickerDialog dpd = new DatePickerDialog(m_ctx, (view, year, month, dayOfMonth) -> {
                    if (callback != null) {
                        JSONObject params = new JSONObject();
                        try {
                            params.put("year", year);
                            params.put("month", month + 1);
                            params.put("day", dayOfMonth);
                            params.put("date", String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth));
                        } catch (Exception e) {}
                        
                        m_executor.execute(() -> {
                            String resp = callPhp(callback, params.toString());
                            m_mainHandler.post(() -> processPhpResponse(resp));
                        });
                    }
                }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH));
                
                dpd.show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing date picker", e);
            }
        });
    }
    
    private void handleTimePickerDialog(JSONObject response) {
        String callback = response.optString("callback", null);
        boolean is24Hour = response.optBoolean("is24Hour", true);
        
        m_mainHandler.post(() -> {
            try {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                
                TimePickerDialog tpd = new TimePickerDialog(m_ctx, (view, hourOfDay, minute) -> {
                    if (callback != null) {
                        JSONObject params = new JSONObject();
                        try {
                            params.put("hour", hourOfDay);
                            params.put("minute", minute);
                            params.put("time", String.format("%02d:%02d", hourOfDay, minute));
                        } catch (Exception e) {}
                        
                        m_executor.execute(() -> {
                            String resp = callPhp(callback, params.toString());
                            m_mainHandler.post(() -> processPhpResponse(resp));
                        });
                    }
                }, cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), is24Hour);
                
                tpd.show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing time picker", e);
            }
        });
    }
    
    private void handleInputDialog(JSONObject response) {
        String title = response.optString("title", "Input");
        String hint = response.optString("hint", "");
        String callback = response.optString("callback", null);
        String initialValue = response.optString("initialValue", "");
        
        m_mainHandler.post(() -> {
            try {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(m_ctx);
                builder.setTitle(title);
                
                EditText input = new EditText(m_ctx);
                input.setHint(hint);
                input.setText(initialValue);
                input.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
                builder.setView(input);
                
                builder.setPositiveButton("OK", (d, which) -> {
                    if (callback != null) {
                        String text = input.getText().toString();
                        JSONObject params = new JSONObject();
                        try {
                            params.put("text", text);
                        } catch (Exception e) {}
                        
                        m_executor.execute(() -> {
                            String resp = callPhp(callback, params.toString());
                            m_mainHandler.post(() -> processPhpResponse(resp));
                        });
                    }
                });
                
                builder.setNegativeButton("Cancel", null);
                m_currentDialog = builder.show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing input dialog", e);
            }
        });
    }
    
    private void handleBottomSheet(JSONObject response) {
        JSONObject contentJson = response.optJSONObject("content");
        
        m_mainHandler.post(() -> {
            try {
                if (contentJson == null) return;
                
                // Create a dialog that slides up from the bottom
                android.app.Dialog dialog = new android.app.Dialog(m_ctx);
                dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
                
                View contentView = processComponentRecursive(contentJson);
                if (contentView != null) {
                    ScrollView sv = new ScrollView(m_ctx);
                    sv.addView(contentView);
                    dialog.setContentView(sv);
                }
                
                android.view.Window window = dialog.getWindow();
                if (window != null) {
                    window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    window.setGravity(Gravity.BOTTOM);
                    window.setBackgroundDrawableResource(android.R.color.white);
                }
                
                m_currentDialog = dialog;
                dialog.show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing bottom sheet", e);
            }
        });
    }
    
    private void handleDismissDialog() {
        m_mainHandler.post(() -> {
            if (m_currentDialog != null && m_currentDialog.isShowing()) {
                m_currentDialog.dismiss();
                m_currentDialog = null;
            }
        });
    }

    // =========================================================================
    // ANIMATION HANDLERS
    // =========================================================================
    
    private void handleAnimate(JSONObject response) {
        String viewId = response.optString("target");
        JSONObject properties = response.optJSONObject("properties");
        int duration = response.optInt("duration", 300);
        String interpolator = response.optString("interpolator", "decelerate");
        
        m_mainHandler.post(() -> {
            try {
                View view = m_viewRegistry.get(viewId);
                if (view == null || properties == null) return;
                
                List<ObjectAnimator> animators = new ArrayList<>();
                
                Iterator<String> keys = properties.keys();
                while (keys.hasNext()) {
                    String prop = keys.next();
                    float targetValue = (float) properties.optDouble(prop, 0);
                    
                    ObjectAnimator anim = null;
                    switch (prop) {
                        case "alpha":
                            anim = ObjectAnimator.ofFloat(view, "alpha", targetValue);
                            break;
                        case "translationX":
                            anim = ObjectAnimator.ofFloat(view, "translationX", dpToPx((int) targetValue));
                            break;
                        case "translationY":
                            anim = ObjectAnimator.ofFloat(view, "translationY", dpToPx((int) targetValue));
                            break;
                        case "rotation":
                            anim = ObjectAnimator.ofFloat(view, "rotation", targetValue);
                            break;
                        case "scaleX":
                            anim = ObjectAnimator.ofFloat(view, "scaleX", targetValue);
                            break;
                        case "scaleY":
                            anim = ObjectAnimator.ofFloat(view, "scaleY", targetValue);
                            break;
                    }
                    
                    if (anim != null) {
                        anim.setDuration(duration);
                        anim.setInterpolator(getAnimInterpolator(interpolator));
                        animators.add(anim);
                    }
                }
                
                if (animators.size() == 1) {
                    animators.get(0).start();
                } else if (animators.size() > 1) {
                    AnimatorSet set = new AnimatorSet();
                    set.playTogether(animators.toArray(new ObjectAnimator[0]));
                    set.start();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error animating view", e);
            }
        });
    }
    
    private void handleAnimateSet(JSONObject response) {
        JSONArray animations = response.optJSONArray("animations");
        boolean sequential = response.optBoolean("sequential", false);
        
        if (animations == null) return;
        
        for (int i = 0; i < animations.length(); i++) {
            try {
                handleAnimate(animations.getJSONObject(i));
            } catch (Exception e) {
                Log.e(TAG, "Error in animate set", e);
            }
        }
    }
    
    private android.view.animation.Interpolator getAnimInterpolator(String name) {
        if (name == null) return new DecelerateInterpolator();
        switch (name.toLowerCase()) {
            case "linear": return new LinearInterpolator();
            case "accelerate": return new AccelerateInterpolator();
            case "decelerate": return new DecelerateInterpolator();
            case "overshoot": return new OvershootInterpolator();
            case "bounce": return new BounceInterpolator();
            default: return new DecelerateInterpolator();
        }
    }

    // =========================================================================
    // CLIPBOARD / SHARE / URL HANDLERS
    // =========================================================================
    
    private void handleClipboardCopy(JSONObject response) {
        String text = response.optString("text", "");
        String label = response.optString("label", "Copied");
        
        m_mainHandler.post(() -> {
            try {
                ClipboardManager clipboard = (ClipboardManager) m_ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(label, text);
                clipboard.setPrimaryClip(clip);
                android.widget.Toast.makeText(m_ctx, label, android.widget.Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error copying to clipboard", e);
            }
        });
    }
    
    private void handleShare(JSONObject response) {
        String text = response.optString("text", "");
        String title = response.optString("title", "Share");
        
        m_mainHandler.post(() -> {
            try {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, text);
                m_activity.startActivity(Intent.createChooser(shareIntent, title));
            } catch (Exception e) {
                Log.e(TAG, "Error sharing", e);
            }
        });
    }
    
    private void handleOpenUrl(JSONObject response) {
        String url = response.optString("url", "");
        
        m_mainHandler.post(() -> {
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                m_activity.startActivity(browserIntent);
            } catch (Exception e) {
                Log.e(TAG, "Error opening URL", e);
            }
        });
    }

    // =========================================================================
    // DYNAMIC VIEW MANIPULATION HANDLERS
    // =========================================================================
    
    private void handleRemoveView(JSONObject response) {
        String viewId = response.optString("target");
        
        m_mainHandler.post(() -> {
            View view = m_viewRegistry.get(viewId);
            if (view != null && view.getParent() instanceof ViewGroup) {
                ((ViewGroup) view.getParent()).removeView(view);
                m_viewRegistry.remove(viewId);
            }
        });
    }
    
    private void handleAddView(JSONObject response) {
        String parentId = response.optString("target");
        JSONObject childJson = response.optJSONObject("child");
        int index = response.optInt("index", -1);
        
        m_mainHandler.post(() -> {
            try {
                View parent = m_viewRegistry.get(parentId);
                if (parent instanceof ViewGroup && childJson != null) {
                    View child = processComponentRecursive(childJson);
                    if (child != null) {
                        if (index >= 0 && index <= ((ViewGroup) parent).getChildCount()) {
                            ((ViewGroup) parent).addView(child, index);
                        } else {
                            ((ViewGroup) parent).addView(child);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error adding view", e);
            }
        });
    }
    
   private void handleReplaceChildren(JSONObject response) {
    String parentId = response.optString("target", "");
    JSONArray childrenJson = response.optJSONArray("children");
    if (parentId.isEmpty() || childrenJson == null) return;

    m_mainHandler.post(() -> {
        View parent = m_viewRegistry.get(parentId);
        if (!(parent instanceof ViewGroup)) return;

        // Build first, THEN swap. If any child is bad, abort without emptying.
        List<View> built = new ArrayList<>();
        for (int i = 0; i < childrenJson.length(); i++) {
            JSONObject cj = childrenJson.optJSONObject(i);
            if (cj == null) {
                Log.e(TAG, "replaceChildren: null child at " + i + " — aborting");
                return;
            }
            try {
                View v = processComponentRecursive(cj);
                if (v != null) built.add(v);
            } catch (Exception e) {
                Log.e(TAG, "replaceChildren: failed at child " + i, e);
                return;
            }
        }

        ViewGroup vg = (ViewGroup) parent;
        vg.removeAllViews();
        for (View v : built) vg.addView(v);
    });
}

private void pruneRegistryFor(View v, Set<String> keepIds) {
    // walk v + descendants, remove from m_viewRegistry any id not in keepIds
    for (Map.Entry<String, View> e : new ArrayList<>(m_viewRegistry.entrySet())) {
        if (e.getValue() == v && !keepIds.contains(e.getKey())) {
            m_viewRegistry.remove(e.getKey());
        }
    }
    if (v instanceof ViewGroup) {
        ViewGroup g = (ViewGroup) v;
        for (int i = 0; i < g.getChildCount(); i++) pruneRegistryFor(g.getChildAt(i), keepIds);
    }
}
    
    private void handleScrollTo(JSONObject response) {
        String viewId = response.optString("target");
        boolean smooth = response.optBoolean("smooth", true);
        
        m_mainHandler.post(() -> {
            View view = m_viewRegistry.get(viewId);
            if (view instanceof ScrollView) {
                ScrollView sv = (ScrollView) view;
                Object scrollTarget = response.opt("scrollTarget");
                
                if (scrollTarget instanceof String) {
                    // Scroll to a child view by ID
                    View targetView = m_viewRegistry.get(scrollTarget.toString());
                    if (targetView != null) {
                        if (smooth) {
                            sv.smoothScrollTo(0, targetView.getTop());
                        } else {
                            sv.scrollTo(0, targetView.getTop());
                        }
                    }
                } else if (scrollTarget instanceof Number) {
                    int y = dpToPx(((Number) scrollTarget).intValue());
                    if (smooth) {
                        sv.smoothScrollTo(0, y);
                    } else {
                        sv.scrollTo(0, y);
                    }
                }
            }
        });
    }

    // =========================================================================
    // ICON HELPER
    // =========================================================================
    
    /**
     * Convert icon name to Unicode character (simple text-based icons)
     * In a real app, you'd use actual icon fonts or drawables
     */
    private String getIconChar(String iconName) {
        if (iconName == null) return "●";
        
        switch (iconName.toLowerCase()) {
            case "menu": return "☰";
            case "arrow_back": case "back": return "←";
            case "close": return "✕";
            case "search": return "🔍";
            case "more_vert": case "more": return "⋮";
            case "more_horiz": return "⋯";
            case "home": return "🏠";
            case "person": case "profile": case "account": return "👤";
            case "settings": return "⚙";
            case "exit_to_app": case "logout": return "⎋";
            case "add": case "plus": return "+";
            case "remove": case "minus": return "−";
            case "check": return "✓";
            case "star": return "★";
            case "star_outline": return "☆";
            case "favorite": case "heart": return "♥";
            case "favorite_outline": return "♡";
            case "share": return "↗";
            case "edit": return "✎";
            case "delete": case "trash": return "🗑";
            case "info": return "ℹ";
            case "warning": return "⚠";
            case "error": return "⊗";
            case "notification": case "bell": return "🔔";
            case "email": case "mail": return "✉";
            case "message": case "chat": return "💬";
            case "phone": case "call": return "📞";
            case "camera": return "📷";
            case "photo": case "image": return "🖼";
            case "location": case "place": return "📍";
            case "calendar": case "event": return "📅";
            case "time": case "clock": return "🕐";
            case "folder": return "📁";
            case "file": case "document": return "📄";
            case "download": return "⬇";
            case "upload": return "⬆";
            case "refresh": return "↻";
            case "sync": return "🔄";
            case "lock": return "🔒";
            case "unlock": return "🔓";
            case "visibility": case "eye": return "👁";
            case "visibility_off": return "🙈";
            case "wifi": return "📶";
            case "bluetooth": return "ᛒ";
            case "battery": return "🔋";
            case "flash": case "bolt": return "⚡";
            case "circle": return "●";
            default: return "●";
        }
    }
}
