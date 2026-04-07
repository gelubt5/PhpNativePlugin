<?php
/**
 * Simple PHP - Beginner-Friendly Layer for PhpNativePlugin
 * 
 * This file provides easy-to-use functions that hide the complexity
 * of the underlying ui_core.php and action system.
 * 
 * Usage:
 *   require_once 'simple.php';
 *   
 *   class MyApp {
 *       function index() {
 *           return page("My App", [
 *               label("Hello World!"),
 *               button("Click Me", "onClick"),
 *           ]);
 *       }
 *       
 *       function onClick($params) {
 *           return alert("You clicked!");
 *       }
 *   }
 */

require_once 'ui_core.php';

// =============================================================================
// PAGE BUILDERS - Create screens easily
// =============================================================================

/**
 * Create a simple page with a title and content
 * 
 * @param string $title Page title (shown at top)
 * @param array $content Array of UI elements
 * @param array $options Optional: ['padding' => 30, 'background' => '#ffffff']
 * @return VerticalLayout
 * 
 * Example:
 *   return page("Welcome", [
 *       label("Hello!"),
 *       button("Start", "onStart")
 *   ]);
 */
function page(string $title, array $content, array $options = []) {
    $padding = $options['padding'] ?? 30;
    $titleColor = $options['titleColor'] ?? '#4ec9b0';
    $titleSize = $options['titleSize'] ?? 24;
    
    $elements = [
        (new TextView())
            ->text($title)
            ->textSize($titleSize)
            ->textColor($titleColor)
            ->gravity("center")
            ->padding(10),
    ];
    
    // Add separator
    $elements[] = spacer(10);
    
    // Add content
    foreach ($content as $item) {
        $elements[] = $item;
    }
    
    return (new VerticalLayout($elements))
        ->padding($padding)
        ->gravity("center");
}

/**
 * Create a card-style grouped section
 * 
 * @param string $title Section title
 * @param array $content Array of UI elements
 * @return VerticalLayout
 */
function card(string $title, array $content) {
    $elements = [
        (new TextView())
            ->text($title)
            ->textSize(16)
            ->textColor('#888888')
            ->padding(5),
    ];
    
    foreach ($content as $item) {
        $elements[] = $item;
    }
    
    return (new VerticalLayout($elements))
        ->padding(15)
        ->backgroundColor('#1e1e1e')
        ->cornerRadius(10)
        ->margin(10);
}

// =============================================================================
// UI ELEMENTS - Simple component creators
// =============================================================================

/**
 * Create a text label
 * 
 * @param string $text The text to display
 * @param array $style Optional: ['size' => 16, 'color' => '#fff', 'bold' => true, 'center' => true]
 * @return TextView
 * 
 * Example:
 *   label("Hello World")
 *   label("Big Title", ['size' => 32, 'bold' => true])
 */
function label(string $text, array $style = []) {
    $tv = (new TextView())->text($text);
    
    if (isset($style['size'])) $tv->textSize($style['size']);
    if (isset($style['color'])) $tv->textColor($style['color']);
    if (isset($style['bold'])) $tv->textStyle("bold");
    if (isset($style['center'])) $tv->gravity("center");
    if (isset($style['id'])) $tv->id($style['id']);
    if (isset($style['padding'])) $tv->padding($style['padding']);
    
    return $tv;
}

/**
 * Create a button
 * 
 * @param string $text Button text
 * @param string $action Method name to call when clicked
 * @param array $style Optional: ['color' => '#4CAF50', 'textColor' => '#fff']
 * @return Button
 * 
 * Example:
 *   button("Save", "onSave")
 *   button("Delete", "onDelete", ['color' => '#f44336'])
 */
function button(string $text, string $action, array $style = []) {
    $btn = (new Button())
        ->text($text)
        ->action($action)
        ->textAllCaps(false);
    
    if (isset($style['color'])) $btn->backgroundColor($style['color']);
    if (isset($style['textColor'])) $btn->textColor($style['textColor']);
    if (isset($style['id'])) $btn->id($style['id']);
    
    return $btn;
}

/**
 * Create a text input field
 * 
 * @param string $id Unique ID to reference this input
 * @param string $hint Placeholder text
 * @param array $options Optional: ['password' => true, 'number' => true, 'multiline' => true]
 * @return EditText
 * 
 * Example:
 *   input("email", "Enter your email")
 *   input("pass", "Password", ['password' => true])
 */
function input(string $id, string $hint = "", array $options = []) {
    $et = (new EditText())
        ->id($id)
        ->hint($hint);
    
    if (isset($options['password']) && $options['password']) {
        $et->inputType("textPassword");
    }
    if (isset($options['number']) && $options['number']) {
        $et->inputType("number");
    }
    if (isset($options['multiline']) && $options['multiline']) {
        $et->inputType("textMultiLine");
        $et->minLines(3);
    }
    if (isset($options['text'])) {
        $et->text($options['text']);
    }
    
    return $et;
}

/**
 * Create a checkbox
 * 
 * @param string $id Unique ID
 * @param string $text Label text
 * @param string|null $onChange Method to call when changed (receives isChecked)
 * @return CheckBox
 * 
 * Example:
 *   checkbox("agree", "I agree to terms", "onAgreeChange")
 */
function checkbox(string $id, string $text, ?string $onChange = null) {
    $cb = (new CheckBox())
        ->id($id)
        ->text($text);
    
    if ($onChange) {
        $cb->onCheckedChange($onChange);
    }
    
    return $cb;
}

/**
 * Create an image
 * 
 * @param string $src Image URL or base64
 * @param array $options Optional: ['width' => 200, 'height' => 200, 'fit' => 'center']
 * @return ImageView
 */
function image(string $src, array $options = []) {
    $iv = (new ImageView())->src($src);
    
    if (isset($options['width'])) $iv->width($options['width']);
    if (isset($options['height'])) $iv->height($options['height']);
    if (isset($options['fit'])) $iv->scaleType($options['fit']);
    if (isset($options['id'])) $iv->id($options['id']);
    
    return $iv;
}

/**
 * Create vertical spacing
 * 
 * @param int $height Height in dp
 * @return TextView
 */
function spacer(int $height = 20) {
    return (new TextView())
        ->text("")
        ->height($height);
}

/**
 * Create a horizontal divider line
 * 
 * @param string $color Line color
 * @return TextView
 */
function divider(string $color = '#333333') {
    return (new TextView())
        ->text("")
        ->height(1)
        ->backgroundColor($color)
        ->margin(10);
}

/**
 * Create a row of elements (horizontal layout simulation)
 * 
 * @param array $elements Elements to place in row
 * @return VerticalLayout (with horizontal orientation flag)
 */
function row(array $elements) {
    return (new VerticalLayout($elements))
        ->orientation("horizontal");
}

// =============================================================================
// ACTIONS - Simple response builders
// =============================================================================

/**
 * Show a toast message
 * 
 * @param string $message Message to show
 * @return array
 * 
 * Example:
 *   return toast("Saved successfully!");
 */
function toast(string $message) {
    return [
        "action" => "TOAST",
        "message" => $message
    ];
}

/**
 * Show an alert dialog
 * 
 * @param string $message Alert message
 * @param string $title Optional title
 * @return array
 * 
 * Example:
 *   return alert("Something went wrong!", "Error");
 */
function alert(string $message, string $title = "Alert") {
    return [
        "action" => "ALERT",
        "title" => $title,
        "message" => $message
    ];
}

/**
 * Update a view's text
 * 
 * @param string $id View ID
 * @param string $text New text
 * @param string|null $color Optional text color
 * @return array
 * 
 * Example:
 *   return setText("my_label", "Updated!");
 */
function setText(string $id, string $text, ?string $color = null) {
    $attrs = ["text" => $text];
    if ($color) $attrs["textColor"] = $color;
    
    return updateView($id, $attrs);
}

/**
 * Navigate to another screen (call another method)
 * 
 * @param string $screen Method name to call
 * @param array $data Optional data to pass
 * @return array
 * 
 * Example:
 *   return goTo("showDetails", ["id" => 123]);
 */
function goTo(string $screen, array $data = []) {
    return [
        "action" => "NAVIGATE",
        "screen" => $screen,
        "data" => $data
    ];
}

// =============================================================================
// SENSORS - One-line sensor access
// =============================================================================

/**
 * Get GPS location
 * 
 * @param string $callback Method to receive location data
 * @return array
 * 
 * Your callback receives: ['lat' => 12.34, 'lng' => 56.78, 'altitude' => 100]
 * 
 * Example:
 *   function getMyLocation($p) { return gps("onLocation"); }
 *   function onLocation($p) { return setText("lbl", "Lat: " . $p['lat']); }
 */
function gps(string $callback) {
    return [
        "action" => "DS_SENSOR_CALL",
        "sensor" => "location",
        "callback" => $callback
    ];
}

/**
 * Get battery status
 * 
 * @param string $callback Method to receive battery data
 * @return array
 * 
 * Your callback receives: ['level' => 85, 'charging' => true]
 */
function battery(string $callback) {
    return [
        "action" => "DS_SENSOR_CALL",
        "sensor" => "battery",
        "callback" => $callback
    ];
}

/**
 * Scan a barcode/QR code
 * 
 * @param string $callback Method to receive scan result
 * @return array
 * 
 * Your callback receives: ['code' => 'scanned-value']
 */
function scanBarcode(string $callback) {
    return [
        "action" => "DS_SENSOR_CALL",
        "sensor" => "barcode",
        "callback" => $callback
    ];
}

/**
 * Get compass heading
 * 
 * @param string $callback Method to receive compass data
 * @return array
 * 
 * Your callback receives: ['azimuth' => 180, 'pitch' => 0, 'roll' => 0]
 */
function compass(string $callback) {
    return [
        "action" => "DS_SENSOR_CALL",
        "sensor" => "compass",
        "callback" => $callback
    ];
}

// =============================================================================
// DATA - Get values from views
// =============================================================================

/**
 * Get text from an input field
 * 
 * @param string $id View ID
 * @param string $callback Method to receive the value
 * @return array
 * 
 * Your callback receives: ['viewId' => 'id', 'property' => 'text', 'value' => 'the text']
 * 
 * Example:
 *   function onSubmit($p) { return getText("name_input", "processName"); }
 *   function processName($p) { $name = $p['value']; ... }
 */
function getText(string $id, string $callback) {
    return getViewProperty($id, "text", $callback);
}

/**
 * Get checkbox state
 * 
 * @param string $id Checkbox ID
 * @param string $callback Method to receive the value
 * @return array
 * 
 * Your callback receives: ['value' => true/false]
 */
function getChecked(string $id, string $callback) {
    return getViewProperty($id, "checked", $callback);
}

// =============================================================================
// UNIVERSAL PROPERTY HELPERS - Easy get/set/update for any view property
// =============================================================================

/**
 * Get any property from a view
 * 
 * @param string $id View ID
 * @param string $property Property name (text, checked, visibility, etc.)
 * @param string $callback Method to receive the value
 * @return array
 * 
 * Your callback receives: ['viewId' => 'id', 'property' => 'name', 'value' => value]
 * 
 * Common properties: text, checked, visibility, enabled, alpha
 * 
 * Example:
 *   return get("my_input", "text", "onGotText");
 *   function onGotText($p) { $text = $p['value']; }
 */
function get(string $id, string $property, string $callback) {
    return getViewProperty($id, $property, $callback);
}

/**
 * Set a single property on a view
 * 
 * @param string $id View ID
 * @param string $property Property name
 * @param mixed $value New value for the property
 * @return array
 * 
 * Common properties:
 *   - text: "Hello"
 *   - textColor: "#ff0000"
 *   - textSize: 18
 *   - backgroundColor: "#333333"
 *   - visibility: "visible" | "invisible" | "gone"
 *   - enabled: true | false
 *   - alpha: 0.0 to 1.0
 *   - checked: true | false (for checkboxes)
 * 
 * Example:
 *   return set("my_label", "text", "Hello World!");
 *   return set("my_button", "backgroundColor", "#4CAF50");
 *   return set("my_input", "enabled", false);
 */
function set(string $id, string $property, $value) {
    return updateView($id, [$property => $value]);
}

/**
 * Update multiple properties on a view at once
 * 
 * @param string $id View ID
 * @param array $properties Array of property => value pairs
 * @return array
 * 
 * Example:
 *   return update("my_label", [
 *       "text" => "Updated!",
 *       "textColor" => "#00ff00",
 *       "textSize" => 24
 *   ]);
 */
function update(string $id, array $properties) {
    return updateView($id, $properties);
}

/**
 * Update multiple views at once
 * 
 * @param array $updates Array of [id => properties] pairs
 * @return array
 * 
 * Example:
 *   return updateMany([
 *       "label1" => ["text" => "First"],
 *       "label2" => ["text" => "Second", "textColor" => "#ff0000"],
 *       "btn1" => ["enabled" => false]
 *   ]);
 */
function updateMany(array $updates) {
    $commands = [];
    foreach ($updates as $id => $properties) {
        $commands[] = updateView($id, $properties);
    }
    return ["action" => "BATCH", "commands" => $commands];
}

// =============================================================================
// SHORTCUT SETTERS - Common property changes in one call
// =============================================================================

/**
 * Set text color of a view
 * 
 * @param string $id View ID
 * @param string $color Color hex code
 * @return array
 * 
 * Example:
 *   return setColor("my_label", "#ff0000");
 *   return setColor("my_label", Colors::DANGER);
 */
function setColor(string $id, string $color) {
    return set($id, "textColor", $color);
}

/**
 * Set text size of a view
 * 
 * @param string $id View ID
 * @param int $size Text size in sp
 * @return array
 * 
 * Example:
 *   return setSize("my_label", 24);
 */
function setSize(string $id, int $size) {
    return set($id, "textSize", $size);
}

/**
 * Set background color of a view
 * 
 * @param string $id View ID
 * @param string $color Color hex code
 * @return array
 * 
 * Example:
 *   return setBackground("my_card", "#1e1e1e");
 */
function setBackground(string $id, string $color) {
    return set($id, "backgroundColor", $color);
}

/**
 * Show a view (set visibility to visible)
 * 
 * @param string $id View ID
 * @return array
 * 
 * Example:
 *   return show("hidden_panel");
 */
function show(string $id) {
    return set($id, "visibility", "visible");
}

/**
 * Hide a view (set visibility to gone - removes from layout)
 * 
 * @param string $id View ID
 * @return array
 * 
 * Example:
 *   return hide("loading_spinner");
 */
function hide(string $id) {
    return set($id, "visibility", "gone");
}

/**
 * Make a view invisible (keeps space in layout)
 * 
 * @param string $id View ID
 * @return array
 */
function invisible(string $id) {
    return set($id, "visibility", "invisible");
}

/**
 * Enable a view (button, input, etc.)
 * 
 * @param string $id View ID
 * @return array
 * 
 * Example:
 *   return enable("submit_button");
 */
function enable(string $id) {
    return set($id, "enabled", true);
}

/**
 * Disable a view (button, input, etc.)
 * 
 * @param string $id View ID
 * @return array
 * 
 * Example:
 *   return disable("submit_button");
 */
function disable(string $id) {
    return set($id, "enabled", false);
}

/**
 * Set the opacity/alpha of a view
 * 
 * @param string $id View ID
 * @param float $alpha Value from 0.0 (invisible) to 1.0 (fully visible)
 * @return array
 * 
 * Example:
 *   return setAlpha("my_image", 0.5);  // 50% transparent
 */
function setAlpha(string $id, float $alpha) {
    return set($id, "alpha", max(0.0, min(1.0, $alpha)));
}

/**
 * Set checkbox/switch checked state
 * 
 * @param string $id Checkbox/Switch ID
 * @param bool $checked Whether it should be checked
 * @return array
 * 
 * Example:
 *   return setChecked("my_checkbox", true);
 */
function setChecked(string $id, bool $checked) {
    return set($id, "checked", $checked);
}

/**
 * Set image source
 * 
 * @param string $id ImageView ID
 * @param string $src Image URL or base64 data
 * @return array
 * 
 * Example:
 *   return setImage("profile_pic", "https://example.com/avatar.png");
 */
function setImage(string $id, string $src) {
    return set($id, "src", $src);
}

/**
 * Set hint/placeholder text for input fields
 * 
 * @param string $id EditText ID
 * @param string $hint Placeholder text
 * @return array
 * 
 * Example:
 *   return setHint("email_input", "Enter your email");
 */
function setHint(string $id, string $hint) {
    return set($id, "hint", $hint);
}

/**
 * Clear text from an input or label
 * 
 * @param string $id View ID
 * @return array
 * 
 * Example:
 *   return clear("my_input");
 */
function clear(string $id) {
    return set($id, "text", "");
}

// =============================================================================
// COMPLETE PROPERTY GETTERS - Get any view property
// =============================================================================

/**
 * Get text value from a view
 * @param string $id View ID
 * @param string $callback Method to receive the value
 * @return array
 */
function getTextValue(string $id, string $callback) {
    return get($id, "text", $callback);
}

/**
 * Get text color from a view
 * @param string $id View ID
 * @param string $callback Method to receive the value
 * @return array
 */
function getColor(string $id, string $callback) {
    return get($id, "textColor", $callback);
}

/**
 * Get text size from a view
 * @param string $id View ID
 * @param string $callback Method to receive the value
 * @return array
 */
function getTextSize(string $id, string $callback) {
    return get($id, "textSize", $callback);
}

/**
 * Get background color from a view
 * @param string $id View ID
 * @param string $callback Method to receive the value
 * @return array
 */
function getBackground(string $id, string $callback) {
    return get($id, "backgroundColor", $callback);
}

/**
 * Get visibility state from a view
 * @param string $id View ID
 * @param string $callback Method to receive: "visible", "invisible", or "gone"
 * @return array
 */
function getVisibility(string $id, string $callback) {
    return get($id, "visibility", $callback);
}

/**
 * Get enabled state from a view
 * @param string $id View ID
 * @param string $callback Method to receive true/false
 * @return array
 */
function getEnabled(string $id, string $callback) {
    return get($id, "enabled", $callback);
}

/**
 * Get alpha/opacity from a view
 * @param string $id View ID
 * @param string $callback Method to receive value 0.0-1.0
 * @return array
 */
function getAlpha(string $id, string $callback) {
    return get($id, "alpha", $callback);
}

/**
 * Get checked state from checkbox/switch
 * @param string $id View ID
 * @param string $callback Method to receive true/false
 * @return array
 */
function getCheckedState(string $id, string $callback) {
    return get($id, "checked", $callback);
}

/**
 * Get hint text from an input field
 * @param string $id View ID
 * @param string $callback Method to receive the hint text
 * @return array
 */
function getHint(string $id, string $callback) {
    return get($id, "hint", $callback);
}

/**
 * Get image source from an ImageView
 * @param string $id View ID
 * @param string $callback Method to receive the source
 * @return array
 */
function getImageSrc(string $id, string $callback) {
    return get($id, "src", $callback);
}

/**
 * Get width from a view
 * @param string $id View ID
 * @param string $callback Method to receive the width
 * @return array
 */
function getWidth(string $id, string $callback) {
    return get($id, "width", $callback);
}

/**
 * Get height from a view
 * @param string $id View ID
 * @param string $callback Method to receive the height
 * @return array
 */
function getHeight(string $id, string $callback) {
    return get($id, "height", $callback);
}

/**
 * Get padding from a view
 * @param string $id View ID
 * @param string $callback Method to receive the padding
 * @return array
 */
function getPadding(string $id, string $callback) {
    return get($id, "padding", $callback);
}

/**
 * Get margin from a view
 * @param string $id View ID
 * @param string $callback Method to receive the margin
 * @return array
 */
function getMargin(string $id, string $callback) {
    return get($id, "margin", $callback);
}

/**
 * Get progress value from ProgressBar/SeekBar
 * @param string $id View ID
 * @param string $callback Method to receive the progress value
 * @return array
 */
function getProgress(string $id, string $callback) {
    return get($id, "progress", $callback);
}

/**
 * Get max value from ProgressBar/SeekBar
 * @param string $id View ID
 * @param string $callback Method to receive the max value
 * @return array
 */
function getMax(string $id, string $callback) {
    return get($id, "max", $callback);
}

/**
 * Get rotation angle from a view
 * @param string $id View ID
 * @param string $callback Method to receive the rotation in degrees
 * @return array
 */
function getRotation(string $id, string $callback) {
    return get($id, "rotation", $callback);
}

/**
 * Get scale X from a view
 * @param string $id View ID
 * @param string $callback Method to receive the scale value
 * @return array
 */
function getScaleX(string $id, string $callback) {
    return get($id, "scaleX", $callback);
}

/**
 * Get scale Y from a view
 * @param string $id View ID
 * @param string $callback Method to receive the scale value
 * @return array
 */
function getScaleY(string $id, string $callback) {
    return get($id, "scaleY", $callback);
}

/**
 * Get translation X from a view
 * @param string $id View ID
 * @param string $callback Method to receive the translation value
 * @return array
 */
function getTranslationX(string $id, string $callback) {
    return get($id, "translationX", $callback);
}

/**
 * Get translation Y from a view
 * @param string $id View ID
 * @param string $callback Method to receive the translation value
 * @return array
 */
function getTranslationY(string $id, string $callback) {
    return get($id, "translationY", $callback);
}

/**
 * Get selected state from a view
 * @param string $id View ID
 * @param string $callback Method to receive true/false
 * @return array
 */
function getSelected(string $id, string $callback) {
    return get($id, "selected", $callback);
}

/**
 * Get focused state from a view
 * @param string $id View ID
 * @param string $callback Method to receive true/false
 * @return array
 */
function getFocused(string $id, string $callback) {
    return get($id, "focused", $callback);
}

/**
 * Get tag from a view
 * @param string $id View ID
 * @param string $callback Method to receive the tag value
 * @return array
 */
function getTag(string $id, string $callback) {
    return get($id, "tag", $callback);
}

// =============================================================================
// COMPLETE PROPERTY SETTERS - Set any view property
// =============================================================================

/**
 * Set text style (bold, italic, normal)
 * @param string $id View ID
 * @param string $style "bold" | "italic" | "bold_italic" | "normal"
 * @return array
 */
function setTextStyle(string $id, string $style) {
    return set($id, "textStyle", $style);
}

/**
 * Set gravity/alignment
 * @param string $id View ID
 * @param string $gravity "left" | "center" | "right" | "top" | "bottom"
 * @return array
 */
function setGravity(string $id, string $gravity) {
    return set($id, "gravity", $gravity);
}

/**
 * Set width of a view
 * @param string $id View ID
 * @param int|string $width Width in dp, or "match_parent" | "wrap_content"
 * @return array
 */
function setWidth(string $id, $width) {
    return set($id, "width", $width);
}

/**
 * Set height of a view
 * @param string $id View ID
 * @param int|string $height Height in dp, or "match_parent" | "wrap_content"
 * @return array
 */
function setHeight(string $id, $height) {
    return set($id, "height", $height);
}

/**
 * Set both width and height at once
 * @param string $id View ID
 * @param int|string $width Width
 * @param int|string $height Height
 * @return array
 */
function setDimensions(string $id, $width, $height) {
    return update($id, ["width" => $width, "height" => $height]);
}

/**
 * Set padding on all sides
 * @param string $id View ID
 * @param int $padding Padding in dp
 * @return array
 */
function setPadding(string $id, int $padding) {
    return set($id, "padding", $padding);
}

/**
 * Set padding on each side individually
 * @param string $id View ID
 * @param int $left Left padding
 * @param int $top Top padding
 * @param int $right Right padding
 * @param int $bottom Bottom padding
 * @return array
 */
function setPaddingAll(string $id, int $left, int $top, int $right, int $bottom) {
    return update($id, [
        "paddingLeft" => $left,
        "paddingTop" => $top,
        "paddingRight" => $right,
        "paddingBottom" => $bottom
    ]);
}

/**
 * Set margin on all sides
 * @param string $id View ID
 * @param int $margin Margin in dp
 * @return array
 */
function setMargin(string $id, int $margin) {
    return set($id, "margin", $margin);
}

/**
 * Set margin on each side individually
 * @param string $id View ID
 * @param int $left Left margin
 * @param int $top Top margin
 * @param int $right Right margin
 * @param int $bottom Bottom margin
 * @return array
 */
function setMarginAll(string $id, int $left, int $top, int $right, int $bottom) {
    return update($id, [
        "marginLeft" => $left,
        "marginTop" => $top,
        "marginRight" => $right,
        "marginBottom" => $bottom
    ]);
}

/**
 * Set corner radius (rounded corners)
 * @param string $id View ID
 * @param int $radius Radius in dp
 * @return array
 */
function setCornerRadius(string $id, int $radius) {
    return set($id, "cornerRadius", $radius);
}

/**
 * Set elevation (shadow)
 * @param string $id View ID
 * @param int $elevation Elevation in dp
 * @return array
 */
function setElevation(string $id, int $elevation) {
    return set($id, "elevation", $elevation);
}

/**
 * Set rotation angle
 * @param string $id View ID
 * @param float $degrees Rotation in degrees (0-360)
 * @return array
 */
function setRotation(string $id, float $degrees) {
    return set($id, "rotation", $degrees);
}

/**
 * Set rotation around X axis
 * @param string $id View ID
 * @param float $degrees Rotation in degrees
 * @return array
 */
function setRotationX(string $id, float $degrees) {
    return set($id, "rotationX", $degrees);
}

/**
 * Set rotation around Y axis
 * @param string $id View ID
 * @param float $degrees Rotation in degrees
 * @return array
 */
function setRotationY(string $id, float $degrees) {
    return set($id, "rotationY", $degrees);
}

/**
 * Set scale (both X and Y)
 * @param string $id View ID
 * @param float $scale Scale factor (1.0 = normal, 2.0 = double size)
 * @return array
 */
function setScale(string $id, float $scale) {
    return update($id, ["scaleX" => $scale, "scaleY" => $scale]);
}

/**
 * Set scale X
 * @param string $id View ID
 * @param float $scale Scale factor
 * @return array
 */
function setScaleX(string $id, float $scale) {
    return set($id, "scaleX", $scale);
}

/**
 * Set scale Y
 * @param string $id View ID
 * @param float $scale Scale factor
 * @return array
 */
function setScaleY(string $id, float $scale) {
    return set($id, "scaleY", $scale);
}

/**
 * Set translation X (move horizontally)
 * @param string $id View ID
 * @param float $translation Translation in dp
 * @return array
 */
function setTranslationX(string $id, float $translation) {
    return set($id, "translationX", $translation);
}

/**
 * Set translation Y (move vertically)
 * @param string $id View ID
 * @param float $translation Translation in dp
 * @return array
 */
function setTranslationY(string $id, float $translation) {
    return set($id, "translationY", $translation);
}

/**
 * Set position (translation X and Y)
 * @param string $id View ID
 * @param float $x X translation
 * @param float $y Y translation
 * @return array
 */
function setPosition(string $id, float $x, float $y) {
    return update($id, ["translationX" => $x, "translationY" => $y]);
}

/**
 * Set progress value for ProgressBar/SeekBar
 * @param string $id View ID
 * @param int $progress Current progress value
 * @return array
 */
function setProgress(string $id, int $progress) {
    return set($id, "progress", $progress);
}

/**
 * Set max value for ProgressBar/SeekBar
 * @param string $id View ID
 * @param int $max Maximum value
 * @return array
 */
function setMax(string $id, int $max) {
    return set($id, "max", $max);
}

/**
 * Set progress and max together
 * @param string $id View ID
 * @param int $progress Current progress
 * @param int $max Maximum value
 * @return array
 */
function setProgressWithMax(string $id, int $progress, int $max) {
    return update($id, ["progress" => $progress, "max" => $max]);
}

/**
 * Set minimum lines for text views
 * @param string $id View ID
 * @param int $lines Minimum number of lines
 * @return array
 */
function setMinLines(string $id, int $lines) {
    return set($id, "minLines", $lines);
}

/**
 * Set maximum lines for text views
 * @param string $id View ID
 * @param int $lines Maximum number of lines
 * @return array
 */
function setMaxLines(string $id, int $lines) {
    return set($id, "maxLines", $lines);
}

/**
 * Set single line mode
 * @param string $id View ID
 * @param bool $singleLine Whether single line only
 * @return array
 */
function setSingleLine(string $id, bool $singleLine = true) {
    return set($id, "singleLine", $singleLine);
}

/**
 * Set input type for EditText
 * @param string $id View ID
 * @param string $type "text" | "textPassword" | "number" | "phone" | "textMultiLine" | "textEmailAddress"
 * @return array
 */
function setInputType(string $id, string $type) {
    return set($id, "inputType", $type);
}

/**
 * Set image scale type
 * @param string $id ImageView ID
 * @param string $type "center" | "centerCrop" | "centerInside" | "fitCenter" | "fitXY"
 * @return array
 */
function setScaleType(string $id, string $type) {
    return set($id, "scaleType", $type);
}

/**
 * Set clickable state
 * @param string $id View ID
 * @param bool $clickable Whether view is clickable
 * @return array
 */
function setClickable(string $id, bool $clickable) {
    return set($id, "clickable", $clickable);
}

/**
 * Set focusable state
 * @param string $id View ID
 * @param bool $focusable Whether view can receive focus
 * @return array
 */
function setFocusable(string $id, bool $focusable) {
    return set($id, "focusable", $focusable);
}

/**
 * Set selected state
 * @param string $id View ID
 * @param bool $selected Whether view is selected
 * @return array
 */
function setSelected(string $id, bool $selected) {
    return set($id, "selected", $selected);
}

/**
 * Set tag/custom data on a view
 * @param string $id View ID
 * @param mixed $tag Tag value to store
 * @return array
 */
function setTag(string $id, $tag) {
    return set($id, "tag", $tag);
}

/**
 * Set text all caps
 * @param string $id View ID
 * @param bool $allCaps Whether text should be all uppercase
 * @return array
 */
function setAllCaps(string $id, bool $allCaps) {
    return set($id, "textAllCaps", $allCaps);
}

/**
 * Set letter spacing
 * @param string $id View ID
 * @param float $spacing Letter spacing (0.0 normal, 0.1 = slight spacing)
 * @return array
 */
function setLetterSpacing(string $id, float $spacing) {
    return set($id, "letterSpacing", $spacing);
}

/**
 * Set line spacing
 * @param string $id View ID
 * @param float $multiplier Line height multiplier (1.0 = normal, 1.5 = 150%)
 * @return array
 */
function setLineSpacing(string $id, float $multiplier) {
    return set($id, "lineSpacingMultiplier", $multiplier);
}

/**
 * Set border/stroke color
 * @param string $id View ID
 * @param string $color Border color hex
 * @return array
 */
function setBorderColor(string $id, string $color) {
    return set($id, "strokeColor", $color);
}

/**
 * Set border/stroke width
 * @param string $id View ID
 * @param int $width Border width in dp
 * @return array
 */
function setBorderWidth(string $id, int $width) {
    return set($id, "strokeWidth", $width);
}

/**
 * Set border with color and width
 * @param string $id View ID
 * @param string $color Border color hex
 * @param int $width Border width in dp
 * @return array
 */
function setBorder(string $id, string $color, int $width) {
    return update($id, ["strokeColor" => $color, "strokeWidth" => $width]);
}

/**
 * Request focus on a view
 * @param string $id View ID
 * @return array
 */
function focus(string $id) {
    return set($id, "requestFocus", true);
}

/**
 * Clear focus from a view
 * @param string $id View ID
 * @return array
 */
function clearFocus(string $id) {
    return set($id, "clearFocus", true);
}

// =============================================================================
// COMPOUND STYLE HELPERS - Apply multiple styles at once
// =============================================================================

/**
 * Style text with common properties at once
 * @param string $id View ID
 * @param array $style ['text' => '', 'color' => '', 'size' => 16, 'bold' => true]
 * @return array
 */
function styleText(string $id, array $style) {
    $props = [];
    if (isset($style['text'])) $props['text'] = $style['text'];
    if (isset($style['color'])) $props['textColor'] = $style['color'];
    if (isset($style['size'])) $props['textSize'] = $style['size'];
    if (isset($style['bold'])) $props['textStyle'] = $style['bold'] ? 'bold' : 'normal';
    if (isset($style['italic'])) $props['textStyle'] = $style['italic'] ? 'italic' : 'normal';
    if (isset($style['center'])) $props['gravity'] = 'center';
    if (isset($style['caps'])) $props['textAllCaps'] = $style['caps'];
    return update($id, $props);
}

/**
 * Style a view's appearance (background, corners, elevation)
 * @param string $id View ID
 * @param array $style ['background' => '#fff', 'corners' => 10, 'elevation' => 4]
 * @return array
 */
function styleView(string $id, array $style) {
    $props = [];
    if (isset($style['background'])) $props['backgroundColor'] = $style['background'];
    if (isset($style['corners'])) $props['cornerRadius'] = $style['corners'];
    if (isset($style['elevation'])) $props['elevation'] = $style['elevation'];
    if (isset($style['alpha'])) $props['alpha'] = $style['alpha'];
    if (isset($style['padding'])) $props['padding'] = $style['padding'];
    if (isset($style['margin'])) $props['margin'] = $style['margin'];
    return update($id, $props);
}

/**
 * Apply a transform (position, rotation, scale)
 * @param string $id View ID
 * @param array $transform ['x' => 0, 'y' => 0, 'rotation' => 0, 'scale' => 1.0]
 * @return array
 */
function transform(string $id, array $transform) {
    $props = [];
    if (isset($transform['x'])) $props['translationX'] = $transform['x'];
    if (isset($transform['y'])) $props['translationY'] = $transform['y'];
    if (isset($transform['rotation'])) $props['rotation'] = $transform['rotation'];
    if (isset($transform['scale'])) {
        $props['scaleX'] = $transform['scale'];
        $props['scaleY'] = $transform['scale'];
    }
    if (isset($transform['scaleX'])) $props['scaleX'] = $transform['scaleX'];
    if (isset($transform['scaleY'])) $props['scaleY'] = $transform['scaleY'];
    return update($id, $props);
}

/**
 * Reset all transforms to default
 * @param string $id View ID
 * @return array
 */
function resetTransform(string $id) {
    return update($id, [
        "translationX" => 0,
        "translationY" => 0,
        "rotation" => 0,
        "scaleX" => 1.0,
        "scaleY" => 1.0,
        "alpha" => 1.0
    ]);
}

// =============================================================================
// COLORS - Pre-defined color palette
// =============================================================================

class Colors {
    const PRIMARY = '#4ec9b0';
    const SUCCESS = '#4CAF50';
    const WARNING = '#FF9800';
    const DANGER = '#f44336';
    const INFO = '#2196F3';
    const PURPLE = '#9C27B0';
    const GRAY = '#607D8B';
    const DARK = '#1e1e1e';
    const LIGHT = '#f5f5f5';
    const WHITE = '#ffffff';
    const BLACK = '#000000';
    const TEXT = '#cccccc';
    const TEXT_MUTED = '#888888';
}

// =============================================================================
// PREBUILT TEMPLATES
// =============================================================================

/**
 * Create a login form
 * 
 * @param string $onLogin Method to call on login click
 * @param array $options Optional customization
 * @return VerticalLayout
 */
function loginForm(string $onLogin, array $options = []) {
    $title = $options['title'] ?? 'Login';
    $userLabel = $options['userLabel'] ?? 'Username';
    $passLabel = $options['passLabel'] ?? 'Password';
    $buttonText = $options['buttonText'] ?? 'Sign In';
    
    return page($title, [
        input("username", $userLabel),
        spacer(10),
        input("password", $passLabel, ['password' => true]),
        spacer(20),
        button($buttonText, $onLogin, ['color' => Colors::PRIMARY]),
    ]);
}

/**
 * Create a simple list display
 * 
 * @param array $items Array of strings to display
 * @param string|null $onItemClick Method to call on item click
 * @return VerticalLayout
 */
function simpleList(array $items, ?string $onItemClick = null) {
    $elements = [];
    foreach ($items as $index => $item) {
        $lbl = label("• " . $item, ['padding' => 10]);
        if ($onItemClick) {
            $lbl->action($onItemClick)->tag($index);
        }
        $elements[] = $lbl;
        $elements[] = divider();
    }
    return (new VerticalLayout($elements))->padding(10);
}

/**
 * Create a confirmation dialog layout
 * 
 * @param string $message Question to ask
 * @param string $onYes Method for Yes button
 * @param string $onNo Method for No button
 * @return VerticalLayout
 */
function confirmDialog(string $message, string $onYes, string $onNo) {
    return page("Confirm", [
        label($message, ['size' => 18, 'center' => true, 'padding' => 30]),
        spacer(20),
        row([
            button("Yes", $onYes, ['color' => Colors::SUCCESS]),
            button("No", $onNo, ['color' => Colors::DANGER]),
        ])
    ]);
}

/**
 * Create a status display with icon
 * 
 * @param string $id Unique ID for updates
 * @param string $initialText Initial text
 * @param string $icon Emoji icon
 * @return TextView
 */
function statusLabel(string $id, string $initialText = "Ready", string $icon = "ℹ️") {
    return label("$icon $initialText", [
        'id' => $id,
        'size' => 16,
        'color' => Colors::TEXT_MUTED,
        'padding' => 15
    ]);
}

// =============================================================================
// UTILITY HELPERS
// =============================================================================

/**
 * Format a number with thousands separator
 */
function formatNumber($num) {
    return number_format($num, 0, '.', ',');
}

/**
 * Format currency
 */
function formatMoney($amount, $symbol = '$') {
    return $symbol . number_format($amount, 2);
}

/**
 * Check if string is empty or whitespace only
 */
function isEmpty($str) {
    return empty(trim($str ?? ''));
}

/**
 * Get compass direction from azimuth
 */
function compassDirection($azimuth) {
    $directions = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'];
    return $directions[round($azimuth / 45) % 8];
}

?>
