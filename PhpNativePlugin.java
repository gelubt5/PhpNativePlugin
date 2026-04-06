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
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhpNativePlugin {
    public static final String TAG = "PhpNativePlugin";
    public static final float VERSION = 1.0f;

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

    // UI Management
    private FrameLayout m_overlayContainer;
    private Map<String, View> m_viewRegistry = new HashMap<>();
    private Handler m_mainHandler;
    private ExecutorService m_executor;

    // Callback function names
    private String m_OnPhpResponse;
    private String m_OnSensorResult;
    private String m_OnUiReady;
    private String m_OnError;

    // Pending sensor callbacks (sensor type -> PHP method to call with result)
    private Map<String, String> m_pendingSensorCallbacks = new HashMap<>();

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
        String[] phpFiles = {"logic.php", "router.php", "ui_core.php", "app.php"};
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
            
            case "InjectSensorCall":
                injectSensorCall(b.getString("p1"), b.getString("p2"));
                break;
            
            case "OnInternalSensorResult":
                handleSensorResult(b.getString("p1"), b.getString("p2"));
                break;
            
            case "StartApp":
                startPhpApp(b.getString("p1"));
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

        try {
            // Use app directory if set, otherwise default
            String scriptDir = getPhpScriptDir();
            File scriptFile = new File(scriptDir, "logic.php");
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
            // Get single view property
            else if ("GET_VIEW_PROPERTY".equals(action)) {
                String viewId = response.optString("viewId");
                String property = response.optString("property");
                String callback = response.optString("callback");
                handleGetViewProperty(viewId, property, callback);
            }
            // Get multiple view properties
            else if ("GET_VIEW_PROPERTIES".equals(action)) {
                JSONArray requests = response.optJSONArray("requests");
                String callback = response.optString("callback");
                handleGetViewProperties(requests, callback);
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

        } catch (Exception e) {
            Log.w(TAG, "Could not parse PHP response: " + e.getMessage());
        }
    }

    /**
     * Handle GET_VIEW_PROPERTY action - get a single property and call PHP callback.
     */
    private void handleGetViewProperty(String viewId, String property, String callback) {
        // Check for null OR empty strings (optString returns "" for missing keys, not null)
        if (viewId == null || viewId.isEmpty() || 
            property == null || property.isEmpty() || 
            callback == null || callback.isEmpty()) {
            Log.w(TAG, "Invalid GET_VIEW_PROPERTY parameters: viewId=" + viewId + ", property=" + property + ", callback=" + callback);
            return;
        }
        
        m_mainHandler.post(() -> {
            View view = m_viewRegistry.get(viewId);
            Object value = null;
            String error = null;
            
            if (view == null) {
                error = "View not found: " + viewId;
                Log.w(TAG, error);
            } else {
                value = getPropertyValue(view, property);
                if (value == null) {
                    // Could be property not found or property value is actually null
                    Log.d(TAG, "Property " + property + " returned null for view " + viewId);
                }
            }
            
            // Build params JSON
            final JSONObject params = new JSONObject();
            try {
                params.put("viewId", viewId);
                params.put("property", property);
                params.put("value", value);
                if (error != null) {
                    params.put("error", error);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error building property params", e);
            }
            
            // Call PHP callback (check executor isn't shutdown)
            if (!m_executor.isShutdown()) {
                m_executor.execute(() -> {
                    String response = callPhp(callback, params.toString());
                    m_mainHandler.post(() -> processPhpResponse(response));
                });
            } else {
                Log.e(TAG, "Executor shutdown, cannot call PHP callback: " + callback);
            }
        });
    }

    /**
     * Handle GET_VIEW_PROPERTIES action - get multiple properties and call PHP callback.
     */
    private void handleGetViewProperties(JSONArray requests, String callback) {
        // Check for null OR empty strings
        if (requests == null || callback == null || callback.isEmpty()) {
            Log.w(TAG, "Invalid GET_VIEW_PROPERTIES parameters: requests=" + (requests != null ? requests.length() : "null") + ", callback=" + callback);
            return;
        }
        
        m_mainHandler.post(() -> {
            JSONArray results = new JSONArray();
            
            for (int i = 0; i < requests.length(); i++) {
                try {
                    JSONObject req = requests.getJSONObject(i);
                    String viewId = req.optString("viewId");
                    String property = req.optString("property");
                    
                    JSONObject result = new JSONObject();
                    result.put("viewId", viewId);
                    result.put("property", property);
                    
                    if (viewId.isEmpty() || property.isEmpty()) {
                        result.put("value", JSONObject.NULL);
                        result.put("error", "Missing viewId or property");
                    } else {
                        View view = m_viewRegistry.get(viewId);
                        if (view == null) {
                            result.put("value", JSONObject.NULL);
                            result.put("error", "View not found: " + viewId);
                        } else {
                            Object value = getPropertyValue(view, property);
                            result.put("value", value != null ? value : JSONObject.NULL);
                        }
                    }
                    results.put(result);
                } catch (Exception e) {
                    Log.e(TAG, "Error getting property at index " + i, e);
                    try {
                        JSONObject errorResult = new JSONObject();
                        errorResult.put("error", e.getMessage());
                        results.put(errorResult);
                    } catch (Exception ignored) {}
                }
            }
            
            // Build params JSON
            final JSONObject params = new JSONObject();
            try {
                params.put("results", results);
            } catch (Exception e) {
                Log.e(TAG, "Error building properties params", e);
            }
            
            // Call PHP callback (check executor isn't shutdown)
            if (!m_executor.isShutdown()) {
                m_executor.execute(() -> {
                    String response = callPhp(callback, params.toString());
                    m_mainHandler.post(() -> processPhpResponse(response));
                });
            } else {
                Log.e(TAG, "Executor shutdown, cannot call PHP callback: " + callback);
            }
        });
    }

    /**
     * Get a property value from a view using reflection.
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
                // Convert CharSequence to String for JSON
                if (result instanceof CharSequence) {
                    return result.toString();
                }
                return result;
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
        m_pendingSensorCallbacks.put(sensorType, phpCallback);
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
            
            case "barcode":
            case "qrcode":
                jsCode = generateBarcodeScript();
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
        return "(function() {" +
            "var loc = app.CreateLocator('GPS,Network');" +
            "loc.SetOnChange(function(pos) {" +
            " app.Alert(pos.latitude);  _phpPlugin.OnSensorResult('gps', JSON.stringify({lat:pos.latitude, lng:pos.longitude}));" +
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

    private String generateBarcodeScript() {
        return "(function() {" +
            "app.ScanBarcode(function(result) {" +
            "  _phpPlugin.OnSensorResult('barcode', JSON.stringify({code:result}));" +
            "}, {});" +
            "})();";
    }

    private String generateCameraScript(String quality) {
        return "(function() {" +
            "app.TakePicture(function(file) {" +
            "  _phpPlugin.OnSensorResult('camera', JSON.stringify({file:file}));" +
            "}, " + quality + ");" +
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
            "app.SpeechRec(function(result) {" +
            "  _phpPlugin.OnSensorResult('speechrecognition', JSON.stringify({text:result}));" +
            "}, {});" +
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

    private void showOverlay() {
        m_mainHandler.post(() -> {
            if (m_overlayContainer == null) {
                m_overlayContainer = new FrameLayout(m_ctx);
                m_overlayContainer.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ));
            }

            // Add overlay to activity's content view
            try {
                ViewGroup rootView = m_activity.findViewById(android.R.id.content);
                if (m_overlayContainer.getParent() == null) {
                    rootView.addView(m_overlayContainer);
                }
                m_overlayContainer.setVisibility(View.VISIBLE);
                m_overlayContainer.setBackgroundColor(Color.parseColor("#FFFFFF")); // Semi-transparent white
            } catch (Exception e) {
                Log.e(TAG, "Failed to show overlay", e);
            }
        });
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

                // Create overlay if needed
                if (m_overlayContainer == null) {
                    showOverlay();
                }

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
                return new android.widget.RatingBar(m_ctx);
            case "Spinner":
                return new android.widget.Spinner(m_ctx);
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
                return new FrameLayout(m_ctx);
            case "RelativeLayout":
                return new android.widget.RelativeLayout(m_ctx);
            case "ScrollView":
                return new ScrollView(m_ctx);
            case "HorizontalScrollView":
                return new android.widget.HorizontalScrollView(m_ctx);
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
                    "com.google.android.material.card."
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
                        m_executor.execute(() -> {
                            String response = callPhp(phpMethod, params.toString());
                            m_mainHandler.post(() -> processPhpResponse(response));
                        });

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
                if (v instanceof TextView) {
                    params.put("text", ((TextView) v).getText().toString());
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
        
        // Then handle regular view attributes
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            // Skip reserved keys and layout params (handled separately)
            if (key.equals("type") || key.equals("children") || key.equals("action") ||
                key.equals("id") || key.equals("target") || key.equals("attributes") ||
                key.equals("border") || key.equals("settings") || key.startsWith("on") || isLayoutParam(key)) continue;

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
               key.equals("maxWidth") || key.equals("maxHeight");
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

    private void startPhpApp(String initialMethod) {
        if (initialMethod == null || initialMethod.isEmpty()) {
            initialMethod = "index";
        }

        final String method = initialMethod;
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
}
