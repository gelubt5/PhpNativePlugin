// DocRunner.js - DroidScript app to run PhpNativePlugin documentation examples
// Place this file in your DroidScript project folder
app.LoadPlugin("PhpNativePlugin");
var php;
var currentExample = "";
var exampleList;
var contentArea;

function OnStart() {
    app.SetOrientation("Portrait");
    
    // Create main layout
    var lay = app.CreateLayout("Linear", "VCenter,FillXY");
   // lay.SetBackgroundColor("#1e1e1e");
    
    // Title
    var title = app.CreateText("📚 Doc Examples Runner", -1, -1, "Bold");
    title.SetTextSize(22);
    title.SetTextColor("#4ec9b0");
    title.SetMargins(0, 0.02, 0, 0.02);
    lay.AddChild(title);
    
    // Instructions
    var info = app.CreateText("Select an example to run it directly.", 0.9);
    info.SetTextColor("#888888");
    info.SetMargins(0, 0, 0, 0.02);
    lay.AddChild(info);
    
    // Example list
    var examples = [
        "01_hello_world.php|Hello World",
        "02_page_cards.php|Page & Cards",
        "03_ui_elements.php|UI Elements",
        "04_counter.php|Counter",
        "05_login_form.php|Login Form",
        "06_todo_list.php|Todo List",
        "07_navigation.php|Navigation Drawer",
        "08_sensors.php|Sensors",
        "09_bottom_nav.php|Bottom Navigation",
        "10_fluent_api.php|Fluent API"
    ];
    
    exampleList = app.CreateList(examples.join(","), 0.9, 0.5);
    exampleList.SetTextColor("#e0e0e0");
    exampleList.SetTextSize(16);
    exampleList.SetOnTouch(OnExampleSelect);
    lay.AddChild(exampleList);
    
    // Run button
    var btnRun = app.CreateButton("▶ Run Selected Example", 0.7, 0.08);
    //btnRun.SetBackgroundColor("#4CAF50");
    btnRun.SetTextColor("#ffffff");
    btnRun.SetMargins(0, 0.03, 0, 0.01);
    btnRun.SetOnTouch(RunExample);
    lay.AddChild(btnRun);
    
    // View code button
    var btnCode = app.CreateButton("📄 View Code", 0.7, 0.08);
   // btnCode.SetBackgroundColor("#2196F3");
    btnCode.SetTextColor("#ffffff");
    btnCode.SetMargins(0, 0.01, 0, 0.01);
    btnCode.SetOnTouch(ViewCode);
    lay.AddChild(btnCode);
    
    // Status
    contentArea = app.CreateText("Select an example above", 0.9);
    contentArea.SetTextColor("#888888");
    contentArea.SetMargins(0, 0.02, 0, 0);
    lay.AddChild(contentArea);
    
    app.AddLayout(lay);
    
    // Load the plugin
    LoadPlugin();
}

function LoadPlugin() {
    try {
        php = app.CreatePhpNative();
        if (php) {
            contentArea.SetText("✓ Plugin loaded. Ready to run examples.");
            contentArea.SetTextColor("#4CAF50");
        }
    } catch(e) {
        contentArea.SetText("⚠ Plugin not found. Install PhpNativePlugin first.");
        contentArea.SetTextColor("#f44336");
    }
}

function OnExampleSelect(title, body, type, index) {
    // In DroidScript Lists with "file|title" format:
    // body = filename (e.g. "01_hello_world.php")
    // title = display name (e.g. "Hello World")
    currentExample = body.split("|")[0];
    contentArea.SetText("Selected: " +currentExample + "");
    contentArea.SetTextColor("#4ec9b0");
}

function RunExample() {
    if (!currentExample) {
        app.ShowPopup("Please select an example first");
        return;
    }
    
    if (!php) {
        app.ShowPopup("Plugin not loaded!");
        return;
    }
    
    contentArea.SetText("Running: " + currentExample + "...");
    contentArea.SetTextColor("#FF9800");
    
    // The examples are in docs_examples/ folder within the plugin assets
    var examplePath = "docs_examples/" + currentExample;
    
    try {
        // Run the PHP file directly
        php.RunFile(examplePath, "index");
        contentArea.SetText("✓ Running: " + currentExample);
        contentArea.SetTextColor("#4CAF50");
    } catch(e) {
        contentArea.SetText("Error: " + e.message);
        contentArea.SetTextColor("#f44336");
    }
}

function ViewCode() {
    if (!currentExample) {
        app.ShowPopup("Please select an example first");
        return;
    }
    
    var examplePath = "docs_examples/" + currentExample;
    
    try {
        // Try to read from plugin assets
        var code = app.ReadFile(examplePath);
        if (!code) {
            code = "// Could not read file: " + examplePath + "\n// Make sure the example files are in the docs_examples folder.";
        }
        
        // Show code in a dialog
        var dlg = app.CreateDialog("📄 " + currentExample);
        var layDlg = app.CreateLayout("Linear", "VCenter");
        layDlg.SetPadding(0.02, 0.02, 0.02, 0.02);
        
        var scroll = app.CreateScroller(0.9, 0.6);
        var txtCode = app.CreateText(code, 0.88, -1, "Monospace,Left");
        txtCode.SetTextSize(11);
        txtCode.SetTextColor("#e0e0e0");
        txtCode.SetBackgroundColor("#1e1e1e");
        txtCode.SetPadding(0.02, 0.02, 0.02, 0.02);
        scroll.AddChild(txtCode);
        layDlg.AddChild(scroll);
        
        var btnCopy = app.CreateButton("📋 Copy Code", 0.4);
        btnCopy.SetOnTouch(function() {
            app.SetClipboardText(code);
            app.ShowPopup("Code copied!");
        });
        layDlg.AddChild(btnCopy);
        
        dlg.AddLayout(layDlg);
        dlg.Show();
        
    } catch(e) {
        app.ShowPopup("Could not read file: " + e.message);
    }
}

function OnBack() {
    // If PHP app is showing, close it and return to runner
    if (php && php.IsAppRunning && php.IsAppRunning()) {
        php.CloseApp();
        return true;
    }
    return false;
}
