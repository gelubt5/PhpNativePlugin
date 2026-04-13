// PhpDocs.js — Comprehensive Documentation Browser for PhpNativePlugin
// Covers ALL functions from simple.php and ALL classes/functions from ui_core.php
// With Copy & Run functionality for every example
//
// Usage: Place in your DroidScript app folder, run from DroidScript IDE.

app.LoadPlugin("PhpNativePlugin");

// ============================================================================
// GLOBAL STATE
// ============================================================================
var php = null;
var currentTab = "simple";  // "simple" | "uicore" | "simpleDoc" | "uicoreDoc" | "ai" | "androidSdk"
var currentSection = null;
var currentItem = null;
var searchQuery = "";

// Layout references
var layMain, layTabs, layContent, layDetail, laySearch;
var txtTitle, txtSearch, lstItems;
var scrollContent, scrollDetail;

// AI Chat state
var layChat, scrollChat, layChatMessages, txtChatInput;
var chatHistory = [];  // [{role:"user"|"assistant", content:"..."}]
var aiContext = null;  // loaded source files
var AI_API_KEY = "";   // optional: get API key from enter.pollinations.ai for multi-turn chat
var AI_MODEL = "openai";  // pollinations model: "openai", "openai-fast", "mistral", "qwen-coder", etc.
var aiIsStreaming = false;

// PHP source files content (loaded at startup)
var phpSimpleContent = "";
var phpUiCoreContent = "";

// Helper: DroidScript has no DestroyAllChildren, so destroy+recreate the layout
function _destroyAllChildren(lay, parent, options, padding) {
    parent.DestroyChild(lay);
    var newLay = app.CreateLayout("Linear", options);
    if (padding) newLay.SetPadding(padding[0], padding[1], padding[2], padding[3]);
    parent.AddChild(newLay);
    return newLay;
}

// Factory functions for closures — DroidScript _Cbm deduplicates by toString() hash.
// Setting _nohash=true forces unique callback IDs via _ObjCbm instead of _ObjCbmH.
function _nh(fn) { fn._nohash = true; return fn; }

function _makeSectionTap(idx) {
    return _nh(function() {
        currentSection = idx;
        var data = currentTab === "simple" ? simpleData : uicoreData;
        ShowSectionItems(data[idx]);
    });
}
function _makeItemTap(si, ii) {
    return _nh(function() {
        var data = currentTab === "simple" ? simpleData : uicoreData;
        ShowDetail(data[si].items[ii], data[si].section);
    });
}
function _makeCopy(code) {
    return _nh(function() { CopyCode(code); });
}
function _makeRun(code, name) {
    return _nh(function() { RunCode(code, name); });
}
function _makeSave(code, name) {
    return _nh(function() { SaveCode(code, name); });
}
function _makeClipboard(text, msg) {
    return _nh(function() { app.SetClipboardText(text); app.ShowPopup(msg); });
}
function _makeSendChat(prompt) {
    return _nh(function() { SendChatMessage(prompt); });
}

// Colors
var C = {
    bg: "#0f1117", bgCard: "#1a1d27", bgCode: "#1e2130", bgHeader: "#141720",
    accent: "#4fc3f7", accent2: "#81c784", accentWarm: "#ffb74d", accentPink: "#f48fb1",
    text: "#e0e0e0", textMuted: "#888", textDim: "#555",
    border: "#2a2d3a", success: "#66bb6a", warn: "#ffa726", danger: "#ef5350",
    purple: "#b39ddb", cyan: "#4dd0e1", orange: "#ffb74d"
};

// ============================================================================
// DOCUMENTATION DATA — simple.php
// ============================================================================
var simpleData = [
{
    section: "Page Builders", icon: "📄", desc: "Create screens easily",
    items: [
        { name: "page", sig: "page(string $title, array $content, array $options = [])", desc: "Create a simple page with a title and content array. The main entry point for building screens.",
          params: [["$title","Page title shown in toolbar"],["$content","Array of UI components"],["$options","Optional: 'theme'=>'dark','statusBarColor'=>'#color'"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("My App", [\n            label("Welcome!"),\n            button("Click Me", "onClick"),\n        ]);\n    }\n    function onClick($p) {\n        return toast("Hello!");\n    }\n}' },
        { name: "card", sig: "card(string $title, array $content)", desc: "Create a card-style grouped section with title and content.",
          params: [["$title","Card header text"],["$content","Array of child components"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Cards", [\n            card("Profile", [\n                label("John Doe"),\n                label("john@example.com"),\n            ]),\n            card("Stats", [\n                label("Posts: 42"),\n                label("Followers: 1.2k"),\n            ]),\n        ]);\n    }\n}' },
    ]
},
{
    section: "UI Elements", icon: "🧩", desc: "Simple component creators",
    items: [
        { name: "label", sig: 'label(string $text, array $style = [])', desc: "Create a text label. Style keys: size, color, bold, center, italic.",
          params: [["$text","Display text"],["$style","Array with keys: size, color, bold, center, italic"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Labels", [\n            label("Normal text"),\n            label("Big Bold", ["size" => 24, "bold" => true]),\n            label("Colored", ["color" => "#FF5722"]),\n            label("Centered Italic", ["center" => true, "italic" => true]),\n        ]);\n    }\n}' },
        { name: "button", sig: 'button(string $text, string $action, array $style = [])', desc: "Create a clickable button bound to a PHP method.",
          params: [["$text","Button label"],["$action","PHP method name to call on click"],["$style","Array with keys: color, textColor, outlined"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Buttons", [\n            button("Primary", "onPrimary"),\n            button("Danger", "onDanger", ["color" => "#f44336"]),\n            button("Outlined", "onOutlined", ["outlined" => true]),\n        ]);\n    }\n    function onPrimary($p) { return toast("Primary clicked!"); }\n    function onDanger($p) { return toast("Danger!"); }\n    function onOutlined($p) { return toast("Outlined!"); }\n}' },
        { name: "input", sig: 'input(string $id, string $hint = "", array $options = [])', desc: "Create a text input field. Options: inputType, lines, password, maxLength.",
          params: [["$id","Unique view ID"],["$hint","Placeholder text"],["$options","inputType, lines, password, maxLength"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Inputs", [\n            input("name", "Your name"),\n            input("email", "Email", ["inputType" => "email"]),\n            input("pass", "Password", ["password" => true]),\n            button("Submit", "onSubmit"),\n        ]);\n    }\n    function onSubmit($p) {\n        return getText("name", "gotName");\n    }\n    function gotName($p) {\n        return toast("Hello, " . $p["value"]);\n    }\n}' },
        { name: "checkbox", sig: 'checkbox(string $id, string $text, ?string $onChange = null)', desc: "Create a checkbox with optional change handler.",
          params: [["$id","Unique ID"],["$text","Label text"],["$onChange","PHP method called on change"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Checkbox", [\n            checkbox("agree", "I agree to terms", "onCheck"),\n            label("Toggle the checkbox above", ["id" => "status"]),\n        ]);\n    }\n    function onCheck($p) {\n        $checked = $p["checked"] ?? false;\n        return setText("status", $checked ? "Agreed ✓" : "Not agreed");\n    }\n}' },
        { name: "toggle", sig: 'toggle(string $id, string $text, bool $checked = false, ?string $onChange = null)', desc: "Create a toggle switch.",
          params: [["$id","Unique ID"],["$text","Label"],["$checked","Initial state"],["$onChange","Change handler"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Toggle", [\n            toggle("dark", "Dark Mode", true, "onToggle"),\n            toggle("notify", "Notifications", false, "onToggle"),\n        ]);\n    }\n    function onToggle($p) {\n        return toast($p["viewId"] . ": " . ($p["checked"] ? "ON" : "OFF"));\n    }\n}' },
        { name: "radioGroup", sig: 'radioGroup(string $id, array $options, ?string $onChange = null, ?string $selected = null)', desc: "Create a radio button group for exclusive selection.",
          params: [["$id","Group ID"],["$options","Array of option strings"],["$onChange","Selection handler"],["$selected","Pre-selected option"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Radio", [\n            label("Pick a size:"),\n            radioGroup("size", ["Small","Medium","Large"], "onSize", "Medium"),\n        ]);\n    }\n    function onSize($p) { return toast("Selected: " . ($p["value"] ?? "")); }\n}' },
        { name: "seekbar", sig: 'seekbar(string $id, int $progress = 0, int $max = 100, ?string $onChange = null)', desc: "Create a slider/seekbar.",
          params: [["$id","Unique ID"],["$progress","Initial value"],["$max","Maximum value"],["$onChange","Change handler"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Seekbar", [\n            label("Volume:", ["id" => "vol_label"]),\n            seekbar("vol", 50, 100, "onVolChange"),\n        ]);\n    }\n    function onVolChange($p) {\n        return setText("vol_label", "Volume: " . ($p["progress"] ?? 0));\n    }\n}' },
        { name: "spinner", sig: 'spinner(string $id, array $items, ?string $onChange = null, int $selectedIndex = 0)', desc: "Create a dropdown select box.",
          params: [["$id","Unique ID"],["$items","Array of choices"],["$onChange","Selection handler"],["$selectedIndex","Initial selection"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Spinner", [\n            label("Choose a language:"),\n            spinner("lang", ["PHP","Java","Python","JS"], "onLang"),\n        ]);\n    }\n    function onLang($p) { return toast("Chose: " . ($p["value"] ?? "")); }\n}' },
        { name: "rating", sig: 'rating(string $id, float $rating = 0, int $numStars = 5, ?string $onChange = null)', desc: "Create a star rating bar.",
          params: [["$id","Unique ID"],["$rating","Initial rating"],["$numStars","Number of stars"],["$onChange","Change handler"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Rating", [\n            label("Rate this app:"),\n            rating("stars", 3, 5, "onRate"),\n        ]);\n    }\n    function onRate($p) { return toast("Rated: " . ($p["rating"] ?? 0) . " stars"); }\n}' },
        { name: "progress", sig: 'progress(string $id, int $progress = 0, int $max = 100)', desc: "Create a progress bar.",
          params: [["$id","Unique ID"],["$progress","Current value"],["$max","Max value"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Progress", [\n            label("Upload Progress:"),\n            progress("upload", 65, 100),\n            button("Increase", "onIncrease"),\n        ]);\n    }\n    function onIncrease($p) { return setProgress("upload", 90); }\n}' },
        { name: "image", sig: 'image(string $src, array $options = [])', desc: "Display an image. Supports URLs or local paths.",
          params: [["$src","Image URL or path"],["$options","width, height, scaleType, cornerRadius"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Image", [\n            image("https://picsum.photos/300/200", [\n                "width" => "match_parent",\n                "cornerRadius" => 16\n            ]),\n            label("Random photo from Picsum"),\n        ]);\n    }\n}' },
        { name: "textField", sig: 'textField(string $id, string $hint, array $options = [])', desc: "Material text input with floating label, helper, error, counter.",
          params: [["$id","Unique ID"],["$hint","Label/hint"],["$options","helperText, errorText, counterMax, inputType, startIcon, endIcon"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("TextFields", [\n            textField("user", "Username", [\n                "helperText" => "Enter your username",\n                "counterMax" => 20\n            ]),\n            textField("email", "Email", [\n                "inputType" => "email",\n                "startIcon" => "email"\n            ]),\n        ]);\n    }\n}' },
        { name: "fab", sig: 'fab(string $id, string $icon, string $action, array $style = [])', desc: "Floating Action Button — circular Material button.",
          params: [["$id","Unique ID"],["$icon","Material icon name"],["$action","Click handler"],["$style","color, size"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("FAB Demo", [\n            label("Press the FAB below"),\n            fab("add_btn", "add", "onFab", ["color" => "#E91E63"]),\n        ]);\n    }\n    function onFab($p) { return toast("FAB pressed!"); }\n}' },
        { name: "chip / chipGroup", sig: 'chip(string $text, ?string $action = null)\nchipGroup(array $chips, ?string $onSelect = null)', desc: "Material chips — small interactive tags. Group them with chipGroup.",
          params: [["$text","Chip label"],["$action","Click handler"],["$chips","Array of Chip"],["$onSelect","Selection callback"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Chips", [\n            label("Select tags:"),\n            chipGroup([\n                chip("PHP", "onChip"),\n                chip("Java", "onChip"),\n                chip("Python", "onChip"),\n            ], "onSelect"),\n        ]);\n    }\n    function onChip($p) { return toast("Tapped: " . ($p["text"] ?? "")); }\n    function onSelect($p) { return toast("Selected: " . ($p["value"] ?? "")); }\n}' },
        { name: "tabs", sig: 'tabs(string $id, array $tabs, ?string $onSelect = null, int $selected = 0)', desc: "Create a tab bar for switching views.",
          params: [["$id","Tab ID"],["$tabs","Array of {title, icon?}"],["$onSelect","Tab selection handler"],["$selected","Initial tab"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Tabs", [\n            tabs("nav", [\n                ["title" => "Home", "icon" => "home"],\n                ["title" => "Search", "icon" => "search"],\n                ["title" => "Profile", "icon" => "person"],\n            ], "onTab"),\n            label("Home content", ["id" => "content"]),\n        ]);\n    }\n    function onTab($p) {\n        return setText("content", "Tab: " . ($p["title"] ?? ""));\n    }\n}' },
        { name: "searchBar", sig: 'searchBar(string $id, string $hint = "Search...", ?string $onSearch = null, ?string $onTextChange = null)', desc: "Search input view.",
          params: [["$id","Unique ID"],["$hint","Placeholder"],["$onSearch","Submit handler"],["$onTextChange","Text change handler"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Search", [\n            searchBar("search", "Search items...", "onSearch"),\n            label("Type and press search", ["id" => "result"]),\n        ]);\n    }\n    function onSearch($p) {\n        return setText("result", "Searching: " . ($p["query"] ?? ""));\n    }\n}' },
        { name: "video", sig: 'video(string $id, string $uri, array $options = [])', desc: "Embed a video player.",
          params: [["$id","Unique ID"],["$uri","Video URL or path"],["$options","width, height, autoPlay"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Video", [\n            video("vid", "https://www.w3schools.com/html/mov_bbb.mp4", [\n                "width" => "match_parent",\n                "height" => 250\n            ]),\n        ]);\n    }\n}' },
        { name: "webView", sig: 'webView(string $id, string $url, array $options = [])', desc: "Embed a web page.",
          params: [["$id","Unique ID"],["$url","Web URL"],["$options","width, height, javaScriptEnabled"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("WebView", [\n            webView("web", "https://example.com", [\n                "width" => "match_parent",\n                "height" => 400\n            ]),\n        ]);\n    }\n}' },
        { name: "calendar", sig: 'calendar(string $id, ?string $onChange = null)', desc: "Create a calendar date picker widget.",
          params: [["$id","Unique ID"],["$onChange","Date change handler"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Calendar", [\n            calendar("cal", "onDatePick"),\n            label("Pick a date", ["id" => "picked"]),\n        ]);\n    }\n    function onDatePick($p) {\n        return setText("picked", "Date: " . ($p["date"] ?? ""));\n    }\n}' },
        { name: "toggleButton", sig: 'toggleButton(string $id, string $textOn = "ON", string $textOff = "OFF", bool $checked = false, ?string $onChange = null)', desc: "Toggle button with customizable on/off text labels.",
          params: [["$id","Unique ID"],["$textOn","Text when on"],["$textOff","Text when off"],["$checked","Initial state"],["$onChange","Change handler"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Toggle Button", [\n            toggleButton("wifi", "WiFi ON", "WiFi OFF", true, "onToggle"),\n            toggleButton("bt", "BT ON", "BT OFF", false, "onToggle"),\n            label("---", ["id" => "status"]),\n        ]);\n    }\n    function onToggle($p) {\n        $id = $p["viewId"] ?? "";\n        $on = ($p["checked"] ?? false) ? "ON" : "OFF";\n        return setText("status", "$id: $on");\n    }\n}' },
        { name: "numberPicker", sig: 'numberPicker(string $id, int $min = 0, int $max = 100, int $value = 0, ?string $onChange = null)', desc: "Numeric picker with min/max range.",
          params: [["$id","Unique ID"],["$min","Minimum value"],["$max","Maximum value"],["$value","Initial value"],["$onChange","Change handler"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Number Picker", [\n            label("Select quantity:"),\n            numberPicker("qty", 1, 50, 5, "onPick"),\n            label("Qty: 5", ["id" => "result"]),\n        ]);\n    }\n    function onPick($p) {\n        return setText("result", "Qty: " . ($p["value"] ?? 0));\n    }\n}' },
        { name: "autoComplete", sig: 'autoComplete(string $id, string $hint, array $suggestions, ?string $onChange = null)', desc: "Auto-complete text input with suggestion dropdown.",
          params: [["$id","Unique ID"],["$hint","Placeholder text"],["$suggestions","Array of suggestion strings"],["$onChange","Selection handler"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("AutoComplete", [\n            autoComplete("lang", "Type a language...", [\n                "PHP", "Python", "Java", "JavaScript", "Kotlin"\n            ], "onSelect"),\n            label("---", ["id" => "picked"]),\n        ]);\n    }\n    function onSelect($p) {\n        return setText("picked", "Selected: " . ($p["value"] ?? ""));\n    }\n}' },
        { name: "materialCard", sig: 'materialCard(array $content, array $style = [])', desc: "Material Design card container with elevation and styling.",
          params: [["$content","Array of child components"],["$style","elevation, cornerRadius, backgroundColor, padding"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Material Card", [\n            materialCard([\n                label("Card Title", ["bold" => true, "size" => 20]),\n                label("Card body with material styling."),\n                button("Action", "onTap"),\n            ], ["elevation" => 8, "cornerRadius" => 16]),\n        ]);\n    }\n    function onTap($p) { return toast("Card action!"); }\n}' },
        { name: "table", sig: 'table(array $rows, array $style = [])', desc: "Table layout from a 2D array of rows and columns.",
          params: [["$rows","2D array: [[col1,col2,...],...]"],["$style","borderColor, headerColor, cellPadding"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Table", [\n            table([\n                ["Name", "Age", "City"],\n                ["Alice", "30", "NYC"],\n                ["Bob", "25", "LA"],\n                ["Charlie", "35", "Chicago"],\n            ], ["headerColor" => "#1a237e"]),\n        ]);\n    }\n}' },
        { name: "horizontalScroll", sig: 'horizontalScroll(array $content)', desc: "Horizontal scroll wrapper for content wider than the screen.",
          params: [["$content","Array of components to scroll horizontally"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        $cards = [];\n        for ($i = 1; $i <= 8; $i++)\n            $cards[] = card("Item $i", [label("Content $i")]);\n        return page("Horizontal Scroll", [\n            label("Swipe horizontally:"),\n            horizontalScroll($cards),\n        ]);\n    }\n}' },
    ]
},
{
    section: "Layout Helpers", icon: "📐", desc: "Arrange components",
    items: [
        { name: "row", sig: 'row(array $elements)', desc: "Create a horizontal row of elements.",
          params: [["$elements","Array of components side-by-side"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Row Layout", [\n            row([\n                button("Left", "onClick"),\n                button("Right", "onClick"),\n            ]),\n            row([\n                label("Col 1"),\n                label("Col 2"),\n                label("Col 3"),\n            ]),\n        ]);\n    }\n    function onClick($p) { return toast("clicked"); }\n}' },
        { name: "scrollView", sig: 'scrollView(array $content)', desc: "Wrap content in a vertical scrollable container.",
          params: [["$content","Array of components"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        $items = [];\n        for ($i = 1; $i <= 30; $i++)\n            $items[] = label("Item #$i");\n        return page("Scroll", [ scrollView($items) ]);\n    }\n}' },
        { name: "grid", sig: 'grid(array $children, int $columns = 2)', desc: "Create a grid layout with N columns.",
          params: [["$children","Array of components"],["$columns","Number of columns"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Grid", [\n            grid([\n                card("A", [label("Cell 1")]),\n                card("B", [label("Cell 2")]),\n                card("C", [label("Cell 3")]),\n                card("D", [label("Cell 4")]),\n            ], 2),\n        ]);\n    }\n}' },
        { name: "stack", sig: 'stack(array $content)', desc: "Stack/overlap components on top of each other.",
          params: [["$content","Array of overlapping components"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Stack", [\n            stack([\n                image("https://picsum.photos/400/200"),\n                label("Overlay Text", ["color" => "#fff", "bold" => true]),\n            ]),\n        ]);\n    }\n}' },
        { name: "spacer / divider", sig: 'spacer(int $height = 20)\ndivider(string $color = "#333333")', desc: "Add vertical spacing or a horizontal line.",
          params: [["$height","Space height in dp"],["$color","Divider color"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Spacing", [\n            label("Section 1"),\n            spacer(30),\n            divider("#4fc3f7"),\n            spacer(30),\n            label("Section 2"),\n        ]);\n    }\n}' },
    ]
},
{
    section: "Actions", icon: "⚡", desc: "Toast, alert, navigate, update",
    items: [
        { name: "toast", sig: 'toast(string $message)', desc: "Show a brief toast notification.",
          params: [["$message","Toast text"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Toast", [\n            button("Show Toast", "doToast"),\n        ]);\n    }\n    function doToast($p) {\n        return toast("Hello from PHP!");\n    }\n}' },
        { name: "alert", sig: 'alert(string $message, string $title = "Alert")', desc: "Show an alert dialog with OK button.",
          params: [["$message","Alert body"],["$title","Dialog title"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Alert", [\n            button("Show Alert", "doAlert"),\n        ]);\n    }\n    function doAlert($p) {\n        return alert("This is important!", "Warning");\n    }\n}' },
        { name: "setText", sig: 'setText(string $id, string $text, ?string $color = null)', desc: "Update a view\'s text content at runtime.",
          params: [["$id","Target view ID"],["$text","New text"],["$color","Optional new color"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("SetText", [\n            label("Click the button", ["id" => "msg"]),\n            button("Update", "doUpdate"),\n        ]);\n    }\n    function doUpdate($p) {\n        return setText("msg", "Updated! ✓", "#4CAF50");\n    }\n}' },
        { name: "goToScreen", sig: 'goToScreen(string $screen, array $data = [])', desc: "Navigate to another PHP method (screen).",
          params: [["$screen","Method name to call"],["$data","Optional data to pass"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Home", [\n            button("Go to Settings", "goSettings"),\n        ]);\n    }\n    function goSettings($p) {\n        return goToScreen("settings");\n    }\n    function settings($p) {\n        return page("Settings", [\n            label("Settings screen"),\n            button("Back", "goHome"),\n        ]);\n    }\n    function goHome($p) { return goToScreen("index"); }\n}' },
        { name: "animateView", sig: 'animateView(string $id, string $property, float $toValue, int $duration = 300, string $interpolator = "decelerate")', desc: "Animate a single property of a view (alpha, translationX, rotation, scaleX, etc.).",
          params: [["$id","View ID"],["$property","Property to animate"],["$toValue","Target value"],["$duration","Animation time in ms"],["$interpolator","decelerate, accelerate, bounce, overshoot"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Animate View", [\n            label("🎯", ["id" => "target", "size" => 48, "center" => true]),\n            button("Slide Right", "doSlide"),\n            button("Fade Half", "doFade"),\n        ]);\n    }\n    function doSlide($p) { return animateView("target", "translationX", 200, 400, "overshoot"); }\n    function doFade($p) { return animateView("target", "alpha", 0.5, 300); }\n}' },
        { name: "closeDialog", sig: 'closeDialog()', desc: "Close any currently open dialog or bottom sheet.",
          params: [],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Close Dialog", [\n            button("Show Sheet", "showSheet"),\n        ]);\n    }\n    function showSheet($p) {\n        return showBottomSheet([\n            label("Bottom Sheet Content"),\n            button("Close", "doClose"),\n        ], "My Sheet");\n    }\n    function doClose($p) { return closeDialog(); }\n}' },
        { name: "resetTransform", sig: 'resetTransform(string $id)', desc: "Reset all transforms (translation, rotation, scale, alpha) to defaults.",
          params: [["$id","View ID to reset"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Reset Transform", [\n            label("🎯", ["id" => "box", "size" => 48, "center" => true]),\n            button("Rotate", "doRotate"),\n            button("Scale Up", "doScale"),\n            button("Reset All", "doReset"),\n        ]);\n    }\n    function doRotate($p) { return rotateView("box", 180, 300); }\n    function doScale($p) { return scaleView("box", 2.0, 300); }\n    function doReset($p) { return resetTransform("box"); }\n}' },
    ]
},
{
    section: "Dialogs & Popups", icon: "💬", desc: "Confirm, prompt, date/time pickers, bottom sheets",
    items: [
        { name: "confirm", sig: 'confirm(string $title, string $message, string $onConfirm, ?string $onCancel = null, ...)', desc: "Show a Yes/No confirmation dialog.",
          params: [["$title","Dialog title"],["$message","Dialog body"],["$onConfirm","Yes handler"],["$onCancel","No handler"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Confirm", [\n            button("Delete Item", "askDelete"),\n        ]);\n    }\n    function askDelete($p) {\n        return confirm("Delete?", "Are you sure?", "doDelete", "cancelDelete");\n    }\n    function doDelete($p) { return toast("Deleted!"); }\n    function cancelDelete($p) { return toast("Cancelled"); }\n}' },
        { name: "prompt", sig: 'prompt(string $title, string $hint, string $callback, string $defaultValue = "")', desc: "Show an input dialog for user text entry.",
          params: [["$title","Dialog title"],["$hint","Input hint"],["$callback","Receives typed text"],["$defaultValue","Pre-filled text"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Prompt", [\n            label("No name yet", ["id" => "name"]),\n            button("Enter Name", "askName"),\n        ]);\n    }\n    function askName($p) {\n        return prompt("Your Name", "Type here...", "gotName");\n    }\n    function gotName($p) {\n        return setText("name", "Hello, " . ($p["value"] ?? ""));\n    }\n}' },
        { name: "selectDialog", sig: 'selectDialog(string $title, array $items, string $onSelect)', desc: "Show a list selection dialog.",
          params: [["$title","Dialog title"],["$items","Array of choices"],["$onSelect","Selection handler"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("List Dialog", [\n            button("Pick Color", "pickColor"),\n            label("None selected", ["id" => "picked"]),\n        ]);\n    }\n    function pickColor($p) {\n        return selectDialog("Pick a Color", ["Red","Green","Blue","Yellow"], "onColor");\n    }\n    function onColor($p) {\n        return setText("picked", "Color: " . ($p["item"] ?? ""));\n    }\n}' },
        { name: "pickDate / pickTime", sig: 'pickDate(string $callback, ?string $initialDate = null)\npickTime(string $callback, bool $is24Hour = false)', desc: "Show a date or time picker dialog.",
          params: [["$callback","Receives the selected date/time"],["$initialDate","YYYY-MM-DD format"],["$is24Hour","Use 24h format"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Pickers", [\n            button("Pick Date", "onPickDate"),\n            button("Pick Time", "onPickTime"),\n            label("---", ["id" => "result"]),\n        ]);\n    }\n    function onPickDate($p) { return pickDate("gotDate"); }\n    function onPickTime($p) { return pickTime("gotTime", true); }\n    function gotDate($p) { return setText("result", "Date: " . ($p["date"] ?? "")); }\n    function gotTime($p) { return setText("result", "Time: " . ($p["hour"]??0) . ":" . ($p["minute"]??0)); }\n}' },
        { name: "showBottomSheet", sig: 'showBottomSheet(array $content, ?string $title = null)', desc: "Show a bottom sheet with custom content.",
          params: [["$content","Array of components"],["$title","Optional title"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Bottom Sheet", [\n            button("Show Options", "showSheet"),\n        ]);\n    }\n    function showSheet($p) {\n        return showBottomSheet([\n            label("Choose an action:", ["bold" => true]),\n            button("Share", "onShare"),\n            button("Delete", "onDelete"),\n            button("Cancel", "dismissSheet"),\n        ], "Options");\n    }\n    function onShare($p) { return toast("Sharing..."); }\n    function onDelete($p) { return toast("Deleting..."); }\n    function dismissSheet($p) { return closeDialog(); }\n}' },
    ]
},
{
    section: "Animation", icon: "🎬", desc: "Animate views with fade, slide, scale, rotate",
    items: [
        { name: "fadeIn / fadeOut", sig: 'fadeIn(string $id, int $duration = 300)\nfadeOut(string $id, int $duration = 300)', desc: "Fade a view in or out.",
          params: [["$id","View ID"],["$duration","Animation time in ms"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Fade", [\n            label("I can fade!", ["id" => "target", "size" => 24]),\n            row([\n                button("Fade Out", "doFadeOut"),\n                button("Fade In", "doFadeIn"),\n            ]),\n        ]);\n    }\n    function doFadeOut($p) { return fadeOut("target", 500); }\n    function doFadeIn($p) { return fadeIn("target", 500); }\n}' },
        { name: "slideX / slideY", sig: 'slideX(string $id, float $toX, int $duration = 300)\nslideY(string $id, float $toY, int $duration = 300)', desc: "Slide a view horizontally or vertically.",
          params: [["$id","View ID"],["$toX/$toY","Target position"],["$duration","Animation ms"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Slide", [\n            label("Slide me!", ["id" => "box", "size" => 20]),\n            button("Slide Right", "slideRight"),\n            button("Slide Back", "slideBack"),\n        ]);\n    }\n    function slideRight($p) { return slideX("box", 200, 400); }\n    function slideBack($p) { return slideX("box", 0, 400); }\n}' },
        { name: "scaleView / rotateView", sig: 'scaleView(string $id, float $scale, int $duration = 300)\nrotateView(string $id, float $degrees, int $duration = 300)', desc: "Scale or rotate a view with animation.",
          params: [["$id","View ID"],["$scale/$degrees","Target value"],["$duration","Animation ms"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Transform", [\n            label("🎯", ["id" => "target", "size" => 48, "center" => true]),\n            button("Scale Up", "scaleUp"),\n            button("Rotate", "rotate"),\n            button("Reset", "reset"),\n        ]);\n    }\n    function scaleUp($p) { return scaleView("target", 2.0, 300); }\n    function rotate($p) { return rotateView("target", 360, 500); }\n    function reset($p) { return resetTransform("target"); }\n}' },
        { name: "bounce / shake", sig: 'bounce(string $id, int $duration = 400)\nshake(string $id, int $duration = 400)', desc: "Playful bounce or shake effect.",
          params: [["$id","View ID"],["$duration","Animation ms"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Effects", [\n            label("🔔", ["id" => "bell", "size" => 48, "center" => true]),\n            button("Bounce", "doBounce"),\n            button("Shake", "doShake"),\n        ]);\n    }\n    function doBounce($p) { return bounce("bell"); }\n    function doShake($p) { return shake("bell"); }\n}' },
    ]
},
{
    section: "View Properties", icon: "🎨", desc: "Get and set any view property",
    items: [
        { name: "get / set / update", sig: 'get(string $id, string $property, string $callback)\nset(string $id, string $property, mixed $value)\nupdate(string $id, array $properties)', desc: "Universal property getters and setters. Set one or multiple properties at once.",
          params: [["$id","View ID"],["$property","Property name"],["$value","New value"],["$callback","Receives value"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Properties", [\n            label("Hello", ["id" => "lbl"]),\n            button("Make Red & Bold", "style"),\n            button("Read Text", "readIt"),\n        ]);\n    }\n    function style($p) {\n        return update("lbl", [\n            "textColor" => "#f44336",\n            "textSize" => 28,\n            "textStyle" => "bold"\n        ]);\n    }\n    function readIt($p) { return get("lbl", "text", "gotText"); }\n    function gotText($p) { return toast("Text: " . ($p["value"] ?? "")); }\n}' },
        { name: "show / hide / enable / disable", sig: 'show(string $id)\nhide(string $id)\nenable(string $id)\ndisable(string $id)', desc: "Toggle visibility and enabled state of views.",
          params: [["$id","View to modify"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Visibility", [\n            label("Now you see me!", ["id" => "secret"]),\n            row([\n                button("Hide", "doHide"),\n                button("Show", "doShow"),\n            ]),\n            button("Disable Me", "doDisable", ["id" => "btn_dis"]),\n        ]);\n    }\n    function doHide($p) { return hide("secret"); }\n    function doShow($p) { return show("secret"); }\n    function doDisable($p) { return disable("btn_dis"); }\n}' },
        { name: "styleText / styleView", sig: 'styleText(string $id, array $style)\nstyleView(string $id, array $style)', desc: "Apply compound text or view styles in one call.",
          params: [["$id","View ID"],["$style","Assoc array of style properties"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Styling", [\n            label("Style me", ["id" => "txt"]),\n            button("Apply Text Style", "textStyle"),\n            button("Apply View Style", "viewStyle"),\n        ]);\n    }\n    function textStyle($p) {\n        return styleText("txt", [\n            "text" => "Styled!",\n            "color" => "#E91E63",\n            "size" => 24,\n            "bold" => true\n        ]);\n    }\n    function viewStyle($p) {\n        return styleView("txt", [\n            "background" => "#1a237e",\n            "corners" => 16,\n            "elevation" => 8,\n            "padding" => 20\n        ]);\n    }\n}' },
        { name: "setColor / setSize / setBackground / setAlpha", sig: 'setColor(string $id, string $color)\nsetSize(string $id, int $size)\nsetBackground(string $id, string $color)\nsetAlpha(string $id, float $alpha)', desc: "Shortcut setters for common view properties: text color, text size, background color, opacity.",
          params: [["$id","View ID"],["$color","Hex color string"],["$size","Text size in sp"],["$alpha","Opacity 0.0 to 1.0"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Setters", [\n            label("Style me", ["id" => "lbl", "size" => 20]),\n            button("Red Text", "doRed"),\n            button("Big Text", "doBig"),\n            button("Blue BG", "doBg"),\n            button("Half Opacity", "doAlpha"),\n        ]);\n    }\n    function doRed($p) { return setColor("lbl", "#f44336"); }\n    function doBig($p) { return setSize("lbl", 32); }\n    function doBg($p) { return setBackground("lbl", "#1a237e"); }\n    function doAlpha($p) { return setAlpha("lbl", 0.5); }\n}' },
        { name: "setChecked / setImage / setHint / clear", sig: 'setChecked(string $id, bool $checked)\nsetImage(string $id, string $src)\nsetHint(string $id, string $hint)\nclear(string $id)', desc: "Set checkbox/switch state, image source, hint text, or clear an input field.",
          params: [["$id","View ID"],["$checked","true/false state"],["$src","Image URL or path"],["$hint","Hint text"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Set Values", [\n            checkbox("agree", "I agree"),\n            input("name", "Your name"),\n            button("Check it", "doCheck"),\n            button("Change hint", "doHint"),\n            button("Clear input", "doClear"),\n        ]);\n    }\n    function doCheck($p) { return setChecked("agree", true); }\n    function doHint($p) { return setHint("name", "Enter full name..."); }\n    function doClear($p) { return clear("name"); }\n}' },
        { name: "invisible", sig: 'invisible(string $id)', desc: "Make a view invisible (hidden but still takes up space in layout).",
          params: [["$id","View ID to make invisible"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Invisible", [\n            label("I take up space", ["id" => "ghost"]),\n            label("I am below"),\n            button("Invisible", "doInvis"),\n            button("Show", "doShow"),\n        ]);\n    }\n    function doInvis($p) { return invisible("ghost"); }\n    function doShow($p) { return show("ghost"); }\n}' },
    ]
},
{
    section: "Sensors", icon: "📡", desc: "GPS, accelerometer, compass, proximity, etc.",
    items: [
        { name: "gps", sig: 'gps(string $callback)', desc: "Get GPS location. Callback receives: {lat, lng, altitude}.",
          params: [["$callback","Method receiving location"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("GPS", [\n            label("Press to get location", ["id" => "loc"]),\n            button("Get Location", "doGps"),\n        ]);\n    }\n    function doGps($p) { return gps("onLocation"); }\n    function onLocation($p) {\n        $lat = $p["lat"] ?? "?";\n        $lng = $p["lng"] ?? "?";\n        return setText("loc", "Lat: $lat, Lng: $lng");\n    }\n}' },
        { name: "battery", sig: 'battery(string $callback)', desc: "Get battery level and charging status.",
          params: [["$callback","Receives {level, charging}"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Battery", [\n            label("---", ["id" => "bat"]),\n            button("Check Battery", "doBattery"),\n        ]);\n    }\n    function doBattery($p) { return battery("onBattery"); }\n    function onBattery($p) {\n        $level = $p["level"] ?? 0;\n        $charging = ($p["charging"] ?? false) ? "Yes" : "No";\n        return setText("bat", "Level: $level% | Charging: $charging");\n    }\n}' },
        { name: "accelerometer / gyroscope / gravity", sig: 'accelerometer(string $callback)\ngyroscope(string $callback)\ngravity(string $callback)', desc: "Read motion sensors. Callback receives: {x, y, z, values}.",
          params: [["$callback","Receives sensor data"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Motion Sensors", [\n            label("---", ["id" => "data"]),\n            button("Accelerometer", "doAccel"),\n            button("Gyroscope", "doGyro"),\n        ]);\n    }\n    function doAccel($p) { return accelerometer("onSensor"); }\n    function doGyro($p) { return gyroscope("onSensor"); }\n    function onSensor($p) {\n        $x = round($p["x"] ?? 0, 2);\n        $y = round($p["y"] ?? 0, 2);\n        $z = round($p["z"] ?? 0, 2);\n        return setText("data", "X:$x  Y:$y  Z:$z");\n    }\n}' },
        { name: "light / proximity / pressure", sig: 'light(string $callback)\nproximity(string $callback)\npressure(string $callback)', desc: "Environment sensors: light (lux), proximity (distance), pressure (hPa).",
          params: [["$callback","Receives sensor data"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Environment", [\n            label("---", ["id" => "data"]),\n            button("Light", "doLight"),\n            button("Proximity", "doProx"),\n            button("Pressure", "doPress"),\n        ]);\n    }\n    function doLight($p) { return light("onEnv"); }\n    function doProx($p) { return proximity("onEnv"); }\n    function doPress($p) { return pressure("onEnv"); }\n    function onEnv($p) {\n        return setText("data", json_encode($p));\n    }\n}' },
        { name: "compass / magneticField / orientation", sig: 'compass(string $callback)\nmagneticField(string $callback)\norientation(string $callback)', desc: "Direction sensors — azimuth, pitch, roll, and magnetic field.",
          params: [["$callback","Receives sensor data"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Direction", [\n            label("---", ["id" => "dir"]),\n            button("Compass", "doCompass"),\n        ]);\n    }\n    function doCompass($p) { return compass("onCompass"); }\n    function onCompass($p) {\n        $az = round($p["azimuth"] ?? 0);\n        $dir = compassDirection($az);\n        return setText("dir", "$dir ($az°)");\n    }\n}' },
        { name: "stepCounter / temperature / humidity", sig: 'stepCounter(string $callback)\ntemperature(string $callback)\nhumidity(string $callback)', desc: "Step counter (steps since reboot), temperature (°C), humidity (%).",
          params: [["$callback","Receives sensor data"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("More Sensors", [\n            label("---", ["id" => "info"]),\n            button("Steps", "doSteps"),\n            button("Temp", "doTemp"),\n            button("Humidity", "doHum"),\n        ]);\n    }\n    function doSteps($p) { return stepCounter("onData"); }\n    function doTemp($p) { return temperature("onData"); }\n    function doHum($p) { return humidity("onData"); }\n    function onData($p) { return setText("info", json_encode($p)); }\n}' },
        { name: "scanBarcode / scanCode", sig: 'scanBarcode(string $callback)\nscanCode(string $callback)', desc: "Scan a barcode or QR code. Requires an external scanner app (like ZXing) to be installed. scanCode is an alias for scanBarcode.",
          params: [["$callback","Receives {code, format} with scanned data, or {error} if no scanner"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Barcode Scanner", [\n            label("Scan a code", ["id" => "result"]),\n            button("Scan Barcode", "doScan"),\n            button("Scan QR", "doQR"),\n        ]);\n    }\n    function doScan($p) { return scanBarcode("onScanned"); }\n    function doQR($p) { return scanCode("onScanned"); }\n    function onScanned($p) {\n        $code = $p["code"] ?? "none";\n        $fmt = $p["format"] ?? "";\n        if (isset($p["error"])) return setText("result", "Error: ".$p["error"]);\n        return setText("result", "Code: $code ($fmt)");\n    }\n}' },
        { name: "locationEnabled", sig: 'locationEnabled(string $callback)', desc: "Check if GPS/location services are enabled on the device.",
          params: [["$callback","Receives {enabled: true/false}"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Location Check", [\n            label("---", ["id" => "loc_status"]),\n            button("Check GPS", "checkGps"),\n        ]);\n    }\n    function checkGps($p) { return locationEnabled("onGpsCheck"); }\n    function onGpsCheck($p) {\n        $on = ($p["enabled"] ?? false) ? "Enabled ✓" : "Disabled ✗";\n        return setText("loc_status", "GPS: $on");\n    }\n}' },
    ]
},
{
    section: "Device & Network", icon: "📱", desc: "Device info, screen, WiFi, Bluetooth, network status",
    items: [
        { name: "deviceInfo / screenInfo", sig: 'deviceInfo(string $callback)\nscreenInfo(string $callback)', desc: "Get device model, OS, screen size, density, etc.",
          params: [["$callback","Receives info object"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Device Info", [\n            label("---", ["id" => "info"]),\n            button("Device", "doDevice"),\n            button("Screen", "doScreen"),\n        ]);\n    }\n    function doDevice($p) { return deviceInfo("onInfo"); }\n    function doScreen($p) { return screenInfo("onInfo"); }\n    function onInfo($p) {\n        $txt = "";\n        foreach ($p as $k => $v) $txt .= "$k: $v\\n";\n        return setText("info", $txt);\n    }\n}' },
        { name: "networkInfo / wifiInfo / wifiScan", sig: 'networkInfo(string $callback)\nwifiInfo(string $callback)\nwifiScan(string $callback)', desc: "Network connectivity status, WiFi SSID/IP, scan nearby networks.",
          params: [["$callback","Receives network data"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Network", [\n            label("---", ["id" => "net"]),\n            button("Network Info", "doNet"),\n            button("WiFi Info", "doWifi"),\n        ]);\n    }\n    function doNet($p) { return networkInfo("onNet"); }\n    function doWifi($p) { return wifiInfo("onNet"); }\n    function onNet($p) {\n        $txt = "";\n        foreach ($p as $k => $v) $txt .= "$k: $v\\n";\n        return setText("net", $txt);\n    }\n}' },
        { name: "bluetoothInfo / btDiscover", sig: 'bluetoothInfo(string $callback)\nbtDiscover(string $callback)', desc: "Bluetooth status, paired devices, discover nearby devices.",
          params: [["$callback","Receives BT data"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Bluetooth", [\n            label("---", ["id" => "bt"]),\n            button("BT Info", "doBt"),\n            button("Discover", "doDiscover"),\n        ]);\n    }\n    function doBt($p) { return bluetoothInfo("onBt"); }\n    function doDiscover($p) { return btDiscover("onBt"); }\n    function onBt($p) { return setText("bt", json_encode($p)); }\n}' },
    ]
},
{
    section: "HTTP & Download", icon: "🌐", desc: "HTTP GET/POST requests, file downloads",
    items: [
        { name: "httpGet / httpPost", sig: 'httpGet(string $url, string $callback)\nhttpPost(string $url, string $body, string $callback, string $headers = "")', desc: "Make HTTP requests. Callback receives: {response, error, url}.",
          params: [["$url","Request URL"],["$body","POST body"],["$callback","Receives response"],["$headers","Extra headers"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("HTTP", [\n            label("---", ["id" => "result"]),\n            button("GET Request", "doGet"),\n            button("POST Request", "doPost"),\n        ]);\n    }\n    function doGet($p) {\n        return httpGet("https://jsonplaceholder.typicode.com/todos/1", "onResp");\n    }\n    function doPost($p) {\n        return httpPost("https://httpbin.org/post", \'{"name":"PHP"}\', "onResp");\n    }\n    function onResp($p) {\n        $resp = $p["response"] ?? $p["error"] ?? "No data";\n        return setText("result", substr($resp, 0, 200));\n    }\n}' },
        { name: "download", sig: 'download(string $url, string $dest, string $callback)', desc: "Download a file to the device. Callback receives: {file, success}.",
          params: [["$url","File URL"],["$dest","Destination path"],["$callback","Receives result"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Download", [\n            label("Ready", ["id" => "status"]),\n            button("Download Image", "doDownload"),\n        ]);\n    }\n    function doDownload($p) {\n        return download(\n            "https://picsum.photos/200",\n            "/sdcard/Download/photo.jpg",\n            "onDone"\n        );\n    }\n    function onDone($p) {\n        $ok = ($p["success"] ?? false) ? "Success!" : "Failed";\n        return setText("status", $ok);\n    }\n}' },
    ]
},
{
    section: "Media", icon: "🎵", desc: "Play audio, record, take photos, ringtones",
    items: [
        { name: "playSound / stopSound", sig: 'playSound(string $file, string $callback)\nstopSound(string $callback)', desc: "Play and stop audio files.",
          params: [["$file","Audio file path"],["$callback","Status callback"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Audio", [\n            label("Idle", ["id" => "status"]),\n            button("Play", "doPlay"),\n            button("Stop", "doStop"),\n        ]);\n    }\n    function doPlay($p) { return playSound("/sdcard/Music/song.mp3", "onAudio"); }\n    function doStop($p) { return stopSound("onAudio"); }\n    function onAudio($p) { return setText("status", $p["status"] ?? ""); }\n}' },
        { name: "recordSound / stopRecord", sig: 'recordSound(string $file, string $callback)\nstopRecord(string $callback)', desc: "Record and stop recording audio.",
          params: [["$file","Save path"],["$callback","Status callback"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Record", [\n            label("Idle", ["id" => "rec_status"]),\n            button("Start Recording", "doRecord"),\n            button("Stop Recording", "doStopRec"),\n        ]);\n    }\n    function doRecord($p) { return recordSound("/sdcard/rec.wav", "onRecord"); }\n    function doStopRec($p) { return stopRecord("onRecord"); }\n    function onRecord($p) { return setText("rec_status", $p["status"] ?? ""); }\n}' },
        { name: "takePhoto", sig: 'takePhoto(string $callback)', desc: "Open camera and take a photo. Callback receives: {uri}.",
          params: [["$callback","Receives photo URI"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Camera", [\n            button("Take Photo", "doPhoto"),\n            label("No photo yet", ["id" => "photo_status"]),\n        ]);\n    }\n    function doPhoto($p) { return takePhoto("onPhoto"); }\n    function onPhoto($p) {\n        return setText("photo_status", "Photo: " . ($p["uri"] ?? "none"));\n    }\n}' },
        { name: "speak / listenSpeech", sig: 'speak(string $text)\nlistenSpeech(string $callback)', desc: "Text-to-speech and speech recognition.",
          params: [["$text","Text to speak aloud"],["$callback","Receives {text} from recognition"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Speech", [\n            label("---", ["id" => "heard"]),\n            button("Speak Hello", "doSpeak"),\n            button("Listen", "doListen"),\n        ]);\n    }\n    function doSpeak($p) { return speak("Hello from PHP!"); }\n    function doListen($p) { return listenSpeech("onHeard"); }\n    function onHeard($p) {\n        return setText("heard", "You said: " . ($p["text"] ?? ""));\n    }\n}' },
        { name: "playRingtone", sig: 'playRingtone(string $type = \x27notification\x27)', desc: "Play a system ringtone sound. Types: notification, alarm, ringtone.",
          params: [["$type","Ringtone type: notification, alarm, ringtone"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Ringtone", [\n            button("Notification Sound", "doNotif"),\n            button("Alarm Sound", "doAlarm"),\n            button("Ringtone", "doRing"),\n        ]);\n    }\n    function doNotif($p) { return playRingtone("notification"); }\n    function doAlarm($p) { return playRingtone("alarm"); }\n    function doRing($p) { return playRingtone("ringtone"); }\n}' },
    ]
},
{
    section: "Hardware", icon: "🔧", desc: "Vibrate, volume, brightness, flashlight, clipboard",
    items: [
        { name: "vibratePhone", sig: 'vibratePhone(int $ms = 200)', desc: "Vibrate the device for given milliseconds.",
          params: [["$ms","Duration in ms"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Vibrate", [\n            button("Vibrate 200ms", "doVib"),\n            button("Vibrate 1 sec", "doVibLong"),\n        ]);\n    }\n    function doVib($p) { return vibratePhone(200); }\n    function doVibLong($p) { return vibratePhone(1000); }\n}' },
        { name: "getVol / setVol / setRinger", sig: 'getVol(string $callback, string $stream = "music")\nsetVol(int $level, string $stream = "music")\nsetRinger(string $mode)', desc: "Volume & ringer control. Streams: music, ring, alarm, notification.",
          params: [["$callback","Receives {volume, stream}"],["$level","Volume level 0-15"],["$mode","normal / vibrate / silent"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Volume", [\n            label("---", ["id" => "vol"]),\n            button("Get Volume", "doGetVol"),\n            button("Set Vol 10", "doSetVol"),\n            button("Silent Mode", "doSilent"),\n        ]);\n    }\n    function doGetVol($p) { return getVol("onVol"); }\n    function doSetVol($p) { return setVol(10, "music"); }\n    function doSilent($p) { return setRinger("silent"); }\n    function onVol($p) { return setText("vol", "Volume: " . ($p["volume"] ?? 0)); }\n}' },
        { name: "setBrightness / keepScreenOn", sig: 'setBrightness(float $level)\nkeepScreenOn(bool $on = true)', desc: "Screen brightness (0.0-1.0) and screen lock prevention.",
          params: [["$level","0.0 to 1.0"],["$on","True to keep on"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Screen", [\n            button("Max Brightness", "maxBright"),\n            button("Dim", "dimBright"),\n            button("Keep On", "stayOn"),\n        ]);\n    }\n    function maxBright($p) { return setBrightness(1.0); }\n    function dimBright($p) { return setBrightness(0.2); }\n    function stayOn($p) { return keepScreenOn(true); }\n}' },
        { name: "toggleFlash", sig: 'toggleFlash(bool $on = true)', desc: "Turn flashlight on or off.",
          params: [["$on","true = on, false = off"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Flash", [\n            button("Flash ON", "flashOn"),\n            button("Flash OFF", "flashOff"),\n        ]);\n    }\n    function flashOn($p) { return toggleFlash(true); }\n    function flashOff($p) { return toggleFlash(false); }\n}' },
        { name: "clipboardCopy / clipboardPaste", sig: 'clipboardCopy(string $text)\nclipboardPaste(string $callback)', desc: "Copy to or read from the system clipboard.",
          params: [["$text","Text to copy"],["$callback","Receives {text}"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Clipboard", [\n            label("---", ["id" => "clip"]),\n            button("Copy Hello", "doCopy"),\n            button("Paste", "doPaste"),\n        ]);\n    }\n    function doCopy($p) { return clipboardCopy("Hello from PHP!"); }\n    function doPaste($p) { return clipboardPaste("onPaste"); }\n    function onPaste($p) { return setText("clip", $p["text"] ?? "empty"); }\n}' },
    ]
},
{
    section: "Communication", icon: "📨", desc: "SMS, phone calls, email, notifications",
    items: [
        { name: "sms / call", sig: 'sms(string $phone, string $message, string $callback = "_noop")\ncall(string $number)', desc: "Send SMS or make a phone call.",
          params: [["$phone","Phone number"],["$message","SMS text"],["$number","Number to call"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Phone", [\n            input("phone", "Phone number"),\n            button("Send SMS", "doSms"),\n            button("Call", "doCall"),\n        ]);\n    }\n    function doSms($p) {\n        return getText("phone", "sendSms");\n    }\n    function sendSms($p) {\n        return sms($p["value"], "Hello from PHP!");\n    }\n    function doCall($p) { return getText("phone", "makeCall"); }\n    function makeCall($p) { return call($p["value"]); }\n}' },
        { name: "email", sig: 'email(string $to, string $subject, string $body, string $attachment = "")', desc: "Open email compose with pre-filled fields.",
          params: [["$to","Recipient email"],["$subject","Subject line"],["$body","Email body"],["$attachment","File path"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Email", [\n            button("Send Email", "doEmail"),\n        ]);\n    }\n    function doEmail($p) {\n        return email("user@example.com", "Hello", "Sent from PHP Android app!");\n    }\n}' },
        { name: "notify", sig: 'notify(string $title, string $message, string $callback = "_noop")', desc: "Show a system notification.",
          params: [["$title","Notification title"],["$message","Body text"],["$callback","Tap handler"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Notification", [\n            button("Send Notification", "doNotify"),\n        ]);\n    }\n    function doNotify($p) {\n        return notify("PHP Alert", "Something happened!");\n    }\n}' },
    ]
},
{
    section: "Encryption & Hashing", icon: "🔐", desc: "AES encrypt/decrypt, MD5, SHA256",
    items: [
        { name: "encrypt / decrypt", sig: 'encrypt(string $text, string $password, string $callback)\ndecrypt(string $text, string $password, string $callback)', desc: "AES encrypt or decrypt text with a password.",
          params: [["$text","Text to process"],["$password","Encryption key"],["$callback","Receives {result}"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Encrypt", [\n            input("txt", "Text to encrypt"),\n            label("---", ["id" => "result"]),\n            button("Encrypt", "doEncrypt"),\n            button("Decrypt", "doDecrypt"),\n        ]);\n    }\n    function doEncrypt($p) { return getText("txt", "encIt"); }\n    function encIt($p) {\n        return encrypt($p["value"], "mySecret", "onEnc");\n    }\n    function onEnc($p) { return setText("result", $p["result"] ?? ""); }\n    function doDecrypt($p) { return get("result", "text", "decIt"); }\n    function decIt($p) {\n        return decrypt($p["value"], "mySecret", "onDec");\n    }\n    function onDec($p) { return toast("Decrypted: " . ($p["result"] ?? "")); }\n}' },
        { name: "hashText / md5Hash / sha256", sig: 'hashText(string $text, string $algorithm, string $callback)\nmd5Hash(string $text, string $callback)\nsha256(string $text, string $callback)', desc: "Hash text with MD5, SHA1, SHA256, SHA512.",
          params: [["$text","Input text"],["$algorithm","MD5, SHA1, SHA256, SHA512"],["$callback","Receives {result}"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Hash", [\n            input("input", "Text to hash"),\n            label("---", ["id" => "hash"]),\n            button("MD5", "doMd5"),\n            button("SHA256", "doSha"),\n        ]);\n    }\n    function doMd5($p) { return getText("input", "hashMd5"); }\n    function hashMd5($p) { return md5Hash($p["value"], "onHash"); }\n    function doSha($p) { return getText("input", "hashSha"); }\n    function hashSha($p) { return sha256($p["value"], "onHash"); }\n    function onHash($p) { return setText("hash", $p["result"] ?? ""); }\n}' },
    ]
},
{
    section: "File System", icon: "📁", desc: "Read, write, list, check files on device",
    items: [
        { name: "loadFile / writeFile", sig: 'loadFile(string $path, string $callback)\nwriteFile(string $path, string $content, string $callback = "_noop")', desc: "Read or write files on device storage. Note: loadFile (not readFile) to avoid conflict with PHP\'s built-in readfile().",
          params: [["$path","File path"],["$content","Data to write"],["$callback","Receives {content} or {success}"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Files", [\n            label("---", ["id" => "data"]),\n            button("Write File", "doWrite"),\n            button("Read File", "doRead"),\n        ]);\n    }\n    function doWrite($p) {\n        return writeFile("/sdcard/test.txt", "Hello from PHP!");\n    }\n    function doRead($p) {\n        return loadFile("/sdcard/test.txt", "onRead");\n    }\n    function onRead($p) {\n        return setText("data", $p["content"] ?? $p["error"] ?? "");\n    }\n}' },
        { name: "listFiles / fileExists", sig: 'listFiles(string $path, string $callback)\nfileExists(string $path, string $callback)', desc: "List folder contents or check if a file exists.",
          params: [["$path","Folder or file path"],["$callback","Receives {files} or {exists}"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("File List", [\n            label("---", ["id" => "files"]),\n            button("List /sdcard/", "doList"),\n            button("Check test.txt", "doCheck"),\n        ]);\n    }\n    function doList($p) { return listFiles("/sdcard/", "onList"); }\n    function doCheck($p) { return fileExists("/sdcard/test.txt", "onExists"); }\n    function onList($p) {\n        $files = $p["files"] ?? [];\n        return setText("files", implode("\\n", array_slice($files, 0, 10)));\n    }\n    function onExists($p) {\n        return toast(($p["exists"] ?? false) ? "File exists!" : "Not found");\n    }\n}' },
    ]
},
{
    section: "Apps & Intents", icon: "🚀", desc: "Launch apps, send Android intents",
    items: [
        { name: "launchApp / sendAndroidIntent", sig: 'launchApp(string $package)\nsendAndroidIntent(string $action, string $uri = "", string $type = "", string $extras = "")', desc: "Open other apps or send system intents.",
          params: [["$package","App package name"],["$action","Intent action"],["$uri","Intent data URI"],["$type","MIME type"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Apps", [\n            button("Open Gmail", "openGmail"),\n            button("Open URL", "openUrl"),\n            button("Share Text", "shareIt"),\n        ]);\n    }\n    function openGmail($p) { return launchApp("com.google.android.gm"); }\n    function openUrl($p) {\n        return sendAndroidIntent("android.intent.action.VIEW", "https://php.net");\n    }\n    function shareIt($p) { return shareText("Check out PHP on Android!"); }\n}' },
    ]
},
{
    section: "Templates", icon: "📋", desc: "Pre-built screens: login, settings, forms, lists",
    items: [
        { name: "loginForm", sig: 'loginForm(string $onLogin, array $options = [])', desc: "Pre-built login form with username, password, and submit button.",
          params: [["$onLogin","Login handler"],["$options","title, showRemember, submitText"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Login", [\n            loginForm("doLogin", ["title" => "Welcome Back"]),\n        ]);\n    }\n    function doLogin($p) {\n        return toast("Logging in...");\n    }\n}' },
        { name: "formPage", sig: 'formPage(string $title, array $fields, string $onSubmit, string $submitText = "Submit")', desc: "Dynamic form from field definitions.",
          params: [["$title","Form title"],["$fields","Array of {id, label, type?, hint?}"],["$onSubmit","Submit handler"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return formPage("Register", [\n            ["id" => "name", "label" => "Full Name"],\n            ["id" => "email", "label" => "Email", "type" => "email"],\n            ["id" => "age", "label" => "Age", "type" => "number"],\n        ], "onSubmit", "Register");\n    }\n    function onSubmit($p) { return toast("Form submitted!"); }\n}' },
        { name: "settingsPage", sig: 'settingsPage(string $title, array $settings)', desc: "Auto-generated settings page with toggles and inputs.",
          params: [["$title","Page title"],["$settings","Array of {id, label, type, value?, onChange?}"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return settingsPage("Settings", [\n            ["id" => "dark", "label" => "Dark Mode", "type" => "toggle", "value" => true],\n            ["id" => "notify", "label" => "Notifications", "type" => "toggle"],\n            ["id" => "name", "label" => "Display Name", "type" => "text"],\n        ]);\n    }\n}' },
        { name: "tabbedPage / profilePage / detailPage", sig: 'tabbedPage(string $title, array $tabDefs, ...)\nprofilePage(array $profile)\ndetailPage(string $title, array $details, array $actions = [])', desc: "Pre-built tabbed, profile, and detail views.",
          params: [["$title","Page title"],["$tabDefs","Tab definitions"],["$profile","Profile data array"],["$details","Key-value pairs"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return profilePage([\n            "name" => "Jane Doe",\n            "email" => "jane@example.com",\n            "avatar" => "https://i.pravatar.cc/150",\n            "bio" => "PHP Android developer",\n        ]);\n    }\n}' },
        { name: "emptyState / loading", sig: 'emptyState(string $message, string $icon = "📭", ...)\nloading(string $message = "Loading...")', desc: "Placeholder screens for empty states or loading indicators.",
          params: [["$message","Display message"],["$icon","Emoji icon"],["$actionText","Optional button"],["$actionMethod","Button handler"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Empty State", [\n            emptyState("No items yet", "📭", "Add Item", "onAdd"),\n        ]);\n    }\n    function onAdd($p) { return toast("Adding..."); }\n}' },
        { name: "simpleList", sig: 'simpleList(array $items, ?string $onItemClick = null)', desc: "Simple scrollable list display from an array of strings or items.",
          params: [["$items","Array of strings or item arrays"],["$onItemClick","Click handler receives {item, index}"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Simple List", [\n            simpleList([\n                "Buy groceries",\n                "Walk the dog",\n                "Study PHP",\n                "Build an app",\n            ], "onItem"),\n        ]);\n    }\n    function onItem($p) {\n        return toast("Tapped: " . ($p["item"] ?? ""));\n    }\n}' },
        { name: "confirmDialog", sig: 'confirmDialog(string $message, string $onYes, string $onNo)', desc: "Quick confirmation dialog template with Yes/No actions.",
          params: [["$message","Confirmation message"],["$onYes","Yes handler"],["$onNo","No handler"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Confirm Dialog", [\n            button("Delete Account", "askDelete"),\n        ]);\n    }\n    function askDelete($p) {\n        return confirmDialog("Are you sure you want to delete?", "doDelete", "doCancel");\n    }\n    function doDelete($p) { return toast("Deleted!"); }\n    function doCancel($p) { return toast("Cancelled"); }\n}' },
        { name: "statusLabel", sig: 'statusLabel(string $id, string $initialText = "Ready", string $icon = "ℹ️")', desc: "Status display label with icon prefix.",
          params: [["$id","Unique ID"],["$initialText","Initial status text"],["$icon","Emoji icon prefix"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Status Label", [\n            statusLabel("status", "Ready", "✅"),\n            button("Start Process", "doStart"),\n        ]);\n    }\n    function doStart($p) {\n        return setText("status", "⏳ Processing...");\n    }\n}' },
        { name: "mediaCard", sig: 'mediaCard(array $data)', desc: "Media card template with image, title, subtitle, and action buttons.",
          params: [["$data","Keys: image, title, subtitle, actions (array of buttons)"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Media Card", [\n            mediaCard([\n                "image" => "https://picsum.photos/400/200",\n                "title" => "Mountain View",\n                "subtitle" => "A beautiful landscape photo",\n                "actions" => [\n                    ["text" => "Share", "action" => "onShare"],\n                    ["text" => "Like", "action" => "onLike"],\n                ],\n            ]),\n        ]);\n    }\n    function onShare($p) { return toast("Sharing..."); }\n    function onLike($p) { return toast("Liked! ❤️"); }\n}' },
    ]
},
{
    section: "Clipboard & Sharing", icon: "📋", desc: "Copy, share text, and open URLs",
    items: [
        { name: "copyText", sig: 'copyText(string $text, string $label = "Copied")', desc: "Copy text to the system clipboard with an optional toast label.",
          params: [["$text","Text to copy"],["$label","Toast message shown after copying"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Copy Text", [\n            input("data", "Enter text to copy"),\n            button("Copy", "doCopy"),\n        ]);\n    }\n    function doCopy($p) {\n        return getText("data", "gotText");\n    }\n    function gotText($p) {\n        return copyText($p["value"] ?? "", "Copied to clipboard!");\n    }\n}' },
        { name: "shareText", sig: 'shareText(string $text, string $title = "Share")', desc: "Share text via the Android share sheet (Bluetooth, email, messaging, etc.).",
          params: [["$text","Text to share"],["$title","Share dialog title"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Share", [\n            input("msg", "Message to share"),\n            button("Share", "doShare"),\n        ]);\n    }\n    function doShare($p) {\n        return getText("msg", "gotMsg");\n    }\n    function gotMsg($p) {\n        return shareText($p["value"] ?? "Hello!", "Share via");\n    }\n}' },
        { name: "openBrowser", sig: 'openBrowser(string $url)', desc: "Open a URL in the device\x27s default web browser.",
          params: [["$url","URL to open"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Open Browser", [\n            button("Visit PHP.net", "openPhp"),\n            button("Visit Google", "openGoogle"),\n        ]);\n    }\n    function openPhp($p) { return openBrowser("https://php.net"); }\n    function openGoogle($p) { return openBrowser("https://google.com"); }\n}' },
    ]
},
{
    section: "View Manipulation", icon: "🔧", desc: "Add, remove, replace, and scroll components dynamically",
    items: [
        { name: "removeComponent", sig: 'removeComponent(string $id)', desc: "Remove a view from the layout by its ID.",
          params: [["$id","ID of the view to remove"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Remove", [\n            label("I can be removed", ["id" => "target"]),\n            button("Remove Label", "doRemove"),\n        ]);\n    }\n    function doRemove($p) { return removeComponent("target"); }\n}' },
        { name: "addComponent", sig: 'addComponent(string $parentId, Component $child, int $index = -1)', desc: "Add a child component to a parent layout at an optional index.",
          params: [["$parentId","Parent layout ID"],["$child","Component to add"],["$index","-1 = append, otherwise insert position"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Add Component", [\n            card("Container", [\n                label("Existing item"),\n            ]),\n            button("Add Label", "doAdd"),\n        ], ["id" => "container"]);\'\n    }\n    function doAdd($p) {\n        return addComponent("container", label("New item added!"));\n    }\n}' },
        { name: "replaceContent", sig: 'replaceContent(string $parentId, array $children)', desc: "Replace all children of a parent layout with new components.",
          params: [["$parentId","Parent layout ID"],["$children","Array of new child components"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Replace", [\n            card("Content", [\n                label("Old content"),\n            ]),\n            button("Replace All", "doReplace"),\n        ], ["id" => "box"]);\'\n    }\n    function doReplace($p) {\n        return replaceContent("box", [\n            label("Brand new content!"),\n            button("Hello", "onTap"),\n        ]);\n    }\n    function onTap($p) { return toast("New button works!"); }\n}' },
        { name: "scrollToPosition", sig: 'scrollToPosition(string $id, int $x = 0, int $y = 0, bool $smooth = true)', desc: "Scroll a scrollable view to a specific position.",
          params: [["$id","ScrollView ID"],["$x","Horizontal position"],["$y","Vertical position"],["$smooth","Use smooth scrolling"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        $items = [];\n        for ($i = 1; $i <= 50; $i++)\n            $items[] = label("Item #$i");\n        return page("Scroll To", [\n            button("Scroll to Bottom", "goBottom"),\n            scrollView($items),\n        ], ["id" => "scroller"]);\'\n    }\n    function goBottom($p) {\n        return scrollToPosition("scroller", 0, 9999, true);\n    }\n}' },
    ]
},
{
    section: "Data — Get Values", icon: "📥", desc: "Retrieve values from input fields and views",
    items: [
        { name: "getText", sig: 'getText(string $id, string $callback)', desc: "Get the current text value from an input field or label.",
          params: [["$id","View ID"],["$callback","Receives {value} with the text"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Get Text", [\n            input("name", "Enter your name"),\n            button("Read", "doRead"),\n        ]);\n    }\n    function doRead($p) { return getText("name", "gotIt"); }\n    function gotIt($p) {\n        return toast("Value: " . ($p["value"] ?? ""));\n    }\n}' },
        { name: "getChecked", sig: 'getChecked(string $id, string $callback)', desc: "Get the current checked state of a checkbox or toggle.",
          params: [["$id","Checkbox or toggle ID"],["$callback","Receives {checked: true/false}"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Get Checked", [\n            checkbox("terms", "Accept Terms"),\n            button("Check State", "doCheck"),\n        ]);\n    }\n    function doCheck($p) { return getChecked("terms", "gotState"); }\n    function gotState($p) {\n        $state = ($p["checked"] ?? false) ? "Checked ✓" : "Unchecked ✗";\n        return toast($state);\n    }\n}' },
    ]
},
{
    section: "Shortcut Setters — Extended", icon: "🎛️", desc: "Fine-grained property setters for text, layout, and transforms",
    items: [
        { name: "setTextStyle / setGravity", sig: 'setTextStyle(string $id, string $style)\nsetGravity(string $id, string $gravity)', desc: "Set text style (bold, italic, normal) or text alignment (left, center, right).",
          params: [["$id","View ID"],["$style","bold, italic, normal, bold_italic"],["$gravity","left, center, right"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Text Style", [\n            label("Style me", ["id" => "lbl", "size" => 24]),\n            button("Bold", "doBold"),\n            button("Italic", "doItalic"),\n            button("Center", "doCenter"),\n        ]);\n    }\n    function doBold($p) { return setTextStyle("lbl", "bold"); }\n    function doItalic($p) { return setTextStyle("lbl", "italic"); }\n    function doCenter($p) { return setGravity("lbl", "center"); }\n}' },
        { name: "setWidth / setHeight / setDimensions", sig: 'setWidth(string $id, mixed $width)\nsetHeight(string $id, mixed $height)\nsetDimensions(string $id, mixed $width, mixed $height)', desc: "Set view width, height, or both. Values can be int (dp), \x27match_parent\x27, or \x27wrap_content\x27.",
          params: [["$id","View ID"],["$width","Width in dp or string"],["$height","Height in dp or string"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Dimensions", [\n            label("Resize me", ["id" => "box", "size" => 18]),\n            button("Set 200x100", "setDim"),\n            button("Full Width", "fullW"),\n        ]);\n    }\n    function setDim($p) { return setDimensions("box", 200, 100); }\n    function fullW($p) { return setWidth("box", "match_parent"); }\n}' },
        { name: "setPadding / setPaddingAll", sig: 'setPadding(string $id, int $left, int $top, int $right, int $bottom)\nsetPaddingAll(string $id, int $padding)', desc: "Set padding on individual sides or all sides at once.",
          params: [["$id","View ID"],["$left/$top/$right/$bottom","Padding values in dp"],["$padding","Uniform padding"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Padding", [\n            label("Padded text", ["id" => "lbl"]),\n            button("Pad All 20", "padAll"),\n            button("Pad Custom", "padCustom"),\n        ]);\n    }\n    function padAll($p) { return setPaddingAll("lbl", 20); }\n    function padCustom($p) { return setPadding("lbl", 10, 20, 10, 5); }\n}' },
        { name: "setMargin / setMarginAll", sig: 'setMargin(string $id, int $left, int $top, int $right, int $bottom)\nsetMarginAll(string $id, int $margin)', desc: "Set margin on individual sides or all sides at once.",
          params: [["$id","View ID"],["$left/$top/$right/$bottom","Margin values in dp"],["$margin","Uniform margin"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Margin", [\n            label("Margin text", ["id" => "lbl"]),\n            button("Margin All 16", "mAll"),\n            button("Custom Margin", "mCustom"),\n        ]);\n    }\n    function mAll($p) { return setMarginAll("lbl", 16); }\n    function mCustom($p) { return setMargin("lbl", 8, 16, 8, 4); }\n}' },
        { name: "setCornerRadius / setElevation", sig: 'setCornerRadius(string $id, int $radius)\nsetElevation(string $id, int $elevation)', desc: "Set rounded corners or shadow elevation on a view.",
          params: [["$id","View ID"],["$radius","Corner radius in dp"],["$elevation","Shadow depth in dp"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Corners & Shadow", [\n            label("Styled Box", ["id" => "box", "size" => 18]),\n            button("Round Corners", "doCorners"),\n            button("Add Shadow", "doElevation"),\n        ]);\n    }\n    function doCorners($p) { return setCornerRadius("box", 24); }\n    function doElevation($p) { return setElevation("box", 12); }\n}' },
        { name: "setRotation / setRotationX / setRotationY", sig: 'setRotation(string $id, float $degrees)\nsetRotationX(string $id, float $degrees)\nsetRotationY(string $id, float $degrees)', desc: "Set rotation around Z, X, or Y axis.",
          params: [["$id","View ID"],["$degrees","Rotation in degrees"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Rotation", [\n            label("🎯", ["id" => "target", "size" => 48, "center" => true]),\n            button("Rotate 45°", "rot45"),\n            button("Flip X", "flipX"),\n            button("Flip Y", "flipY"),\n        ]);\n    }\n    function rot45($p) { return setRotation("target", 45); }\n    function flipX($p) { return setRotationX("target", 180); }\n    function flipY($p) { return setRotationY("target", 180); }\n}' },
        { name: "setScale / setScaleX / setScaleY", sig: 'setScale(string $id, float $scale)\nsetScaleX(string $id, float $scaleX)\nsetScaleY(string $id, float $scaleY)', desc: "Set uniform scale or individual X/Y scale factors.",
          params: [["$id","View ID"],["$scale","Uniform scale factor"],["$scaleX/$scaleY","Axis scale factor"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Scale", [\n            label("🔍", ["id" => "icon", "size" => 48, "center" => true]),\n            button("Scale 2x", "doScale"),\n            button("Stretch X", "stretchX"),\n            button("Reset", "doReset"),\n        ]);\n    }\n    function doScale($p) { return setScale("icon", 2.0); }\n    function stretchX($p) { return setScaleX("icon", 2.0); }\n    function doReset($p) { return setScale("icon", 1.0); }\n}' },
        { name: "setTranslationX / setTranslationY / setPosition", sig: 'setTranslationX(string $id, float $x)\nsetTranslationY(string $id, float $y)\nsetPosition(string $id, float $x, float $y)', desc: "Move a view by translating it on X, Y, or both axes.",
          params: [["$id","View ID"],["$x","X offset in dp"],["$y","Y offset in dp"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Position", [\n            label("Move me", ["id" => "mover", "size" => 20]),\n            button("Right 100", "moveRight"),\n            button("Down 50", "moveDown"),\n            button("Position 100,100", "setPos"),\n        ]);\n    }\n    function moveRight($p) { return setTranslationX("mover", 100); }\n    function moveDown($p) { return setTranslationY("mover", 50); }\n    function setPos($p) { return setPosition("mover", 100, 100); }\n}' },
        { name: "setProgress / setMax / setProgressWithMax", sig: 'setProgress(string $id, int $progress)\nsetMax(string $id, int $max)\nsetProgressWithMax(string $id, int $progress, int $max)', desc: "Set progress bar or seekbar values.",
          params: [["$id","View ID"],["$progress","Current value"],["$max","Maximum value"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Progress Setters", [\n            progress("bar", 30, 100),\n            button("Set 75%", "do75"),\n            button("Set Max 200", "doMax"),\n            button("Set 150/200", "doBoth"),\n        ]);\n    }\n    function do75($p) { return setProgress("bar", 75); }\n    function doMax($p) { return setMax("bar", 200); }\n    function doBoth($p) { return setProgressWithMax("bar", 150, 200); }\n}' },
        { name: "setMinLines / setMaxLines / setSingleLine", sig: 'setMinLines(string $id, int $lines)\nsetMaxLines(string $id, int $lines)\nsetSingleLine(string $id, bool $single = true)', desc: "Control text line limits on labels and inputs.",
          params: [["$id","View ID"],["$lines","Number of lines"],["$single","Force single line"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Line Control", [\n            input("notes", "Enter notes", ["id" => "notes", "lines" => 5]),\n            button("Single Line", "doSingle"),\n            button("Max 3 Lines", "doMax3"),\n        ]);\n    }\n    function doSingle($p) { return setSingleLine("notes"); }\n    function doMax3($p) { return setMaxLines("notes", 3); }\n}' },
        { name: "setInputType / setScaleType", sig: 'setInputType(string $id, string $type)\nsetScaleType(string $id, string $scaleType)', desc: "Change input type (text, email, number, phone, password) or image scale type (fitCenter, centerCrop, fitXY).",
          params: [["$id","View ID"],["$type","Input type string"],["$scaleType","Image scale type"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Input Type", [\n            input("field", "Enter value"),\n            button("Number Mode", "doNumber"),\n            button("Email Mode", "doEmail"),\n            button("Password", "doPass"),\n        ]);\n    }\n    function doNumber($p) { return setInputType("field", "number"); }\n    function doEmail($p) { return setInputType("field", "email"); }\n    function doPass($p) { return setInputType("field", "textPassword"); }\n}' },
        { name: "setClickable / setFocusable / setSelected", sig: 'setClickable(string $id, bool $clickable)\nsetFocusable(string $id, bool $focusable)\nsetSelected(string $id, bool $selected)', desc: "Toggle interactive states: clickable, focusable, or selected.",
          params: [["$id","View ID"],["$clickable/$focusable/$selected","true or false"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Interactive States", [\n            button("Target", "onTap", ["id" => "btn"]),\n            button("Disable Click", "noClick"),\n            button("Enable Click", "yesClick"),\n        ]);\n    }\n    function onTap($p) { return toast("Tapped!"); }\n    function noClick($p) { return setClickable("btn", false); }\n    function yesClick($p) { return setClickable("btn", true); }\n}' },
        { name: "setTag / setAllCaps / setLetterSpacing / setLineSpacing", sig: 'setTag(string $id, string $tag)\nsetAllCaps(string $id, bool $caps)\nsetLetterSpacing(string $id, float $spacing)\nsetLineSpacing(string $id, float $add, float $mult = 1.0)', desc: "Set tag data, uppercase mode, letter spacing, and line spacing.",
          params: [["$id","View ID"],["$tag","Custom tag data"],["$caps","Force uppercase"],["$spacing","Letter spacing em"],["$add/$mult","Line spacing values"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Text Extras", [\n            label("hello world", ["id" => "lbl", "size" => 20]),\n            button("ALL CAPS", "doCaps"),\n            button("Letter Space", "doSpace"),\n            button("Line Space", "doLine"),\n        ]);\n    }\n    function doCaps($p) { return setAllCaps("lbl", true); }\n    function doSpace($p) { return setLetterSpacing("lbl", 0.3); }\n    function doLine($p) { return setLineSpacing("lbl", 8, 1.5); }\n}' },
        { name: "setBorderColor / setBorderWidth / setBorder", sig: 'setBorderColor(string $id, string $color)\nsetBorderWidth(string $id, int $width)\nsetBorder(string $id, int $width, string $color)', desc: "Set border styling: color, width, or both together.",
          params: [["$id","View ID"],["$color","Border color"],["$width","Border width in dp"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Borders", [\n            label("Bordered", ["id" => "box", "size" => 20]),\n            button("Red Border", "doRed"),\n            button("Thick", "doThick"),\n            button("Blue 3dp", "doBoth"),\n        ]);\n    }\n    function doRed($p) { return setBorderColor("box", "#f44336"); }\n    function doThick($p) { return setBorderWidth("box", 4); }\n    function doBoth($p) { return setBorder("box", 3, "#2196F3"); }\n}' },
        { name: "focus / clearFocus", sig: 'focus(string $id)\nclearFocus(string $id)', desc: "Programmatically focus or remove focus from an input field.",
          params: [["$id","View ID"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Focus", [\n            input("first", "First field"),\n            input("second", "Second field"),\n            button("Focus Second", "doFocus"),\n            button("Clear Focus", "doClear"),\n        ]);\n    }\n    function doFocus($p) { return focus("second"); }\n    function doClear($p) { return clearFocus("second"); }\n}' },
    ]
},
{
    section: "Complete Getters", icon: "📤", desc: "Retrieve any view property value",
    items: [
        { name: "getTextValue / getColor / getTextSize / getBackground", sig: 'getTextValue(string $id, string $callback)\ngetColor(string $id, string $callback)\ngetTextSize(string $id, string $callback)\ngetBackground(string $id, string $callback)', desc: "Get text content, text color, text size, or background color of a view.",
          params: [["$id","View ID"],["$callback","Receives {value} with the property"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Get Text Props", [\n            label("Hello", ["id" => "lbl", "color" => "#4fc3f7", "size" => 24]),\n            button("Get Text", "doText"),\n            button("Get Color", "doColor"),\n            button("Get Size", "doSize"),\n        ]);\n    }\n    function doText($p) { return getTextValue("lbl", "onVal"); }\n    function doColor($p) { return getColor("lbl", "onVal"); }\n    function doSize($p) { return getTextSize("lbl", "onVal"); }\n    function onVal($p) { return toast("Value: " . ($p["value"] ?? "")); }\n}' },
        { name: "getVisibility / getEnabled / getAlpha / getCheckedState", sig: 'getVisibility(string $id, string $callback)\ngetEnabled(string $id, string $callback)\ngetAlpha(string $id, string $callback)\ngetCheckedState(string $id, string $callback)', desc: "Get visibility, enabled state, alpha, or checked state of a view.",
          params: [["$id","View ID"],["$callback","Receives {value} with the state"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Get States", [\n            checkbox("chk", "Check me"),\n            label("Visible label", ["id" => "lbl"]),\n            button("Get Visibility", "doVis"),\n            button("Get Checked", "doChk"),\n            button("Get Alpha", "doAlpha"),\n        ]);\n    }\n    function doVis($p) { return getVisibility("lbl", "onVal"); }\n    function doChk($p) { return getCheckedState("chk", "onVal"); }\n    function doAlpha($p) { return getAlpha("lbl", "onVal"); }\n    function onVal($p) { return toast("Result: " . json_encode($p["value"] ?? "")); }\n}' },
        { name: "getHint / getImageSrc / getWidth / getHeight", sig: 'getHint(string $id, string $callback)\ngetImageSrc(string $id, string $callback)\ngetWidth(string $id, string $callback)\ngetHeight(string $id, string $callback)', desc: "Get hint text, image source, width, or height of a view.",
          params: [["$id","View ID"],["$callback","Receives {value}"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Get Dimensions", [\n            input("inp", "Type here"),\n            button("Get Hint", "doHint"),\n            button("Get Width", "doWidth"),\n            button("Get Height", "doHeight"),\n        ]);\n    }\n    function doHint($p) { return getHint("inp", "onVal"); }\n    function doWidth($p) { return getWidth("inp", "onVal"); }\n    function doHeight($p) { return getHeight("inp", "onVal"); }\n    function onVal($p) { return toast("Value: " . ($p["value"] ?? "")); }\n}' },
        { name: "getPadding / getMargin / getProgress / getMax", sig: 'getPadding(string $id, string $callback)\ngetMargin(string $id, string $callback)\ngetProgress(string $id, string $callback)\ngetMax(string $id, string $callback)', desc: "Get padding, margin, progress, or max value of a view.",
          params: [["$id","View ID"],["$callback","Receives {value} or {left,top,right,bottom}"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Get Layout Props", [\n            progress("bar", 60, 100),\n            button("Get Progress", "doProgress"),\n            button("Get Max", "doMax"),\n        ]);\n    }\n    function doProgress($p) { return getProgress("bar", "onVal"); }\n    function doMax($p) { return getMax("bar", "onVal"); }\n    function onVal($p) { return toast("Value: " . ($p["value"] ?? "")); }\n}' },
        { name: "getRotation / getScaleX / getScaleY / getTranslationX / getTranslationY", sig: 'getRotation(string $id, string $callback)\ngetScaleX(string $id, string $callback)\ngetScaleY(string $id, string $callback)\ngetTranslationX(string $id, string $callback)\ngetTranslationY(string $id, string $callback)', desc: "Get current rotation, scale, or translation values of a view.",
          params: [["$id","View ID"],["$callback","Receives {value}"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Get Transforms", [\n            label("🎯", ["id" => "box", "size" => 48, "center" => true]),\n            button("Rotate 45", "doRot"),\n            button("Get Rotation", "getRot"),\n            button("Get ScaleX", "getSX"),\n        ]);\n    }\n    function doRot($p) { return setRotation("box", 45); }\n    function getRot($p) { return getRotation("box", "onVal"); }\n    function getSX($p) { return getScaleX("box", "onVal"); }\n    function onVal($p) { return toast("Value: " . ($p["value"] ?? "")); }\n}' },
        { name: "getSelected / getFocused / getTag", sig: 'getSelected(string $id, string $callback)\ngetFocused(string $id, string $callback)\ngetTag(string $id, string $callback)', desc: "Get selected state, focus state, or custom tag data from a view.",
          params: [["$id","View ID"],["$callback","Receives {value}"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Get States", [\n            input("field", "Focus me"),\n            button("Set Tag", "doTag"),\n            button("Get Tag", "getTag"),\n            button("Is Focused?", "isFocused"),\n        ]);\n    }\n    function doTag($p) { return setTag("field", "myData123"); }\n    function getTag($p) { return getTag("field", "onVal"); }\n    function isFocused($p) { return getFocused("field", "onVal"); }\n    function onVal($p) { return toast("Value: " . json_encode($p["value"] ?? "")); }\n}' },
    ]
},
{
    section: "Utility Helpers", icon: "🛠️", desc: "Formatting and validation helper functions",
    items: [
        { name: "formatNumber", sig: 'formatNumber($num)', desc: "Format a number with thousands separator (e.g. 1,234,567).",
          params: [["$num","Number to format"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Format Number", [\n            label(formatNumber(1234567)),\n            label(formatNumber(9876.54)),\n            label(formatNumber(42)),\n        ]);\n    }\n}' },
        { name: "formatMoney", sig: 'formatMoney($amount, $symbol = \x27$\x27)', desc: "Format a number as currency with symbol and two decimals.",
          params: [["$amount","Amount to format"],["$symbol","Currency symbol"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Format Money", [\n            label(formatMoney(1234.5)),\n            label(formatMoney(99.9, "€")),\n            label(formatMoney(50000, "¥")),\n        ]);\n    }\n}' },
        { name: "isEmpty", sig: 'isEmpty($str)', desc: "Check if a string is empty, null, or only whitespace. Returns boolean.",
          params: [["$str","String to check"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("isEmpty Check", [\n            input("val", "Enter something"),\n            button("Check", "doCheck"),\n        ]);\n    }\n    function doCheck($p) { return getText("val", "gotVal"); }\n    function gotVal($p) {\n        $v = $p["value"] ?? "";\n        $msg = isEmpty($v) ? "Field is empty!" : "Has value: $v";\n        return toast($msg);\n    }\n}' },
        { name: "compassDirection", sig: 'compassDirection($azimuth)', desc: "Convert compass azimuth (0-360) to cardinal direction string (N, NE, E, SE, etc.).",
          params: [["$azimuth","Compass azimuth in degrees"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."simple.php";\n\nclass MyApp {\n    function index() {\n        return page("Compass", [\n            label("---", ["id" => "dir"]),\n            button("Read Compass", "doCompass"),\n        ]);\n    }\n    function doCompass($p) { return compass("onCompass"); }\n    function onCompass($p) {\n        $az = round($p["azimuth"] ?? 0);\n        $dir = compassDirection($az);\n        return setText("dir", "$dir ($az°)");\n    }\n}' },
    ]
},
];

// ============================================================================
// DOCUMENTATION DATA — ui_core.php
// ============================================================================
var uicoreData = [
{
    section: "Component Base", icon: "🏗️", desc: "Abstract base class — fluent API, _action methods, state sync",
    items: [
        { name: "Component (class)", sig: 'abstract class Component', desc: "Base class for all UI components. Use the fluent API: (new Button())->id(\"btn\")->text(\"Click\"). Read runtime state with _get*() methods. Update views with _set*() action methods.",
          params: [["Fluent setters","->id(), ->text(), ->textColor(), ->textSize(), etc."],["_get*()","Synchronous reads: _getText(), _getChecked(), _getAlpha(), ..."],["_set*()","Action returns: _setText(), _setBackground(), _show(), _hide(), ..."],["_style*()","Compound: _styleText([...]), _styleView([...])"],["_transform()","Apply position, rotation, scale, alpha at once"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $btn = (new Button())\n            ->id("main_btn")\n            ->text("Click Me")\n            ->textColor("#FFFFFF")\n            ->backgroundColor("#2196F3")\n            ->cornerRadius(12)\n            ->padding(16)\n            ->action("onClick");\n\n        $layout = (new VerticalLayout([$btn]));\n        return $layout;\n    }\n\n    function onClick($p) {\n        $btn = new Button();\n        $btn->id("main_btn");\n        // Read current text synchronously\n        $text = $btn->_getText();\n        // Return action to update it\n        return $btn->_setText("Clicked! Was: $text", "#4CAF50");\n    }\n}' },
    ]
},
{
    section: "Standard Views", icon: "📦", desc: "TextView, Button, EditText, ImageView, and more",
    items: [
        { name: "TextView", sig: 'class TextView extends Component', desc: "Display text. Supports size, color, bold, gravity, padding, click actions.",
          params: [["->text()","Display text"],["->textSize()","Font size dp"],["->textColor()","Hex color"],["->textStyle()","bold, italic, normal"],["->gravity()","center, left, right"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new TextView())->text("Title")->textSize(28)->textColor("#4fc3f7")->textStyle("bold"),\n            (new TextView())->text("Subtitle")->textSize(16)->textColor("#888")->gravity("center"),\n            (new TextView())->text("Body text with padding")->padding(16)->backgroundColor("#1a1a2e")->cornerRadius(8),\n        ]);\n        return $layout;\n    }\n}' },
        { name: "Button", sig: 'class Button extends Component', desc: "Clickable button with action handler. Supports all text + view properties.",
          params: [["->text()","Button label"],["->action()","PHP method name"],["->backgroundColor()","Button color"],["->cornerRadius()","Rounded corners"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new Button())->id("btn1")->text("Primary")->action("onClick")\n                ->backgroundColor("#2196F3")->textColor("#fff")->cornerRadius(8),\n            (new Button())->id("btn2")->text("Danger")->action("onClick")\n                ->backgroundColor("#f44336")->textColor("#fff"),\n            (new Button())->id("btn3")->text("Outlined")->action("onClick")\n                ->backgroundColor("transparent")->textColor("#4fc3f7")\n                ->strokeColor("#4fc3f7")->strokeWidth(2)->cornerRadius(8),\n        ]);\n        return $layout;\n    }\n    function onClick($p) {\n        return ["action" => "toast", "message" => "Button: " . ($p["viewId"] ?? "")];\n    }\n}' },
        { name: "EditText", sig: 'class EditText extends Component', desc: "Text input field with hint, inputType, and text change handling.",
          params: [["->hint()","Placeholder text"],["->inputType()","text, email, number, phone, password"],["->maxLines()","Limit lines"],["->onTextChanged()","Change handler"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new EditText())->id("name")->hint("Enter name")->padding(12),\n            (new EditText())->id("email")->hint("Email")->inputType("email")->padding(12),\n            (new EditText())->id("pass")->hint("Password")->inputType("textPassword")->padding(12),\n            (new Button())->text("Submit")->action("onSubmit"),\n        ]);\n        return $layout;\n    }\n    function onSubmit($p) {\n        $name = (new EditText())->id("name")->_getText();\n        return ["action" => "toast", "message" => "Hello, $name!"];\n    }\n}' },
        { name: "CheckBox / ToggleButton / RatingBar / SeekBar", sig: 'class CheckBox extends Component\nclass ToggleButton extends Component\nclass RatingBar extends Component\nclass SeekBar extends Component', desc: "Interactive input controls: checkbox, toggle, stars, slider.",
          params: [["->checked()","Initial state"],["->onChange()","Change handler"],["->max()","Max value (SeekBar)"],["->numStars()","Star count (RatingBar)"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new CheckBox())->id("agree")->text("I agree")->onChange("onCheck"),\n            (new SeekBar())->id("vol")->max(100)->progress(50)->onChange("onSeek"),\n            (new RatingBar())->id("stars")->numStars(5)->rating(3)->onChange("onRate"),\n        ]);\n        return $layout;\n    }\n    function onCheck($p) { return ["action"=>"toast","message"=>"Checked: ".($p["checked"]?"yes":"no")]; }\n    function onSeek($p) { return ["action"=>"toast","message"=>"Vol: ".($p["progress"]??0)]; }\n    function onRate($p) { return ["action"=>"toast","message"=>"Stars: ".($p["rating"]??0)]; }\n}' },
        { name: "ImageView", sig: 'class ImageView extends Component', desc: "Display images from URLs or local paths.",
          params: [["->src()","Image URL or path"],["->scaleType()","fitCenter, centerCrop, fitXY"],["->cornerRadius()","Rounded corners"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new ImageView())->src("https://picsum.photos/300/200")\n                ->width("match_parent")->height(200)\n                ->scaleType("centerCrop")->cornerRadius(16),\n            (new TextView())->text("Photo from Picsum")->gravity("center")->padding(8),\n        ]);\n        return $layout;\n    }\n}' },
        { name: "Spinner / AutoComplete / SearchView", sig: 'class Spinner extends Component\nclass AutoCompleteTextView extends Component\nclass SearchView extends Component', desc: "Selection and search input widgets.",
          params: [["->items()","Array of options (Spinner)"],["->suggestions()","Autocomplete list"],["->hint()","Search placeholder"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new Spinner())->id("lang")->items(["PHP","Java","Python","Kotlin"])\n                ->onChange("onLang"),\n            (new SearchView())->id("search")->hint("Search...")\n                ->onSubmit("onSearch"),\n        ]);\n        return $layout;\n    }\n    function onLang($p) { return ["action"=>"toast","message"=>($p["value"]??"")]; }\n    function onSearch($p) { return ["action"=>"toast","message"=>"Searching: ".($p["query"]??"")]; }\n}' },
    ]
},
{
    section: "Material Components", icon: "🎨", desc: "FAB, Chip, TextInputLayout, TabLayout, Material Switch",
    items: [
        { name: "FloatingActionButton", sig: 'class FloatingActionButton extends Component', desc: "Material FAB — circular icon button.",
          params: [["->icon()","Material icon name"],["->backgroundColor()","FAB color"],["->action()","Click handler"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $fab = (new FloatingActionButton())\n            ->id("fab")->icon("add")\n            ->backgroundColor("#E91E63")\n            ->action("onFab");\n        $layout = new VerticalLayout([\n            (new TextView())->text("Press the FAB!")->padding(20),\n            $fab,\n        ]);\n        return $layout;\n    }\n    function onFab($p) { return ["action"=>"toast","message"=>"FAB!"]; }\n}' },
        { name: "Chip / ChipGroup", sig: 'class Chip extends Component\nclass ChipGroup extends Component', desc: "Material chips for tags, filters, selections.",
          params: [["->text()","Chip label"],["->closeable()","Show close icon"],["->action()","Click handler"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $chips = new ChipGroup([\n            (new Chip())->text("PHP")->action("onChip"),\n            (new Chip())->text("Java")->action("onChip"),\n            (new Chip())->text("Kotlin")->action("onChip"),\n        ]);\n        $layout = new VerticalLayout([\n            (new TextView())->text("Tags:")->padding(8),\n            $chips,\n        ]);\n        return $layout;\n    }\n    function onChip($p) { return ["action"=>"toast","message"=>($p["text"]??"")]; }\n}' },
        { name: "TextInputLayout", sig: 'class TextInputLayout extends Component', desc: "Material text field with floating label, helper text, error, counter.",
          params: [["->hint()","Floating label"],["->helperText()","Helper below field"],["->errorText()","Error message"],["->counterMax()","Character counter"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new TextInputLayout())->id("username")->hint("Username")\n                ->helperText("Enter 3-20 characters")->counterMax(20),\n            (new TextInputLayout())->id("email")->hint("Email")\n                ->inputType("email")->startIcon("email"),\n            (new Button())->text("Register")->action("onRegister"),\n        ]);\n        return $layout;\n    }\n    function onRegister($p) {\n        $user = (new TextInputLayout())->id("username")->_getText();\n        return ["action"=>"toast","message"=>"User: $user"];\n    }\n}' },
        { name: "TabLayout", sig: 'class TabLayout extends Component', desc: "Material tab bar with icons and badges.",
          params: [["->tabs()","Array of {title, icon?}"],["->onTabSelected()","Tab handler"],["_selectTab()","Action: select tab"],["_setBadge()","Action: set badge"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $tabs = (new TabLayout())->id("tabs")\n            ->tabs([\n                ["title" => "Home", "icon" => "home"],\n                ["title" => "Search", "icon" => "search"],\n                ["title" => "Profile", "icon" => "person"],\n            ])\n            ->onTabSelected("onTab");\n        $content = (new TextView())->id("content")->text("Home")->padding(20);\n        return (new VerticalLayout([$tabs, $content]));\n    }\n    function onTab($p) {\n        $tab = $p["title"] ?? "?";\n        $txt = new TextView(); $txt->id("content");\n        return $txt->_setText("Tab: $tab");\n    }\n}' },
        { name: "SwitchMaterial / SwitchView", sig: 'class SwitchMaterial extends Component\nclass SwitchView extends Component', desc: "Material Design switch toggle and standard Android Switch.",
          params: [["->text()","Label text"],["->checked()","Initial state"],["->onChange()","Change handler"],["_toggle()","Toggle state"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new SwitchMaterial())->id("dark")->text("Dark Mode")->checked(true)->onChange("onSwitch"),\n            (new SwitchView())->id("notify")->text("Notifications")->onChange("onSwitch"),\n            (new TextView())->id("status")->text("---")->padding(12),\n        ]);\n        return $layout;\n    }\n    function onSwitch($p) {\n        $txt = new TextView(); $txt->id("status");\n        $id = $p["viewId"] ?? "";\n        $on = ($p["checked"] ?? false) ? "ON" : "OFF";\n        return $txt->_setText("$id: $on");\n    }\n}' },
        { name: "RadioGroup", sig: 'class RadioGroup extends Component', desc: "Container for RadioButton — exclusive selection with _getCheckedId, _checkButton, _clearCheck.",
          params: [["->onCheckedChange()","Selection handler"],["_getCheckedId()","Get selected radio ID"],["_checkButton()","Select by ID"],["_clearCheck()","Clear selection"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $group = new RadioGroup([\n            (new RadioButton())->id("s")->text("Small"),\n            (new RadioButton())->id("m")->text("Medium")->checked(true),\n            (new RadioButton())->id("l")->text("Large"),\n        ]);\n        $group->id("sizes")->onCheckedChange("onPick");\n        $layout = new VerticalLayout([\n            $group,\n            (new Button())->text("Read Selection")->action("readSel"),\n            (new Button())->text("Clear")->action("clearSel"),\n        ]);\n        return $layout;\n    }\n    function onPick($p) { return ["action"=>"toast","message"=>"Picked: ".($p["checkedId"]??"")]; }\n    function readSel($p) {\n        $grp = new RadioGroup(); $grp->id("sizes");\n        $sel = $grp->_getCheckedId("none");\n        return ["action"=>"toast","message"=>"Current: $sel"];\n    }\n    function clearSel($p) {\n        $grp = new RadioGroup(); $grp->id("sizes");\n        return $grp->_clearCheck();\n    }\n}' },
        { name: "Toolbar", sig: 'class Toolbar extends Component', desc: "Standard Android Toolbar — lighter alternative to TopAppBar.",
          params: [["->title()","Toolbar title"],["->subtitle()","Subtitle"],["->backgroundColor()","Background color"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $toolbar = (new Toolbar())->title("My Toolbar")->subtitle("Lightweight")->backgroundColor("#333");\n        $layout = new VerticalLayout([\n            $toolbar,\n            (new TextView())->text("Content below toolbar")->padding(20),\n        ]);\n        return $layout;\n    }\n}' },
        { name: "SwipeRefreshLayout", sig: 'class SwipeRefreshLayout extends Component', desc: "Pull-to-refresh container. Wrap scrollable content to add swipe-down refresh.",
          params: [["->onRefresh()","Refresh handler"],["_setRefreshing()","Set refreshing state"],["_stopRefreshing()","Stop spinner"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $list = new VerticalLayout([\n            (new TextView())->text("Item 1")->padding(16),\n            (new TextView())->text("Item 2")->padding(16),\n            (new TextView())->text("Item 3")->padding(16),\n        ]);\n        $swipe = new SwipeRefreshLayout([$list]);\n        $swipe->id("refresh")->onRefresh("onRefresh");\n        return $swipe;\n    }\n    function onRefresh($p) {\n        $sr = new SwipeRefreshLayout(); $sr->id("refresh");\n        return $sr->_stopRefreshing();\n    }\n}' },
    ]
},
{
    section: "Layouts", icon: "📐", desc: "VerticalLayout, HorizontalLayout, CardView, GridLayout, ScrollView",
    items: [
        { name: "VerticalLayout / HorizontalLayout", sig: 'class VerticalLayout extends Component\nclass HorizontalLayout extends VerticalLayout', desc: "Stack children vertically or horizontally. Foundation of all layouts.",
          params: [["__construct","Array of children"],["->addChild()","Add one child"],["->addChildren()","Add array of children"],["->gravity()","Alignment"],["->padding()","Inner padding"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $row = new HorizontalLayout([\n            (new Button())->text("Left")->action("onClick")->weight(1),\n            (new Button())->text("Right")->action("onClick")->weight(1),\n        ]);\n        $layout = new VerticalLayout([\n            (new TextView())->text("Vertical + Horizontal")->textSize(20)->padding(16),\n            $row,\n            (new TextView())->text("Below the row")->padding(16),\n        ]);\n        return $layout;\n    }\n    function onClick($p) { return ["action"=>"toast","message"=>"Clicked"]; }\n}' },
        { name: "CardView", sig: 'class CardView extends VerticalLayout', desc: "Material card container with elevation and corner radius.",
          params: [["->cornerRadius()","Rounded corners"],["->elevation()","Shadow depth"],["->backgroundColor()","Card color"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $card = new CardView([\n            (new TextView())->text("Card Title")->textSize(18)->textStyle("bold")->padding(16),\n            (new TextView())->text("This is a card with elevation and rounded corners.")->padding(16),\n            (new Button())->text("Action")->action("onClick"),\n        ]);\n        $card->cornerRadius(16)->elevation(8)->margin(16);\n        return (new VerticalLayout([$card]));\n    }\n    function onClick($p) { return ["action"=>"toast","message"=>"Card action"]; }\n}' },
        { name: "ScrollView / GridLayout / TableLayout", sig: 'class ScrollView extends VerticalLayout\nclass GridLayout extends VerticalLayout\nclass TableLayout extends VerticalLayout', desc: "Scrollable content, grid arrangements, and table layouts.",
          params: [["->columnCount()","Grid columns"],["TableRow","Row in table"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $items = [];\n        for ($i = 1; $i <= 6; $i++) {\n            $items[] = (new CardView([\n                (new TextView())->text("Item $i")->padding(20)->gravity("center"),\n            ]))->cornerRadius(8)->elevation(4)->margin(4);\n        }\n        $grid = new GridLayout($items);\n        $grid->columnCount(2);\n        return (new ScrollView([$grid]));\n    }\n}' },
        { name: "FrameLayout / RelativeLayout / HorizontalScrollView", sig: 'class FrameLayout extends VerticalLayout\nclass RelativeLayout extends VerticalLayout\nclass HorizontalScrollView extends VerticalLayout', desc: "Overlay/stack container, relative positioning, and horizontal scrollable layout.",
          params: [["FrameLayout","Children stack on top of each other"],["RelativeLayout","Position children relative to parent"],["HorizontalScrollView","Horizontal scrolling container"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $frame = new FrameLayout([\n            (new ImageView())->src("https://picsum.photos/400/200")->width("match_parent")->height(200),\n            (new TextView())->text("Overlay")->textColor("#fff")->textSize(24)->gravity("center")->padding(16),\n        ]);\n        $cards = [];\n        for ($i = 1; $i <= 10; $i++)\n            $cards[] = (new CardView([(new TextView())->text("Card $i")->padding(20)]))->margin(4);\n        $hscroll = new HorizontalScrollView($cards);\n        return (new VerticalLayout([$frame, $hscroll]));\n    }\n}' },
        { name: "StackLayout", sig: 'class StackLayout extends VerticalLayout', desc: "ConstraintLayout-like positioning using FrameLayout with gravity. Children overlap with gravity control.",
          params: [["->gravity()","center, top, bottom, left, right, bottom|right"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $stack = new StackLayout([\n            (new ImageView())->src("https://picsum.photos/400/300")->width("match_parent")->height(300),\n            (new TextView())->text("Bottom Left")->textColor("#fff")->gravity("bottom|left")->padding(16),\n            (new FloatingActionButton())->icon("add")->action("onFab")->gravity("bottom|right")->margin(16),\n        ]);\n        return $stack;\n    }\n    function onFab($p) { return ["action"=>"toast","message"=>"FAB!"]; }\n}' },
        { name: "TableRow", sig: 'class TableRow extends VerticalLayout', desc: "A single row inside a TableLayout. Add cells as children.",
          params: [["__construct","Array of child components (columns)"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $table = new TableLayout([\n            new TableRow([\n                (new TextView())->text("Name")->textStyle("bold")->padding(8),\n                (new TextView())->text("Age")->textStyle("bold")->padding(8),\n            ]),\n            new TableRow([\n                (new TextView())->text("Alice")->padding(8),\n                (new TextView())->text("30")->padding(8),\n            ]),\n            new TableRow([\n                (new TextView())->text("Bob")->padding(8),\n                (new TextView())->text("25")->padding(8),\n            ]),\n        ]);\n        $table->stretchColumns("*");\n        return (new VerticalLayout([$table]));\n    }\n}' },
    ]
},
{
    section: "Navigation", icon: "🧭", desc: "DrawerLayout, NavigationDrawer, TopAppBar, BottomNavBar",
    items: [
        { name: "DrawerLayout + NavigationDrawer", sig: 'class DrawerLayout extends Component\nclass NavigationDrawer extends Component', desc: "Side navigation drawer with header and menu items.",
          params: [["->drawer()","Drawer content"],["->content()","Main content"],["->items()","Menu items"],["->onItemSelected()","Selection handler"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $nav = (new NavigationDrawer())\n            ->header((new TextView())->text("My App")->textSize(22)->padding(20)->textColor("#4fc3f7"))\n            ->items([\n                ["id" => "home", "title" => "Home", "icon" => "home"],\n                ["id" => "settings", "title" => "Settings", "icon" => "settings"],\n                ["id" => "about", "title" => "About", "icon" => "info"],\n            ])\n            ->onItemSelected("onNav")\n            ->selectedItem("home");\n\n        $content = new VerticalLayout([\n            (new TopAppBar())->title("My App")->navigationIcon("menu")->onNavigationClick("toggleDrawer"),\n            (new TextView())->id("page")->text("Home Page")->padding(20),\n        ]);\n\n        $drawer = (new DrawerLayout())->id("drawer")->drawer($nav)->content($content);\n        return $drawer;\n    }\n    function toggleDrawer($p) {\n        return (new DrawerLayout())->id("drawer")->_toggle();\n    }\n    function onNav($p) {\n        $item = $p["itemId"] ?? "";\n        $txt = new TextView(); $txt->id("page");\n        return $txt->_setText("Page: $item");\n    }\n}' },
        { name: "TopAppBar", sig: 'class TopAppBar extends Component', desc: "Material toolbar with title, navigation icon, and action buttons.",
          params: [["->title()","Toolbar title"],["->subtitle()","Subtitle"],["->navigationIcon()","menu, arrow_back"],["->actions()","Action buttons"],["->onNavigationClick()","Nav click handler"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $appBar = (new TopAppBar())\n            ->title("My App")\n            ->subtitle("Subtitle")\n            ->navigationIcon("menu")\n            ->actions([\n                ["id" => "search", "icon" => "search"],\n                ["id" => "more", "icon" => "more_vert"],\n            ])\n            ->onActionClick("onAction")\n            ->backgroundColor("#1a237e");\n\n        $layout = new VerticalLayout([\n            $appBar,\n            (new TextView())->text("Content area")->padding(20),\n        ]);\n        return $layout;\n    }\n    function onAction($p) { return ["action"=>"toast","message"=>"Action: ".($p["actionId"]??"")]; }\n}' },
        { name: "BottomNavBar", sig: 'class BottomNavBar extends Component', desc: "Bottom navigation with 3-5 items, badges, and selection handler.",
          params: [["->items()","Nav items: [{id, title, icon}]"],["->selectedItem()","Initial selection"],["->onItemSelected()","Selection handler"],["_setBadge()","Badge count"],["_setSelectedItem()","Change selection"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $nav = (new BottomNavBar())->id("bnav")\n            ->items([\n                ["id" => "home", "title" => "Home", "icon" => "home"],\n                ["id" => "chat", "title" => "Chat", "icon" => "chat"],\n                ["id" => "profile", "title" => "Profile", "icon" => "person"],\n            ])\n            ->selectedItem("home")\n            ->onItemSelected("onNav");\n\n        $content = (new TextView())->id("content")->text("Home")->padding(20)->weight(1);\n        return (new VerticalLayout([$content, $nav]));\n    }\n    function onNav($p) {\n        $id = $p["itemId"] ?? "";\n        $txt = new TextView(); $txt->id("content");\n        return $txt->_setText("Page: $id");\n    }\n}' },
        { name: "appWithDrawer", sig: 'function appWithDrawer(array $config)', desc: "All-in-one app scaffold: TopAppBar + DrawerLayout + NavigationDrawer + optional BottomNavBar.",
          params: [["title","App title"],["drawerItems","Side menu items"],["bottomNavItems","Bottom nav items"],["content","Main content"],["onDrawerItemSelected","Drawer handler"],["onBottomNavSelected","Bottom nav handler"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        return appWithDrawer([\n            "title" => "My App",\n            "drawerItems" => [\n                ["id" => "home", "title" => "Home", "icon" => "home"],\n                ["id" => "settings", "title" => "Settings", "icon" => "settings"],\n            ],\n            "bottomNavItems" => [\n                ["id" => "feed", "title" => "Feed", "icon" => "dynamic_feed"],\n                ["id" => "chat", "title" => "Chat", "icon" => "chat"],\n                ["id" => "me", "title" => "Me", "icon" => "person"],\n            ],\n            "content" => (new TextView())->id("main")->text("Welcome!")->padding(20),\n            "onDrawerItemSelected" => "onDrawer",\n            "onBottomNavSelected" => "onBottomNav",\n        ]);\n    }\n    function onDrawer($p) { return ["action"=>"toast","message"=>"Drawer: ".($p["itemId"]??"")]; }\n    function onBottomNav($p) { return ["action"=>"toast","message"=>"Nav: ".($p["itemId"]??"")]; }\n}' },
    ]
},
{
    section: "ListView & RecyclerList", icon: "📜", desc: "Scrolling lists with item templates and full CRUD",
    items: [
        { name: "ListView", sig: 'class ListView extends Component', desc: "Scrollable list with built-in layouts: simple, two_line, icon, checkbox. Full CRUD with _addItem, _removeItem, _updateItem.",
          params: [["->items()","Data array"],["->itemLayout()","simple, two_line, icon, checkbox"],["->onItemClick()","Click handler"],["_addItem()","Append item"],["_removeItem()","Remove by position"],["_clear()","Clear all"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $list = (new ListView())->id("tasks")\n            ->items([\n                ["text" => "Buy groceries", "secondary" => "Milk, eggs, bread"],\n                ["text" => "Clean house", "secondary" => "Kitchen and bathroom"],\n                ["text" => "Study PHP", "secondary" => "Android plugin development"],\n            ])\n            ->itemLayout("two_line")\n            ->onItemClick("onTask");\n\n        $layout = new VerticalLayout([\n            (new TopAppBar())->title("Tasks"),\n            $list,\n            (new FloatingActionButton())->icon("add")->action("addTask"),\n        ]);\n        return $layout;\n    }\n    function onTask($p) { return ["action"=>"toast","message"=>"Clicked: ".($p["text"]??"")]; }\n    function addTask($p) {\n        $list = new ListView(); $list->id("tasks");\n        return $list->_addItem(["text"=>"New Task","secondary"=>"Just added"]);\n    }\n}' },
        { name: "RecyclerList", sig: 'class RecyclerList extends Component', desc: "Advanced list with custom item templates via a PHP callable.",
          params: [["->items()","Data array"],["->itemTemplate()","fn($item, $index) returning Component"],["_setItems()","Replace all items"],["_addItem()","Append item"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $list = (new RecyclerList())->id("users")\n            ->items([\n                ["name" => "Alice", "role" => "Admin"],\n                ["name" => "Bob", "role" => "User"],\n                ["name" => "Charlie", "role" => "User"],\n            ])\n            ->itemTemplate(function($item, $i) {\n                return new CardView([\n                    (new TextView())->text($item["name"])->textSize(18)->textStyle("bold"),\n                    (new TextView())->text($item["role"])->textColor("#888"),\n                ]);\n            });\n        return (new ScrollView([$list]));\n    }\n}' },
    ]
},
{
    section: "View State & Batch", icon: "🔄", desc: "Read state synchronously, update many views, batch actions",
    items: [
        { name: "readViewState / getViewProperty", sig: 'readViewState() → array\ngetViewProperty(string $viewId, string $property, $default = null)', desc: "Read all view states or a single property from the synchronized state file. Java writes this before each PHP call.",
          params: [["$viewId","Target view"],["$property","text, checked, alpha, etc."],["$default","Fallback value"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new EditText())->id("name")->hint("Type here"),\n            (new Button())->text("Read State")->action("readIt"),\n        ]);\n        return $layout;\n    }\n    function readIt($p) {\n        // Direct synchronous read (no callback needed!)\n        $name = getViewProperty("name", "text", "empty");\n        return ["action" => "toast", "message" => "Name is: $name"];\n    }\n}' },
        { name: "updateView / updateMany / batch", sig: 'updateView(string $id, array $attrs)\nupdateMany(array $updates)\nbatch(...$actions)', desc: "Update one or many views at once. Batch combines multiple action arrays.",
          params: [["$id","View ID"],["$attrs","Properties to update"],["$updates","Array of [id => [attrs]]"],["$actions","Multiple action arrays"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new TextView())->id("a")->text("View A"),\n            (new TextView())->id("b")->text("View B"),\n            (new Button())->text("Update Both")->action("updateBoth"),\n        ]);\n        return $layout;\n    }\n    function updateBoth($p) {\n        return updateMany([\n            "a" => ["text" => "Updated A!", "textColor" => "#4CAF50"],\n            "b" => ["text" => "Updated B!", "textColor" => "#2196F3"],\n        ]);\n    }\n}' },
    ]
},
{
    section: "Dialogs & Snackbar", icon: "💬", desc: "Snackbar, dialog, listDialog, date/time pickers, bottom sheet",
    items: [
        { name: "snackbar / dialog / inputDialog", sig: 'snackbar(string $message, ?string $actionText, ?string $callback)\ndialog(string $title, string $message, string $onYes, ?string $onNo)\ninputDialog(string $title, string $hint, string $callback)', desc: "Material snackbar messages, confirmation dialogs, and input prompts.",
          params: [["$message","Display text"],["$actionText","Snackbar button"],["$onYes/$onNo","Dialog handlers"],["$hint","Input placeholder"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new Button())->text("Snackbar")->action("doSnack"),\n            (new Button())->text("Confirm")->action("doConfirm"),\n            (new Button())->text("Input")->action("doInput"),\n        ]);\n        return $layout;\n    }\n    function doSnack($p) { return snackbar("Hello!", "UNDO", "onUndo"); }\n    function doConfirm($p) { return dialog("Delete?", "Sure?", "onYes", "onNo"); }\n    function doInput($p) { return inputDialog("Name", "Enter name", "gotName"); }\n    function onUndo($p) { return snackbar("Undone!"); }\n    function onYes($p) { return snackbar("Deleted"); }\n    function onNo($p) { return snackbar("Cancelled"); }\n    function gotName($p) { return snackbar("Name: " . ($p["value"]??"")); }\n}' },
        { name: "listDialog / dismissDialog", sig: 'listDialog(string $title, array $items, string $onSelect)\ndismissDialog()', desc: "List selection dialog and dismiss any open dialog or bottom sheet.",
          params: [["$title","Dialog title"],["$items","Array of selectable strings"],["$onSelect","Callback receives {item, index}"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new TextView())->id("pick")->text("None selected")->padding(16),\n            (new Button())->text("Pick Color")->action("showList"),\n        ]);\n        return $layout;\n    }\n    function showList($p) {\n        return listDialog("Pick a Color", ["Red","Green","Blue","Yellow"], "onPick");\n    }\n    function onPick($p) {\n        $txt = new TextView(); $txt->id("pick");\n        return $txt->_setText("Picked: ".($p["item"]??""));\n    }\n}' },
        { name: "datePickerDialog / timePickerDialog / bottomSheet", sig: 'datePickerDialog(string $callback, ?string $initialDate)\ntimePickerDialog(string $callback, bool $is24Hour)\nbottomSheet(Component $content)', desc: "Date/time pickers and custom bottom sheets.",
          params: [["$callback","Receives picked value"],["$initialDate","YYYY-MM-DD"],["$content","Component tree"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new TextView())->id("result")->text("Pick a date or time")->padding(16),\n            (new Button())->text("Date Picker")->action("pickDate"),\n            (new Button())->text("Time Picker")->action("pickTime"),\n            (new Button())->text("Bottom Sheet")->action("showSheet"),\n        ]);\n        return $layout;\n    }\n    function pickDate($p) { return datePickerDialog("onDate"); }\n    function pickTime($p) { return timePickerDialog("onTime", true); }\n    function onDate($p) {\n        $txt = new TextView(); $txt->id("result");\n        return $txt->_setText("Date: " . ($p["date"]??""));\n    }\n    function onTime($p) {\n        $txt = new TextView(); $txt->id("result");\n        return $txt->_setText("Time: " . ($p["hour"]??0) . ":" . ($p["minute"]??0));\n    }\n    function showSheet($p) {\n        return bottomSheet(\n            new VerticalLayout([\n                (new TextView())->text("Bottom Sheet")->textSize(20)->padding(16),\n                (new Button())->text("Close")->action("closeSheet"),\n            ])\n        );\n    }\n    function closeSheet($p) { return dismissDialog(); }\n}' },
    ]
},
{
    section: "Animation", icon: "🎬", desc: "Animate properties, run sequential/parallel animations",
    items: [
        { name: "animate / animateSet", sig: 'animate(string $viewId, array $properties, int $duration, string $interpolator)\nanimateSet(array $animations, bool $sequential = false)', desc: "Animate view properties (alpha, translationX/Y, rotation, scaleX/Y). Run multiple animations together or in sequence.",
          params: [["$viewId","Target view"],["$properties","Animatable props"],["$duration","Time in ms"],["$interpolator","decelerate, accelerate, bounce, overshoot"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new TextView())->id("box")->text("🎯")->textSize(48)->gravity("center"),\n            (new Button())->text("Animate")->action("doAnimate"),\n            (new Button())->text("Multi-Animate")->action("doMulti"),\n        ]);\n        return $layout;\n    }\n    function doAnimate($p) {\n        return animate("box", [\n            "translationX" => 200,\n            "rotation" => 360,\n            "alpha" => 0.5\n        ], 500, "overshoot");\n    }\n    function doMulti($p) {\n        return animateSet([\n            ["viewId" => "box", "properties" => ["scaleX" => 2, "scaleY" => 2], "duration" => 300],\n            ["viewId" => "box", "properties" => ["scaleX" => 1, "scaleY" => 1], "duration" => 300],\n        ], true); // sequential\n    }\n}' },
    ]
},
{
    section: "Native / Sensors", icon: "📡", desc: "All DroidScript native calls: sensors, HTTP, media, files, etc.",
    items: [
        { name: "nativeCall (generic)", sig: 'nativeCall(string $type, string $callback, array $params = [])', desc: "Generic bridge to any DroidScript native call. All sensor/native functions are built on this.",
          params: [["$type","Sensor/call type string"],["$callback","PHP method to receive result"],["$params","Extra parameters"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new TextView())->id("data")->text("---")->padding(16),\n            (new Button())->text("Native Call")->action("doNative"),\n        ]);\n        return $layout;\n    }\n    function doNative($p) {\n        return nativeCall("deviceinfo", "onInfo");\n    }\n    function onInfo($p) {\n        $txt = new TextView(); $txt->id("data");\n        $info = "";\n        foreach ($p as $k => $v) $info .= "$k: $v\\n";\n        return $txt->_setText($info);\n    }\n}' },
        { name: "Sensors: readAccelerometer, readGyroscope, readLight, readProximity, ...", sig: 'readAccelerometer(string $cb)\nreadGyroscope(string $cb)\nreadGravity(string $cb)\nreadOrientation(string $cb)\nreadMagneticField(string $cb)\nreadLight(string $cb)\nreadProximity(string $cb)\nreadPressure(string $cb)\nreadHumidity(string $cb)\nreadTemperature(string $cb)\nreadStepCounter(string $cb)', desc: "Read hardware sensors. Motion sensors return {x,y,z}. Environment sensors return single values.",
          params: [["$cb","Callback receives sensor data"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new TextView())->id("sensor")->text("---")->padding(16)->textSize(16),\n            (new Button())->text("Accelerometer")->action("doAccel"),\n            (new Button())->text("Light")->action("doLight"),\n            (new Button())->text("Proximity")->action("doProx"),\n        ]);\n        return $layout;\n    }\n    function doAccel($p) { return readAccelerometer("onSensor"); }\n    function doLight($p) { return readLight("onSensor"); }\n    function doProx($p) { return readProximity("onSensor"); }\n    function onSensor($p) {\n        $txt = new TextView(); $txt->id("sensor");\n        return $txt->_setText(json_encode($p));\n    }\n}' },
        { name: "readLocation / readBattery / readDeviceInfo / readScreenInfo", sig: 'readLocation(string $cb)\nreadBattery(string $cb)\nreadDeviceInfo(string $cb)\nreadScreenInfo(string $cb)', desc: "Location (lat,lng), battery (level,charging), device model/OS, screen dimensions.",
          params: [["$cb","Callback method"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new TextView())->id("info")->text("---")->padding(16),\n            (new Button())->text("Location")->action("getLoc"),\n            (new Button())->text("Battery")->action("getBat"),\n            (new Button())->text("Device")->action("getDev"),\n        ]);\n        return $layout;\n    }\n    function getLoc($p) { return readLocation("onInfo"); }\n    function getBat($p) { return readBattery("onInfo"); }\n    function getDev($p) { return readDeviceInfo("onInfo"); }\n    function onInfo($p) {\n        $txt = new TextView(); $txt->id("info");\n        $s = "";\n        foreach ($p as $k => $v) $s .= "$k: $v\\n";\n        return $txt->_setText($s);\n    }\n}' },
        { name: "httpRequest / httpGet / httpPost / downloadFile", sig: 'httpRequest(string $url, string $cb, string $method = "GET", ...)\nhttpGet(string $url, string $cb)\nhttpPost(string $url, string $cb, ?string $body)\ndownloadFile(string $url, string $dest, string $cb)', desc: "HTTP requests and file downloads via DroidScript bridge.",
          params: [["$url","Request URL"],["$cb","Response callback"],["$method","GET, POST, PUT, DELETE"],["$body","Request body"],["$dest","Download destination"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new TextView())->id("resp")->text("---")->padding(16)->maxLines(10),\n            (new Button())->text("GET Request")->action("doGet"),\n            (new Button())->text("POST")->action("doPost"),\n        ]);\n        return $layout;\n    }\n    function doGet($p) { return httpGet("https://jsonplaceholder.typicode.com/todos/1", "onResp"); }\n    function doPost($p) { return httpPost("https://httpbin.org/post", "onResp", \'{"hello":"world"}\'); }\n    function onResp($p) {\n        $txt = new TextView(); $txt->id("resp");\n        $r = $p["response"] ?? $p["error"] ?? "none";\n        return $txt->_setText(substr($r, 0, 300));\n    }\n}' },
        { name: "takePhoto / playAudio / recordAudio / textToSpeech / speechRecognition", sig: 'takePhoto(string $cb, int $quality = 80)\nplayAudio(string $file, string $cb)\nrecordAudio(string $file, string $cb)\ntextToSpeech(string $text, string $cb, float $pitch, float $rate)\nspeechRecognition(string $cb)', desc: "Camera, audio playback/recording, TTS, and speech-to-text.",
          params: [["$cb","Result callback"],["$file","Audio file path"],["$text","Text to speak"],["$quality","Photo quality 1-100"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new TextView())->id("status")->text("Ready")->padding(16),\n            (new Button())->text("Take Photo")->action("doPhoto"),\n            (new Button())->text("Speak")->action("doSpeak"),\n            (new Button())->text("Listen")->action("doListen"),\n        ]);\n        return $layout;\n    }\n    function doPhoto($p) { return takePhoto("onPhoto"); }\n    function doSpeak($p) { return textToSpeech("Hello from PHP!"); }\n    function doListen($p) { return speechRecognition("onSpeech"); }\n    function onPhoto($p) {\n        $txt = new TextView(); $txt->id("status");\n        return $txt->_setText("Photo: " . ($p["uri"]??""));\n    }\n    function onSpeech($p) {\n        $txt = new TextView(); $txt->id("status");\n        return $txt->_setText("Heard: " . ($p["text"]??""));\n    }\n}' },
        { name: "File: readNativeFile / writeNativeFile / listNativeFolder / nativeFileExists", sig: 'readNativeFile(string $path, string $cb)\nwriteNativeFile(string $path, string $content, string $cb)\nlistNativeFolder(string $path, string $cb)\nnativeFileExists(string $path, string $cb)', desc: "Read, write, list, and check files on Android device storage via DroidScript.",
          params: [["$path","File or folder path"],["$content","Data to write"],["$cb","Result callback"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new TextView())->id("files")->text("---")->padding(16),\n            (new Button())->text("Write")->action("doWrite"),\n            (new Button())->text("Read")->action("doRead"),\n            (new Button())->text("List /sdcard/")->action("doList"),\n        ]);\n        return $layout;\n    }\n    function doWrite($p) { return writeNativeFile("/sdcard/phptest.txt", "Hello PHP!"); }\n    function doRead($p) { return readNativeFile("/sdcard/phptest.txt", "onFile"); }\n    function doList($p) { return listNativeFolder("/sdcard/", "onFile"); }\n    function onFile($p) {\n        $txt = new TextView(); $txt->id("files");\n        $data = $p["content"] ?? json_encode($p["files"] ?? $p);\n        return $txt->_setText(substr($data, 0, 500));\n    }\n}' },
        { name: "Network: readWifiInfo / scanWifi / readBluetoothInfo / readNetworkInfo", sig: 'readWifiInfo(string $cb)\nscanWifi(string $cb)\nreadBluetoothInfo(string $cb)\ndiscoverBluetooth(string $cb)\nreadNetworkInfo(string $cb)', desc: "WiFi info/scan, Bluetooth info/discovery, and general network status.",
          params: [["$cb","Callback receives: {ssid, ip, connected, rssi} (wifi), {enabled, paired} (bt), {connected, type} (net)"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new TextView())->id("net")->text("---")->padding(16)->textSize(14),\n            (new Button())->text("WiFi Info")->action("doWifi"),\n            (new Button())->text("Bluetooth")->action("doBt"),\n            (new Button())->text("Network")->action("doNet"),\n        ]);\n        return $layout;\n    }\n    function doWifi($p) { return readWifiInfo("onInfo"); }\n    function doBt($p) { return readBluetoothInfo("onInfo"); }\n    function doNet($p) { return readNetworkInfo("onInfo"); }\n    function onInfo($p) {\n        $txt = new TextView(); $txt->id("net");\n        $s = "";\n        foreach ($p as $k => $v) $s .= "$k: " . (is_array($v) ? json_encode($v) : $v) . "\\n";\n        return $txt->_setText($s);\n    }\n}' },
        { name: "Audio: stopAudio / stopRecording / playRingtone / getVolume / setVolume", sig: 'stopAudio(string $cb)\nstopRecording(string $cb)\nplayRingtone(string $type)\ngetVolume(string $cb, string $stream)\nsetVolume(int $level, string $stream)', desc: "Stop audio/recording, play system ringtones, get/set volume. Streams: music, alarm, ring, notification.",
          params: [["$cb","Status callback"],["$type","notification, alarm, ringtone"],["$level","Volume 0-15"],["$stream","Audio stream name"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new TextView())->id("vol")->text("---")->padding(16),\n            (new Button())->text("Get Volume")->action("getVol"),\n            (new Button())->text("Vol Up")->action("volUp"),\n            (new Button())->text("Play Ringtone")->action("doRing"),\n        ]);\n        return $layout;\n    }\n    function getVol($p) { return getVolume("onVol"); }\n    function onVol($p) {\n        $txt = new TextView(); $txt->id("vol");\n        return $txt->_setText("Volume: ".($p["volume"]??0)."/".($p["max"]??15));\n    }\n    function volUp($p) { return setVolume(12, "music"); }\n    function doRing($p) { return playRingtone("notification"); }\n}' },
        { name: "Screen & Hardware: setRingerMode / setScreenBrightness / preventScreenLock / vibrate / flashlight", sig: 'setRingerMode(string $mode)\nsetScreenBrightness(float $level)\npreventScreenLock(bool $prevent)\nvibrate(string $pattern)\nflashlight(bool $on)', desc: "Ringer mode (normal/vibrate/silent), screen brightness (0.0-1.0), keep screen on, vibration patterns, flashlight toggle.",
          params: [["$mode","normal, vibrate, silent"],["$level","0.0 (dim) to 1.0 (bright)"],["$pattern","ms: on,off,on"],["$on","Flashlight on/off"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new Button())->text("Vibrate")->action("doVib"),\n            (new Button())->text("Flash ON")->action("flashOn"),\n            (new Button())->text("Silent Mode")->action("doSilent"),\n            (new Button())->text("Max Brightness")->action("doBright"),\n            (new Button())->text("Keep Screen On")->action("keepOn"),\n        ]);\n        return $layout;\n    }\n    function doVib($p) { return vibrate("200,100,200"); }\n    function flashOn($p) { return flashlight(true); }\n    function doSilent($p) { return setRingerMode("silent"); }\n    function doBright($p) { return setScreenBrightness(1.0); }\n    function keepOn($p) { return preventScreenLock(true); }\n}' },
        { name: "Clipboard: setClipboard / getClipboard / copyToClipboard / share / openUrl", sig: 'setClipboard(string $text)\ngetClipboard(string $cb)\ncopyToClipboard(string $text, string $label)\nshare(string $text, string $title)\nopenUrl(string $url)', desc: "Clipboard operations, share sheet, and URL opening via DroidScript bridge.",
          params: [["$text","Text to copy/share"],["$cb","Callback receives {text}"],["$label","Toast message"],["$title","Share dialog title"],["$url","URL to open"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new Button())->text("Copy")->action("doCopy"),\n            (new Button())->text("Get Clipboard")->action("doGet"),\n            (new Button())->text("Share")->action("doShare"),\n            (new Button())->text("Open URL")->action("doOpen"),\n        ]);\n        return $layout;\n    }\n    function doCopy($p) { return copyToClipboard("Hello from PHP!", "Copied!"); }\n    function doGet($p) { return getClipboard("onClip"); }\n    function onClip($p) { return ["action"=>"toast","message"=>"Clipboard: ".($p["text"]??"")]; }\n    function doShare($p) { return share("Check out PHP on Android!", "Share via"); }\n    function doOpen($p) { return openUrl("https://php.net"); }\n}' },
        { name: "Communication: phoneCall / sendSms / sendEmail / showNotification / scanQrCode", sig: 'phoneCall(string $number)\nsendSms(string $phone, string $msg, string $cb)\nsendEmail(string $to, string $subject, string $body, string $cb)\nshowNotification(string $title, string $body, string $cb)\nscanQrCode(string $cb)', desc: "Phone calls, SMS, email, notifications, and QR code scanning.",
          params: [["$number","Phone number"],["$msg","SMS body"],["$to/$subject/$body","Email fields"],["$cb","Result callback"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new Button())->text("Send SMS")->action("doSms"),\n            (new Button())->text("Send Email")->action("doEmail"),\n            (new Button())->text("Notify")->action("doNotify"),\n            (new Button())->text("Scan QR")->action("doScan"),\n        ]);\n        return $layout;\n    }\n    function doSms($p) { return sendSms("+1234567890", "Hello!", "onSent"); }\n    function doEmail($p) { return sendEmail("test@example.com", "Hello", "Message body"); }\n    function doNotify($p) { return showNotification("PHP", "Hello from PHP!"); }\n    function doScan($p) { return scanQrCode("onScan"); }\n    function onSent($p) { return ["action"=>"toast","message"=>"SMS: ".json_encode($p)]; }\n    function onScan($p) { return ["action"=>"toast","message"=>"Scanned: ".($p["code"]??"")]; }\n}' },
        { name: "Encryption: encryptText / decryptText / hashText", sig: 'encryptText(string $text, string $pass, string $cb)\ndecryptText(string $text, string $pass, string $cb)\nhashText(string $text, string $algo, string $cb)', desc: "AES encryption/decryption and hashing (MD5, SHA1, SHA256, SHA512).",
          params: [["$text","Data to encrypt/hash"],["$pass","Encryption password"],["$algo","MD5, SHA1, SHA256, SHA512"],["$cb","Receives {result}"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new TextView())->id("out")->text("---")->padding(16),\n            (new Button())->text("Encrypt")->action("doEnc"),\n            (new Button())->text("Hash SHA256")->action("doHash"),\n        ]);\n        return $layout;\n    }\n    function doEnc($p) { return encryptText("secret", "pass123", "onResult"); }\n    function doHash($p) { return hashText("hello", "SHA256", "onResult"); }\n    function onResult($p) {\n        $txt = new TextView(); $txt->id("out");\n        return $txt->_setText(substr($p["result"]??"", 0, 60));\n    }\n}' },
        { name: "Apps: openApp / sendIntent", sig: 'openApp(string $packageName, string $cb)\nsendIntent(string $action, string $cb, ?string $type, ?string $uri)', desc: "Launch another app or send custom Android intents.",
          params: [["$packageName","App package (e.g. com.whatsapp)"],["$action","Intent action"],["$uri","Intent data URI"],["$type","MIME type"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $layout = new VerticalLayout([\n            (new Button())->text("Open Calculator")->action("doCalc"),\n            (new Button())->text("Open URL")->action("doUrl"),\n        ]);\n        return $layout;\n    }\n    function doCalc($p) { return openApp("com.android.calculator2"); }\n    function doUrl($p) { return sendIntent("android.intent.action.VIEW", "onResult", null, "https://php.net"); }\n    function onResult($p) { return ["action"=>"toast","message"=>json_encode($p)]; }\n}' },
        { name: "View Manipulation: scrollTo / removeView / addView / replaceChildren", sig: 'scrollTo(string $scrollViewId, mixed $target, bool $smooth)\nremoveView(string $viewId)\naddView(string $parentId, Component $child, int $index)\nreplaceChildren(string $parentId, array $children)', desc: "Scroll to position/view, remove views, add children dynamically, replace container content.",
          params: [["$scrollViewId","ScrollView ID"],["$target","View ID or pixel position"],["$viewId","View to remove"],["$parentId","Container ID"],["$child","Component to add"],["$index","-1 = append"]],
          example: '<?php\nrequire_once dirname(__FILE__)."/"."ui_core.php";\n\nclass MyApp {\n    function index() {\n        $list = new VerticalLayout([\n            (new TextView())->id("item1")->text("Item 1")->padding(12),\n            (new TextView())->id("item2")->text("Item 2")->padding(12),\n        ]);\n        $list->id("container");\n        $layout = new VerticalLayout([\n            $list,\n            (new Button())->text("Add Item")->action("doAdd"),\n            (new Button())->text("Remove Item 1")->action("doRemove"),\n            (new Button())->text("Replace All")->action("doReplace"),\n        ]);\n        return (new ScrollView([$layout]));\n    }\n    function doAdd($p) { return addView("container", (new TextView())->text("New!")->padding(12)->textColor("#4CAF50")); }\n    function doRemove($p) { return removeView("item1"); }\n    function doReplace($p) {\n        return replaceChildren("container", [\n            (new TextView())->text("Replaced A")->padding(12),\n            (new TextView())->text("Replaced B")->padding(12),\n        ]);\n    }\n}' },
    ]
},
];

// ============================================================================
// UI BUILDING — App Layout
// ============================================================================

function OnStart() {
    app.SetOrientation("Portrait");
    app.SetStatusBarColor(C.bg);

    // Load saved settings
    try {
        var savedKey = app.LoadText("AI_API_KEY", "");
        var savedModel = app.LoadText("AI_MODEL", "openai");
        if (savedKey) AI_API_KEY = savedKey;
        if (savedModel) AI_MODEL = savedModel;
    } catch(e) {}

    // Load PHP source files for AI context
    try {
        phpSimpleContent = app.ReadFile("simple.php") || "";
        phpUiCoreContent = app.ReadFile("ui_core.php") || "";
        console.log("Loaded PHP files: simple.php=" + phpSimpleContent.length + " chars, ui_core.php=" + phpUiCoreContent.length + " chars");
    } catch(e) {
        console.log("Failed to load PHP files: " + e);
    }

    // Root layout
    layMain = app.CreateLayout("Linear", "VCenter,FillXY");
    layMain.SetBackColor(C.bg);

    // Header
    var layHeader = app.CreateLayout("Linear", "Horizontal,FillX,VCenter");
    layHeader.SetBackColor(C.bgHeader);
    layHeader.SetPadding(0.04, 0.015, 0.04, 0.015);

    txtTitle = app.CreateText("PHP Docs", -1, -1, "Bold");
    txtTitle.SetTextSize(20);
    txtTitle.SetTextColor(C.accent);
    layHeader.AddChild(txtTitle);

    var btnSearch = app.CreateButton("[fa-search]", -1, -1, "FontAwesome,Custom");
    btnSearch.SetTextSize(18);
    btnSearch.SetTextColor(C.textMuted);
    btnSearch.SetOnTouch(ToggleSearch);
    layHeader.AddChild(btnSearch);

    layMain.AddChild(layHeader);

    // Search bar (hidden initially)
    laySearch = app.CreateLayout("Linear", "Horizontal,FillX,VCenter");
    laySearch.SetBackColor(C.bgCard);
    laySearch.SetPadding(0.04, 0.01, 0.04, 0.01);
    laySearch.SetVisibility("Gone");

    txtSearch = app.CreateTextEdit("", 0.78, -1, "SingleLine");
    txtSearch.SetHint("Search functions...");
    txtSearch.SetTextColor(C.text);
    txtSearch.SetTextSize(15);
    txtSearch.SetOnChange(OnSearch);
    laySearch.AddChild(txtSearch);

    var btnClearSearch = app.CreateButton("✕", 0.1, -1, "Custom");
    btnClearSearch.SetTextColor(C.textMuted);
    btnClearSearch.SetOnTouch(ClearSearch);
    laySearch.AddChild(btnClearSearch);

    layMain.AddChild(laySearch);

    // Tabs (two rows: code + docs)
    layTabs = app.CreateLayout("Linear", "FillX");
    layTabs.SetBackColor(C.bgCard);
    CreateTabs();
    layMain.AddChild(layTabs);

    // Content scroller
    scrollContent = app.CreateScroller(1, -1);
    scrollContent.SetBackColor(C.bg);

    layContent = app.CreateLayout("Linear", "Left,FillXY");
    layContent.SetPadding(0, 0.01, 0, 0.02);
    scrollContent.AddChild(layContent);
    layMain.AddChild(scrollContent);

    // Detail overlay (hidden)
    scrollDetail = app.CreateScroller(1, -1);
    scrollDetail.SetBackColor(C.bg);
    scrollDetail.SetVisibility("Gone");

    layDetail = app.CreateLayout("Linear", "Left,FillXY");
    layDetail.SetPadding(0.04, 0.01, 0.04, 0.04);
    scrollDetail.AddChild(layDetail);
    layMain.AddChild(scrollDetail);

    // AI Chat overlay (hidden)
    layChat = app.CreateLayout("Linear", "FillXY,Top");
    layChat.SetBackColor(C.bg);
    layChat.SetVisibility("Gone");
    BuildChatUI();
    layMain.AddChild(layChat);

    app.AddLayout(layMain);

    ShowSections(simpleData);
}

// ============================================================================
// TAB BAR
// ============================================================================
var tabSimple, tabUicore, tabSimpleDoc, tabUicoreDoc, tabAi, tabAndroidSdk;
var webDocSimple, webDocUicore, webAndroidSdk; // WebViews for HTML docs

function CreateTabs() {
    // Row 1 — code data tabs
    var topRow = app.CreateLayout("Linear", "Horizontal,FillX");
    topRow.SetBackColor(C.bgCard);

    tabSimple = app.CreateButton("simple.php", 0.33, 0.045, "Custom");
    tabSimple.SetTextSize(11);
    tabSimple.SetStyle(C.accent, C.accent, 0);
    tabSimple.SetTextColor("#000");
    tabSimple.SetOnTouch(function() { SwitchTab("simple"); });
    topRow.AddChild(tabSimple);

    tabUicore = app.CreateButton("ui_core.php", 0.33, 0.045, "Custom");
    tabUicore.SetTextSize(11);
    tabUicore.SetStyle(C.bgCode, C.bgCode, 0);
    tabUicore.SetTextColor(C.textMuted);
    tabUicore.SetOnTouch(function() { SwitchTab("uicore"); });
    topRow.AddChild(tabUicore);

    tabAi = app.CreateButton("\ud83e\udd16 AI Chat", 0.34, 0.045, "Custom");
    tabAi.SetTextSize(11);
    tabAi.SetStyle(C.bgCode, C.bgCode, 0);
    tabAi.SetTextColor(C.textMuted);
    tabAi.SetOnTouch(function() { SwitchTab("ai"); });
    topRow.AddChild(tabAi);

    layTabs.AddChild(topRow);

    // Row 2 — HTML doc tabs
    var botRow = app.CreateLayout("Linear", "Horizontal,FillX");
    botRow.SetBackColor(C.bgHeader);

    tabSimpleDoc = app.CreateButton("\ud83d\udcd6 simple Docs", 0.33, 0.04, "Custom");
    tabSimpleDoc.SetTextSize(10);
    tabSimpleDoc.SetStyle(C.bgCode, C.bgCode, 0);
    tabSimpleDoc.SetTextColor(C.textMuted);
    tabSimpleDoc.SetOnTouch(function() { SwitchTab("simpleDoc"); });
    botRow.AddChild(tabSimpleDoc);

    tabUicoreDoc = app.CreateButton("\ud83d\udcd6 ui_core Docs", 0.33, 0.04, "Custom");
    tabUicoreDoc.SetTextSize(10);
    tabUicoreDoc.SetStyle(C.bgCode, C.bgCode, 0);
    tabUicoreDoc.SetTextColor(C.textMuted);
    tabUicoreDoc.SetOnTouch(function() { SwitchTab("uicoreDoc"); });
    botRow.AddChild(tabUicoreDoc);

    tabAndroidSdk = app.CreateButton("\ud83d\udcf1 Android SDK", 0.34, 0.04, "Custom");
    tabAndroidSdk.SetTextSize(10);
    tabAndroidSdk.SetStyle(C.bgCode, C.bgCode, 0);
    tabAndroidSdk.SetTextColor(C.textMuted);
    tabAndroidSdk.SetOnTouch(function() { SwitchTab("androidSdk"); });
    botRow.AddChild(tabAndroidSdk);

    layTabs.AddChild(botRow);
}

function SwitchTab(tab) {
    currentTab = tab;
    currentSection = null;
    searchQuery = "";
    HideDetail();

    // Reset all tabs to inactive
    tabSimple.SetStyle(C.bgCode, C.bgCode, 0); tabSimple.SetTextColor(C.textMuted);
    tabUicore.SetStyle(C.bgCode, C.bgCode, 0); tabUicore.SetTextColor(C.textMuted);
    tabAi.SetStyle(C.bgCode, C.bgCode, 0); tabAi.SetTextColor(C.textMuted);
    tabSimpleDoc.SetStyle(C.bgCode, C.bgCode, 0); tabSimpleDoc.SetTextColor(C.textMuted);
    tabUicoreDoc.SetStyle(C.bgCode, C.bgCode, 0); tabUicoreDoc.SetTextColor(C.textMuted);
    tabAndroidSdk.SetStyle(C.bgCode, C.bgCode, 0); tabAndroidSdk.SetTextColor(C.textMuted);

    // Hide all major panels
    scrollContent.SetVisibility("Gone");
    scrollDetail.SetVisibility("Gone");
    layChat.SetVisibility("Gone");
    if (webDocSimple) webDocSimple.SetVisibility("Gone");
    if (webDocUicore) webDocUicore.SetVisibility("Gone");
    if (webAndroidSdk) webAndroidSdk.SetVisibility("Gone");

    if (tab === "simple") {
        tabSimple.SetStyle(C.accent, C.accent, 0); tabSimple.SetTextColor("#000");
        scrollContent.SetVisibility("Show");
        ShowSections(simpleData);
    } else if (tab === "uicore") {
        tabUicore.SetStyle(C.accent, C.accent, 0); tabUicore.SetTextColor("#000");
        scrollContent.SetVisibility("Show");
        ShowSections(uicoreData);
    } else if (tab === "simpleDoc") {
        tabSimpleDoc.SetStyle(C.accent2, C.accent2, 0); tabSimpleDoc.SetTextColor("#000");
        ShowDocWebView("simple");
    } else if (tab === "uicoreDoc") {
        tabUicoreDoc.SetStyle(C.accent2, C.accent2, 0); tabUicoreDoc.SetTextColor("#000");
        ShowDocWebView("uicore");
    } else if (tab === "ai") {
        tabAi.SetStyle(C.accentPink, C.accentPink, 0); tabAi.SetTextColor("#000");
        layChat.SetVisibility("Show");
        LoadAiContextIfNeeded();
    } else if (tab === "androidSdk") {
        tabAndroidSdk.SetStyle(C.orange, C.orange, 0); tabAndroidSdk.SetTextColor("#000");
        ShowAndroidSdkWebView();
    }
}

// ============================================================================
// HTML DOCS WEBVIEW
// ============================================================================

function ShowDocWebView(which) {
    if (which === "simple") {
        if (!webDocSimple) {
            webDocSimple = app.CreateWebView(1, 1, "NoActionBar,IgnoreErrors,ScrollFade");
            webDocSimple.SetBackColor(C.bg);
            var docPath = _resolveDocPath("simple.html");
            webDocSimple.LoadUrl("file://" + docPath);
            layMain.AddChild(webDocSimple);
        }
        webDocSimple.SetVisibility("Show");
    } else {
        if (!webDocUicore) {
            webDocUicore = app.CreateWebView(1, 1, "NoActionBar,IgnoreErrors,ScrollFade");
            webDocUicore.SetBackColor(C.bg);
            var docPath2 = _resolveDocPath("ui_core.html");
            webDocUicore.LoadUrl("file://" + docPath2);
            layMain.AddChild(webDocUicore);
        }
        webDocUicore.SetVisibility("Show");
    }
}

function _resolveDocPath(fileName) {
    // Try multiple known paths (plugin assets, app docs folder, flat)
    var candidates = [
        app.GetAppPath() + "/assets/docs/" + fileName,
        app.GetAppPath() + "/docs/" + fileName,
        "/sdcard/DroidScript/" + app.GetAppName() + "/docs/" + fileName,
        "/sdcard/DroidScript/" + app.GetAppName() + "/assets/docs/" + fileName,
        app.GetAppPath() + "/" + fileName
    ];
    for (var i = 0; i < candidates.length; i++) {
        try { if (app.FileExists(candidates[i])) return candidates[i]; } catch(e) {}
    }
    // Fallback — return first candidate and hope for the best
    return candidates[0];
}

// ============================================================================
// ANDROID SDK WEBVIEW
// ============================================================================

function ShowAndroidSdkWebView() {
    if (!webAndroidSdk) {
        webAndroidSdk = app.CreateWebView(1, 1, "NoActionBar,IgnoreErrors,ScrollFade,AllowZoom");
        webAndroidSdk.SetBackColor(C.bg);
        // Load Android developer widget reference docs
        webAndroidSdk.LoadUrl("https://developer.android.com/reference/android/widget/package-summary");
        layMain.AddChild(webAndroidSdk);
    }
    webAndroidSdk.SetVisibility("Show");
}

// ============================================================================
// SEARCH
// ============================================================================
function ToggleSearch() {
    if (laySearch.GetVisibility() === "Gone") {
        laySearch.SetVisibility("Show");
        txtSearch.Focus();
    } else {
        ClearSearch();
    }
}

function ClearSearch() {
    txtSearch.SetText("");
    searchQuery = "";
    laySearch.SetVisibility("Gone");
    var data = currentTab === "simple" ? simpleData : uicoreData;
    if (currentSection !== null) {
        ShowSectionItems(data[currentSection]);
    } else {
        ShowSections(data);
    }
}

function OnSearch() {
    searchQuery = txtSearch.GetText().toLowerCase().trim();
    if (!searchQuery) {
        var data = currentTab === "simple" ? simpleData : uicoreData;
        ShowSections(data);
        return;
    }
    ShowSearchResults();
}

function ShowSearchResults() {
    layContent = _destroyAllChildren(layContent, scrollContent, "Left,FillXY", [0, 0.01, 0, 0.02]);
    var data = currentTab === "simple" ? simpleData : uicoreData;
    var results = [];

    for (var s = 0; s < data.length; s++) {
        var sec = data[s];
        for (var i = 0; i < sec.items.length; i++) {
            var item = sec.items[i];
            if (item.name.toLowerCase().indexOf(searchQuery) >= 0 ||
                item.desc.toLowerCase().indexOf(searchQuery) >= 0 ||
                item.sig.toLowerCase().indexOf(searchQuery) >= 0) {
                results.push({ sectionIdx: s, itemIdx: i, item: item, section: sec.section });
            }
        }
    }

    if (results.length === 0) {
        var noRes = app.CreateText("No results for '" + searchQuery + "'", 0.9);
        noRes.SetTextColor(C.textMuted);
        noRes.SetPadding(0, 0.05, 0, 0);
        noRes.SetTextSize(16);
        layContent.AddChild(noRes);
        return;
    }

    var header = app.CreateText(results.length + " result(s)", 0.92, -1, "Left");
    header.SetTextColor(C.textMuted);
    header.SetTextSize(12);
    header.SetPadding(0.04, 0.01, 0.04, 0.01);
    layContent.AddChild(header);

    for (var r = 0; r < results.length; r++) {
        CreateItemRow(results[r].item, results[r].sectionIdx, results[r].itemIdx, results[r].section);
    }
}

// ============================================================================
// SECTION LIST
// ============================================================================
function ShowSections(data) {
    layContent = _destroyAllChildren(layContent, scrollContent, "Left,FillXY", [0, 0.01, 0, 0.02]);
    scrollContent.ScrollTo(0, 0);
    currentSection = null;

    var fileLabel = currentTab === "simple" ? "simple.php" : "ui_core.php";
    var countLabel = currentTab === "simple" ? "225 functions" : "38 classes + 55 functions";

    var infoLay = app.CreateLayout("Linear", "Left,FillX");
    infoLay.SetPadding(0.04, 0.02, 0.04, 0.01);

    var fileText = app.CreateText(fileLabel, -1, -1, "Bold");
    fileText.SetTextSize(22);
    fileText.SetTextColor(C.accent);
    infoLay.AddChild(fileText);

    var countText = app.CreateText(countLabel + " · " + data.length + " categories", 0.9);
    countText.SetTextSize(13);
    countText.SetTextColor(C.textMuted);
    countText.SetMargins(0, 0.005, 0, 0.015);
    infoLay.AddChild(countText);

    layContent.AddChild(infoLay);

    for (var s = 0; s < data.length; s++) {
        CreateSectionCard(data[s], s);
    }
}

function CreateSectionCard(sec, index) {
    var lay = app.CreateLayout("Linear", "Left,FillX");
    lay.SetPadding(0.04, 0.015, 0.04, 0.015);
    lay.SetMargins(0.03, 0.005, 0.03, 0.005);
    lay.SetBackColor(C.bgCard);
    lay.SetCornerRadius(12);
    lay.SetElevation(2);

    var row = app.CreateLayout("Linear", "Horizontal,FillX,VCenter");
    row.SetTouchThrough(true);

    var icon = app.CreateText(sec.icon, -1, -1);
    icon.SetTextSize(24);
    icon.SetMargins(0, 0, 0.03, 0);
    icon.SetTouchable(false);
    row.AddChild(icon);

    var textLay = app.CreateLayout("Linear", "Left");
    textLay.SetTouchThrough(true);

    var title = app.CreateText(sec.section, -1, -1, "Bold");
    title.SetTextSize(16);
    title.SetTextColor(C.text);
    title.SetTouchable(false);
    textLay.AddChild(title);

    var desc = app.CreateText(sec.desc + " · " + sec.items.length + " items", -1, -1);
    desc.SetTextSize(12);
    desc.SetTextColor(C.textMuted);
    desc.SetTouchable(false);
    textLay.AddChild(desc);

    row.AddChild(textLay);
    lay.AddChild(row);

    lay.SetOnTouchUp(_makeSectionTap(index));

    layContent.AddChild(lay);
}

// ============================================================================
// SECTION ITEMS LIST
// ============================================================================
function ShowSectionItems(section) {
    layContent = _destroyAllChildren(layContent, scrollContent, "Left,FillXY", [0, 0.01, 0, 0.02]);
    scrollContent.ScrollTo(0, 0);

    // Back button
    var backRow = app.CreateLayout("Linear", "Horizontal,Left,VCenter,FillX");
    backRow.SetPadding(0.04, 0.015, 0.04, 0.01);

    var btnBack = app.CreateButton("◀ Back", -1, 0.045, "Custom");
    btnBack.SetTextSize(14);
    btnBack.SetTextColor(C.accent);
    btnBack.SetOnTouch(function() {
        currentSection = null;
        var data = currentTab === "simple" ? simpleData : uicoreData;
        ShowSections(data);
    });
    backRow.AddChild(btnBack);
    layContent.AddChild(backRow);

    // Section header
    var headLay = app.CreateLayout("Linear", "Left,FillX");
    headLay.SetPadding(0.04, 0.005, 0.04, 0.015);

    var headTitle = app.CreateText(section.icon + " " + section.section, -1, -1, "Bold");
    headTitle.SetTextSize(20);
    headTitle.SetTextColor(C.accent);
    headLay.AddChild(headTitle);

    var headDesc = app.CreateText(section.desc, 0.9);
    headDesc.SetTextSize(13);
    headDesc.SetTextColor(C.textMuted);
    headLay.AddChild(headDesc);

    layContent.AddChild(headLay);

    // Items
    for (var i = 0; i < section.items.length; i++) {
        CreateItemRow(section.items[i], currentSection, i, null);
    }
}

function CreateItemRow(item, sectionIdx, itemIdx, sectionLabel) {
    var lay = app.CreateLayout("Linear", "Left,FillX");
    lay.SetPadding(0.04, 0.015, 0.04, 0.015);
    lay.SetMargins(0.03, 0.004, 0.03, 0.004);
    lay.SetBackColor(C.bgCard);
    lay.SetCornerRadius(8);

    var nameText = app.CreateText(item.name, -1, -1, "Bold,Left");
    nameText.SetTextSize(15);
    nameText.SetTextColor(C.accentWarm);
    nameText.SetTouchable(false);
    lay.AddChild(nameText);

    var descText = app.CreateText(item.desc.substring(0, 80) + (item.desc.length > 80 ? "..." : ""), 0.84, -1, "Left");
    descText.SetTextSize(12);
    descText.SetTextColor(C.textMuted);
    descText.SetMargins(0, 0.003, 0, 0);
    descText.SetTouchable(false);
    lay.AddChild(descText);

    if (sectionLabel) {
        var secTag = app.CreateText(sectionLabel, -1, -1, "Left");
        secTag.SetTextSize(10);
        secTag.SetTextColor(C.purple);
        secTag.SetMargins(0, 0.003, 0, 0);
        secTag.SetTouchable(false);
        lay.AddChild(secTag);
    }

    lay.SetOnTouchUp(_makeItemTap(sectionIdx, itemIdx));

    layContent.AddChild(lay);
}

// ============================================================================
// DETAIL VIEW
// ============================================================================
function ShowDetail(item, sectionName) {
    currentItem = item;
    layDetail = _destroyAllChildren(layDetail, scrollDetail, "Left,FillXY", [0.04, 0.01, 0.04, 0.04]);
    scrollDetail.ScrollTo(0, 0);

    // Back button
    var btnBack = app.CreateButton("◀ Back", -1, 0.045, "Custom");
    btnBack.SetTextSize(14);
    btnBack.SetTextColor(C.accent);
    btnBack.SetOnTouch(HideDetail);
    layDetail.AddChild(btnBack);

    // Section tag
    var secTag = app.CreateText(sectionName, -1, -1, "Left");
    secTag.SetTextSize(11);
    secTag.SetTextColor(C.purple);
    secTag.SetMargins(0, 0.01, 0, 0.005);
    layDetail.AddChild(secTag);

    // Name
    var nameText = app.CreateText(item.name, -1, -1, "Bold,Left");
    nameText.SetTextSize(22);
    nameText.SetTextColor(C.accentWarm);
    layDetail.AddChild(nameText);

    // Description
    var descText = app.CreateText(item.desc, 0.92, -1, "Left,MultiLine");
    descText.SetTextSize(14);
    descText.SetTextColor(C.text);
    descText.SetMargins(0, 0.01, 0, 0.015);
    layDetail.AddChild(descText);

    // Signature
    var sigHeader = app.CreateText("SIGNATURE", -1, -1, "Bold,Left");
    sigHeader.SetTextSize(11);
    sigHeader.SetTextColor(C.textMuted);
    layDetail.AddChild(sigHeader);

    var sigLay = app.CreateLayout("Linear", "Left,FillX");
    sigLay.SetBackColor(C.bgCode);
    sigLay.SetCornerRadius(8);
    sigLay.SetPadding(0.03, 0.015, 0.03, 0.015);
    sigLay.SetMargins(0, 0.005, 0, 0.015);

    var sigText = app.CreateText(item.sig, 0.84, -1, "Left,Monospace,MultiLine");
    sigText.SetTextSize(12);
    sigText.SetTextColor(C.cyan);
    sigLay.AddChild(sigText);
    layDetail.AddChild(sigLay);

    // Parameters
    if (item.params && item.params.length > 0) {
        var paramHeader = app.CreateText("PARAMETERS", -1, -1, "Bold,Left");
        paramHeader.SetTextSize(11);
        paramHeader.SetTextColor(C.textMuted);
        layDetail.AddChild(paramHeader);

        for (var p = 0; p < item.params.length; p++) {
            var paramLay = app.CreateLayout("Linear", "Horizontal,Left,FillX");
            paramLay.SetPadding(0.02, 0.008, 0.02, 0.008);
            paramLay.SetMargins(0, 0.003, 0, 0);
            paramLay.SetBackColor(C.bgCard);
            if (p === 0) paramLay.SetCornerRadius(8);
            if (p === item.params.length - 1) paramLay.SetCornerRadius(8);

            var paramName = app.CreateText(item.params[p][0], 0.28, -1, "Left,Bold,Monospace");
            paramName.SetTextSize(12);
            paramName.SetTextColor(C.orange);
            paramLay.AddChild(paramName);

            var paramDesc = app.CreateText(item.params[p][1], 0.6, -1, "Left,MultiLine");
            paramDesc.SetTextSize(12);
            paramDesc.SetTextColor(C.textMuted);
            paramLay.AddChild(paramDesc);

            layDetail.AddChild(paramLay);
        }

        var spacer = app.CreateText("", -1, 0.015);
        layDetail.AddChild(spacer);
    }

    // Example Code
    if (item.example) {
        var exHeader = app.CreateText("EXAMPLE", -1, -1, "Bold,Left");
        exHeader.SetTextSize(11);
        exHeader.SetTextColor(C.textMuted);
        layDetail.AddChild(exHeader);

        // Button row
        var btnRow = app.CreateLayout("Linear", "Horizontal,Left,FillX");
        btnRow.SetMargins(0, 0.008, 0, 0.005);

        var btnCopy = app.CreateButton("📋 Copy", -1, 0.045, "Custom");
        btnCopy.SetTextSize(13);
        btnCopy.SetTextColor(C.text);
        btnCopy.SetOnTouch(_makeCopy(item.example));
        btnRow.AddChild(btnCopy);

        var btnRun = app.CreateButton("▶ Run", -1, 0.045, "Custom");
        btnRun.SetTextSize(13);
        btnRun.SetTextColor(C.success);
        btnRun.SetOnTouch(_makeRun(item.example, item.name));
        btnRow.AddChild(btnRun);

        var btnSave = app.CreateButton("💾 Save", -1, 0.045, "Custom");
        btnSave.SetTextSize(13);
        btnSave.SetTextColor(C.accentWarm);
        btnSave.SetOnTouch(_makeSave(item.example, item.name));
        btnRow.AddChild(btnSave);

        layDetail.AddChild(btnRow);

        // Code block — CodeMirror WebView with line numbers and syntax highlighting
        var lineCount = item.example.split("\n").length;
        var codeHeight = Math.min(Math.max(lineCount * 0.026 + 0.06, 0.22), 0.55);
        var codeWeb = app.CreateWebView(0.92, codeHeight, "NoScrollBars");
        codeWeb.SetBackColor(C.bgCode);
        codeWeb.SetMargins(0, 0.005, 0, 0);
        var escapedCode = item.example
            .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;").replace(/'/g, "&#039;");
        var cmHtml = '<!DOCTYPE html><html><head>'
            + '<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">'
            + '<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.16/codemirror.min.css">'
            + '<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.16/theme/material-darker.min.css">'
            + '<style>'
            + 'body{margin:0;padding:0;background:#1e2130;overflow:hidden}'
            + '.CodeMirror{height:auto;background:#1e2130;color:#c9d1d9;font-size:12px;font-family:"Fira Code",monospace;line-height:1.45;border:none}'
            + '.CodeMirror-gutters{background:#181a27;border-right:1px solid #2a2d3a}'
            + '.CodeMirror-linenumber{color:#555;font-size:11px;padding:0 6px 0 4px}'
            + '.CodeMirror-lines{padding:8px 0}'
            + '.CodeMirror-cursor{border-left-color:#4fc3f7}'
            + '.CodeMirror-selected{background:rgba(79,195,247,0.15)}'
            + '.cm-s-material-darker .cm-keyword{color:#c586c0}'
            + '.cm-s-material-darker .cm-variable{color:#9cdcfe}'
            + '.cm-s-material-darker .cm-variable-2{color:#9cdcfe}'
            + '.cm-s-material-darker .cm-string{color:#ce9178}'
            + '.cm-s-material-darker .cm-string-2{color:#ce9178}'
            + '.cm-s-material-darker .cm-number{color:#b5cea8}'
            + '.cm-s-material-darker .cm-comment{color:#6a9955}'
            + '.cm-s-material-darker .cm-atom{color:#569cd6}'
            + '.cm-s-material-darker .cm-def{color:#dcdcaa}'
            + '.cm-s-material-darker .cm-tag{color:#569cd6}'
            + '.cm-s-material-darker .cm-attribute{color:#9cdcfe}'
            + '.cm-s-material-darker .cm-meta{color:#4ec9b0}'
            + '.cm-s-material-darker .cm-operator{color:#d4d4d4}'
            + '.cm-s-material-darker .cm-property{color:#9cdcfe}'
            + '.cm-s-material-darker .cm-builtin{color:#4ec9b0}'
            + '</style>'
            + '<script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.16/codemirror.min.js"><\/script>'
            + '<script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.16/mode/php/php.min.js"><\/script>'
            + '<script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.16/mode/htmlmixed/htmlmixed.min.js"><\/script>'
            + '<script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.16/mode/xml/xml.min.js"><\/script>'
            + '<script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.16/mode/javascript/javascript.min.js"><\/script>'
            + '<script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.16/mode/css/css.min.js"><\/script>'
            + '<script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.16/mode/clike/clike.min.js"><\/script>'
            + '</head><body>'
            + '<textarea id="code">' + escapedCode + '</textarea>'
            + '<script>'
            + 'var editor=CodeMirror.fromTextArea(document.getElementById("code"),{'
            + 'mode:"application/x-httpd-php",theme:"material-darker",lineNumbers:true,'
            + 'readOnly:true,lineWrapping:false,viewportMargin:Infinity,'
            + 'matchBrackets:true,scrollbarStyle:null'
            + '});'
            + '<\/script>'
            + '</body></html>';
        codeWeb.LoadHtml(cmHtml, "file:///");
        layDetail.AddChild(codeWeb);
    }

    // Show detail, hide content
    scrollContent.SetVisibility("Gone");
    scrollDetail.SetVisibility("Show");
}

function HideDetail() {
    scrollDetail.SetVisibility("Gone");
    scrollContent.SetVisibility("Show");
    currentItem = null;
}

// ============================================================================
// COPY / RUN / SAVE FUNCTIONALITY
// ============================================================================

function CopyCode(code) {
    app.SetClipboardText(code);
    app.ShowPopup("📋 Code copied to clipboard!");
}

function RunCode(code, name) {
    _RunPhpCode(code, name);
}

// Original logic.php content — used to restore after running examples
var _LOGIC_PHP_ORIGINAL = '<?php\n' +
    '// Dezactivăm orice output care nu e JSON\n' +
    'error_reporting(0);\n' +
    'ini_set(\'display_errors\', 0);\n' +
    '\n' +
    'require_once dirname(__FILE__) . \'/router.php\';\n' +
    'require_once dirname(__FILE__) . \'/app.php\';\n' +
    '\n' +
    '// Executăm logica prin router\n' +
    'Router::handle(new MyApp());\n' +
    '?>';

var _LOGIC_PHP_EXAMPLE = '<?php\n' +
    'error_reporting(0);\n' +
    'ini_set(\'display_errors\', 0);\n' +
    '\n' +
    'require_once dirname(__FILE__) . \'/router.php\';\n' +
    'require_once dirname(__FILE__) . \'/_doc_example.php\';\n' +
    '\n' +
    'Router::handle(new MyApp());\n' +
    '?>';

function _onPhpError(msg) {
    _restoreLogicPhp();
    app.Alert("PHP Error: " + msg, "Error");
}

var _exampleRunning = false;

function _restoreLogicPhp() {
    var appDir = "/sdcard/DroidScript/" + app.GetAppName() + "/";
    try { app.WriteFile(appDir + "logic.php", _LOGIC_PHP_ORIGINAL); } catch(e) {}
    // Also hide the PHP overlay if visible
    try { if (php) php.HideOverlay(); } catch(e) {}
    _exampleRunning = false;
}

function _RunPhpCode(code, name) {
    var appDir = "/sdcard/DroidScript/" + app.GetAppName() + "/";

    try {
        // 1. Write the example code to _doc_example.php
        app.WriteFile(appDir + "_doc_example.php", code);

        // 2. Patch logic.php to include _doc_example.php instead of app.php
        app.WriteFile(appDir + "logic.php", _LOGIC_PHP_EXAMPLE);

        // 3. Create or reuse the PHP plugin and start it
        if (!php) {
            php = app.CreatePhpNative();
            php.SetOnError(_onPhpError);
            php.SetApp(app.GetAppName());
        }

        php.Start("index");
        app.ShowPopup("▶ Running: " + name);
        _exampleRunning = true;

        // NOTE: Do NOT restore logic.php here!
        // It must stay patched so button clicks route to _doc_example.php.
        // It gets restored when the user closes the overlay or runs another example.

    } catch(e) {
        _restoreLogicPhp();
        app.Alert("Run failed: " + e.message, "Error");
    }
}

function SaveCode(code, name) {
    var cleanName = name.replace(/[^a-zA-Z0-9_]/g, "_").toLowerCase();
    var fileName = "example_" + cleanName + ".php";
    var savePath = "/sdcard/DroidScript/" + app.GetAppName() + "/" + fileName;

    try {
        app.WriteFile(savePath, code);
        app.ShowPopup("💾 Saved: " + fileName);
    } catch(e) {
        app.Alert("Save failed: " + e.message, "Error");
    }
}

// ============================================================================
// BACK BUTTON HANDLING
// ============================================================================
function OnBack() {
    // If a PHP example is running, close it and restore logic.php
    if (_exampleRunning) {
        _restoreLogicPhp();
        return true;
    }

    // If AI chat or doc tabs are showing, go back to simple tab
    if (currentTab === "ai" || currentTab === "simpleDoc" || currentTab === "uicoreDoc" || currentTab === "androidSdk") {
        SwitchTab("simple");
        return true;
    }
    // Detail → items → sections
    if (scrollDetail.GetVisibility() === "Show") {
        HideDetail();
        return true;
    }
    if (currentSection !== null) {
        currentSection = null;
        var data = currentTab === "simple" ? simpleData : uicoreData;
        ShowSections(data);
        return true;
    }
    // If search is visible, close it
    if (laySearch.GetVisibility() !== "Gone") {
        ClearSearch();
        return true;
    }
    // Ask before exit
    ShowExitConfirmation();
    return true;
}

function ShowExitConfirmation() {
    app.Alert("Exit PhpDocs?", "Confirm Exit", "Exit,Cancel", function(btn) {
        if (btn === "Exit") {
            app.Exit();
        }
    });
}

// ============================================================================
// AI CHAT — Pollinations API with PhpNativePlugin context
// ============================================================================

var _sourceFiles = [
    { name: "simple.php", path: "simple.php" },
    { name: "ui_core.php", path: "ui_core.php" },
    { name: "router.php", path: "router.php" },
    { name: "logic.php", path: "logic.php" },
    { name: "PhpNativePlugin.java", path: "../PhpNativePlugin.java" }
];

function LoadAiContextIfNeeded() {
    if (aiContext) return;
    aiContext = "";
    var basePath = app.GetAppPath() + "/";

    for (var i = 0; i < _sourceFiles.length; i++) {
        var sf = _sourceFiles[i];
        try {
            var content = null;
            // Try multiple path strategies
            var paths = [
                basePath + sf.path,
                "/sdcard/DroidScript/" + app.GetAppName() + "/" + sf.path,
                sf.path
            ];
            for (var p = 0; p < paths.length; p++) {
                try {
                    content = app.ReadFile(paths[p]);
                    if (content && content.length > 10) break;
                    content = null;
                } catch(e2) { content = null; }
            }
            if (content) {
                // Truncate large files to keep payload under API limits
                if (content.length > 8000) {
                    content = content.substring(0, 8000) + "\n... [truncated, " + content.length + " chars total]";
                }
                aiContext += "\n===== " + sf.name + " =====\n" + content + "\n";
            } else {
                aiContext += "\n===== " + sf.name + " =====\n[File not found at tested paths]\n";
            }
        } catch(e) {
            aiContext += "\n===== " + sf.name + " =====\n[Error reading: " + e.message + "]\n";
        }
    }
    AddSystemMessage("Context loaded: " + _sourceFiles.length + " source files ready.");
}

function GetSystemPrompt() {
    // Build system prompt with actual PHP source code
    var prompt = "You are a PHP code generator for PhpNativePlugin (PHP on Android framework).\n\n";
    prompt += "STRICT RULES:\n";
    prompt += "1) OUTPUT CODE ONLY - No explanations, no comments outside code, no descriptions\n";
    prompt += "2) Wrap ALL code in ```php fences\n";
    prompt += "3) ONLY use functions/classes from the source files below - NOTHING ELSE\n";
    prompt += "4) NEVER use ->toJson() - the framework handles JSON output automatically\n";
    prompt += "5) Always use 'class MyApp' with 'function index()' as entry point\n";
    prompt += "6) Return Component objects directly from index() - NO echo, NO print, NO ->toJson()\n";
    prompt += "7) Action callbacks receive $p array with 'tag' for identifying clicked item\n\n";
    
    // Include actual PHP source files if loaded
    if (phpSimpleContent && phpSimpleContent.length > 100) {
        prompt += "======== SIMPLE.PHP SOURCE CODE ========\n";
        prompt += phpSimpleContent + "\n\n";
    }
    
    if (phpUiCoreContent && phpUiCoreContent.length > 100) {
        prompt += "======== UI_CORE.PHP SOURCE CODE ========\n";
        prompt += phpUiCoreContent + "\n\n";
    }
    
    // If no files loaded, use fallback summary
    if ((!phpSimpleContent || phpSimpleContent.length < 100) && (!phpUiCoreContent || phpUiCoreContent.length < 100)) {
        prompt += "[PHP source files not loaded - using summary]\n";
        prompt += "simple.php: page(), card(), label(), button(), input(), checkbox(), toggle(), rating(), seekbar(), progress(), spinner(), fab(), chip(), tabs(), grid(), table(), row(), stack(), spacer(), divider(), image(), video(), webView(), calendar(), toast(), alert(), confirm(), prompt(), bounce(), shake(), gps(), battery(), compass(), accelerometer(), get(), set(), update(), show(), hide(), enable(), disable(), download(), speak(), sms(), call(), notify(), email(), encrypt(), decrypt()\n";
        prompt += "ui_core.php: TextView, Button, CheckBox, EditText, ImageView, ProgressBar, SeekBar, Spinner, WebView, ToggleButton, RatingBar, VerticalLayout, HorizontalLayout, ScrollView, CardView, GridLayout, TableLayout, StackLayout, DrawerLayout, BottomNavBar. Chainable: ->id()->text()->textSize()->textColor()->backgroundColor()->padding()->margin()->action()->visibility()\n";
    }
    
    prompt += "======== CORRECT PATTERNS ========\n";
    prompt += "--- simple.php example ---\n";
    prompt += "```php\n<?php\nrequire_once 'simple.php';\nclass MyApp {\n    function index() {\n        return page('Title', [label('Hello'), button('Click', 'onClick')]);\n    }\n    function onClick($p) { return toast('Clicked!'); }\n}\n```\n\n";
    prompt += "--- ui_core.php example ---\n";
    prompt += "```php\n<?php\nrequire_once 'ui_core.php';\nclass MyApp {\n    function index() {\n        return (new VerticalLayout([\n            (new TextView())->text('Hello')->textSize(24),\n            (new Button())->text('Click')->action('onClick')->tag('btn1')\n        ]))->padding(20);\n    }\n    function onClick($p) { return toast('Clicked: '.$p['tag']); }\n}\n```\n\n";
    prompt += "RESPOND WITH CODE ONLY. NO EXPLANATIONS.";
    
    return prompt;
}

// ============================================================================
// AI CHAT UI
// ============================================================================

function BuildChatUI() {
    // Chat header
    var chatHeader = app.CreateLayout("Linear", "Left,FillX");
    chatHeader.SetPadding(0.04, 0.015, 0.04, 0.01);
    chatHeader.SetBackColor(C.bgHeader);

    var chatTitle = app.CreateText("🤖 AI Code Assistant", -1, -1, "Bold");
    chatTitle.SetTextSize(17);
    chatTitle.SetTextColor(C.accentPink);
    chatHeader.AddChild(chatTitle);

    var chatSubtitle = app.CreateText("Pollinations AI · PhpNativePlugin context", -1, -1);
    chatSubtitle.SetTextSize(11);
    chatSubtitle.SetTextColor(C.textMuted);
    chatHeader.AddChild(chatSubtitle);

    layChat.AddChild(chatHeader);

    // Quick-action chips
    var chipRow = app.CreateLayout("Linear", "Horizontal,Left,FillX");
    chipRow.SetPadding(0.02, 0.008, 0.02, 0.008);
    chipRow.SetBackColor(C.bgCard);

    var quickPrompts = [
        ["📱 Build app", "Create a complete app with simple.php that has a login screen, a home screen with navigation, and a settings page."],
        ["📐 Layout help", "Show me how to create a complex layout with ui_core.php using cards, grids and a bottom navigation bar."],
        ["📡 Sensors", "Write a PHP app that reads accelerometer, GPS, and battery data and displays them live."],
        ["🔧 Debug", "I have a PHP app that is not rendering correctly. What are the common issues and how do I debug PhpNativePlugin apps?"]
    ];

    for (var q = 0; q < quickPrompts.length; q++) {
        var chip = app.CreateButton(quickPrompts[q][0], -1, 0.04, "Custom");
        chip.SetTextSize(10);
        chip.SetTextColor(C.cyan);
        chip.SetMargins(0.005, 0, 0.005, 0);
        chip.SetOnTouch(_makeSendChat(quickPrompts[q][1]));
        chipRow.AddChild(chip);
    }
    layChat.AddChild(chipRow);

    // Messages area
    scrollChat = app.CreateScroller(1, 0.55);
    scrollChat.SetBackColor(C.bg);

    layChatMessages = app.CreateLayout("Linear", "Left,FillX,Top");
    layChatMessages.SetPadding(0.02, 0.01, 0.02, 0.01);
    scrollChat.AddChild(layChatMessages);
    layChat.AddChild(scrollChat);

    // Welcome message
    AddAssistantBubble("Hello! I'm your PhpNativePlugin AI assistant. I have access to the full source code of:\n\n" +
        "• simple.php (225 functions)\n• ui_core.php (38 classes + 55 functions)\n• router.php\n• logic.php\n• PhpNativePlugin.java\n\n" +
        "Ask me anything — I can generate complete PHP apps, explain APIs, debug issues, or suggest architecture patterns. " +
        "Use the quick-action chips above or type your question below!");

    // Input row
    var inputRow = app.CreateLayout("Linear", "Horizontal,FillX,VCenter");
    inputRow.SetBackColor(C.bgCard);
    inputRow.SetPadding(0.02, 0.015, 0.02, 0.015);

    // Settings button
    var btnSettings = app.CreateButton("⚙", 0.08, 0.05, "Custom");
    btnSettings.SetTextSize(16);
    btnSettings.SetTextColor(C.textMuted);
    btnSettings.SetOnTouch(ShowAiSettings);
    inputRow.AddChild(btnSettings);

    txtChatInput = app.CreateTextEdit("", 0.65, 0.05, "SingleLine");
    txtChatInput.SetHint("Ask about PhpNativePlugin...");
    txtChatInput.SetTextColor(C.text);
    txtChatInput.SetBackColor(C.bgCode);
    txtChatInput.SetTextSize(14);
    inputRow.AddChild(txtChatInput);

    var btnSend = app.CreateButton("➤", 0.12, 0.05, "Custom");
    btnSend.SetTextSize(20);
    btnSend.SetTextColor(C.accentPink);
    btnSend.SetBackColor(C.bgCode);
    btnSend.SetOnTouch(OnSendChat);
    inputRow.AddChild(btnSend);

    layChat.AddChild(inputRow);
}

function OnSendChat() {
    var msg = txtChatInput.GetText().trim();
    if (!msg || aiIsStreaming) return;
    txtChatInput.SetText("");
    SendChatMessage(msg);
}

function SendChatMessage(userMessage) {
    if (aiIsStreaming) return;

    // Load source-file context on first use
    LoadAiContextIfNeeded();

    // Add user bubble
    AddUserBubble(userMessage);
    chatHistory.push({ role: "user", content: userMessage });

    // Build messages array for API
    var messages = [{ role: "system", content: GetSystemPrompt() }];

    // Include last 10 conversation turns for context
    var histStart = Math.max(0, chatHistory.length - 20);
    for (var i = histStart; i < chatHistory.length; i++) {
        messages.push(chatHistory[i]);
    }

    // Add thinking indicator
    var thinkingBubble = AddAssistantBubble("⏳ Thinking...");
    aiIsStreaming = true;

    // Safety timeout — if no response after 2 min, unblock
    var _responded = false;
    setTimeout(function() {
        if (!_responded && aiIsStreaming) {
            aiIsStreaming = false;
            try { layChatMessages.DestroyChild(thinkingBubble); } catch(e) {}
            AddAssistantBubble("❌ No response received. Check internet connection and try again.");
        }
    }, 130000);

    // Call Pollinations API
    CallPollinationsAPI(messages, function(response) {
        if (_responded) return;
        _responded = true;
        aiIsStreaming = false;
        // Remove thinking bubble
        try { layChatMessages.DestroyChild(thinkingBubble); } catch(e) {}

        if (response.error) {
            AddAssistantBubble("❌ Error: " + response.error);
        } else {
            var reply = response.text || "(empty response)";
            chatHistory.push({ role: "assistant", content: reply });
            AddAssistantBubble(reply);

            // Extract code blocks and show copy/run buttons
            ExtractAndShowCodeActions(reply);
        }
    });
}

// ============================================================================
// POLLINATIONS API
// ============================================================================

function CallPollinationsAPI(messages, callback) {
    var _called = false;
    function safeCallback(result) {
        if (_called) return;
        _called = true;
        callback(result);
    }

    // Always use POST /openai - works with or without API key
    _callPollinationsPOST(messages, safeCallback);
}

// Free API: GET https://text.pollinations.ai/{prompt}?system=...&model=...
function _callPollinationsGET(messages, safeCallback) {
    // Extract last user message only (skip full system prompt to avoid HTTP 431)
    var userPrompt = "";
    for (var i = 0; i < messages.length; i++) {
        if (messages[i].role === "user") userPrompt = messages[i].content;
    }

    // Short system prompt for GET with links to source files
    var shortSystem = "You are an expert for PhpNativePlugin (PHP on Android). " +
        "Refs: simple.php https://evidentacolumbofila.eu/simple.php | " +
        "ui_core.php https://evidentacolumbofila.eu/ui_core.php | " +
        "router.php https://evidentacolumbofila.eu/router.php | " +
        "logic.php https://evidentacolumbofila.eu/logic.php. " +
        "RESPOND WITH CODE ONLY. Minimal explanation. Always wrap code in ```php fences.";

    var url = "https://text.pollinations.ai/" + encodeURIComponent(userPrompt)
        + "?model=" + encodeURIComponent(AI_MODEL)
        + "&system=" + encodeURIComponent(shortSystem);

    // ---- XMLHttpRequest ----
    try {
        var xhr = new XMLHttpRequest();
        xhr.open("GET", url, true);
        xhr.timeout = 120000;

        xhr.onload = _nh(function() {
            if (xhr.status >= 200 && xhr.status < 300) {
                safeCallback({ text: xhr.responseText || "(empty response)" });
            } else {
                safeCallback({ error: "HTTP " + xhr.status + ": " + (xhr.responseText || "").substring(0, 300) });
            }
        });
        xhr.onerror = _nh(function() {
            _tryDroidScriptHttpGET(url, safeCallback);
        });
        xhr.ontimeout = _nh(function() {
            safeCallback({ error: "Request timed out (2 min). Try a shorter question." });
        });
        xhr.send();
        return;
    } catch(e) {
        // XHR not available — fall through
    }

    _tryDroidScriptHttpGET(url, safeCallback);
}

function _tryDroidScriptHttpGET(url, safeCallback) {
    try {
        var httpCb = _nh(function(response, status) {
            if (!response) {
                safeCallback({ error: "Empty response from API" });
                return;
            }
            if (response.indexOf && response.indexOf("Error") === 0) {
                safeCallback({ error: response });
            } else {
                safeCallback({ text: response });
            }
        });
        var parts = url.match(/^(https?:\/\/[^\/]+)(\/.*)$/);
        app.HttpRequest("GET", parts[1], parts[2], "", httpCb, "");
    } catch(e2) {
        safeCallback({ error: "HTTP failed: " + e2.message + ". Check internet permissions." });
    }
}

// With API key: POST https://text.pollinations.ai/openai (OpenAI-compatible)
function _callPollinationsPOST(messages, safeCallback) {
    var url = "https://text.pollinations.ai/openai";

    var payload = JSON.stringify({
        model: AI_MODEL,
        messages: messages,
        temperature: 0.7,
        max_tokens: 4096
    });

    // ---- XMLHttpRequest ----
    try {
        var xhr = new XMLHttpRequest();
        xhr.open("POST", url, true);
        xhr.setRequestHeader("Content-Type", "application/json");
        xhr.timeout = 120000;

        xhr.onload = _nh(function() {
            var respText = xhr.responseText || "";
            if (xhr.status >= 200 && xhr.status < 300) {
                if (!respText || respText.trim() === "") {
                    safeCallback({ error: "Empty response from API (status " + xhr.status + ")" });
                    return;
                }
                // Extract text content from response
                var text = _parseApiResponse(respText);
                safeCallback({ text: text });
            } else {
                safeCallback({ error: "HTTP " + xhr.status + ": " + respText.substring(0, 300) });
            }
        });
        xhr.onerror = _nh(function() {
            _tryDroidScriptHttpPOST(url, payload, safeCallback);
        });
        xhr.ontimeout = _nh(function() {
            safeCallback({ error: "Request timed out (2 min). Try a shorter question." });
        });
        xhr.send(payload);
        return;
    } catch(e) {
        // XHR not available — fall through
    }

    _tryDroidScriptHttpPOST(url, payload, safeCallback);
}

function _tryDroidScriptHttpPOST(url, payload, safeCallback) {
    try {
        var httpCb = _nh(function(response, status) {
            if (!response || response.trim() === "") {
                safeCallback({ error: "Empty response from API" });
                return;
            }
            // Extract text content from response
            var text = _parseApiResponse(response);
            if (text.indexOf("Error") === 0) {
                safeCallback({ error: text });
            } else {
                safeCallback({ text: text });
            }
        });
        var parts = url.match(/^(https?:\/\/[^\/]+)(\/.*)$/);
        var headers = "Content-Type:application/json";
        app.HttpRequest("POST", parts[1], parts[2], payload, httpCb, headers);
    } catch(e2) {
        safeCallback({ error: "HTTP failed: " + e2.message + ". Check internet permissions." });
    }
}

// Parse API response - extract text content from various JSON formats
function _parseApiResponse(respText) {
    if (!respText) return "(empty response)";
    
    // Check if it looks like JSON
    var trimmed = respText.trim();
    if (trimmed.charAt(0) !== '{' && trimmed.charAt(0) !== '[') {
        // Plain text response - return as-is
        return respText;
    }
    
    try {
        var json = JSON.parse(respText);
        
        // OpenAI-compatible format: choices[0].message.content
        if (json.choices && Array.isArray(json.choices) && json.choices.length > 0) {
            var choice = json.choices[0];
            if (choice.message) {
                // Primary: content field
                if (typeof choice.message.content === 'string' && choice.message.content.length > 0) {
                    return choice.message.content;
                }
                // Fallback: reasoning_content field (some models use this)
                if (typeof choice.message.reasoning_content === 'string' && choice.message.reasoning_content.length > 0) {
                    return choice.message.reasoning_content;
                }
            }
            // Legacy: text field
            if (typeof choice.text === 'string') {
                return choice.text;
            }
            // Streaming: delta.content
            if (choice.delta && typeof choice.delta.content === 'string') {
                return choice.delta.content;
            }
        }
        
        // Direct content field
        if (typeof json.content === 'string') {
            return json.content;
        }
        
        // Response field
        if (typeof json.response === 'string') {
            return json.response;
        }
        
        // Message field
        if (typeof json.message === 'string') {
            return json.message;
        }
        
        // Text field
        if (typeof json.text === 'string') {
            return json.text;
        }
        
        // Output field
        if (typeof json.output === 'string') {
            return json.output;
        }
        
        // Result field
        if (typeof json.result === 'string') {
            return json.result;
        }
        
        // If we got here, JSON parsed but no known content field
        return "⚠️ Unexpected response format. Raw: " + respText.substring(0, 500);
        
    } catch(e) {
        // JSON parsing failed - might still be valid text response
        return respText;
    }
}

// ============================================================================
// CHAT BUBBLES
// ============================================================================

function AddUserBubble(text) {
    var bubble = app.CreateLayout("Linear", "Right,FillX");
    bubble.SetPadding(0.15, 0.008, 0.02, 0.008);

    var card = app.CreateLayout("Linear", "Left");
    card.SetBackColor("#1a3a5c");
    card.SetCornerRadius(12);
    card.SetPadding(0.03, 0.015, 0.03, 0.015);

    var txt = app.CreateText(text, 0.7, -1, "Left,MultiLine");
    txt.SetTextSize(13);
    txt.SetTextColor(C.text);
    card.AddChild(txt);
    bubble.AddChild(card);

    layChatMessages.AddChild(bubble);
    ScrollChatToBottom();
    return bubble;
}

function AddAssistantBubble(text) {
    var bubble = app.CreateLayout("Linear", "Left,FillX");
    bubble.SetPadding(0.02, 0.008, 0.15, 0.008);

    var card = app.CreateLayout("Linear", "Left");
    card.SetBackColor(C.bgCard);
    card.SetCornerRadius(12);
    card.SetPadding(0.03, 0.015, 0.03, 0.015);

    // Parse and render text with code block support
    // First try to split by ``` code fences
    var parts = text.split(/(```[\s\S]*?```)/g);
    
    // If no code fences found, try to detect <?php blocks
    if (parts.length === 1 && text.indexOf("<?php") >= 0) {
        // Split by <?php ... (end at next <?php or end of text)
        parts = text.split(/(\<\?php[\s\S]*?)(?=\<\?php|$)/g);
    }
    
    for (var i = 0; i < parts.length; i++) {
        var part = parts[i];
        if (!part || !part.trim()) continue;
        part = part.trim();

        // Check if it's a code block (``` fenced or <?php)
        var isCodeBlock = part.indexOf("```") === 0;
        var isPhpBlock = !isCodeBlock && part.indexOf("<?php") >= 0;
        
        if (isCodeBlock || isPhpBlock) {
            // Extract code content
            var codeContent = isCodeBlock 
                ? part.replace(/^```\w*\n?/, "").replace(/\n?```$/, "")
                : part;

            var codeLay = app.CreateLayout("Linear", "Left,FillX");
            codeLay.SetBackColor(C.bgCode);
            codeLay.SetCornerRadius(8);
            codeLay.SetPadding(0.02, 0.01, 0.02, 0.01);
            codeLay.SetMargins(0, 0.005, 0, 0.005);

            var codeTxt = app.CreateText(codeContent, 0.62, -1, "Left,Monospace,MultiLine");
            codeTxt.SetTextSize(10);
            codeTxt.SetTextColor("#c9d1d9");
            codeLay.AddChild(codeTxt);

            // Action buttons row
            var codeActions = app.CreateLayout("Linear", "Horizontal,Left,FillX");
            codeActions.SetMargins(0, 0.005, 0, 0);

            // Copy button - always show
            var copyBtn = app.CreateButton("📋 Copy", -1, 0.04, "Custom");
            copyBtn.SetTextSize(11);
            copyBtn.SetTextColor(C.accent);
            copyBtn.SetBackColor(C.bgCard);
            copyBtn.SetMargins(0, 0, 0.02, 0);
            copyBtn.SetOnTouch(_makeClipboard(codeContent, "📋 Code copied!"));
            codeActions.AddChild(copyBtn);

            // Run & Save buttons if it looks like PHP
            var isPhp = codeContent.indexOf("<?php") >= 0 || codeContent.indexOf("require_once") >= 0 || codeContent.indexOf("function ") >= 0;
            if (isPhp) {
                var runBtn = app.CreateButton("▶ Run", -1, 0.04, "Custom");
                runBtn.SetTextSize(11);
                runBtn.SetTextColor(C.success);
                runBtn.SetBackColor(C.bgCard);
                runBtn.SetMargins(0, 0, 0.02, 0);
                runBtn.SetOnTouch(_makeRun(codeContent, "AI Example"));
                codeActions.AddChild(runBtn);

                var saveBtn = app.CreateButton("💾 Save", -1, 0.04, "Custom");
                saveBtn.SetTextSize(11);
                saveBtn.SetTextColor(C.accentWarm);
                saveBtn.SetBackColor(C.bgCard);
                saveBtn.SetOnTouch(_makeSave(codeContent, "ai_generated"));
                codeActions.AddChild(saveBtn);
            }

            codeLay.AddChild(codeActions);
            card.AddChild(codeLay);
        } else {
            // Normal text
            var txt = app.CreateText(part, 0.65, -1, "Left,MultiLine");
            txt.SetTextSize(13);
            txt.SetTextColor(C.text);
            card.AddChild(txt);
        }
    }

    // Add copy response button at bottom
    var responseActions = app.CreateLayout("Linear", "Horizontal,Right,FillX");
    responseActions.SetMargins(0, 0.008, 0, 0);

    var copyResponseBtn = app.CreateButton("📋 Copy Response", -1, 0.035, "Custom");
    copyResponseBtn.SetTextSize(10);
    copyResponseBtn.SetTextColor(C.textMuted);
    copyResponseBtn.SetOnTouch(_makeClipboard(text, "📋 Response copied!"));
    responseActions.AddChild(copyResponseBtn);

    card.AddChild(responseActions);

    bubble.AddChild(card);
    layChatMessages.AddChild(bubble);
    ScrollChatToBottom();
    return bubble;
}

function AddSystemMessage(text) {
    var lay = app.CreateLayout("Linear", "FillX,VCenter");
    lay.SetPadding(0.04, 0.005, 0.04, 0.005);

    var txt = app.CreateText("ℹ " + text, 0.85, -1, "Left,MultiLine");
    txt.SetTextSize(11);
    txt.SetTextColor(C.textDim);
    lay.AddChild(txt);

    layChatMessages.AddChild(lay);
    ScrollChatToBottom();
}

function ScrollChatToBottom() {
    // Scroll after a small delay to let layout settle
    setTimeout(function() {
        scrollChat.ScrollTo(0, 99999);
    }, 100);
}

function ExtractAndShowCodeActions(text) {
    // If the response has PHP code blocks, add a combined copy/run action bar
    var phpBlocks = [];
    var regex = /```(?:php)?\s*\n?([\s\S]*?)\n?```/g;
    var match;
    while ((match = regex.exec(text)) !== null) {
        var code = match[1].trim();
        if (code.indexOf("<?php") >= 0 || code.indexOf("function") >= 0) {
            phpBlocks.push(code);
        }
    }

    if (phpBlocks.length > 0) {
        var actionLay = app.CreateLayout("Linear", "Horizontal,Left,FillX");
        actionLay.SetPadding(0.02, 0.005, 0.02, 0.005);

        var infoTxt = app.CreateText(phpBlocks.length + " PHP block(s)", -1, -1);
        infoTxt.SetTextSize(10);
        infoTxt.SetTextColor(C.textMuted);
        infoTxt.SetMargins(0, 0, 0.02, 0);
        actionLay.AddChild(infoTxt);

        var allCode = phpBlocks.join("\n\n");

        var copyAllBtn = app.CreateButton("📋 Copy All", -1, 0.035, "Custom");
        copyAllBtn.SetTextSize(10);
        copyAllBtn.SetTextColor(C.accent);
        copyAllBtn.SetOnTouch(_makeClipboard(allCode, "📋 All PHP code copied!"));
        actionLay.AddChild(copyAllBtn);

        var saveAllBtn = app.CreateButton("💾 Save", -1, 0.035, "Custom");
        saveAllBtn.SetTextSize(10);
        saveAllBtn.SetTextColor(C.accentWarm);
        saveAllBtn.SetOnTouch(_makeSave(allCode, "ai_generated"));
        actionLay.AddChild(saveAllBtn);

        layChatMessages.AddChild(actionLay);
        ScrollChatToBottom();
    }
}

// ============================================================================
// AI SETTINGS
// ============================================================================

function ShowAiSettings() {
    var dlg = app.CreateDialog("AI Settings");
    dlg.SetBackColor(C.bgCard);

    var lay = app.CreateLayout("Linear", "Left,FillX");
    lay.SetPadding(0.04, 0.02, 0.04, 0.02);

    var lblModel = app.CreateText("Model:", -1, -1, "Left,Bold");
    lblModel.SetTextSize(14); lblModel.SetTextColor(C.text);
    lay.AddChild(lblModel);

    var spinModel = app.CreateSpinner("openai,mistral,llama,qwen,deepseek", 0.7);
    spinModel.SetText(AI_MODEL);
    spinModel.SetTextSize(14);
    lay.AddChild(spinModel);

    var lblKey = app.CreateText("API Key (optional):", -1, -1, "Left,Bold");
    lblKey.SetTextSize(14); lblKey.SetTextColor(C.text);
    lblKey.SetMargins(0, 0.02, 0, 0);
    lay.AddChild(lblKey);

    var edtKey = app.CreateTextEdit(AI_API_KEY, 0.7, -1, "SingleLine");
    edtKey.SetHint("Optional - for multi-turn chat");
    edtKey.SetTextSize(13);
    lay.AddChild(edtKey);

    var lblInfo = app.CreateText("Free without key (basic mode).\nWith key: enhanced multi-turn chat.", 0.7, -1, "Left,MultiLine");
    lblInfo.SetTextSize(11); lblInfo.SetTextColor(C.textMuted);
    lblInfo.SetMargins(0, 0.02, 0, 0.01);
    lay.AddChild(lblInfo);

    var btnRow = app.CreateLayout("Linear", "Horizontal,FillX");

    var btnClear = app.CreateButton("Clear Chat", 0.35, 0.05, "Custom");
    btnClear.SetTextSize(13); btnClear.SetTextColor(C.danger);
    btnClear.SetOnTouch(function() {
        chatHistory = [];
        layChatMessages = _destroyAllChildren(layChatMessages, scrollChat, "Left,FillX,Top", [0.02, 0.01, 0.02, 0.01]);
        AddAssistantBubble("Chat cleared. Ask me anything about PhpNativePlugin!");
        dlg.Dismiss();
    });
    btnRow.AddChild(btnClear);

    var btnSave = app.CreateButton("Save", 0.35, 0.05, "Custom");
    btnSave.SetTextSize(13); btnSave.SetTextColor(C.success);
    btnSave.SetOnTouch(function() {
        AI_MODEL = spinModel.GetText();
        AI_API_KEY = edtKey.GetText().trim();
        // Persist settings
        try {
            app.SaveText("AI_API_KEY", AI_API_KEY);
            app.SaveText("AI_MODEL", AI_MODEL);
        } catch(e) {}
        app.ShowPopup("Settings saved! Model: " + AI_MODEL);
        dlg.Dismiss();
    });
    btnRow.AddChild(btnSave);

    lay.AddChild(btnRow);
    dlg.AddLayout(lay);
    dlg.Show();

    var btnReload = app.CreateButton("Reload Context", 0.7, 0.05, "Custom");
    btnReload.SetTextSize(13); btnReload.SetTextColor(C.accent);
    btnReload.SetOnTouch(function() {
        aiContext = null;
        LoadAiContextIfNeeded();
        app.ShowPopup("Context reloaded from source files");
        dlg.Dismiss();
    });
    lay.AddChild(btnReload);
}
