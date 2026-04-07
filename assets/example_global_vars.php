<?php
/**
 * Example: Using Global Variables for UI Elements
 * 
 * This approach lets you define UI elements as global variables,
 * then access them from anywhere in your code using object methods.
 * 
 * KEY CONCEPT:
 * - Methods like ->text(), ->id(), ->backgroundColor() SET ATTRIBUTES (go to XML)
 * - Methods prefixed with _ like ->_setText(), ->_hide() return ACTIONS (NOT in XML)
 * 
 * WHY underscore prefix?
 * - Clearly distinguishes action methods from attribute setters
 * - _setText() returns an action array to update the view
 * - setText() would be an attribute setter (not what we want for updates)
 */

require_once 'simple.php';

// =============================================================================
// DEFINE GLOBAL UI ELEMENTS
// These are defined once and can be accessed from any function
// =============================================================================

// Labels
$titleLabel = (new TextView())
    ->id("title")
    ->text("My App")
    ->textSize(28)
    ->textColor(Colors::PRIMARY)
    ->gravity("center");

$statusLabel = (new TextView())
    ->id("status")
    ->text("Ready")
    ->textSize(16)
    ->textColor(Colors::TEXT_MUTED)
    ->padding(10);

$counterLabel = (new TextView())
    ->id("counter")
    ->text("Count: 0")
    ->textSize(20)
    ->textColor(Colors::WHITE)
    ->gravity("center");

// Buttons
$incrementBtn = (new Button())
    ->id("btn_increment")
    ->text("+ Increment")
    ->action("onIncrement")
    ->backgroundColor(Colors::SUCCESS)
    ->textAllCaps(false);

$decrementBtn = (new Button())
    ->id("btn_decrement")
    ->text("- Decrement")
    ->action("onDecrement")
    ->backgroundColor(Colors::DANGER)
    ->textAllCaps(false);

$resetBtn = (new Button())
    ->id("btn_reset")
    ->text("Reset")
    ->action("onReset")
    ->backgroundColor(Colors::GRAY)
    ->textAllCaps(false);

// Input fields
$nameInput = (new EditText())
    ->id("input_name")
    ->hint("Enter your name")
    ->padding(10);

$emailInput = (new EditText())
    ->id("input_email")
    ->hint("Enter your email")
    ->inputType("textEmailAddress")
    ->padding(10);

// Checkbox
$agreeCheckbox = (new CheckBox())
    ->id("chk_agree")
    ->text("I agree to the terms")
    ->onCheckedChange("onAgreeChanged");

// Image
$profileImage = (new ImageView())
    ->id("profile_img")
    ->src("https://via.placeholder.com/100")
    ->width(100)
    ->height(100)
    ->scaleType("centerCrop");

// Progress bar
$progressBar = (new ProgressBar())
    ->id("progress")
    ->max(100)
    ->progress(0);

// Hidden panel (shown/hidden dynamically)
$detailsPanel = (new VerticalLayout([
    (new TextView())->id("details_text")->text("Hidden details here"),
]))
    ->id("details_panel")
    ->backgroundColor(Colors::DARK)
    ->padding(15)
    ->cornerRadius(10)
    ->visibility("gone");  // Start hidden

// =============================================================================
// APP CLASS
// =============================================================================

class MyApp {
    private $count = 0;
    
    /**
     * Main screen - uses the global UI elements
     */
    public function index() {
        global $titleLabel, $statusLabel, $counterLabel;
        global $incrementBtn, $decrementBtn, $resetBtn;
        global $nameInput, $emailInput, $agreeCheckbox;
        global $profileImage, $progressBar, $detailsPanel;
        
        return (new VerticalLayout([
            $titleLabel,
            spacer(10),
            $statusLabel,
            divider(),
            
            // Counter section
            card("Counter Demo", [
                $counterLabel,
                spacer(10),
                row([$incrementBtn, $decrementBtn]),
                $resetBtn,
            ]),
            
            spacer(10),
            
            // Form section
            card("Form Demo", [
                $nameInput,
                $emailInput,
                $agreeCheckbox,
                spacer(10),
                button("Submit", "onSubmit", ['color' => Colors::PRIMARY]),
                button("Get Form Data", "onGetFormData", ['color' => Colors::INFO]),
            ]),
            
            spacer(10),
            
            // Visibility demo
            card("Visibility Demo", [
                button("Toggle Details", "onToggleDetails", ['color' => Colors::PURPLE]),
                $detailsPanel,
            ]),
            
            spacer(10),
            
            // Progress demo
            card("Progress Demo", [
                $progressBar,
                row([
                    button("+10%", "onProgressUp"),
                    button("-10%", "onProgressDown"),
                ]),
            ]),
            
        ]))->padding(20);
    }
    
    // =========================================================================
    // COUNTER HANDLERS - Using global variable methods
    // =========================================================================
    
    public function onIncrement($params) {
        global $counterLabel, $statusLabel;
        
        // Access instance counter (would need session/storage in real app)
        static $count = 0;
        $count++;
        
        // Use the object's _setText method to return an update action
        // The underscore prefix means this returns an ACTION, not an attribute
        return updateMany([
            "counter" => ["text" => "Count: $count"],
            "status" => ["text" => "Incremented!", "textColor" => Colors::SUCCESS]
        ]);
        
        // Alternative: Use the object method directly
        // return $counterLabel->_setText("Count: $count");
    }
    
    public function onDecrement($params) {
        global $counterLabel, $statusLabel;
        
        static $count = 0;
        $count--;
        
        // Using the global variable's action method
        return $counterLabel->_setText("Count: $count", Colors::WARNING);
    }
    
    public function onReset($params) {
        global $counterLabel, $statusLabel;
        
        // Return multiple actions using updateMany
        return updateMany([
            "counter" => ["text" => "Count: 0", "textColor" => Colors::WHITE],
            "status" => ["text" => "Reset!", "textColor" => Colors::INFO]
        ]);
    }
    
    // =========================================================================
    // FORM HANDLERS
    // =========================================================================
    
    public function onSubmit($params) {
        global $nameInput, $statusLabel;
        
        // Get the input value using the object's _getText method
        // This returns an action that triggers a callback
        return $nameInput->_getText("onGotName");
    }
    
    public function onGotName($params) {
        global $statusLabel;
        $name = $params['value'] ?? '';
        
        if (empty(trim($name))) {
            return $statusLabel->_setText("⚠️ Please enter a name", Colors::WARNING);
        }
        
        return $statusLabel->_setText("✅ Hello, $name!", Colors::SUCCESS);
    }
    
    public function onGetFormData($params) {
        global $nameInput, $emailInput, $agreeCheckbox;
        
        // Get multiple values at once
        return getViewProperties([
            ["viewId" => $nameInput->getId(), "property" => "text"],
            ["viewId" => $emailInput->getId(), "property" => "text"],
            ["viewId" => $agreeCheckbox->getId(), "property" => "checked"],
        ], "onFormDataReceived");
    }
    
    public function onFormDataReceived($params) {
        global $statusLabel;
        
        $results = $params['results'] ?? [];
        $info = [];
        foreach ($results as $r) {
            $info[] = "{$r['viewId']}: {$r['value']}";
        }
        
        return alert(implode("\n", $info), "Form Data");
    }
    
    public function onAgreeChanged($params) {
        global $statusLabel;
        $checked = $params['isChecked'] ?? false;
        
        return $statusLabel->_setText(
            $checked ? "✅ Terms accepted" : "❌ Terms not accepted",
            $checked ? Colors::SUCCESS : Colors::WARNING
        );
    }
    
    // =========================================================================
    // VISIBILITY HANDLERS
    // =========================================================================
    
    public function onToggleDetails($params) {
        global $detailsPanel;
        
        // Get current visibility state
        return $detailsPanel->_getVisibility("onGotVisibility");
    }
    
    public function onGotVisibility($params) {
        global $detailsPanel, $statusLabel;
        
        $visible = ($params['value'] ?? 'gone') === 'visible';
        
        // Toggle visibility using object methods
        if ($visible) {
            return $detailsPanel->_hide();
        } else {
            return $detailsPanel->_show();
        }
    }
    
    // =========================================================================
    // PROGRESS HANDLERS
    // =========================================================================
    
    public function onProgressUp($params) {
        global $progressBar;
        return $progressBar->_getProgress("onGotProgressUp");
    }
    
    public function onGotProgressUp($params) {
        global $progressBar;
        $current = intval($params['value'] ?? 0);
        $newProgress = min(100, $current + 10);
        return $progressBar->_setProgress($newProgress);
    }
    
    public function onProgressDown($params) {
        global $progressBar;
        return $progressBar->_getProgress("onGotProgressDown");
    }
    
    public function onGotProgressDown($params) {
        global $progressBar;
        $current = intval($params['value'] ?? 0);
        $newProgress = max(0, $current - 10);
        return $progressBar->_setProgress($newProgress);
    }
}

/**
 * SUMMARY OF THE PATTERN:
 * 
 * 1. DEFINE GLOBAL VARIABLES at the top of the file:
 *    $myLabel = (new TextView())->id("my_label")->text("Hello");
 * 
 * 2. USE THEM IN YOUR CLASS with 'global' keyword:
 *    public function myMethod() {
 *        global $myLabel;
 *        return $myLabel->_setText("New text");
 *    }
 * 
 * 3. ATTRIBUTE vs ACTION methods:
 *    
 *    ATTRIBUTES (go to XML, used when building UI):
 *    $myLabel->text("Hello")           // Sets text attribute
 *    $myLabel->textColor("#fff")       // Sets color attribute
 *    $myLabel->id("my_id")             // Sets id attribute
 *    
 *    ACTIONS (return array, used for runtime updates):
 *    $myLabel->_setText("New")         // Returns update action
 *    $myLabel->_getText("callback")    // Returns get action
 *    $myLabel->_hide()                 // Returns visibility action
 *    $myLabel->_setBackground("#000")  // Returns update action
 * 
 * 4. The underscore _ prefix is the key distinction:
 *    - Without _ : Sets an attribute on the object (for XML serialization)
 *    - With _    : Returns an action array (for runtime updates)
 * 
 * 5. Available action methods (all prefixed with _):
 *    
 *    Universal:
 *    _get($prop, $callback), _set($prop, $value), _update($props)
 *    
 *    Text:
 *    _getText, _setText, _setTextColor, _setTextSize, _setTextStyle
 *    _setGravity, _setHint, _clear, _setAllCaps, _setLetterSpacing
 *    
 *    Appearance:
 *    _setBackground, _setCornerRadius, _setElevation, _setBorder
 *    
 *    Visibility:
 *    _show, _hide, _invisible, _setVisibility, _getVisibility
 *    
 *    State:
 *    _enable, _disable, _setEnabled, _setClickable, _focus, _clearFocus
 *    
 *    Alpha:
 *    _setAlpha, _getAlpha, _fadeIn, _fadeOut
 *    
 *    Dimensions:
 *    _setWidth, _setHeight, _setDimensions, _setPadding, _setMargin
 *    
 *    Transforms:
 *    _setRotation, _setScale, _setPosition, _transform, _resetTransform
 *    
 *    Checkbox/Switch:
 *    _getChecked, _setChecked, _check, _uncheck, _toggle
 *    
 *    Progress:
 *    _getProgress, _setProgress, _setMax, _setProgressWithMax
 *    
 *    Image:
 *    _getImage, _setImage, _setScaleType
 *    
 *    Compound:
 *    _styleText(['text'=>'', 'color'=>'', 'size'=>16, 'bold'=>true])
 *    _styleView(['background'=>'', 'corners'=>10, 'elevation'=>4])
 */
?>
