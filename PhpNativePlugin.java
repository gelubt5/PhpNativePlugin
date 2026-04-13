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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhpNativePlugin {
    public static final String TAG = "PhpNativePlugin";
    public static final float VERSION = 1.0f;

    // Properties to skip during reflection-based property extraction
    private static final Set<String> SKIP_PROPERTIES = new HashSet<>(Arrays.asList(
    
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

    // UI Management
    private FrameLayout m_overlayContainer;
    private Map<String, View> m_viewRegistry = new HashMap<>();
    private Map<String, android.widget.ArrayAdapter<String>> m_listAdapters = new HashMap<>();
    private Map<String, List<String>> m_listData = new HashMap<>();
    private Map<String, DrawerState> m_drawerStates = new HashMap<>();
    private Map<String, BottomNavState> m_bottomNavStates = new HashMap<>();
    private Handler m_mainHandler;
    private ExecutorService m_executor;

    // Callback function names
    private String m_OnPhpResponse;
    private String m_OnSensorResult;
    private String m_OnUiReady;
    private String m_OnError;

    // Pending sensor callbacks (sensor type -> PHP method to call with result)
    private Map<String, String> m_pendingSensorCallbacks = new HashMap<>();

    // Navigation history stack: each entry is [method, dataJson]
    private ArrayList<String[]> m_screenHistory = new ArrayList<>();
    private String m_OnBack;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public PhpNativePlugin() {
        Log.d(TAG, "Creating PhpNativePlugin");
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

        // Initialize PHP environment
        initPhpEnvironment();
    }

    public void Release() {
        if (m_executor != null) {
            m_executor.shutdown();
        }
        if (m_overlayContainer != null) {
            removeOverlay();
        }
    }

    // -------------------------------------------------------------------------
    // Activity Events
    // -------------------------------------------------------------------------

    public void OnResume() { }
    public void OnPause() { }
    public void OnConfig() { }
    public void OnNewIntent(Intent intent) { }
    public void OnActivityResult(int requestCode, int resultCode, Intent data) { }

    // -------------------------------------------------------------------------
    // PHP Environment Setup
    // -------------------------------------------------------------------------

    private void initPhpEnvironment() {
        m_executor.execute(() -> {
            try {
                // Extract PHP binary from plugin assets
                extractPhpBinary();
                
                // Extract PHP script files
           
           
                m_phpReady = true;
                Log.d(TAG, "PHP environment ready");

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
            
            case "ShowOverlay":
                showOverlay();
                break;
            
            case "HideOverlay":
                hideOverlay();
                break;
            
            case "RemoveOverlay":
                removeOverlay();
                break;
            
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
            
            case "ExtractDocs":
                return extractDocsToApp(b.getString("p1"));
            
            case "RunFile":
                runPhpFile(b.getString("p1"), b.getString("p2"), b.getString("p3"));
                break;
            
            case "RunFileDebug":
                return runPhpFileDebug(b.getString("p1"), b.getString("p2"));
            
            case "DebugPaths":
                return debugPluginPaths();
            
            default:
                Log.w(TAG, "Unknown command: " + cmd);
        }
        return null;
    }

    public String CallPlugin(Bundle b) throws Exception {
        return CallPlugin(b, null);
    }

    public Object CreateObject(Bundle b) {
        String type = b.getString("type");
        // No custom controls for now
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

        try {
            // Use app directory if set, otherwise default
            String scriptDir = getPhpScriptDir();
            File scriptFile = new File(scriptDir, m_entryFile != null ? m_entryFile : "logic.php");
            if (!scriptFile.exists()) {
                return "{\"error\": \"logic.php not found in " + scriptDir + "\"}";
            }

            if (paramsJson == null || paramsJson.isEmpty()) {
                paramsJson = "{}";
            }

            ProcessBuilder pb = new ProcessBuilder(
                m_phpPath,
                scriptFile.getAbsolutePath(),
                "--method=" + method,
                "--params=" + paramsJson
            );
            pb.redirectErrorStream(true);
            pb.directory(new File(scriptDir));  // Set working directory
            
            Log.d(TAG, "Executing PHP: " + m_phpPath + " " + scriptFile.getAbsolutePath() + " --method=" + method);
            
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            int exitCode = process.waitFor();

            String result = output.toString().trim();
            Log.d(TAG, "PHP exit code: " + exitCode + ", output length: " + result.length());
            Log.d(TAG, "PHP raw output: " + result.substring(0, Math.min(500, result.length())));
            
            // Extract JSON from output (in case of warnings/notices before JSON)
            int jsonStart = result.indexOf("{");
            if (jsonStart > 0) {
                Log.w(TAG, "Non-JSON prefix: " + result.substring(0, jsonStart));
                result = result.substring(jsonStart);
            } else if (jsonStart < 0) {
                Log.e(TAG, "No JSON found in PHP output");
                return "{\"error\": \"PHP returned non-JSON: " + result.substring(0, Math.min(100, result.length())).replace("\"", "'") + "\"}";
            }

            // Process the PHP response for special actions
            processPhpResponse(result);

            return result;

        } catch (Exception e) {
            Log.e(TAG, "PHP call failed: " + method, e);
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
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
            try {
                ProcessBuilder pb = new ProcessBuilder(m_phpPath, "-v");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(" ");
                }
                int exitCode = process.waitFor();
                String phpVersion = output.toString().trim().replace("\"", "'").replace("\n", " ");
                debug.append("{\"success\": true, \"exitCode\": ").append(exitCode);
                debug.append(", \"output\": \"").append(phpVersion.substring(0, Math.min(100, phpVersion.length()))).append("\"}");
            } catch (Exception e) {
                debug.append("{\"success\": false, \"error\": \"").append(e.getMessage().replace("\"", "'")).append("\"}");
            }
        }
        debug.append(", ");
        
        // 6. Test actual PHP script execution
        debug.append("\"logicTest\": ");
        File logicFile = new File(scriptDir, "logic.php");
        if (m_phpPath == null) {
            debug.append("{\"success\": false, \"error\": \"phpPath is null\"}");
        } else if (logicFile.exists()) {
            try {
                ProcessBuilder pb = new ProcessBuilder(m_phpPath, logicFile.getAbsolutePath(), "--method=index");
                pb.redirectErrorStream(true);
                pb.directory(new File(scriptDir));
                Process process = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
                int exitCode = process.waitFor();
                String result = output.toString().trim();
                // Escape for JSON
                result = result.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
                debug.append("{\"success\": true, \"exitCode\": ").append(exitCode);
                debug.append(", \"outputLength\": ").append(result.length());
                debug.append(", \"output\": \"").append(result.substring(0, Math.min(500, result.length()))).append("\"}");
            } catch (Exception e) {
                debug.append("{\"success\": false, \"error\": \"").append(e.getMessage().replace("\"", "'")).append("\"}");
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
        try {
            ProcessBuilder pb = new ProcessBuilder(
                m_phpPath,
                logicFile.getAbsolutePath(),
                "--method=" + method
            );
            pb.redirectErrorStream(true);
            pb.directory(new File(scriptDir));
            
            result.append("\"command\": \"").append(m_phpPath).append(" ").append(logicFile.getAbsolutePath()).append(" --method=").append(method).append("\", ");
            
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\\n");
            }
            int exitCode = process.waitFor();
            
            String rawOutput = output.toString();
            result.append("\"exitCode\": ").append(exitCode).append(", ");
            result.append("\"outputLength\": ").append(rawOutput.length()).append(", ");
            
            // Escape for JSON
            String escaped = rawOutput.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
            result.append("\"rawOutput\": \"").append(escaped).append("\"");
            
        } catch (Exception e) {
            result.append("\"error\": \"").append(e.getMessage().replace("\"", "'")).append("\", ");
            result.append("\"exception\": \"").append(e.getClass().getName()).append("\"");
        }
        
        result.append("}");
        return result.toString();
    }

    private void processPhpResponse(String jsonResponse) {
        try {
            JSONObject response = new JSONObject(jsonResponse);
            String action = response.optString("action", "");

            // Check for special DroidScript sensor call action
            if ("DS_SENSOR_CALL".equals(action)) {
                String sensor = response.optString("sensor");
                String callback = response.optString("callback", "handle_sensor_result");
                
                // Store the PHP callback for when sensor returns
                m_pendingSensorCallbacks.put(sensor, callback);
                
                // Inject sensor call into DroidScript
                injectSensorCallForType(sensor, response);
            }
            // Check for UI render action
            else if ("render".equals(action) || response.has("type") || response.has("children")) {
                m_mainHandler.post(() -> renderUI(jsonResponse));
            }
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
            
            // Batch multiple actions
            else if ("batch".equals(action)) {
                JSONArray batchActions = response.optJSONArray("actions");
                if (batchActions != null) {
                    for (int i = 0; i < batchActions.length(); i++) {
                        processPhpResponse(batchActions.getJSONObject(i).toString());
                    }
                }
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
     * Uses reflection to discover and sync all readable properties.
     */
    private void syncViewStateToFile() {
        try {
            String scriptDir = getPhpScriptDir();
            File stateFile = new File(scriptDir, VIEW_STATE_FILE);
            
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
            
            // Write to file
            try (FileOutputStream fos = new FileOutputStream(stateFile)) {
                fos.write(state.toString().getBytes("UTF-8"));
            }
            
            Log.d(TAG, "Synced view state to: " + stateFile.getAbsolutePath() + " (" + m_viewRegistry.size() + " views)");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to sync view state", e);
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
        //m_pendingSensorCallbacks.put(sensorType, phpCallback);
        injectSensorCallForType(sensorType, null);
    
    }

    private void injectSensorCallForType(String sensorType, JSONObject params) {
        String jsCode = "";
        
        switch (sensorType.toLowerCase()) {
            case "location":
            case "gps":
                jsCode = generateLocationScript();
                break;
            
            case "battery":
                jsCode = generateBatteryScript();
                break;
            
            case "accelerometer":
                jsCode = generateAccelerometerScript();
                break;
            
            case "compass":
            case "orientation":
                jsCode = generateCompassScript();
                break;
            
           
          
            
            case "camera":
                String quality = params != null ? params.optString("quality", "80") : "80";
                jsCode = generateCameraScript(quality);
                break;
            
            case "wifi":
                jsCode = generateWifiScript();
                break;
            
            case "bluetooth":
                jsCode = generateBluetoothScript();
                break;
            
            case "nfc":
                jsCode = generateNfcScript();
                break;
            
            case "sms":
                String phone = params != null ? params.optString("phone", "") : "";
                String message = params != null ? params.optString("message", "") : "";
                jsCode = generateSmsScript(phone, message);
                break;
            
            case "notification":
                String title = params != null ? params.optString("title", "Notification") : "Notification";
                String body = params != null ? params.optString("body", "") : "";
                jsCode = generateNotificationScript(title, body);
                break;
            
            case "vibrate":
                String pattern = params != null ? params.optString("pattern", "500") : "500";
                jsCode = generateVibrateScript(pattern);
                break;
            
            case "speech":
                String text = params != null ? params.optString("text", "") : "";
                jsCode = generateSpeechScript(text);
                break;
            
            case "speechrecognition":
                jsCode = generateSpeechRecognitionScript();
                break;

            // ---- Additional Sensors (from DroidScript app.CreateSensor) ----
            case "gyroscope":
                jsCode = generateGenericSensorScript("Gyroscope", "gyroscope");
                break;
            case "gravity":
                jsCode = generateGenericSensorScript("Gravity", "gravity");
                break;
            case "proximity":
                jsCode = generateProximitySensorScript();
                break;
            case "light":
                jsCode = generateSingleValueSensorScript("Light", "light", "light");
                break;
            case "pressure":
                jsCode = generateSingleValueSensorScript("Pressure", "pressure", "pressure");
                break;
            case "humidity":
                jsCode = generateSingleValueSensorScript("Humidity", "humidity", "humidity");
                break;
            case "temperature":
                jsCode = generateSingleValueSensorScript("Temperature", "temperature", "temperature");
                break;
            case "magneticfield":
                jsCode = generateGenericSensorScript("MagneticField", "magneticfield");
                break;
            case "stepcounter":
                jsCode = generateSingleValueSensorScript("StepCounter", "stepcounter", "steps");
                break;

            // ---- Device Info ----
            case "deviceinfo":
                jsCode = generateDeviceInfoScript();
                break;
            case "screeninfo":
                jsCode = generateScreenInfoScript();
                break;
            case "locationenabled":
                jsCode = generateLocationEnabledScript();
                break;

            // ---- Network ----
            case "networkinfo":
                jsCode = generateNetworkInfoScript();
                break;
            case "wifiscan":
                jsCode = generateWifiScanScript();
                break;
            case "btdiscover":
                jsCode = generateBtDiscoverScript();
                break;

            // ---- HTTP ----
            case "http":
                String httpUrl = params != null ? params.optString("url", "") : "";
                String httpMethod = params != null ? params.optString("httpMethod", "GET") : "GET";
                String httpBody = params != null ? params.optString("body", "") : "";
                String httpHeaders = params != null ? params.optString("headers", "") : "";
                jsCode = generateHttpRequestScript(httpUrl, httpMethod, httpBody, httpHeaders);
                break;
            case "download":
                String dlUrl = params != null ? params.optString("url", "") : "";
                String dlDest = params != null ? params.optString("dest", "") : "";
                jsCode = generateDownloadScript(dlUrl, dlDest);
                break;

            // ---- Media ----
            case "playaudio":
                String audioFile = params != null ? params.optString("file", "") : "";
                jsCode = generatePlayAudioScript(audioFile);
                break;
            case "stopaudio":
                jsCode = generateStopAudioScript();
                break;
            case "recordaudio":
                String recFile = params != null ? params.optString("file", "") : "";
                jsCode = generateRecordAudioScript(recFile);
                break;
            case "stoprecording":
                jsCode = generateStopRecordingScript();
                break;
            case "ringtone":
                String ringtoneType = params != null ? params.optString("ringtoneType", "notification") : "notification";
                jsCode = generateRingtoneScript(ringtoneType);
                break;

            // ---- Volume & Audio ----
            case "getvolume":
                String volStream = params != null ? params.optString("stream", "music") : "music";
                jsCode = generateGetVolumeScript(volStream);
                break;
            case "setvolume":
                int volLevel = params != null ? params.optInt("level", 7) : 7;
                String setVolStream = params != null ? params.optString("stream", "music") : "music";
                jsCode = generateSetVolumeScript(volLevel, setVolStream);
                break;
            case "setringermode":
                String ringerMode = params != null ? params.optString("mode", "normal") : "normal";
                jsCode = generateSetRingerModeScript(ringerMode);
                break;

            // ---- Screen ----
            case "setbrightness":
                double brightness = params != null ? params.optDouble("level", 0.5) : 0.5;
                jsCode = generateSetBrightnessScript(brightness);
                break;
            case "preventscreenlock":
                boolean prevent = params != null ? params.optBoolean("prevent", true) : true;
                jsCode = generatePreventScreenLockScript(prevent);
                break;

            // ---- Clipboard ----
            case "clipboard_set":
                String clipText = params != null ? params.optString("text", "") : "";
                jsCode = generateClipboardSetScript(clipText);
                break;
            case "clipboard_get":
                jsCode = generateClipboardGetScript();
                break;

            // ---- Encryption / Hashing ----
            case "encrypt":
                String encText = params != null ? params.optString("text", "") : "";
                String encPass = params != null ? params.optString("password", "") : "";
                jsCode = generateEncryptScript(encText, encPass);
                break;
            case "decrypt":
                String decText = params != null ? params.optString("text", "") : "";
                String decPass = params != null ? params.optString("password", "") : "";
                jsCode = generateDecryptScript(decText, decPass);
                break;
            case "hash":
                String hashText = params != null ? params.optString("text", "") : "";
                String hashAlgo = params != null ? params.optString("algorithm", "SHA256") : "SHA256";
                jsCode = generateHashScript(hashText, hashAlgo);
                break;

            // ---- Flashlight ----
            case "flashlight":
                boolean flashOn = params != null ? params.optBoolean("on", true) : true;
                jsCode = generateFlashlightScript(flashOn);
                break;

            // ---- Phone ----
            case "phonecall":
                String callNumber = params != null ? params.optString("number", "") : "";
                jsCode = generatePhoneCallScript(callNumber);
                break;

            // ---- Email ----
            case "sendemail":
                String emailTo = params != null ? params.optString("recipient", "") : "";
                String emailSubject = params != null ? params.optString("subject", "") : "";
                String emailBody = params != null ? params.optString("body", "") : "";
                String emailAttach = params != null ? params.optString("attachment", "") : "";
                jsCode = generateSendEmailScript(emailTo, emailSubject, emailBody, emailAttach);
                break;

            // ---- File System ----
            case "readfile":
                String readPath = params != null ? params.optString("path", "") : "";
                jsCode = generateReadFileScript(readPath);
                break;
            case "writefile":
                String writePath = params != null ? params.optString("path", "") : "";
                String writeContent = params != null ? params.optString("content", "") : "";
                jsCode = generateWriteFileScript(writePath, writeContent);
                break;
            case "listfolder":
                String listPath = params != null ? params.optString("path", "") : "";
                jsCode = generateListFolderScript(listPath);
                break;
            case "fileexists":
                String existsPath = params != null ? params.optString("path", "") : "";
                jsCode = generateFileExistsScript(existsPath);
                break;

            // ---- Intent / App ----
            case "openapp":
                String pkg = params != null ? params.optString("package", "") : "";
                jsCode = generateOpenAppScript(pkg);
                break;
            case "intent":
                String intentAction = params != null ? params.optString("intentAction", "") : "";
                String intentType = params != null ? params.optString("type", "") : "";
                String intentUri = params != null ? params.optString("uri", "") : "";
                String intentExtras = params != null ? params.optString("extras", "") : "";
                jsCode = generateIntentScript(intentAction, intentType, intentUri, intentExtras);
                break;

            default:
                Log.w(TAG, "Unknown sensor type: " + sensorType);
                return;
        }

        if (!jsCode.isEmpty()) {
            try {
                ExecScript(jsCode);
                Log.d(TAG, "Injected sensor script for: " + sensorType);
            } catch (Exception e) {
                Log.e(TAG, "Failed to inject sensor script", e);
                notifyError("Sensor injection failed: " + e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // JavaScript Generation for Sensors
    // -------------------------------------------------------------------------

    private String generateLocationScript() {
        // DroidScript Locator callback receives: data object with latitude, longitude, speed, bearing, altitude
        return "(function() {" +
            "var loc = app.CreateLocator('GPS,Network');" +
            "loc.SetOnChange(function(data) {" +
            "  _phpPlugin.OnSensorResult('location', JSON.stringify({lat:data.latitude, lng:data.longitude}));" +
            "  loc.Stop();" +
            "});" +
            "loc.Start();" +
            "})();";
    }

    private String generateBatteryScript() {
        return "(function() {" +
            "var pct = app.GetBatteryLevel();" +
            "var charging = app.IsCharging();" +
            "_phpPlugin.OnSensorResult('battery', JSON.stringify({level:pct, charging:charging}));" +
            "})();";
    }

    private String generateAccelerometerScript() {
        return "(function() {" +
            "var sns = app.CreateSensor('Accelerometer');" +
            "sns.SetOnChange(function(x, y, z) {" +
            "  _phpPlugin.OnSensorResult('accelerometer', JSON.stringify({x:x, y:y, z:z}));" +
            "  sns.Stop();" +
            "});" +
            "sns.Start();" +
            "})();";
    }

    private String generateCompassScript() {
        return "(function() {" +
            "var sns = app.CreateSensor('Orientation');" +
            "sns.SetOnChange(function(azimuth, pitch, roll) {" +
            "  _phpPlugin.OnSensorResult('compass', JSON.stringify({azimuth:azimuth, pitch:pitch, roll:roll}));" +
            "  sns.Stop();" +
            "});" +
            "sns.Start();" +
            "})();";
    }


    private String generateCameraScript(String quality) {
        // DroidScript: Use app.ChooseImage for photo selection, or CameraView for custom camera
        // ChooseImage callback receives file path
        return "(function() {" +
            "app.ChooseImage('internal', function(file) {" +
            "  if(file) _phpPlugin.OnSensorResult('camera', JSON.stringify({file:file}));" +
            "  else _phpPlugin.OnSensorResult('camera', JSON.stringify({error:'cancelled'}));" +
            "});" +
            "})();";
    }

    private String generateWifiScript() {
        return "(function() {" +
            "var wifi = app.CreateNetClient('TCP');" +
            "var ssid = app.GetSSID();" +
            "var ip = app.GetIPAddress();" +
            "_phpPlugin.OnSensorResult('wifi', JSON.stringify({ssid:ssid, ip:ip, connected:app.IsConnected()}));" +
            "})();";
    }

    private String generateBluetoothScript() {
        return "(function() {" +
            "var bt = app.CreateBluetoothSerial();" +
            "var paired = app.GetPairedBtDevices();" +
            "_phpPlugin.OnSensorResult('bluetooth', JSON.stringify({enabled:app.IsBluetoothEnabled(), paired:paired}));" +
            "})();";
    }

    private String generateNfcScript() {
        return "(function() {" +
            "var nfc = app.CreateNxt();" +
            "_phpPlugin.OnSensorResult('nfc', JSON.stringify({available:true}));" +
            "})();";
    }

    private String generateSmsScript(String phone, String message) {
        return "(function() {" +
            "app.SendSMS('" + escapeJs(phone) + "', '" + escapeJs(message) + "');" +
            "_phpPlugin.OnSensorResult('sms', JSON.stringify({sent:true, phone:'" + escapeJs(phone) + "'}));" +
            "})();";
    }

    private String generateNotificationScript(String title, String body) {
        return "(function() {" +
            "app.ShowPopup('" + escapeJs(body) + "', 'Long');" +
            "_phpPlugin.OnSensorResult('notification', JSON.stringify({shown:true}));" +
            "})();";
    }

    private String generateVibrateScript(String pattern) {
        return "(function() {" +
            "app.Vibrate('" + pattern + "');" +
            "_phpPlugin.OnSensorResult('vibrate', JSON.stringify({done:true}));" +
            "})();";
    }

    private String generateSpeechScript(String text) {
        return "(function() {" +
            "app.TextToSpeech('" + escapeJs(text) + "', 1.0, 1.0, function() {" +
            "  _phpPlugin.OnSensorResult('speech', JSON.stringify({done:true}));" +
            "});" +
            "})();";
    }

    private String generateSpeechRecognitionScript() {
        return "(function() {" +
            "var spr = app.CreateSpeechRec();" +
            "spr.SetOnResult(function(text) {" +
            "  _phpPlugin.OnSensorResult('speechrecognition', JSON.stringify({text:text}));" +
            "});" +
            "spr.Recognize();" +
            "})();";
    }

    // =========================================================================
    // Generic Sensor Script Generators
    // =========================================================================

    /**
     * Generic 3-axis sensor (accelerometer, gyroscope, gravity, magnetic field).
     * Uses app.CreateSensor() and GetValues() for a uniform approach.
     */
    private String generateGenericSensorScript(String sensorName, String resultKey) {
        return "(function() {" +
            "var sns = app.CreateSensor('" + sensorName + "');" +
            "sns.SetOnChange(function() {" +
            "  var vals = sns.GetValues();" +
            "  var data = {values:vals};" +
            "  if(vals && vals.length>=1) data.x = vals[0];" +
            "  if(vals && vals.length>=2) data.y = vals[1];" +
            "  if(vals && vals.length>=3) data.z = vals[2];" +
            "  _phpPlugin.OnSensorResult('" + resultKey + "', JSON.stringify(data));" +
            "  sns.Stop();" +
            "});" +
            "sns.Start();" +
            "})();";
    }

    /**
     * Single-value sensor (light, pressure, humidity, temperature, step counter).
     */
    private String generateSingleValueSensorScript(String sensorName, String resultKey, String valueName) {
        return "(function() {" +
            "var sns = app.CreateSensor('" + sensorName + "');" +
            "sns.SetOnChange(function() {" +
            "  var vals = sns.GetValues();" +
            "  var data = {};" +
            "  data['" + valueName + "'] = vals && vals.length>=1 ? vals[0] : 0;" +
            "  data.values = vals;" +
            "  _phpPlugin.OnSensorResult('" + resultKey + "', JSON.stringify(data));" +
            "  sns.Stop();" +
            "});" +
            "sns.Start();" +
            "})();";
    }

    /**
     * Proximity sensor returns distance + near boolean.
     */
    private String generateProximitySensorScript() {
        return "(function() {" +
            "var sns = app.CreateSensor('Proximity');" +
            "sns.SetOnChange(function() {" +
            "  var vals = sns.GetValues();" +
            "  var dist = vals && vals.length>=1 ? vals[0] : 0;" +
            "  _phpPlugin.OnSensorResult('proximity', JSON.stringify({distance:dist, near:dist<5}));" +
            "  sns.Stop();" +
            "});" +
            "sns.Start();" +
            "})();";
    }

    // =========================================================================
    // Device Info Script Generators
    // =========================================================================

    private String generateDeviceInfoScript() {
        // DroidScript doesn't have GetManufacturer - removed
        // GetOSVersion returns Android API level (integer)
        return "(function() {" +
            "var data = {" +
            "  model: app.GetModel()," +
            "  osVersion: app.GetOSVersion()," +
            "  apiLevel: app.GetBuildNum()," +
            "  deviceId: app.GetDeviceId()," +
            "  isTablet: app.IsTablet()," +
            "  language: app.GetLanguage()," +
            "  country: app.GetCountry()," +
            "  appName: app.GetAppName()," +
            "  packageName: app.GetPackageName()," +
            "  freeSpace: app.GetFreeSpace('internal')" +
            "};" +
            "_phpPlugin.OnSensorResult('deviceinfo', JSON.stringify(data));" +
            "})();";
    }

    private String generateScreenInfoScript() {
        return "(function() {" +
            "var data = {" +
            "  width: app.GetScreenWidth()," +
            "  height: app.GetScreenHeight()," +
            "  density: app.GetScreenDensity()," +
            "  rotation: app.GetRotation()," +
            "  orientation: app.GetOrientation()" +
            "};" +
            "_phpPlugin.OnSensorResult('screeninfo', JSON.stringify(data));" +
            "})();";
    }

    private String generateLocationEnabledScript() {
        return "(function() {" +
            "var en = app.IsLocationEnabled();" +
            "_phpPlugin.OnSensorResult('locationenabled', JSON.stringify({enabled:en}));" +
            "})();";
    }

    // =========================================================================
    // Network Script Generators
    // =========================================================================

    private String generateNetworkInfoScript() {
        return "(function() {" +
            "var data = {" +
            "  connected: app.IsConnected()," +
            "  ip: app.GetIPAddress()," +
            "  mac: app.GetMacAddress()," +
            "  ssid: app.GetSSID()," +
            "  rssi: app.GetRSSI()," +
            "  wifiEnabled: app.IsWifiEnabled()," +
            "  bluetoothEnabled: app.IsBluetoothEnabled()" +
            "};" +
            "_phpPlugin.OnSensorResult('networkinfo', JSON.stringify(data));" +
            "})();";
    }

    private String generateWifiScanScript() {
        // DroidScript: app.WifiScan(callback, options) - callback receives array of networks
        return "(function() {" +
            "app.WifiScan(function(results) {" +
            "  _phpPlugin.OnSensorResult('wifiscan', JSON.stringify({networks:results}));" +
            "}, '');" +
            "})();";
    }

    private String generateBtDiscoverScript() {
        return "(function() {" +
            "app.DiscoverBtDevices('', function(name, address) {" +
            "  _phpPlugin.OnSensorResult('btdiscover', JSON.stringify({name:name, address:address}));" +
            "});" +
            "})();";
    }

    // =========================================================================
    // HTTP Script Generators
    // =========================================================================

    private String generateHttpRequestScript(String url, String method, String body, String headers) {
        // DroidScript app.HttpRequest(type, baseUrl, path, params, callback, headers)
        // We use baseUrl=url, path='', params=body for simplicity
        return "(function() {" +
            "app.HttpRequest('" + escapeJs(method) + "', '" + escapeJs(url) + "', '', '" + escapeJs(body) + "', function(error, response) {" +
            "  _phpPlugin.OnSensorResult('http', JSON.stringify({error:error, response:response, url:'" + escapeJs(url) + "'}));" +
            "}, '" + escapeJs(headers) + "');" +
            "})();";
    }

    private String generateDownloadScript(String url, String dest) {
        return "(function() {" +
            "app.DownloadFile('" + escapeJs(url) + "', '" + escapeJs(dest) + "', '', '', function(path) {" +
            "  _phpPlugin.OnSensorResult('download', JSON.stringify({file:path, success:true, url:'" + escapeJs(url) + "'}));" +
            "});" +
            "})();";
    }

    // =========================================================================
    // Media Script Generators
    // =========================================================================

    private String generatePlayAudioScript(String file) {
        return "(function() {" +
            "if(!window._phpAudioPlayer) window._phpAudioPlayer = app.CreateMediaPlayer();" +
            "var p = window._phpAudioPlayer;" +
            "p.SetFile('" + escapeJs(file) + "');" +
            "p.SetOnReady(function() {" +
            "  p.Play();" +
            "  _phpPlugin.OnSensorResult('playaudio', JSON.stringify({status:'playing', file:'" + escapeJs(file) + "'}));" +
            "});" +
            "p.SetOnComplete(function() {" +
            "  _phpPlugin.OnSensorResult('playaudio', JSON.stringify({status:'complete', file:'" + escapeJs(file) + "'}));" +
            "});" +
            "})();";
    }

    private String generateStopAudioScript() {
        return "(function() {" +
            "if(window._phpAudioPlayer) {" +
            "  window._phpAudioPlayer.Stop();" +
            "  _phpPlugin.OnSensorResult('stopaudio', JSON.stringify({status:'stopped'}));" +
            "} else {" +
            "  _phpPlugin.OnSensorResult('stopaudio', JSON.stringify({status:'no_player'}));" +
            "}" +
            "})();";
    }

    private String generateRecordAudioScript(String file) {
        return "(function() {" +
            "if(!window._phpAudioRecorder) window._phpAudioRecorder = app.CreateAudioRecorder();" +
            "var r = window._phpAudioRecorder;" +
            "r.SetFile('" + escapeJs(file) + "');" +
            "r.Start();" +
            "_phpPlugin.OnSensorResult('recordaudio', JSON.stringify({status:'recording', file:'" + escapeJs(file) + "'}));" +
            "})();";
    }

    private String generateStopRecordingScript() {
        return "(function() {" +
            "if(window._phpAudioRecorder) {" +
            "  window._phpAudioRecorder.Stop();" +
            "  _phpPlugin.OnSensorResult('stoprecording', JSON.stringify({status:'stopped'}));" +
            "} else {" +
            "  _phpPlugin.OnSensorResult('stoprecording', JSON.stringify({status:'no_recorder'}));" +
            "}" +
            "})();";
    }

    private String generateRingtoneScript(String type) {
        return "(function() {" +
            "app.PlayRingtone('" + escapeJs(type) + "');" +
            "_phpPlugin.OnSensorResult('ringtone', JSON.stringify({done:true, type:'" + escapeJs(type) + "'}));" +
            "})();";
    }

    // =========================================================================
    // Volume & Audio Script Generators
    // =========================================================================

    private String generateGetVolumeScript(String stream) {
        return "(function() {" +
            "var vol = app.GetVolume('" + escapeJs(stream) + "');" +
            "_phpPlugin.OnSensorResult('getvolume', JSON.stringify({volume:vol, stream:'" + escapeJs(stream) + "'}));" +
            "})();";
    }

    private String generateSetVolumeScript(int level, String stream) {
        return "(function() {" +
            "app.SetVolume('" + escapeJs(stream) + "', " + level + ");" +
            "_phpPlugin.OnSensorResult('setvolume', JSON.stringify({done:true, level:" + level + ", stream:'" + escapeJs(stream) + "'}));" +
            "})();";
    }

    private String generateSetRingerModeScript(String mode) {
        return "(function() {" +
            "app.SetRingerMode('" + escapeJs(mode) + "');" +
            "_phpPlugin.OnSensorResult('setringermode', JSON.stringify({done:true, mode:'" + escapeJs(mode) + "'}));" +
            "})();";
    }

    // =========================================================================
    // Screen Script Generators
    // =========================================================================

    private String generateSetBrightnessScript(double level) {
        return "(function() {" +
            "app.SetScreenBrightness(" + level + ");" +
            "_phpPlugin.OnSensorResult('setbrightness', JSON.stringify({done:true, level:" + level + "}));" +
            "})();";
    }

    private String generatePreventScreenLockScript(boolean prevent) {
        return "(function() {" +
            "app.PreventScreenLock(" + (prevent ? "true" : "false") + ");" +
            "_phpPlugin.OnSensorResult('preventscreenlock', JSON.stringify({done:true, prevent:" + prevent + "}));" +
            "})();";
    }

    // =========================================================================
    // Clipboard Script Generators
    // =========================================================================

    private String generateClipboardSetScript(String text) {
        return "(function() {" +
            "app.SetClipboardText('" + escapeJs(text) + "');" +
            "_phpPlugin.OnSensorResult('clipboard_set', JSON.stringify({done:true}));" +
            "})();";
    }

    private String generateClipboardGetScript() {
        return "(function() {" +
            "var text = app.GetClipboardText();" +
            "_phpPlugin.OnSensorResult('clipboard_get', JSON.stringify({text:text}));" +
            "})();";
    }

    // =========================================================================
    // Encryption / Hashing Script Generators
    // =========================================================================

    private String generateEncryptScript(String text, String password) {
        return "(function() {" +
            "var crp = app.CreateCrypt();" +
            "var result = crp.Encrypt('" + escapeJs(text) + "', '" + escapeJs(password) + "');" +
            "_phpPlugin.OnSensorResult('encrypt', JSON.stringify({result:result}));" +
            "})();";
    }

    private String generateDecryptScript(String text, String password) {
        return "(function() {" +
            "var crp = app.CreateCrypt();" +
            "var result = crp.Decrypt('" + escapeJs(text) + "', '" + escapeJs(password) + "');" +
            "_phpPlugin.OnSensorResult('decrypt', JSON.stringify({result:result}));" +
            "})();";
    }

    private String generateHashScript(String text, String algorithm) {
        return "(function() {" +
            "var crp = app.CreateCrypt();" +
            "var result = crp.Hash('" + escapeJs(text) + "', '" + escapeJs(algorithm) + "');" +
            "_phpPlugin.OnSensorResult('hash', JSON.stringify({result:result, algorithm:'" + escapeJs(algorithm) + "'}));" +
            "})();";
    }

    // =========================================================================
    // Flashlight Script Generator
    // =========================================================================

    private String generateFlashlightScript(boolean on) {
        return "(function() {" +
            "try {" +
            "  if(!window._phpFlashCam) {" +
            "    window._phpFlashCam = app.CreateCameraView(0.01, 0.01, 'Back');" +
            "    app.AddLayout(window._phpFlashCam);" +
            "    window._phpFlashCam.StartPreview();" +
            "  }" +
            "  window._phpFlashCam.SetFlash(" + on + ");" +
            "  _phpPlugin.OnSensorResult('flashlight', JSON.stringify({on:" + on + "}));" +
            "} catch(e) {" +
            "  _phpPlugin.OnSensorResult('flashlight', JSON.stringify({error:e.message}));" +
            "}" +
            "})();";
    }

    // =========================================================================
    // Phone & Email Script Generators
    // =========================================================================

    private String generatePhoneCallScript(String number) {
        return "(function() {" +
            "app.Call('" + escapeJs(number) + "');" +
            "_phpPlugin.OnSensorResult('phonecall', JSON.stringify({calling:true, number:'" + escapeJs(number) + "'}));" +
            "})();";
    }

    private String generateSendEmailScript(String to, String subject, String body, String attachment) {
        return "(function() {" +
            "app.SendMail('" + escapeJs(to) + "', '" + escapeJs(subject) + "', '" + escapeJs(body) + "'" +
            (attachment != null && !attachment.isEmpty() ? ", '" + escapeJs(attachment) + "'" : "") + ");" +
            "_phpPlugin.OnSensorResult('sendemail', JSON.stringify({sent:true, recipient:'" + escapeJs(to) + "'}));" +
            "})();";
    }

    // =========================================================================
    // File System Script Generators
    // =========================================================================

    private String generateReadFileScript(String path) {
        return "(function() {" +
            "try {" +
            "  var content = app.ReadFile('" + escapeJs(path) + "');" +
            "  _phpPlugin.OnSensorResult('readfile', JSON.stringify({content:content, path:'" + escapeJs(path) + "'}));" +
            "} catch(e) {" +
            "  _phpPlugin.OnSensorResult('readfile', JSON.stringify({error:e.message, path:'" + escapeJs(path) + "'}));" +
            "}" +
            "})();";
    }

    private String generateWriteFileScript(String path, String content) {
        return "(function() {" +
            "try {" +
            "  app.WriteFile('" + escapeJs(path) + "', '" + escapeJs(content) + "');" +
            "  _phpPlugin.OnSensorResult('writefile', JSON.stringify({success:true, path:'" + escapeJs(path) + "'}));" +
            "} catch(e) {" +
            "  _phpPlugin.OnSensorResult('writefile', JSON.stringify({success:false, error:e.message, path:'" + escapeJs(path) + "'}));" +
            "}" +
            "})();";
    }

    private String generateListFolderScript(String path) {
        return "(function() {" +
            "try {" +
            "  var files = app.ListFolder('" + escapeJs(path) + "');" +
            "  _phpPlugin.OnSensorResult('listfolder', JSON.stringify({files:files, path:'" + escapeJs(path) + "'}));" +
            "} catch(e) {" +
            "  _phpPlugin.OnSensorResult('listfolder', JSON.stringify({error:e.message, path:'" + escapeJs(path) + "'}));" +
            "}" +
            "})();";
    }

    private String generateFileExistsScript(String path) {
        return "(function() {" +
            "var exists = app.FileExists('" + escapeJs(path) + "');" +
            "_phpPlugin.OnSensorResult('fileexists', JSON.stringify({exists:exists, path:'" + escapeJs(path) + "'}));" +
            "})();";
    }

    // =========================================================================
    // Intent / App Script Generators
    // =========================================================================

    private String generateOpenAppScript(String packageName) {
        return "(function() {" +
            "try {" +
            "  app.StartApp('" + escapeJs(packageName) + "');" +
            "  _phpPlugin.OnSensorResult('openapp', JSON.stringify({opened:true, package:'" + escapeJs(packageName) + "'}));" +
            "} catch(e) {" +
            "  _phpPlugin.OnSensorResult('openapp', JSON.stringify({error:e.message, package:'" + escapeJs(packageName) + "'}));" +
            "}" +
            "})();";
    }

    private String generateIntentScript(String action, String type, String uri, String extras) {
        return "(function() {" +
            "try {" +
            "  app.SendIntent('" + escapeJs(action) + "', " +
            "    " + (uri != null && !uri.isEmpty() ? "'" + escapeJs(uri) + "'" : "null") + ", " +
            "    " + (type != null && !type.isEmpty() ? "'" + escapeJs(type) + "'" : "null") + ", " +
            "    " + (extras != null && !extras.isEmpty() ? "'" + escapeJs(extras) + "'" : "null") + ");" +
            "  _phpPlugin.OnSensorResult('intent', JSON.stringify({sent:true, action:'" + escapeJs(action) + "'}));" +
            "} catch(e) {" +
            "  _phpPlugin.OnSensorResult('intent', JSON.stringify({error:e.message}));" +
            "}" +
            "})();";
    }

    private String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"").replace("\n", "\\n");
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

    // -------------------------------------------------------------------------
    // Native UI Rendering (The Overlay)
    // -------------------------------------------------------------------------

    /**
     * Ensure the overlay container is created and attached.
     * MUST be called on the main (UI) thread.
     */
    private void ensureOverlayCreated() {
        if (m_overlayContainer == null) {
            m_overlayContainer = new FrameLayout(m_ctx);
            m_overlayContainer.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ));

            // Ensure content starts below the status bar
            m_overlayContainer.setFitsSystemWindows(true);

            // Also apply top padding equal to the status bar height as a fallback
            int statusBarHeight = 0;
            int resourceId = m_ctx.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                statusBarHeight = m_ctx.getResources().getDimensionPixelSize(resourceId);
            }
            m_overlayContainer.setPadding(0, statusBarHeight, 0, 0);

            // Intercept back button on the overlay
            m_overlayContainer.setFocusableInTouchMode(true);
            m_overlayContainer.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    handleBackPress();
                    return true;
                }
                return false;
            });
        }

        // Add overlay to activity's content view
        try {
            ViewGroup rootView = m_activity.findViewById(android.R.id.content);
            if (m_overlayContainer.getParent() == null) {
                rootView.addView(m_overlayContainer);
            }
            m_overlayContainer.setVisibility(View.VISIBLE);
            m_overlayContainer.setBackgroundColor(Color.parseColor("#FFFFFF"));
            m_overlayContainer.requestFocus();
        } catch (Exception e) {
            Log.e(TAG, "Failed to show overlay", e);
        }
    }

    private void showOverlay() {
        m_mainHandler.post(() -> ensureOverlayCreated());
    }

    private void hideOverlay() {
        m_mainHandler.post(() -> {
            if (m_overlayContainer != null) {
                m_overlayContainer.setVisibility(View.GONE);
            }
        });
    }

    private void removeOverlay() {
        m_mainHandler.post(() -> {
            if (m_overlayContainer != null) {
                ViewGroup parent = (ViewGroup) m_overlayContainer.getParent();
                if (parent != null) {
                    parent.removeView(m_overlayContainer);
                }
                m_overlayContainer.removeAllViews();
                m_viewRegistry.clear();
                m_listAdapters.clear();
                m_listData.clear();
            }
        });
    }

    /**
     * Handle back button press on the overlay.
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
     * Show exit confirmation dialog. On confirm, hide overlay and clear history.
     */
    private void showExitConfirmDialog() {
        m_mainHandler.post(() -> {
            try {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(m_ctx);
                builder.setTitle("Exit");
                builder.setMessage("Close this app?");
                builder.setPositiveButton("Yes", (dialog, which) -> {
                    m_screenHistory.clear();
                    hideOverlay();
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

                // Create overlay if needed (synchronous — we're already on the main thread)
                ensureOverlayCreated();

                // Clear existing views
                m_overlayContainer.removeAllViews();
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
                      m_overlayContainer.addView(rootView);
                
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
                     m_overlayContainer.addView(fallbackLayout);
                }

                Log.d(TAG, "UI rendered with " + m_viewRegistry.size() + " views");

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
        Iterator<String> keys = item.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String phpMethod = item.optString(key);

            if (key.equals("action")) {
                bindEvent(view, "onClick", phpMethod, viewId);
            } else if (key.startsWith("on")) {
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
                            m_executor.execute(() -> {
                                try {
                                    String response = callPhp(phpMethod, params.toString());
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
                    showOverlay();
                    renderUI(uiJson);
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
                        showOverlay();
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
                        showOverlay();
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
        String parentId = response.optString("target");
        JSONArray childrenJson = response.optJSONArray("children");
        
        m_mainHandler.post(() -> {
            try {
                View parent = m_viewRegistry.get(parentId);
                if (parent instanceof ViewGroup && childrenJson != null) {
                    ((ViewGroup) parent).removeAllViews();
                    
                    for (int i = 0; i < childrenJson.length(); i++) {
                        JSONObject childJson = childrenJson.getJSONObject(i);
                        View child = processComponentRecursive(childJson);
                        if (child != null) {
                            ((ViewGroup) parent).addView(child);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error replacing children", e);
            }
        });
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
