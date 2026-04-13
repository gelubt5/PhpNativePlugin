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
 * Create a toggle switch
 * 
 * @param string $id Unique ID
 * @param string $text Label text
 * @param bool $checked Initial state
 * @param string|null $onChange Method to call when toggled
 * @return SwitchView
 * 
 * Example:
 *   toggle("dark_mode", "Dark Mode", false, "onDarkModeToggle")
 */
function toggle(string $id, string $text, bool $checked = false, ?string $onChange = null) {
    $sw = (new SwitchView())
        ->id($id)
        ->text($text)
        ->checked($checked);
    
    if ($onChange) {
        $sw->onCheckedChange($onChange);
    }
    
    return $sw;
}


/**
 * Create a ToggleButton
 * 
 * @param string $id Unique ID
 * @param string $textOn Text when ON
 * @param string $textOff Text when OFF
 * @param bool $checked Initial state
 * @param string|null $onChange Method to call when toggled
 * @return ToggleButton
 */
function toggleButton(string $id, string $textOn = "ON", string $textOff = "OFF", bool $checked = false, ?string $onChange = null) {
    $tb = (new ToggleButton())
        ->id($id)
        ->textOn($textOn)
        ->textOff($textOff)
        ->checked($checked);
    
    if ($onChange) {
        $tb->onCheckedChange($onChange);
    }
    
    return $tb;
}

/**
 * Create a radio button group
 * 
 * @param string $id Group ID
 * @param array $options Array of ["id" => "opt1", "text" => "Option 1"] or simple strings
 * @param string|null $onChange Method to call when selection changes
 * @param string|null $selected Initially selected option ID
 * @return RadioGroup
 * 
 * Example:
 *   radioGroup("size", ["Small", "Medium", "Large"], "onSizeChange")
 *   radioGroup("color", [
 *       ["id" => "red", "text" => "Red"],
 *       ["id" => "blue", "text" => "Blue"],
 *   ], "onColorChange", "red")
 */
function radioGroup(string $id, array $options, ?string $onChange = null, ?string $selected = null) {
    $buttons = [];
    foreach ($options as $index => $option) {
        if (is_string($option)) {
            $rb = (new RadioButton())
                ->id($id . "_" . $index)
                ->text($option);
            if ($selected !== null && $selected == $id . "_" . $index) {
                $rb->checked(true);
            }
        } else {
            $optId = $option['id'] ?? $id . "_" . $index;
            $rb = (new RadioButton())
                ->id($optId)
                ->text($option['text'] ?? "Option $index");
            if ($selected !== null && $selected == $optId) {
                $rb->checked(true);
            }
        }
        $buttons[] = $rb;
    }
    
    $group = (new RadioGroup($buttons))->id($id);
    if ($onChange) {
        $group->onCheckedChange($onChange);
    }
    
    return $group;
}

/**
 * Create a rating bar (stars)
 * 
 * @param string $id Unique ID
 * @param float $rating Initial rating
 * @param int $numStars Number of stars
 * @param string|null $onChange Method to call when rating changes
 * @return RatingBar
 * 
 * Example:
 *   rating("product_rating", 3.5, 5, "onRatingChange")
 */
function rating(string $id, float $rating = 0, int $numStars = 5, ?string $onChange = null) {
    $rb = (new RatingBar())
        ->id($id)
        ->numStars($numStars)
        ->rating($rating)
        ->stepSize(0.5);
    
    if ($onChange) {
        $rb->onRatingBarChange($onChange);
    }
    
    return $rb;
}

/**
 * Create a seek bar (slider)
 * 
 * @param string $id Unique ID
 * @param int $progress Initial progress value
 * @param int $max Maximum value
 * @param string|null $onChange Method to call when value changes
 * @return SeekBar
 * 
 * Example:
 *   seekbar("volume", 50, 100, "onVolumeChange")
 */
function seekbar(string $id, int $progress = 0, int $max = 100, ?string $onChange = null) {
    $sb = (new SeekBar())
        ->id($id)
        ->progress($progress)
        ->max($max);
    
    if ($onChange) {
        $sb->onSeekBarChange($onChange);
    }
    
    return $sb;
}

/**
 * Create a progress bar
 * 
 * @param string $id Unique ID
 * @param int $progress Initial progress (0-100)
 * @param int $max Maximum value
 * @return ProgressBar
 * 
 * Example:
 *   progress("download", 45)
 */
function progress(string $id, int $progress = 0, int $max = 100) {
    return (new ProgressBar())
        ->id($id)
        ->progress($progress)
        ->max($max);
}

/**
 * Create a dropdown spinner (select box)
 * 
 * @param string $id Unique ID
 * @param array $items Array of string options
 * @param string|null $onChange Method to call when selection changes
 * @param int $selectedIndex Initially selected index
 * @return Spinner
 * 
 * Example:
 *   spinner("country", ["USA", "UK", "Canada"], "onCountryChange")
 */
function spinner(string $id, array $items, ?string $onChange = null, int $selectedIndex = 0) {
    $sp = (new Spinner())
        ->id($id)
        ->items($items)
        ->selectedPosition($selectedIndex);
    
    if ($onChange) {
        $sp->onItemSelected($onChange);
    }
    
    return $sp;
}

/**
 * Create a number picker
 * 
 * @param string $id Unique ID
 * @param int $min Minimum value
 * @param int $max Maximum value
 * @param int $value Initial value
 * @param string|null $onChange Method to call when value changes
 * @return NumberPicker
 * 
 * Example:
 *   numberPicker("age", 1, 120, 25, "onAgeChange")
 */
function numberPicker(string $id, int $min = 0, int $max = 100, int $value = 0, ?string $onChange = null) {
    $np = (new NumberPicker())
        ->id($id)
        ->minValue($min)
        ->maxValue($max)
        ->value($value);
    
    if ($onChange) {
        $np->onValueChange($onChange);
    }
    
    return $np;
}

/**
 * Create an auto-complete text input
 * 
 * @param string $id Unique ID
 * @param string $hint Placeholder text
 * @param array $suggestions Auto-complete suggestions
 * @param string|null $onChange Method to call when text changes
 * @return AutoCompleteTextView
 * 
 * Example:
 *   autoComplete("city", "Enter city", ["New York", "London", "Tokyo"])
 */
function autoComplete(string $id, string $hint, array $suggestions, ?string $onChange = null) {
    $ac = (new AutoCompleteTextView())
        ->id($id)
        ->hint($hint)
        ->suggestions($suggestions)
        ->completionThreshold(1);
    
    if ($onChange) {
        $ac->onItemClick($onChange);
    }
    
    return $ac;
}

/**
 * Create a search view / search bar
 * 
 * @param string $id Unique ID
 * @param string $hint Search hint text
 * @param string|null $onSearch Method to call when search is submitted
 * @param string|null $onTextChange Method to call on each keystroke
 * @return SearchView
 * 
 * Example:
 *   searchBar("search", "Search products...", "onSearch", "onSearchType")
 */
function searchBar(string $id, string $hint = "Search...", ?string $onSearch = null, ?string $onTextChange = null) {
    $sv = (new SearchView())
        ->id($id)
        ->queryHint($hint)
        ->iconifiedByDefault(false);
    
    if ($onSearch) {
        $sv->onQueryTextSubmit($onSearch);
    }
    if ($onTextChange) {
        $sv->onQueryTextChange($onTextChange);
    }
    
    return $sv;
}

/**
 * Create a Material-style text input with floating label
 * 
 * @param string $id Unique ID
 * @param string $hint Label / hint text
 * @param array $options ['helperText', 'error', 'counter', 'maxLength', 'inputType']
 * @return TextInputLayout
 * 
 * Example:
 *   textField("email", "Email Address", ['helperText' => 'Required', 'inputType' => 'textEmailAddress'])
 */
function textField(string $id, string $hint, array $options = []) {
    $til = (new TextInputLayout())
        ->id($id)
        ->hint($hint);
    
    if (isset($options['helperText'])) $til->helperText($options['helperText']);
    if (isset($options['error'])) $til->errorText($options['error']);
    if (isset($options['counter']) && $options['counter']) {
        $til->counterEnabled(true);
        if (isset($options['maxLength'])) $til->counterMaxLength($options['maxLength']);
    }
    if (isset($options['inputType'])) $til->inputType($options['inputType']);
    
    return $til;
}

/**
 * Create a floating action button (FAB)
 * 
 * @param string $id Unique ID
 * @param string $icon Icon name (e.g., "add", "edit", "check")
 * @param string $action Method to call when clicked
 * @param array $style ['color' => '#FF4081', 'iconColor' => '#fff']
 * @return FloatingActionButton
 * 
 * Example:
 *   fab("add_btn", "add", "onAddItem")
 */
function fab(string $id, string $icon, string $action, array $style = []) {
    $f = (new FloatingActionButton())
        ->id($id)
        ->icon($icon)
        ->action($action);
    
    if (isset($style['color'])) $f->backgroundColor($style['color']);
    if (isset($style['iconColor'])) $f->iconColor($style['iconColor']);
    
    return $f;
}

/**
 * Create a chip tag
 * 
 * @param string $text Chip text
 * @param string|null $action Method to call when clicked
 * @param array $style ['color' => '#E0E0E0', 'textColor' => '#333']
 * @return Chip
 * 
 * Example:
 *   chip("Android", "onChipClick")
 */
function chip(string $text, ?string $action = null, array $style = []) {
    $c = (new Chip())->text($text);
    
    if ($action) $c->action($action);
    if (isset($style['color'])) $c->backgroundColor($style['color']);
    if (isset($style['textColor'])) $c->textColor($style['textColor']);
    if (isset($style['id'])) $c->id($style['id']);
    
    return $c;
}

/**
 * Create a group of chips
 * 
 * @param array $chips Array of chip() results or strings
 * @param string|null $onSelect Handler for selection
 * @return ChipGroup
 * 
 * Example:
 *   chipGroup(["PHP", "Java", "Python"])
 *   chipGroup([chip("Tag1"), chip("Tag2")])
 */
function chipGroup(array $chips, ?string $onSelect = null) {
    $children = [];
    foreach ($chips as $c) {
        if (is_string($c)) {
            $ch = chip($c, $onSelect);
            $children[] = $ch;
        } else {
            $children[] = $c;
        }
    }
    
    return new ChipGroup($children);
}

/**
 * Create a tab bar
 * 
 * @param string $id Unique ID
 * @param array $tabs Array of tab definitions: ["Home", "Profile"] or [["text" => "Home", "icon" => "home"], ...]
 * @param string $onSelect Method to call when tab changes
 * @param int $selected Initially selected tab index
 * @return TabLayout
 * 
 * Example:
 *   tabs("my_tabs", ["Home", "Search", "Profile"], "onTabChange")
 */
function tabs(string $id, array $tabs, ?string $onSelect = null, int $selected = 0) {
    $tabItems = [];
    foreach ($tabs as $tab) {
        if (is_string($tab)) {
            $tabItems[] = ["text" => $tab];
        } else {
            $tabItems[] = $tab;
        }
    }
    
    $tl = (new TabLayout())
        ->id($id)
        ->tabs($tabItems)
        ->selectedTab($selected);
    
    if ($onSelect) {
        $tl->onTabSelected($onSelect);
    }
    
    return $tl;
}

/**
 * Create a Material card container
 * 
 * @param array $content Array of child components
 * @param array $style ['background' => '#fff', 'corners' => 12, 'elevation' => 4]
 * @return CardView
 * 
 * Example:
 *   materialCard([
 *       label("Card Title", ['bold' => true]),
 *       label("Some content here"),
 *       button("Action", "onAction"),
 *   ], ['corners' => 16, 'elevation' => 8])
 */
function materialCard(array $content, array $style = []) {
    $card = (new CardView($content))
        ->cornerRadius($style['corners'] ?? 12)
        ->elevation($style['elevation'] ?? 4)
        ->padding($style['padding'] ?? 16)
        ->margin($style['margin'] ?? 8);
    
    if (isset($style['background'])) $card->backgroundColor($style['background']);
    
    return $card;
}

/**
 * Create a grid layout
 * 
 * @param array $children Array of child components
 * @param int $columns Number of columns
 * @return GridLayout
 * 
 * Example:
 *   grid([
 *       button("1", "onClick"), button("2", "onClick"),
 *       button("3", "onClick"), button("4", "onClick"),
 *   ], 2)
 */
function grid(array $children, int $columns = 2) {
    $layout = (new GridLayout($children))->columnCount($columns);
    return $layout;
}

/**
 * Create a table layout
 * 
 * @param array $rows Array of arrays (each sub-array is a row of components/strings)
 * @param array $style ['headerBg' => '#333', 'headerColor' => '#fff']
 * @return TableLayout
 * 
 * Example:
 *   table([
 *       ["Name", "Age", "City"],      // header row
 *       ["Alice", "30", "NYC"],
 *       ["Bob", "25", "London"],
 *   ], ['headerBg' => '#1976D2', 'headerColor' => '#fff'])
 */
function table(array $rows, array $style = []) {
    $tableRows = [];
    foreach ($rows as $rowIndex => $row) {
        $cells = [];
        foreach ($row as $cell) {
            if ($cell instanceof Component) {
                $cells[] = $cell;
            } else {
                $tv = (new TextView())
                    ->text((string) $cell)
                    ->padding(8);
                
                // Style header row differently
                if ($rowIndex === 0 && isset($style['headerColor'])) {
                    $tv->textColor($style['headerColor'])->textStyle("bold");
                }
                
                $cells[] = $tv;
            }
        }
        
        $tr = new TableRow($cells);
        if ($rowIndex === 0 && isset($style['headerBg'])) {
            $tr->backgroundColor($style['headerBg']);
        }
        $tableRows[] = $tr;
    }
    
    return (new TableLayout($tableRows))->stretchColumns("*");
}

/**
 * Create a video player
 * 
 * @param string $id Unique ID
 * @param string $uri Video URI (URL or file path)
 * @param array $options ['autoPlay' => false, 'width' => -1, 'height' => 200]
 * @return VideoView
 */
function video(string $id, string $uri, array $options = []) {
    $vv = (new VideoView())
        ->id($id)
        ->videoUri($uri);
    
    if (isset($options['autoPlay'])) $vv->autoPlay($options['autoPlay']);
    if (isset($options['width'])) $vv->width($options['width']);
    if (isset($options['height'])) $vv->height($options['height']);
    
    return $vv;
}

/**
 * Create a web view
 * 
 * @param string $id Unique ID
 * @param string $url URL to load
 * @param array $options ['height' => 400, 'javaScriptEnabled' => true]
 * @return WebView
 * 
 * Example:
 *   webView("browser", "https://example.com")
 */
function webView(string $id, string $url, array $options = []) {
    $wv = (new WebView())
        ->id($id)
        ->loadUrl($url);
    
    if (isset($options['height'])) $wv->height($options['height']);
    if (isset($options['javaScriptEnabled'])) {
        $wv->settings(["javaScriptEnabled" => $options['javaScriptEnabled']]);
    }
    
    return $wv;
}

/**
 * Create a calendar view
 * 
 * @param string $id Unique ID
 * @param string|null $onChange Method to call when date changes
 * @return CalendarView
 */
function calendar(string $id, ?string $onChange = null) {
    $cv = (new CalendarView())->id($id);
    if ($onChange) $cv->onDateChange($onChange);
    return $cv;
}

/**
 * Create a scroll view wrapper
 * 
 * @param array $content Array of child components
 * @return ScrollView
 */
function scrollView(array $content) {
    return new ScrollView([
        new VerticalLayout($content)
    ]);
}

/**
 * Create a horizontal scroll wrapper
 * 
 * @param array $content Array of child components 
 * @return HorizontalScrollView
 */
function horizontalScroll(array $content) {
    return new HorizontalScrollView([
        (new HorizontalLayout($content))
    ]);
}

/**
 * Create a frame/stack layout where children overlap
 * 
 * @param array $content Array of child components
 * @return StackLayout
 * 
 * Example:
 *   stack([
 *       image("bg.jpg", ['width' => -1, 'height' => -1]),
 *       label("Overlay Text", ['center' => true]),
 *   ])
 */
function stack(array $content) {
    return new StackLayout($content);
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
function goToScreen(string $screen, array $data = []) {
    return [
        "action" => "NAVIGATE",
        "screen" => $screen,
        "data" => $data
    ];
}

/**
 * Go back to the previous screen in navigation history.
 * If at the root screen, shows an exit confirmation dialog.
 * 
 * @return array
 * 
 * Example:
 *   return goBack();
 */
function goBack() {
    return [
        "action" => "GO_BACK"
    ];
}

// =============================================================================
// DIALOGS & POPUPS - Rich dialog builders
// =============================================================================

/**
// =============================================================================
// DIALOGS & POPUPS - Rich dialog builders
// =============================================================================

// Note: snackbar(), dialog(), listDialog(), datePickerDialog(), timePickerDialog(),
// inputDialog(), bottomSheet(), dismissDialog() are already available from ui_core.php.
// These wrappers provide simpler, more intuitive names.

/**
 * Show a confirmation dialog with Yes/No buttons
 * 
 * @param string $title Dialog title
 * @param string $message Dialog message
 * @param string $onConfirm Method to call on "Yes"
 * @param string|null $onCancel Method to call on "No" (optional)
 * @param string $confirmText Positive button text
 * @param string $cancelText Negative button text
 * @return array
 * 
 * Example:
 *   return confirm("Delete?", "This cannot be undone.", "doDelete");
 */
function confirm(string $title, string $message, string $onConfirm, ?string $onCancel = null, string $confirmText = "Yes", string $cancelText = "No") {
    return dialog($title, $message, $onConfirm, $onCancel, $confirmText, $cancelText);
}

/**
 * Show a selection list dialog
 * 
 * @param string $title Dialog title
 * @param array $items List of string items to choose from
 * @param string $onSelect Method to call with selected item (receives index and text)
 * @return array
 * 
 * Example:
 *   return selectDialog("Pick a color", ["Red", "Green", "Blue"], "onColorPick");
 */
function selectDialog(string $title, array $items, string $onSelect) {
    return listDialog($title, $items, $onSelect);
}

/**
 * Show a date picker dialog
 * 
 * @param string $callback Method to receive the date (year, month, day)
 * @param string|null $initialDate Initial date as "YYYY-MM-DD"
 * @return array
 * 
 * Example:
 *   return pickDate("onDatePicked");
 */
function pickDate(string $callback, ?string $initialDate = null) {
    return datePickerDialog($callback, $initialDate);
}

/**
 * Show a time picker dialog
 * 
 * @param string $callback Method to receive the time (hour, minute)
 * @param bool $is24Hour Use 24-hour format
 * @return array
 * 
 * Example:
 *   return pickTime("onTimePicked");
 */
function pickTime(string $callback, bool $is24Hour = false) {
    return timePickerDialog($callback, $is24Hour);
}

/**
 * Show an input dialog (text prompt)
 * 
 * @param string $title Dialog title
 * @param string $hint Input hint text
 * @param string $callback Method to receive the entered text
 * @param string $defaultValue Pre-filled value
 * @return array
 * 
 * Example:
 *   return prompt("Enter Name", "Your name...", "onNameEntered");
 */
function prompt(string $title, string $hint, string $callback, string $defaultValue = "") {
    return inputDialog($title, $hint, $callback, $defaultValue);
}

/**
 * Show a bottom sheet with custom content
 * 
 * @param array $content Array of child components
 * @param string|null $title Optional title
 * @return array
 * 
 * Example:
 *   return showBottomSheet([
 *       label("Choose an option"),
 *       button("Option A", "onPickA"),
 *       button("Option B", "onPickB"),
 *   ], "Options");
 */
function showBottomSheet(array $content, ?string $title = null) {
    $children = $content;
    if ($title) {
        array_unshift($children, label($title, ['bold' => true, 'size' => 20, 'padding' => 8]));
    }
    $layout = (new VerticalLayout($children))->padding(16);
    return bottomSheet($layout);
}

/**
 * Close any open dialog/bottom sheet
 * @return array
 */
function closeDialog() {
    return dismissDialog();
}

// =============================================================================
// ANIMATION - Animate views
// =============================================================================

// Note: animate(), animateSet() are already available from ui_core.php.
// These wrappers provide single-property convenience and preset animations.

/**
 * Animate a single property of a view
 * 
 * @param string $id View ID to animate
 * @param string $property Property to animate (alpha, translationX, translationY, scaleX, scaleY, rotation)
 * @param float $toValue Target value
 * @param int $duration Duration in milliseconds
 * @param string $interpolator Interpolator type (linear, accelerate, decelerate, bounce, overshoot)
 * @return array
 * 
 * Example:
 *   return animateView("my_view", "alpha", 0.5, 500);
 */
function animateView(string $id, string $property, float $toValue, int $duration = 300, string $interpolator = "decelerate") {
    return animate($id, [$property => $toValue], $duration, $interpolator);
}

/**
 * Fade in a view
 * 
 * @param string $id View ID
 * @param int $duration Duration in ms
 * @return array
 */
function fadeIn(string $id, int $duration = 300) {
    return animate($id, ["alpha" => 1.0], $duration);
}

/**
 * Fade out a view
 * 
 * @param string $id View ID
 * @param int $duration Duration in ms
 * @return array
 */
function fadeOut(string $id, int $duration = 300) {
    return animate($id, ["alpha" => 0.0], $duration);
}

/**
 * Slide a view horizontally
 * 
 * @param string $id View ID
 * @param float $toX Target X translation in pixels
 * @param int $duration Duration in ms
 * @return array
 */
function slideX(string $id, float $toX, int $duration = 300) {
    return animate($id, ["translationX" => $toX], $duration);
}

/**
 * Slide a view vertically
 * 
 * @param string $id View ID
 * @param float $toY Target Y translation in pixels
 * @param int $duration Duration in ms
 * @return array
 */
function slideY(string $id, float $toY, int $duration = 300) {
    return animate($id, ["translationY" => $toY], $duration);
}

/**
 * Scale a view uniformly
 * 
 * @param string $id View ID
 * @param float $scale Scale factor (1.0 = normal)
 * @param int $duration Duration in ms
 * @return array
 */
function scaleView(string $id, float $scale, int $duration = 300) {
    return animate($id, ["scaleX" => $scale, "scaleY" => $scale], $duration);
}

/**
 * Rotate a view
 * 
 * @param string $id View ID
 * @param float $degrees Rotation angle in degrees
 * @param int $duration Duration in ms
 * @return array
 */
function rotateView(string $id, float $degrees, int $duration = 300) {
    return animate($id, ["rotation" => $degrees], $duration);
}

/**
 * Run a "bounce" animation - scale up then back
 * 
 * @param string $id View ID
 * @param int $duration Duration in ms
 * @return array
 */
function bounce(string $id, int $duration = 400) {
    return animateSet([
        animate($id, ["scaleX" => 1.3, "scaleY" => 1.3], $duration / 2, "overshoot"),
        animate($id, ["scaleX" => 1.0, "scaleY" => 1.0], $duration / 2, "decelerate"),
    ], true);
}

/**
 * Run a "shake" animation
 * 
 * @param string $id View ID
 * @param int $duration Duration in ms
 * @return array
 */
function shake(string $id, int $duration = 400) {
    $step = (int)($duration / 7);
    return animateSet([
        animate($id, ["translationX" => -15], $step, "linear"),
        animate($id, ["translationX" => 15], $step, "linear"),
        animate($id, ["translationX" => -10], $step, "linear"),
        animate($id, ["translationX" => 10], $step, "linear"),
        animate($id, ["translationX" => -5], $step, "linear"),
        animate($id, ["translationX" => 5], $step, "linear"),
        animate($id, ["translationX" => 0], $step, "decelerate"),
    ], true);
}

// =============================================================================
// CLIPBOARD & SHARING
// =============================================================================

/**
 * Copy text to clipboard
 * 
 * @param string $text Text to copy
 * @param string $label Clipboard label
 * @return array
 * 
 * Example:
 *   return copyText("Hello world!");
 */
function copyText(string $text, string $label = "Copied") {
    return copyToClipboard($text, $label);
}

/**
 * Share text/content via Android share sheet
 * 
 * @param string $text Text to share
 * @param string $title Share dialog title
 * @return array
 * 
 * Example:
 *   return shareText("Check out this app!", "Share via");
 */
function shareText(string $text, string $title = "Share") {
    return share($text, $title);
}

/**
 * Open a URL in the browser
 * 
 * @param string $url URL to open
 * @return array
 * 
 * Example:
 *   return openBrowser("https://example.com");
 */
function openBrowser(string $url) {
    return openUrl($url);
}

// =============================================================================
// VIEW MANIPULATION - Dynamic add/remove/replace
// =============================================================================

/**
 * Remove a view from the layout
 * 
 * @param string $id ID of the view to remove
 * @return array
 */
function removeComponent(string $id) {
    return removeView($id);
}

/**
 * Add a child component to a parent layout
 * 
 * @param string $parentId Parent layout ID
 * @param Component $child Component to add
 * @param int $index Position to insert at (-1 = end)
 * @return array
 */
function addComponent(string $parentId, Component $child, int $index = -1) {
    return addView($parentId, $child, $index);
}

/**
 * Replace all children of a layout
 * 
 * @param string $parentId Parent layout ID
 * @param array $children New children components
 * @return array
 */
function replaceContent(string $parentId, array $children) {
    return replaceChildren($parentId, $children);
}

/**
 * Scroll a scrollable view to a specific position
 * 
 * @param string $id ScrollView ID
 * @param int $x X position
 * @param int $y Y position
 * @param bool $smooth Use smooth scrolling
 * @return array
 */
function scrollToPosition(string $id, int $x = 0, int $y = 0, bool $smooth = true) {
    return scrollTo($id, $x, $smooth);
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
 * Your callback receives: ['lat' => 12.34, 'lng' => 56.78, 'altitude' => 100, 'speed' => 0, 'bearing' => 0]
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
 * NOTE: Requires an external barcode scanner app (like ZXing) to be installed.
 * 
 * @param string $callback Method to receive scan result
 * @return array
 * 
 * Your callback receives: ['code' => 'scanned-value', 'format' => 'QR_CODE']
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
// MOTION SENSORS
// =============================================================================

/**
 * Read accelerometer (device acceleration). Callback receives: {x, y, z, values}
 */
function accelerometer(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "accelerometer", "callback" => $callback];
}

/**
 * Read gyroscope (rotation rate). Callback receives: {x, y, z, values}
 */
function gyroscope(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "gyroscope", "callback" => $callback];
}

/**
 * Read gravity sensor. Callback receives: {x, y, z, values}
 */
function gravity(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "gravity", "callback" => $callback];
}

/**
 * Read magnetic field sensor. Callback receives: {x, y, z, values}
 */
function magneticField(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "magneticfield", "callback" => $callback];
}

/**
 * Read orientation (azimuth, pitch, roll). Alias for compass with extra data.
 */
function orientation(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "compass", "callback" => $callback];
}

// =============================================================================
// ENVIRONMENT SENSORS
// =============================================================================

/**
 * Read ambient light level. Callback receives: {light}
 */
function light(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "light", "callback" => $callback];
}

/**
 * Read proximity sensor. Callback receives: {distance, near (bool)}
 */
function proximity(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "proximity", "callback" => $callback];
}

/**
 * Read barometric pressure. Callback receives: {pressure} (hPa)
 */
function pressure(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "pressure", "callback" => $callback];
}

/**
 * Read relative humidity. Callback receives: {humidity} (%)
 */
function humidity(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "humidity", "callback" => $callback];
}

/**
 * Read ambient temperature. Callback receives: {temperature} (°C)
 */
function temperature(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "temperature", "callback" => $callback];
}

/**
 * Read step counter. Callback receives: {steps}
 */
function stepCounter(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "stepcounter", "callback" => $callback];
}

// =============================================================================
// DEVICE & SCREEN INFO
// =============================================================================

/**
 * Get device information. Callback receives: {model, osVersion, apiLevel, deviceId, isTablet, language, country, appName, packageName, freeSpace}
 */
function deviceInfo(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "deviceinfo", "callback" => $callback];
}

/**
 * Get screen information. Callback receives: {width, height, density, rotation, orientation}
 */
function screenInfo(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "screeninfo", "callback" => $callback];
}

/**
 * Check if location/GPS is enabled. Callback receives: {enabled}
 */
function locationEnabled(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "locationenabled", "callback" => $callback];
}

// =============================================================================
// NETWORK
// =============================================================================

/**
 * Get WiFi info. Callback receives: {ssid, ip, rssi, mac}
 */
function wifiInfo(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "wifi", "callback" => $callback];
}

/**
 * Scan for available WiFi networks. Callback receives: {networks}
 */
function wifiScan(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "wifiscan", "callback" => $callback];
}

/**
 * Get Bluetooth info. Callback receives: {enabled, paired}
 */
function bluetoothInfo(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "bluetooth", "callback" => $callback];
}

/**
 * Discover nearby Bluetooth devices. Callback receives: {name, address} per device found.
 */
function btDiscover(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "btdiscover", "callback" => $callback];
}

/**
 * Get network status. Callback receives: {connected, ip, mac, ssid, rssi, wifiEnabled, bluetoothEnabled}
 */
function networkInfo(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "networkinfo", "callback" => $callback];
}

// =============================================================================
// HTTP REQUESTS
// =============================================================================

/**
 * Make an HTTP GET request
 * 
 * @param string $url The URL to request
 * @param string $callback Callback receives: {response, error, url}
 */
if (!function_exists('httpGet')) {
function httpGet(string $url, string $callback) {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "http", "callback" => $callback,
        "params" => ["url" => $url, "httpMethod" => "GET"]
    ];
}
}

/**
 * Make an HTTP POST request
 * 
 * @param string $url The URL to request
 * @param string $body POST body content
 * @param string $callback Callback receives: {response, error, url}
 * @param string $headers Optional comma-separated headers 
 */
if (!function_exists('httpPost')) {
function httpPost(string $url, string $body, string $callback, string $headers = '') {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "http", "callback" => $callback,
        "params" => ["url" => $url, "httpMethod" => "POST", "body" => $body, "headers" => $headers]
    ];
}
}

/**
 * Download a file from URL
 * 
 * @param string $url URL to download
 * @param string $dest Destination path on device
 * @param string $callback Callback receives: {file, success, url}
 */
function download(string $url, string $dest, string $callback) {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "download", "callback" => $callback,
        "params" => ["url" => $url, "dest" => $dest]
    ];
}

// =============================================================================
// MEDIA - Audio, Photo, Recording
// =============================================================================

/**
 * Take a photo with the camera (uses existing camera sensor)
 * 
 * @param string $callback Callback receives: {uri}
 */
if (!function_exists('takePhoto')) {
function takePhoto(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "camera", "callback" => $callback];
}
}

/**
 * Play an audio file
 * 
 * @param string $file Path to audio file
 * @param string $callback Callback receives: {status: 'playing'|'complete', file}
 */
function playSound(string $file, string $callback) {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "playaudio", "callback" => $callback,
        "params" => ["file" => $file]
    ];
}

/**
 * Stop the current audio player
 * 
 * @param string $callback Callback receives: {status: 'stopped'}
 */
function stopSound(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "stopaudio", "callback" => $callback];
}

/**
 * Start recording audio
 * 
 * @param string $file Path to save recording
 * @param string $callback Callback receives: {status: 'recording', file}
 */
function recordSound(string $file, string $callback) {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "recordaudio", "callback" => $callback,
        "params" => ["file" => $file]
    ];
}

/**
 * Stop audio recording
 * 
 * @param string $callback Callback receives: {status: 'stopped'}
 */
function stopRecord(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "stoprecording", "callback" => $callback];
}

/**
 * Play a system ringtone
 * 
 * @param string $type Ringtone type: 'notification', 'alarm', 'ringtone'
 */
if (!function_exists('playRingtone')) {
function playRingtone(string $type = 'notification') {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "ringtone", "callback" => "_noop",
        "params" => ["ringtoneType" => $type]
    ];
}
}

// =============================================================================
// TEXT-TO-SPEECH & SPEECH RECOGNITION
// =============================================================================

/**
 * Speak text aloud (text-to-speech)
 * 
 * @param string $text Text to speak
 */
function speak(string $text) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "speech", "callback" => "_noop",
        "params" => ["text" => $text]
    ];
}

/**
 * Start speech recognition (speech-to-text)
 * 
 * @param string $callback Callback receives: {text}
 */
function listenSpeech(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "speechrecognition", "callback" => $callback];
}

// =============================================================================
// HARDWARE CONTROL
// =============================================================================

/**
 * Vibrate the device
 * 
 * @param int $ms Duration in milliseconds (default 200)
 */
function vibratePhone(int $ms = 200) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "vibrate", "callback" => "_noop",
        "params" => ["duration" => $ms]
    ];
}

/**
 * Get current volume level
 * 
 * @param string $callback Callback receives: {volume, stream}
 * @param string $stream Audio stream: 'music', 'ring', 'alarm', 'notification'
 */
function getVol(string $callback, string $stream = 'music') {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "getvolume", "callback" => $callback,
        "params" => ["stream" => $stream]
    ];
}

/**
 * Set volume level
 * 
 * @param int $level Volume level (0-15 typically)
 * @param string $stream Audio stream: 'music', 'ring', 'alarm', 'notification'
 */
function setVol(int $level, string $stream = 'music') {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "setvolume", "callback" => "_noop",
        "params" => ["level" => $level, "stream" => $stream]
    ];
}

/**
 * Set ringer mode
 * 
 * @param string $mode 'normal', 'vibrate', or 'silent'
 */
function setRinger(string $mode) {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "setringermode", "callback" => "_noop",
        "params" => ["mode" => $mode]
    ];
}

/**
 * Set screen brightness
 * 
 * @param float $level Brightness 0.0 to 1.0
 */
function setBrightness(float $level) {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "setbrightness", "callback" => "_noop",
        "params" => ["level" => $level]
    ];
}

/**
 * Prevent screen from locking
 * 
 * @param bool $on True to keep screen on, false to allow lock
 */
function keepScreenOn(bool $on = true) {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "preventscreenlock", "callback" => "_noop",
        "params" => ["prevent" => $on]
    ];
}

/**
 * Toggle flashlight on/off
 * 
 * @param bool $on True = on, false = off
 */
function toggleFlash(bool $on = true) {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "flashlight", "callback" => "_noop",
        "params" => ["on" => $on]
    ];
}

// =============================================================================
// CLIPBOARD
// =============================================================================

/**
 * Copy text to clipboard
 * 
 * @param string $text Text to copy
 */
function clipboardCopy(string $text) {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "clipboard_set", "callback" => "_noop",
        "params" => ["text" => $text]
    ];
}

/**
 * Get text from clipboard
 * 
 * @param string $callback Callback receives: {text}
 */
function clipboardPaste(string $callback) {
    return ["action" => "DS_SENSOR_CALL", "sensor" => "clipboard_get", "callback" => $callback];
}

// =============================================================================
// COMMUNICATION - SMS, Phone, Email, Notifications
// =============================================================================

/**
 * Send an SMS message
 * 
 * @param string $phone Phone number
 * @param string $message SMS text
 * @param string $callback Callback receives: {status}
 */
function sms(string $phone, string $message, string $callback = '_noop') {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "sms", "callback" => $callback,
        "params" => ["phone" => $phone, "message" => $message]
    ];
}

/**
 * Make a phone call
 * 
 * @param string $number Phone number to call
 */
function call(string $number) {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "phonecall", "callback" => "_noop",
        "params" => ["number" => $number]
    ];
}

/**
 * Show a notification
 * 
 * @param string $title Notification title
 * @param string $message Notification body
 * @param string $callback Callback receives: {action} when tapped
 */
function notify(string $title, string $message, string $callback = '_noop') {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "notification", "callback" => $callback,
        "params" => ["title" => $title, "message" => $message]
    ];
}

/**
 * Send an email (opens email chooser)
 * 
 * @param string $to Recipient email
 * @param string $subject Email subject
 * @param string $body Email body
 * @param string $attachment Optional file attachment path
 */
function email(string $to, string $subject, string $body, string $attachment = '') {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "sendemail", "callback" => "_noop",
        "params" => ["recipient" => $to, "subject" => $subject, "body" => $body, "attachment" => $attachment]
    ];
}

/**
 * Scan a QR code / barcode (alias for scanBarcode)
 */
function scanCode(string $callback) {
    return scanBarcode($callback);
}

// =============================================================================
// ENCRYPTION & HASHING
// =============================================================================

/**
 * Encrypt text with a password
 * 
 * @param string $text Text to encrypt
 * @param string $password Encryption password
 * @param string $callback Callback receives: {result}
 */
function encrypt(string $text, string $password, string $callback) {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "encrypt", "callback" => $callback,
        "params" => ["text" => $text, "password" => $password]
    ];
}

/**
 * Decrypt text with a password
 * 
 * @param string $text Encrypted text
 * @param string $password Decryption password
 * @param string $callback Callback receives: {result}
 */
function decrypt(string $text, string $password, string $callback) {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "decrypt", "callback" => $callback,
        "params" => ["text" => $text, "password" => $password]
    ];
}

/**
 * Hash text with specified algorithm
 * 
 * @param string $text Text to hash
 * @param string $algorithm Algorithm: 'MD5', 'SHA1', 'SHA256', 'SHA512'
 * @param string $callback Callback receives: {result, algorithm}
 */
if (!function_exists('hashText')) {
function hashText(string $text, string $algorithm, string $callback) {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "hash", "callback" => $callback,
        "params" => ["text" => $text, "algorithm" => $algorithm]
    ];
}
}

/** Convenience: MD5 hash */
function md5Hash(string $text, string $callback) {
    return hashText($text, 'MD5', $callback);
}

/** Convenience: SHA-256 hash */
function sha256(string $text, string $callback) {
    return hashText($text, 'SHA256', $callback);
}

// =============================================================================
// FILE SYSTEM - Read/Write files on the Android device
// =============================================================================

/**
 * Read a file from device storage
 * 
 * Note: Named loadFile() instead of readFile() to avoid conflict
 * with PHP's built-in readfile() function (case-insensitive).
 * 
 * @param string $path File path on device
 * @param string $callback Callback receives: {content, path} or {error, path}
 */
function loadFile(string $path, string $callback) {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "readfile", "callback" => $callback,
        "params" => ["path" => $path]
    ];
}

/**
 * Write content to a file on device storage
 * 
 * @param string $path File path on device
 * @param string $content Content to write
 * @param string $callback Callback receives: {success, path} or {error}
 */
function writeFile(string $path, string $content, string $callback = '_noop') {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "writefile", "callback" => $callback,
        "params" => ["path" => $path, "content" => $content]
    ];
}

/**
 * List files in a folder on device storage
 * 
 * @param string $path Folder path
 * @param string $callback Callback receives: {files, path} or {error}
 */
function listFiles(string $path, string $callback) {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "listfolder", "callback" => $callback,
        "params" => ["path" => $path]
    ];
}

/**
 * Check if a file exists on device storage
 * 
 * @param string $path File path
 * @param string $callback Callback receives: {exists, path}
 */
function fileExists(string $path, string $callback) {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "fileexists", "callback" => $callback,
        "params" => ["path" => $path]
    ];
}

// =============================================================================
// APPS & INTENTS
// =============================================================================

/**
 * Launch another app by package name
 * 
 * @param string $package Package name e.g. 'com.google.android.gm'
 */
function launchApp(string $package) {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "openapp", "callback" => "_noop",
        "params" => ["package" => $package]
    ];
}

/**
 * Send an Android Intent
 * 
 * @param string $action Intent action e.g. 'android.intent.action.VIEW'
 * @param string $uri URI for the intent
 * @param string $type MIME type
 * @param string $extras JSON-encoded extras
 */
function sendAndroidIntent(string $action, string $uri = '', string $type = '', string $extras = '') {
    return [
        "action" => "DS_SENSOR_CALL", "sensor" => "intent", "callback" => "_noop",
        "params" => ["intentAction" => $action, "uri" => $uri, "type" => $type, "extras" => $extras]
    ];
}

/**
 * Internal no-op callback for fire-and-forget sensor calls.
 * Used when no response handling is needed.
 */
function _noop($p) {
    return null;
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

/**
 * Create a tabbed page layout
 * 
 * Each tab maps to a method/content builder.
 * 
 * @param string $title Page title
 * @param array $tabDefs Array of ['name' => tab title, 'content' => [components]]
 * @param string|null $onTabChange Method to call on tab change
 * @return VerticalLayout
 * 
 * Example:
 *   return tabbedPage("My App", [
 *       ['name' => 'Home', 'content' => [label("Welcome!")]],
 *       ['name' => 'Settings', 'content' => [toggle("dark", "Dark Mode")]],
 *   ]);
 */
function tabbedPage(string $title, array $tabDefs, ?string $onTabChange = null) {
    $tabNames = array_map(fn($t) => $t['name'], $tabDefs);
    
    $content = [
        tabs("page_tabs", $tabNames, $onTabChange ?? ""),
    ];
    
    // Show first tab's content by default
    if (!empty($tabDefs[0]['content'])) {
        $contentArea = (new VerticalLayout($tabDefs[0]['content']))
            ->id("tab_content")
            ->padding(16);
        $content[] = $contentArea;
    }
    
    return page($title, $content);
}

/**
 * Create a settings page with labeled toggles/inputs
 * 
 * @param string $title Page title
 * @param array $settings Array of settings items
 * @return VerticalLayout
 * 
 * Example:
 *   return settingsPage("Settings", [
 *       ['type' => 'toggle', 'id' => 'dark', 'label' => 'Dark Mode', 'onChange' => 'onDarkToggle'],
 *       ['type' => 'toggle', 'id' => 'notif', 'label' => 'Notifications', 'onChange' => 'onNotifToggle'],
 *       ['type' => 'spinner', 'id' => 'lang', 'label' => 'Language', 'items' => ['English', 'Spanish', 'French']],
 *       ['type' => 'slider', 'id' => 'font_size', 'label' => 'Font Size', 'max' => 30],
 *   ]);
 */
function settingsPage(string $title, array $settings) {
    $items = [];
    foreach ($settings as $s) {
        $type = $s['type'] ?? 'toggle';
        
        $rowContent = [label($s['label'] ?? '', ['size' => 16])];
        
        switch ($type) {
            case 'toggle':
                $rowContent[] = toggle($s['id'], '', $s['value'] ?? false, $s['onChange'] ?? null);
                break;
            case 'spinner':
                $rowContent[] = spinner($s['id'], $s['items'] ?? [], $s['onChange'] ?? null);
                break;
            case 'slider':
                $rowContent[] = seekbar($s['id'], $s['value'] ?? 50, $s['max'] ?? 100, $s['onChange'] ?? null);
                break;
            case 'input':
                $rowContent[] = input($s['id'], $s['hint'] ?? '', []);
                break;
        }
        
        $items[] = row($rowContent);
        $items[] = divider();
    }
    
    return page($title, $items);
}

/**
 * Create a form with labeled fields
 * 
 * @param string $title Form title
 * @param array $fields Array of field definitions
 * @param string $onSubmit Method to call on submit
 * @param string $submitText Submit button text
 * @return VerticalLayout
 * 
 * Example:
 *   return formPage("Register", [
 *       ['id' => 'name', 'label' => 'Name'],
 *       ['id' => 'email', 'label' => 'Email', 'inputType' => 'textEmailAddress'],
 *       ['id' => 'password', 'label' => 'Password', 'password' => true],
 *       ['id' => 'agree', 'label' => 'I agree to terms', 'type' => 'checkbox'],
 *   ], "onRegister", "Register");
 */
function formPage(string $title, array $fields, string $onSubmit, string $submitText = "Submit") {
    $elements = [];
    
    foreach ($fields as $f) {
        $type = $f['type'] ?? 'text';
        
        switch ($type) {
            case 'checkbox':
                $elements[] = checkbox($f['id'], $f['label'], $f['onChange'] ?? null);
                break;
            case 'toggle':
                $elements[] = toggle($f['id'], $f['label']);
                break;
            case 'spinner':
                $elements[] = label($f['label'], ['size' => 14, 'color' => Colors::TEXT_MUTED]);
                $elements[] = spinner($f['id'], $f['items'] ?? []);
                break;
            case 'radio':
                $elements[] = label($f['label'], ['size' => 14, 'color' => Colors::TEXT_MUTED]);
                $elements[] = radioGroup($f['id'], $f['options'] ?? []);
                break;
            case 'material':
                $elements[] = textField($f['id'], $f['label'], $f);
                break;
            default:
                if (isset($f['password']) && $f['password']) {
                    $elements[] = input($f['id'], $f['label'], ['password' => true]);
                } else {
                    $elements[] = input($f['id'], $f['label'], $f);
                }
                break;
        }
        $elements[] = spacer(8);
    }
    
    $elements[] = spacer(16);
    $elements[] = button($submitText, $onSubmit, ['color' => Colors::PRIMARY]);
    
    return page($title, $elements);
}

/**
 * Create a profile page layout
 * 
 * @param array $profile ['name', 'subtitle', 'image', 'actions' => [button components]]
 * @return VerticalLayout
 * 
 * Example:
 *   return profilePage([
 *       'name' => 'John Doe',
 *       'subtitle' => 'john@example.com',
 *       'image' => 'https://example.com/avatar.jpg',
 *       'actions' => [
 *           button("Edit Profile", "onEditProfile"),
 *           button("Logout", "onLogout", ['color' => Colors::DANGER]),
 *       ],
 *   ]);
 */
function profilePage(array $profile) {
    $content = [];
    
    if (isset($profile['image'])) {
        $content[] = (new ImageView())
            ->src($profile['image'])
            ->width(100)->height(100)
            ->scaleType("centerCrop");
    }
    
    $content[] = spacer(16);
    $content[] = label($profile['name'] ?? 'User', ['bold' => true, 'size' => 24, 'center' => true]);
    
    if (isset($profile['subtitle'])) {
        $content[] = label($profile['subtitle'], ['size' => 14, 'color' => Colors::TEXT_MUTED, 'center' => true]);
    }
    
    $content[] = spacer(24);
    
    foreach ($profile['actions'] ?? [] as $action) {
        $content[] = $action;
        $content[] = spacer(8);
    }
    
    return page('', $content);
}

/**
 * Create a detail/info page with key-value pairs
 * 
 * @param string $title Page title
 * @param array $details ['key' => 'value', ...] pairs
 * @param array $actions Optional action buttons
 * @return VerticalLayout
 * 
 * Example:
 *   return detailPage("Order #123", [
 *       'Status' => 'Shipped',
 *       'Date' => '2025-01-15',
 *       'Total' => '$99.99',
 *   ], [button("Track", "onTrack")]);
 */
function detailPage(string $title, array $details, array $actions = []) {
    $items = [];
    foreach ($details as $key => $value) {
        $items[] = row([
            label($key, ['size' => 14, 'color' => Colors::TEXT_MUTED, 'width' => 120]),
            label((string) $value, ['size' => 16, 'bold' => true]),
        ]);
        $items[] = divider();
    }
    
    if (!empty($actions)) {
        $items[] = spacer(16);
        $items[] = row($actions);
    }
    
    return page($title, $items);
}

/**
 * Create a media card (image + text + actions)
 * 
 * @param array $data ['image', 'title', 'subtitle', 'body', 'actions']
 * @return CardView
 * 
 * Example:
 *   return mediaCard([
 *       'image' => 'https://example.com/photo.jpg',
 *       'title' => 'Beautiful Sunset',
 *       'subtitle' => 'By John',
 *       'body' => 'A stunning sunset captured at the beach.',
 *       'actions' => [
 *           button("Like", "onLike"),
 *           button("Share", "onShare"),
 *       ]
 *   ]);
 */
function mediaCard(array $data) {
    $content = [];
    
    if (isset($data['image'])) {
        $content[] = (new ImageView())
            ->src($data['image'])
            ->width(-1)->height(200)
            ->scaleType("centerCrop");
    }
    
    if (isset($data['title'])) {
        $content[] = label($data['title'], ['bold' => true, 'size' => 20, 'padding' => 12]);
    }
    
    if (isset($data['subtitle'])) {
        $content[] = label($data['subtitle'], ['size' => 14, 'color' => Colors::TEXT_MUTED, 'paddingLeft' => 12]);
    }
    
    if (isset($data['body'])) {
        $content[] = label($data['body'], ['size' => 16, 'padding' => 12]);
    }
    
    if (!empty($data['actions'])) {
        $content[] = row($data['actions']);
    }
    
    return materialCard($content, ['corners' => 16, 'elevation' => 4, 'padding' => 0]);
}

/**
 * Create an empty state placeholder
 * 
 * @param string $message Message to display
 * @param string $icon Optional emoji icon
 * @param string|null $actionText Button text for action
 * @param string|null $actionMethod Method for the action button
 * @return VerticalLayout
 * 
 * Example:
 *   return emptyState("No items yet", "📋", "Add Item", "onAddItem");
 */
function emptyState(string $message, string $icon = "📭", ?string $actionText = null, ?string $actionMethod = null) {
    $content = [
        spacer(60),
        label($icon, ['size' => 48, 'center' => true]),
        spacer(16),
        label($message, ['size' => 18, 'color' => Colors::TEXT_MUTED, 'center' => true]),
    ];
    
    if ($actionText && $actionMethod) {
        $content[] = spacer(24);
        $content[] = button($actionText, $actionMethod, ['color' => Colors::PRIMARY]);
    }
    
    return (new VerticalLayout($content))
        ->gravity("center")
        ->width(-1)->height(-1);
}

/**
 * Create a loading indicator with message
 * 
 * @param string $message Loading message
 * @return VerticalLayout
 * 
 * Example:
 *   return loading("Please wait...");
 */
function loading(string $message = "Loading...") {
    return (new VerticalLayout([
        spacer(60),
        (new ProgressBar())->id("loading_spinner"),
        spacer(16),
        label($message, ['size' => 16, 'color' => Colors::TEXT_MUTED, 'center' => true]),
    ]))->gravity("center")->width(-1)->height(-1);
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

// =============================================================================
// AUTO-RUN BOOTSTRAP
// When a PHP file includes simple.php and defines an App or MyApp class,
// this code automatically runs the appropriate method and outputs JSON.
// Uses shutdown function to run AFTER all code is loaded.
// =============================================================================

// Global flag to prevent double execution (in case router.php also runs)
$GLOBALS['_simple_php_did_output'] = false;

function _simple_php_autorun() {
    // Prevent double execution
    if (!empty($GLOBALS['_simple_php_did_output'])) {
        return;
    }
    
    // Only run in CLI mode
    if (php_sapi_name() !== 'cli') {
        return;
    }
    
    // Suppress errors to keep output clean JSON
    error_reporting(0);
    ini_set('display_errors', 0);
    
    // Parse --method argument
    global $argv;
    $method = 'index';
    foreach ($argv ?? [] as $arg) {
        if (strpos($arg, '--method=') === 0) {
            $method = substr($arg, 9);
            break;
        }
    }
    
    // Find and instantiate App class (check both App and MyApp names)
    $appInstance = null;
    if (class_exists('App')) {
        $appInstance = new App();
    } elseif (class_exists('MyApp')) {
        $appInstance = new MyApp();
    }
    
    if ($appInstance && method_exists($appInstance, $method)) {
        // Get parameters from env or global
        $params = [];
        
        // Call method and output result
        $result = $appInstance->$method($params);
        
        // Mark that we've produced output
        $GLOBALS['_simple_php_did_output'] = true;
        
        // If result is a Component, render to JSON
        if ($result instanceof Component) {
            echo $result->toJson();
        } elseif (is_array($result)) {
            echo json_encode($result, JSON_UNESCAPED_UNICODE);
        } elseif (is_string($result)) {
            echo $result;
        }
    }
}

// Register to run after all code is loaded
register_shutdown_function('_simple_php_autorun');

?>
