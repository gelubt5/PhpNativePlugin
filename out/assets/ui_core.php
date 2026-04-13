<?php
// ui_core.php

/**
 * Base Component class with helper methods for getting/setting properties.
 * 
 * IMPORTANT: Methods prefixed with underscore (_) are ACTION HELPERS.
 * They return action arrays and are NOT serialized to XML/JSON.
 * Only $attributes are serialized.
 * 
 * Usage with global variables:
 *   $myLabel = (new TextView())->id("my_label")->text("Hello");
 *   
 *   // Later, to update it:
 *   return $myLabel->_setText("New text");
 *   
 *   // To get a value:
 *   return $myLabel->_getText("onGotText");
 */
abstract class Component {
    protected $attributes = [];
    
    // =========================================================================
    // ATTRIBUTE SETTER (via __call) - These GO to XML
    // =========================================================================
    
    public function __call($name, $arguments) {
        // Only set attribute if method name does NOT start with underscore
        // Underscore methods are helper actions, not attributes
        if (strpos($name, '_') !== 0) {
            $this->attributes[$name] = isset($arguments[0]) ? $arguments[0] : null;
        }
        return $this;
    }
    
    // =========================================================================
    // SERIALIZATION - Only $attributes are serialized
    // =========================================================================

    public function toArray() {
        try {
            $reflection = new ReflectionClass($this);
            $type = $reflection->getShortName();
        } catch (Exception $e) {
            $type = get_class($this);
        }

        return array_merge(
            ["type" => $type],
            $this->attributes
        );
    }

    public function toJson() {
        return json_encode($this->toArray());
    }
    
    // =========================================================================
    // HELPER: Get the view ID (returns string, not action)
    // =========================================================================
    
    public function getId() {
        return $this->attributes['id'] ?? null;
    }
    
    public function getAttr($name) {
        return $this->attributes[$name] ?? null;
    }
    
    // =========================================================================
    // ACTION HELPERS - Return action arrays, NOT serialized to XML
    // Prefix: _ (underscore) to distinguish from XML attributes
    // =========================================================================
    
    // ----- UNIVERSAL GET/SET -----
    
    /**
     * Get any property from this view (SYNCHRONOUS - reads from shared file).
     * @param string $property Property name
     * @param mixed $default Default value if not found
     * @return mixed The property value
     */
    public function _get(string $property, $default = null) {
        return getViewProperty($this->getId(), $property, $default);
    }
    
    /**
     * Get all properties of this view from shared state.
     * @return array All properties
     */
    public function _getState(): array {
        return getViewState($this->getId());
    }
    
    /**
     * Set any property on this view
     * @param string $property Property name
     * @param mixed $value New value
     * @return array Action array (NOT serialized)
     */
    public function _set(string $property, $value): array {
        return updateView($this->getId(), [$property => $value]);
    }
    
    /**
     * Update multiple properties at once
     * @param array $properties Key-value pairs
     * @return array Action array (NOT serialized)
     */
    public function _update(array $properties): array {
        return updateView($this->getId(), $properties);
    }
    
    // ----- TEXT PROPERTIES -----
    
    /**
     * Get text from this view (SYNCHRONOUS).
     * @param string $default Default value if not found
     * @return string|null The text value
     */
    public function _getText(string $default = ""): ?string {
        return $this->_get("text", $default);
    }
    
    public function _setText(string $text, ?string $color = null): array {
        $attrs = ["text" => $text];
        if ($color) $attrs["textColor"] = $color;
        return $this->_update($attrs);
    }
    
    /**
     * Get text color (SYNCHRONOUS).
     */
    public function _getTextColor(?string $default = null): ?string {
        return $this->_get("textColor", $default);
    }
    
    public function _setTextColor(string $color): array {
        return $this->_set("textColor", $color);
    }
    
    public function _getTextSize($default = null) {
        return $this->_get("textSize", $default);
    }
    
    public function _setTextSize(int $size): array {
        return $this->_set("textSize", $size);
    }
    
    public function _setTextStyle(string $style): array {
        return $this->_set("textStyle", $style);
    }
    
    public function _setAllCaps(bool $caps): array {
        return $this->_set("textAllCaps", $caps);
    }
    
    public function _setLetterSpacing(float $spacing): array {
        return $this->_set("letterSpacing", $spacing);
    }
    
    public function _setLineSpacing(float $multiplier): array {
        return $this->_set("lineSpacingMultiplier", $multiplier);
    }
    
    public function _setGravity(string $gravity): array {
        return $this->_set("gravity", $gravity);
    }
    
    public function _setHint(string $hint): array {
        return $this->_set("hint", $hint);
    }
    
    public function _getHint($default = null) {
        return $this->_get("hint", $default);
    }
    
    public function _clear(): array {
        return $this->_set("text", "");
    }
    
    // ----- APPEARANCE -----
    
    public function _getBackground($default = null) {
        return $this->_get("backgroundColor", $default);
    }
    
    public function _setBackground(string $color): array {
        return $this->_set("backgroundColor", $color);
    }
    
    public function _setCornerRadius(int $radius): array {
        return $this->_set("cornerRadius", $radius);
    }
    
    public function _setElevation(int $elevation): array {
        return $this->_set("elevation", $elevation);
    }
    
    public function _setBorder(string $color, int $width): array {
        return $this->_update(["strokeColor" => $color, "strokeWidth" => $width]);
    }
    
    public function _setBorderColor(string $color): array {
        return $this->_set("strokeColor", $color);
    }
    
    public function _setBorderWidth(int $width): array {
        return $this->_set("strokeWidth", $width);
    }
    
    // ----- VISIBILITY & STATE -----
    
    public function _getVisibility($default = null) {
        return $this->_get("visibility", $default);
    }
    
    public function _setVisibility(string $visibility): array {
        return $this->_set("visibility", $visibility);
    }
    
    public function _show(): array {
        return $this->_set("visibility", "visible");
    }
    
    public function _hide(): array {
        return $this->_set("visibility", "gone");
    }
    
    public function _invisible(): array {
        return $this->_set("visibility", "invisible");
    }
    
    public function _getEnabled($default = null) {
        return $this->_get("enabled", $default);
    }
    
    public function _enable(): array {
        return $this->_set("enabled", true);
    }
    
    public function _disable(): array {
        return $this->_set("enabled", false);
    }
    
    public function _setEnabled(bool $enabled): array {
        return $this->_set("enabled", $enabled);
    }
    
    public function _setClickable(bool $clickable): array {
        return $this->_set("clickable", $clickable);
    }
    
    public function _setFocusable(bool $focusable): array {
        return $this->_set("focusable", $focusable);
    }
    
    public function _setSelected(bool $selected): array {
        return $this->_set("selected", $selected);
    }
    
    public function _getSelected($default = null) {
        return $this->_get("selected", $default);
    }
    
    public function _focus(): array {
        return $this->_set("requestFocus", true);
    }
    
    public function _clearFocus(): array {
        return $this->_set("clearFocus", true);
    }
    
    // ----- ALPHA/OPACITY -----
    
    public function _getAlpha($default = null) {
        return $this->_get("alpha", $default);
    }
    
    public function _setAlpha(float $alpha): array {
        return $this->_set("alpha", max(0.0, min(1.0, $alpha)));
    }
    
    public function _fadeIn(): array {
        return $this->_set("alpha", 1.0);
    }
    
    public function _fadeOut(): array {
        return $this->_set("alpha", 0.0);
    }
    
    // ----- DIMENSIONS -----
    
    public function _getWidth($default = null) {
        return $this->_get("width", $default);
    }
    
    public function _setWidth($width): array {
        return $this->_set("width", $width);
    }
    
    public function _getHeight($default = null) {
        return $this->_get("height", $default);
    }
    
    public function _setHeight($height): array {
        return $this->_set("height", $height);
    }
    
    public function _setDimensions($width, $height): array {
        return $this->_update(["width" => $width, "height" => $height]);
    }
    
    // ----- PADDING & MARGIN -----
    
    public function _getPadding($default = null) {
        return $this->_get("padding", $default);
    }
    
    public function _setPadding(int $padding): array {
        return $this->_set("padding", $padding);
    }
    
    public function _setPaddingAll(int $left, int $top, int $right, int $bottom): array {
        return $this->_update([
            "paddingLeft" => $left,
            "paddingTop" => $top,
            "paddingRight" => $right,
            "paddingBottom" => $bottom
        ]);
    }
    
    public function _getMargin($default = null) {
        return $this->_get("margin", $default);
    }
    
    public function _setMargin(int $margin): array {
        return $this->_set("margin", $margin);
    }
    
    public function _setMarginAll(int $left, int $top, int $right, int $bottom): array {
        return $this->_update([
            "marginLeft" => $left,
            "marginTop" => $top,
            "marginRight" => $right,
            "marginBottom" => $bottom
        ]);
    }
    
    // ----- TRANSFORMS -----
    
    public function _getRotation($default = null) {
        return $this->_get("rotation", $default);
    }
    
    public function _setRotation(float $degrees): array {
        return $this->_set("rotation", $degrees);
    }
    
    public function _setRotationX(float $degrees): array {
        return $this->_set("rotationX", $degrees);
    }
    
    public function _setRotationY(float $degrees): array {
        return $this->_set("rotationY", $degrees);
    }
    
    public function _getScaleX($default = null) {
        return $this->_get("scaleX", $default);
    }
    
    public function _getScaleY($default = null) {
        return $this->_get("scaleY", $default);
    }
    
    public function _setScale(float $scale): array {
        return $this->_update(["scaleX" => $scale, "scaleY" => $scale]);
    }
    
    public function _setScaleX(float $scale): array {
        return $this->_set("scaleX", $scale);
    }
    
    public function _setScaleY(float $scale): array {
        return $this->_set("scaleY", $scale);
    }
    
    public function _getTranslationX($default = null) {
        return $this->_get("translationX", $default);
    }
    
    public function _getTranslationY($default = null) {
        return $this->_get("translationY", $default);
    }
    
    public function _setTranslationX(float $translation): array {
        return $this->_set("translationX", $translation);
    }
    
    public function _setTranslationY(float $translation): array {
        return $this->_set("translationY", $translation);
    }
    
    public function _setPosition(float $x, float $y): array {
        return $this->_update(["translationX" => $x, "translationY" => $y]);
    }
    
    public function _transform(array $transform): array {
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
        if (isset($transform['alpha'])) $props['alpha'] = $transform['alpha'];
        return $this->_update($props);
    }
    
    public function _resetTransform(): array {
        return $this->_update([
            "translationX" => 0,
            "translationY" => 0,
            "rotation" => 0,
            "scaleX" => 1.0,
            "scaleY" => 1.0,
            "alpha" => 1.0
        ]);
    }
    
    // ----- CHECKBOX/SWITCH -----
    
    public function _getChecked($default = false) {
        return $this->_get("checked", $default);
    }
    
    public function _setChecked(bool $checked): array {
        return $this->_set("checked", $checked);
    }
    
    public function _check(): array {
        return $this->_set("checked", true);
    }
    
    public function _uncheck(): array {
        return $this->_set("checked", false);
    }
    
    public function _toggle(): array {
        // Note: This requires knowing current state
        // Better to use setChecked with opposite of current
        return $this->_set("toggle", true);
    }
    
    // ----- PROGRESS BAR / SEEKBAR -----
    
    public function _getProgress($default = 0) {
        return $this->_get("progress", $default);
    }
    
    public function _setProgress(int $progress): array {
        return $this->_set("progress", $progress);
    }
    
    public function _getMax($default = 100) {
        return $this->_get("max", $default);
    }
    
    public function _setMax(int $max): array {
        return $this->_set("max", $max);
    }
    
    public function _setProgressWithMax(int $progress, int $max): array {
        return $this->_update(["progress" => $progress, "max" => $max]);
    }
    
    // ----- IMAGE VIEW -----
    
    public function _getImage($default = null) {
        return $this->_get("src", $default);
    }
    
    public function _setImage(string $src): array {
        return $this->_set("src", $src);
    }
    
    public function _setScaleType(string $type): array {
        return $this->_set("scaleType", $type);
    }
    
    // ----- TEXT INPUT -----
    
    public function _setInputType(string $type): array {
        return $this->_set("inputType", $type);
    }
    
    public function _setMinLines(int $lines): array {
        return $this->_set("minLines", $lines);
    }
    
    public function _setMaxLines(int $lines): array {
        return $this->_set("maxLines", $lines);
    }
    
    public function _setSingleLine(bool $singleLine = true): array {
        return $this->_set("singleLine", $singleLine);
    }
    
    // ----- TAG -----
    
    public function _getTag($default = null) {
        return $this->_get("tag", $default);
    }
    
    public function _setTag($tag): array {
        return $this->_set("tag", $tag);
    }
    
    // ----- COMPOUND STYLE HELPERS -----
    
    /**
     * Apply multiple text styles at once
     * @param array $style ['text', 'color', 'size', 'bold', 'center', 'caps']
     * @return array Action
     */
    public function _styleText(array $style): array {
        $props = [];
        if (isset($style['text'])) $props['text'] = $style['text'];
        if (isset($style['color'])) $props['textColor'] = $style['color'];
        if (isset($style['size'])) $props['textSize'] = $style['size'];
        if (isset($style['bold'])) $props['textStyle'] = $style['bold'] ? 'bold' : 'normal';
        if (isset($style['italic'])) $props['textStyle'] = $style['italic'] ? 'italic' : 'normal';
        if (isset($style['center'])) $props['gravity'] = 'center';
        if (isset($style['caps'])) $props['textAllCaps'] = $style['caps'];
        return $this->_update($props);
    }
    
    /**
     * Apply multiple view styles at once
     * @param array $style ['background', 'corners', 'elevation', 'alpha', 'padding', 'margin']
     * @return array Action
     */
    public function _styleView(array $style): array {
        $props = [];
        if (isset($style['background'])) $props['backgroundColor'] = $style['background'];
        if (isset($style['corners'])) $props['cornerRadius'] = $style['corners'];
        if (isset($style['elevation'])) $props['elevation'] = $style['elevation'];
        if (isset($style['alpha'])) $props['alpha'] = $style['alpha'];
        if (isset($style['padding'])) $props['padding'] = $style['padding'];
        if (isset($style['margin'])) $props['margin'] = $style['margin'];
        return $this->_update($props);
    }
}

// =============================================================================
// STANDARD COMPONENTS - Basic Views
// =============================================================================

class TextView extends Component {}
class Button extends Component {}
class CheckBox extends Component {}
class EditText extends Component {}
class ImageView extends Component {}
class ProgressBar extends Component {}
class SeekBar extends Component {}
class RadioButton extends Component {}
class Spinner extends Component {}
class WebView extends Component {}
class ToggleButton extends Component {}
class RatingBar extends Component {}
class NumberPicker extends Component {}
class AutoCompleteTextView extends Component {}
class MultiAutoCompleteTextView extends Component {}
class SearchView extends Component {}
class CalendarView extends Component {}
class DatePicker extends Component {}
class TimePicker extends Component {}
class VideoView extends Component {}
class Space extends Component {}
class Chronometer extends Component {}
class TextClock extends Component {}
class AnalogClock extends Component {}

// =============================================================================
// MATERIAL / EXTENDED COMPONENTS
// =============================================================================

/**
 * FloatingActionButton - Material Design FAB.
 * 
 * Usage:
 *   $fab = (new FloatingActionButton())
 *       ->id("fab_add")
 *       ->icon("add")
 *       ->action("onFabClick")
 *       ->backgroundColor("#FF4081");
 */
class FloatingActionButton extends Component {
    public function toArray() {
        $data = parent::toArray();
        $data['type'] = 'FloatingActionButton';
        return $data;
    }
}

/**
 * Chip - Material Design Chip (tag/filter/choice/action).
 * 
 * Usage:
 *   $chip = (new Chip())
 *       ->id("chip_tag")
 *       ->text("Android")
 *       ->chipStyle("filter")        // "action", "filter", "choice", "entry"
 *       ->closeable(true)
 *       ->action("onChipClick")
 *       ->onClose("onChipClose");
 */
class Chip extends Component {
    public function toArray() {
        $data = parent::toArray();
        $data['type'] = 'Chip';
        return $data;
    }
}

/**
 * ChipGroup - Container for Chip components.
 * 
 * Usage:
 *   $group = (new ChipGroup([
 *       (new Chip())->text("PHP")->id("c1"),
 *       (new Chip())->text("Java")->id("c2"),
 *   ]))->singleSelection(true);
 */
class ChipGroup extends Component {
    private $children;
    
    public function __construct($children = []) {
        $this->children = $children;
    }
    
    public function addChild($child) {
        $this->children[] = $child;
        return $this;
    }
    
    public function toArray() {
        $data = parent::toArray();
        $data['type'] = 'ChipGroup';
        $data['children'] = array_map(function($child) {
            return ($child instanceof Component) ? $child->toArray() : $child;
        }, $this->children);
        return $data;
    }
}

/**
 * TextInputLayout - Material text field wrapper with floating label.
 * 
 * Usage:
 *   $field = (new TextInputLayout())
 *       ->id("email_field")
 *       ->hint("Email Address")
 *       ->helperText("We'll never share your email")
 *       ->errorText("")
 *       ->counterEnabled(true)
 *       ->counterMaxLength(50)
 *       ->inputType("textEmailAddress");
 */
class TextInputLayout extends Component {
    public function toArray() {
        $data = parent::toArray();
        $data['type'] = 'TextInputLayout';
        return $data;
    }
}

/**
 * RadioGroup - Container for RadioButton components.
 * 
 * Usage:
 *   $group = (new RadioGroup([
 *       (new RadioButton())->id("opt1")->text("Option A"),
 *       (new RadioButton())->id("opt2")->text("Option B"),
 *       (new RadioButton())->id("opt3")->text("Option C"),
 *   ]))->id("my_radio_group")
 *     ->onCheckedChange("onRadioChange");
 */
class RadioGroup extends Component {
    private $children;
    
    public function __construct($children = []) {
        $this->children = $children;
    }
    
    public function addChild($child) {
        $this->children[] = $child;
        return $this;
    }
    
    public function toArray() {
        $data = parent::toArray();
        $data['type'] = 'RadioGroup';
        $data['children'] = array_map(function($child) {
            return ($child instanceof Component) ? $child->toArray() : $child;
        }, $this->children);
        return $data;
    }
    
    // ACTION METHODS
    
    public function _getCheckedId($default = null) {
        return $this->_get("checkedRadioButtonId", $default);
    }
    
    public function _checkButton(string $radioButtonId): array {
        return $this->_set("check", $radioButtonId);
    }
    
    public function _clearCheck(): array {
        return $this->_set("clearCheck", true);
    }
}

/**
 * TabLayout - Material Design tab bar.
 * 
 * Usage:
 *   $tabs = (new TabLayout())
 *       ->id("my_tabs")
 *       ->tabs([
 *           ["text" => "Home", "icon" => "home"],
 *           ["text" => "Profile", "icon" => "person"],
 *           ["text" => "Settings", "icon" => "settings"],
 *       ])
 *       ->selectedTab(0)
 *       ->onTabSelected("onTabChanged");
 */
class TabLayout extends Component {
    private $tabItems = [];
    
    public function tabs(array $tabs) {
        $this->tabItems = $tabs;
        return $this;
    }
    
    public function toArray() {
        $data = parent::toArray();
        $data['type'] = 'TabLayout';
        $data['tabs'] = $this->tabItems;
        return $data;
    }
    
    // ACTION METHODS
    
    public function _selectTab(int $index): array {
        return ["action" => "tab_select", "target" => $this->getId(), "index" => $index];
    }
    
    public function _getSelectedTab($default = 0): int {
        return (int) $this->_get("selectedTab", $default);
    }
    
    public function _setTabs(array $tabs): array {
        return ["action" => "tab_set_items", "target" => $this->getId(), "tabs" => $tabs];
    }
    
    public function _setBadge(int $tabIndex, $text): array {
        return ["action" => "tab_badge", "target" => $this->getId(), "index" => $tabIndex, "text" => $text];
    }
}

/**
 * Toolbar - Standard Android Toolbar (lighter than TopAppBar).
 * 
 * Usage:
 *   $toolbar = (new Toolbar())
 *       ->id("toolbar")
 *       ->title("My App")
 *       ->subtitle("Dashboard")
 *       ->backgroundColor("#333333")
 *       ->titleColor("#ffffff");
 */
class Toolbar extends Component {
    public function toArray() {
        $data = parent::toArray();
        $data['type'] = 'Toolbar';
        return $data;
    }
}

/**
 * SwipeRefreshLayout - Pull-to-refresh container.
 *
 * Usage:
 *   $swipe = (new SwipeRefreshLayout([$contentView]))
 *       ->id("swipe_refresh")
 *       ->onRefresh("onPullRefresh");
 */
class SwipeRefreshLayout extends Component {
    private $children;
    
    public function __construct($children = []) {
        $this->children = $children;
    }
    
    public function toArray() {
        $data = parent::toArray();
        $data['type'] = 'SwipeRefreshLayout';
        $data['children'] = array_map(function($child) {
            return ($child instanceof Component) ? $child->toArray() : $child;
        }, $this->children);
        return $data;
    }
    
    public function _setRefreshing(bool $refreshing): array {
        return $this->_set("refreshing", $refreshing);
    }
    
    public function _stopRefreshing(): array {
        return $this->_set("refreshing", false);
    }
}

/**
 * RecyclerView-like scrolling list built on top of ScrollView + LinearLayout.
 * Not a true RecyclerView, but provides the same PHP API with item templates.
 *
 * Usage:
 *   $recycler = (new RecyclerList())
 *       ->id("user_list")
 *       ->items([
 *           ["name" => "Alice", "role" => "Dev"],
 *           ["name" => "Bob", "role" => "Designer"],
 *       ])
 *       ->itemTemplate(function($item) {
 *           return (new HorizontalLayout([
 *               (new TextView())->text($item['name'])->textSize(18)->weight(1),
 *               (new TextView())->text($item['role'])->textColor("#888"),
 *           ]))->padding(12);
 *       })
 *       ->onItemClick("onUserClick");
 */
class RecyclerList extends Component {
    private $itemsData = [];
    private $templateFn = null;
    
    public function items(array $items) {
        $this->itemsData = $items;
        return $this;
    }
    
    public function itemTemplate(callable $fn) {
        $this->templateFn = $fn;
        return $this;
    }
    
    public function toArray() {
        // Expand items using template if provided
        $children = [];
        $onClick = $this->attributes['onItemClick'] ?? null;
        
        foreach ($this->itemsData as $index => $item) {
            $child = null;
            if ($this->templateFn) {
                $child = call_user_func($this->templateFn, $item, $index);
            } else {
                // Default: simple text
                $text = is_array($item) ? ($item['title'] ?? json_encode($item)) : (string) $item;
                $child = (new TextView())->text($text)->padding(12);
            }
            
            if ($child instanceof Component) {
                if ($onClick) {
                    $child->action($onClick)->tag(json_encode($item));
                }
                $children[] = $child->toArray();
            }
        }
        
        return [
            "type" => "ScrollView",
            "id" => $this->attributes['id'] ?? null,
            "children" => [
                array_merge(
                    ["type" => "VerticalLayout"],
                    array_filter($this->attributes, fn($k) => $k !== 'onItemClick', ARRAY_FILTER_USE_KEY),
                    ["children" => $children]
                )
            ]
        ];
    }
    
    // ACTION METHODS - delegate to list_* actions
    
    public function _setItems(array $items): array {
        return ["action" => "list_set_items", "target" => $this->getId(), "items" => $items];
    }
    
    public function _addItem($item): array {
        return ["action" => "list_add_item", "target" => $this->getId(), "item" => $item];
    }
    
    public function _removeItem(int $position): array {
        return ["action" => "list_remove_item", "target" => $this->getId(), "position" => $position];
    }
}

// =============================================================================
// LIST VIEW COMPONENT
// =============================================================================

/**
 * ListView component for displaying scrollable lists.
 * 
 * Usage:
 *   $list = (new ListView())
 *       ->id("my_list")
 *       ->items(["Item 1", "Item 2", "Item 3"])
 *       ->onItemClick("onListItemClick");
 * 
 * Or with custom item layout:
 *   $list = (new ListView())
 *       ->id("my_list")
 *       ->items([
 *           ["title" => "John", "subtitle" => "Developer"],
 *           ["title" => "Jane", "subtitle" => "Designer"],
 *       ])
 *       ->itemLayout("two_line")  // simple, two_line, icon
 *       ->onItemClick("onListItemClick");
 */
class ListView extends Component {
    
    /**
     * Set list items (simple strings or arrays for complex layouts)
     * @param array $items Array of strings or associative arrays
     */
    public function items(array $items) {
        $this->attributes['items'] = $items;
        return $this;
    }
    
    /**
     * Set the item layout style
     * @param string $layout "simple", "two_line", "icon", "checkbox"
     */
    public function itemLayout(string $layout) {
        $this->attributes['itemLayout'] = $layout;
        return $this;
    }
    
    /**
     * Set divider visibility
     */
    public function showDividers(bool $show = true) {
        $this->attributes['showDividers'] = $show;
        return $this;
    }
    
    /**
     * Set item click handler (PHP method name)
     */
    public function onItemClick(string $handler) {
        $this->attributes['onItemClick'] = $handler;
        return $this;
    }
    
    /**
     * Set item long click handler (PHP method name)
     */
    public function onItemLongClick(string $handler) {
        $this->attributes['onItemLongClick'] = $handler;
        return $this;
    }
    
    // =========================================================================
    // ACTION METHODS (runtime updates)
    // =========================================================================
    
    /**
     * Get all items (SYNCHRONOUS)
     */
    public function _getItems($default = []): array {
        return $this->_get("items", $default);
    }
    
    /**
     * Set/replace all items
     */
    public function _setItems(array $items): array {
        return [
            "action" => "list_set_items",
            "target" => $this->getId(),
            "items" => $items
        ];
    }
    
    /**
     * Add a single item to the end
     */
    public function _addItem($item): array {
        return [
            "action" => "list_add_item",
            "target" => $this->getId(),
            "item" => $item
        ];
    }
    
    /**
     * Add multiple items to the end
     */
    public function _addItems(array $items): array {
        return [
            "action" => "list_add_items",
            "target" => $this->getId(),
            "items" => $items
        ];
    }
    
    /**
     * Insert item at specific position
     */
    public function _insertItem(int $position, $item): array {
        return [
            "action" => "list_insert_item",
            "target" => $this->getId(),
            "position" => $position,
            "item" => $item
        ];
    }
    
    /**
     * Remove item at position
     */
    public function _removeItem(int $position): array {
        return [
            "action" => "list_remove_item",
            "target" => $this->getId(),
            "position" => $position
        ];
    }
    
    /**
     * Update item at position
     */
    public function _updateItem(int $position, $item): array {
        return [
            "action" => "list_update_item",
            "target" => $this->getId(),
            "position" => $position,
            "item" => $item
        ];
    }
    
    /**
     * Clear all items
     */
    public function _clear(): array {
        return $this->_setItems([]);
    }
    
    /**
     * Get selected item position (SYNCHRONOUS)
     */
    public function _getSelectedPosition($default = -1): int {
        return (int) $this->_get("selectedPosition", $default);
    }
    
    /**
     * Set selection
     */
    public function _setSelection(int $position): array {
        return $this->_set("selection", $position);
    }
    
    /**
     * Scroll to position
     */
    public function _scrollToPosition(int $position): array {
        return [
            "action" => "list_scroll",
            "target" => $this->getId(),
            "position" => $position
        ];
    }
    
    /**
     * Smooth scroll to position
     */
    public function _smoothScrollToPosition(int $position): array {
        return [
            "action" => "list_smooth_scroll",
            "target" => $this->getId(),
            "position" => $position
        ];
    }
    
    /**
     * Get item count (SYNCHRONOUS)
     */
    public function _getCount($default = 0): int {
        return (int) $this->_get("count", $default);
    }
}

// Material component with full path
class SwitchMaterial extends Component {
    public function toArray() {
        $data = parent::toArray();
        $data['type'] = "com.google.android.material.switchmaterial.SwitchMaterial";
        return $data;
    }
}

// Simple Switch alias (uses standard Android Switch widget)
class SwitchView extends Component {
    public function toArray() {
        $data = parent::toArray();
        $data['type'] = "Switch";
        return $data;
    }
}

// =============================================================================
// LAYOUT COMPONENTS
// =============================================================================

class VerticalLayout extends Component {
    private $children;

    public function __construct($children = []) {
        $this->children = $children;
    }
    
    public function addChild($child) {
        $this->children[] = $child;
        return $this;
    }
    
    public function addChildren(array $children) {
        $this->children = array_merge($this->children, $children);
        return $this;
    }

    public function toArray() {
        $data = parent::toArray();
        $data["children"] = array_map(function($child) {
            return ($child instanceof Component) ? $child->toArray() : $child;
        }, $this->children);
        return $data;
    }
}

class HorizontalLayout extends VerticalLayout {
    public function __construct($children = []) {
        parent::__construct($children);
        $this->orientation("horizontal");
    }
}

class ScrollView extends VerticalLayout {}
class HorizontalScrollView extends VerticalLayout {}
class FrameLayout extends VerticalLayout {}
class RelativeLayout extends VerticalLayout {}

/**
 * CardView - Material Design card container with elevation and corners.
 * 
 * Usage:
 *   $card = (new CardView([
 *       (new TextView())->text("Card Title")->textSize(18),
 *       (new TextView())->text("Card body content"),
 *   ]))->cornerRadius(12)->elevation(4)->padding(16)->margin(8);
 */
class CardView extends VerticalLayout {}

/**
 * GridLayout - Arranges children in a grid.
 * 
 * Usage:
 *   $grid = (new GridLayout([
 *       (new Button())->text("1")->layoutRow(0)->layoutColumn(0),
 *       (new Button())->text("2")->layoutRow(0)->layoutColumn(1),
 *       (new Button())->text("3")->layoutRow(1)->layoutColumn(0),
 *       (new Button())->text("4")->layoutRow(1)->layoutColumn(1),
 *   ]))->columnCount(2)->rowCount(2);
 */
class GridLayout extends VerticalLayout {
    public function toArray() {
        $data = parent::toArray();
        $data['type'] = 'GridLayout';
        return $data;
    }
}

/**
 * TableLayout - Table with rows.
 * 
 * Usage:
 *   $table = (new TableLayout([
 *       new TableRow([(new TextView())->text("Name"), (new TextView())->text("Age")]),
 *       new TableRow([(new TextView())->text("Alice"), (new TextView())->text("30")]),
 *   ]))->stretchColumns("*");
 */
class TableLayout extends VerticalLayout {
    public function toArray() {
        $data = parent::toArray();
        $data['type'] = 'TableLayout';
        return $data;
    }
}

/**
 * TableRow - Single row inside a TableLayout.
 */
class TableRow extends VerticalLayout {
    public function __construct($children = []) {
        parent::__construct($children);
    }
    
    public function toArray() {
        $data = parent::toArray();
        $data['type'] = 'TableRow';
        return $data;
    }
}

/**
 * ConstraintLayout-like positioning using a FrameLayout with gravity.
 * 
 * Usage:
 *   $stack = (new StackLayout([
 *       (new ImageView())->src("bg.jpg")->width(-1)->height(-1),
 *       (new TextView())->text("Overlay")->layoutGravity("center"),
 *       (new FloatingActionButton())->icon("add")->layoutGravity("bottom|end"),
 *   ]));
 */
class StackLayout extends VerticalLayout {
    public function toArray() {
        $data = parent::toArray();
        $data['type'] = 'FrameLayout';
        return $data;
    }
}

// =============================================================================
// NAVIGATION COMPONENTS - Drawer, TopAppBar, BottomNavBar
// =============================================================================

/**
 * DrawerLayout - Container for navigation drawer pattern.
 * 
 * Usage:
 *   return (new DrawerLayout())
 *       ->id("main_drawer")
 *       ->drawer($drawerContent)      // The side menu content
 *       ->content($mainContent)       // The main screen content
 *       ->drawerWidth(280);           // Optional: drawer width in dp
 */
class DrawerLayout extends Component {
    private $drawerContent = null;
    private $mainContent = null;
    
    /**
     * Set the drawer (side menu) content
     */
    public function drawer($content) {
        $this->drawerContent = $content;
        return $this;
    }
    
    /**
     * Set the main content
     */
    public function content($content) {
        $this->mainContent = $content;
        return $this;
    }
    
    /**
     * Set drawer width in dp
     */
    public function drawerWidth(int $width) {
        $this->attributes['drawerWidth'] = $width;
        return $this;
    }
    
    /**
     * Set drawer gravity (start or end)
     */
    public function drawerGravity(string $gravity) {
        $this->attributes['drawerGravity'] = $gravity;
        return $this;
    }
    
    public function toArray() {
        $data = parent::toArray();
        $data['type'] = 'DrawerLayout';
        
        if ($this->drawerContent) {
            $data['drawer'] = ($this->drawerContent instanceof Component) 
                ? $this->drawerContent->toArray() 
                : $this->drawerContent;
        }
        
        if ($this->mainContent) {
            $data['content'] = ($this->mainContent instanceof Component) 
                ? $this->mainContent->toArray() 
                : $this->mainContent;
        }
        
        return $data;
    }
    
    // =========================================================================
    // ACTION METHODS
    // =========================================================================
    
    public function _open(): array {
        return ["action" => "drawer_open", "target" => $this->getId()];
    }
    
    public function _close(): array {
        return ["action" => "drawer_close", "target" => $this->getId()];
    }
    
    public function _toggle(): array {
        return ["action" => "drawer_toggle", "target" => $this->getId()];
    }
    
    public function _isOpen(): bool {
        return (bool) $this->_get("drawerOpen", false);
    }
}

/**
 * NavigationDrawer - Side navigation menu content.
 * 
 * Usage:
 *   $drawer = (new NavigationDrawer())
 *       ->id("nav_drawer")
 *       ->header($headerView)   // Optional header (e.g., user info)
 *       ->items([
 *           ["id" => "home", "title" => "Home", "icon" => "home"],
 *           ["id" => "profile", "title" => "Profile", "icon" => "person"],
 *           ["id" => "settings", "title" => "Settings", "icon" => "settings"],
 *           "divider",  // Add a divider
 *           ["id" => "logout", "title" => "Logout", "icon" => "exit_to_app"],
 *       ])
 *       ->onItemSelected("onNavItemSelected");
 */
class NavigationDrawer extends Component {
    private $headerContent = null;
    private $menuItems = [];
    
    /**
     * Set header content (optional)
     */
    public function header($content) {
        $this->headerContent = $content;
        return $this;
    }
    
    /**
     * Set menu items
     * @param array $items Array of items: ["id" => "", "title" => "", "icon" => ""] or "divider"
     */
    public function items(array $items) {
        $this->menuItems = $items;
        return $this;
    }
    
    /**
     * Set item selection handler
     */
    public function onItemSelected(string $handler) {
        $this->attributes['onItemSelected'] = $handler;
        return $this;
    }
    
    /**
     * Set selected item ID
     */
    public function selectedItem(string $itemId) {
        $this->attributes['selectedItem'] = $itemId;
        return $this;
    }
    
    public function toArray() {
        $data = parent::toArray();
        $data['type'] = 'NavigationDrawer';
        $data['items'] = $this->menuItems;
        
        if ($this->headerContent) {
            $data['header'] = ($this->headerContent instanceof Component) 
                ? $this->headerContent->toArray() 
                : $this->headerContent;
        }
        
        return $data;
    }
    
    // =========================================================================
    // ACTION METHODS
    // =========================================================================
    
    public function _setSelectedItem(string $itemId): array {
        return $this->_set("selectedItem", $itemId);
    }
    
    public function _getSelectedItem($default = null): ?string {
        return $this->_get("selectedItem", $default);
    }
    
    public function _setItems(array $items): array {
        return ["action" => "nav_set_items", "target" => $this->getId(), "items" => $items];
    }
}

/**
 * TopAppBar - Material Design top app bar / toolbar.
 * 
 * Usage:
 *   $appBar = (new TopAppBar())
 *       ->id("app_bar")
 *       ->title("My App")
 *       ->subtitle("Welcome")
 *       ->navigationIcon("menu")      // Shows hamburger icon
 *       ->onNavigationClick("onMenuClick")
 *       ->actions([
 *           ["id" => "search", "icon" => "search"],
 *           ["id" => "more", "icon" => "more_vert"],
 *       ])
 *       ->onActionClick("onActionClick");
 */
class TopAppBar extends Component {
    private $actionItems = [];
    
    /**
     * Set the title
     */
    public function title(string $title) {
        $this->attributes['title'] = $title;
        return $this;
    }
    
    /**
     * Set the subtitle
     */
    public function subtitle(string $subtitle) {
        $this->attributes['subtitle'] = $subtitle;
        return $this;
    }
    
    /**
     * Set navigation icon (e.g., "menu", "arrow_back")
     */
    public function navigationIcon(string $icon) {
        $this->attributes['navigationIcon'] = $icon;
        return $this;
    }
    
    /**
     * Set navigation click handler
     */
    public function onNavigationClick(string $handler) {
        $this->attributes['onNavigationClick'] = $handler;
        return $this;
    }
    
    /**
     * Set action items (toolbar buttons)
     * @param array $actions Array of ["id" => "", "icon" => "", "title" => ""]
     */
    public function actions(array $actions) {
        $this->actionItems = $actions;
        return $this;
    }
    
    /**
     * Set action click handler
     */
    public function onActionClick(string $handler) {
        $this->attributes['onActionClick'] = $handler;
        return $this;
    }
    
    /**
     * Set background color
     */
    public function backgroundColor(string $color) {
        $this->attributes['backgroundColor'] = $color;
        return $this;
    }
    
    /**
     * Set title text color
     */
    public function titleColor(string $color) {
        $this->attributes['titleColor'] = $color;
        return $this;
    }
    
    /**
     * Set elevation
     */
    public function elevation(int $dp) {
        $this->attributes['elevation'] = $dp;
        return $this;
    }
    
    public function toArray() {
        $data = parent::toArray();
        $data['type'] = 'TopAppBar';
        if (!empty($this->actionItems)) {
            $data['actions'] = $this->actionItems;
        }
        return $data;
    }
    
    // =========================================================================
    // ACTION METHODS
    // =========================================================================
    
    public function _setTitle(string $title): array {
        return $this->_set("title", $title);
    }
    
    public function _setSubtitle(string $subtitle): array {
        return $this->_set("subtitle", $subtitle);
    }
    
    public function _setNavigationIcon(string $icon): array {
        return $this->_set("navigationIcon", $icon);
    }
}

/**
 * BottomNavBar - Material Design bottom navigation bar.
 * 
 * Usage:
 *   $bottomNav = (new BottomNavBar())
 *       ->id("bottom_nav")
 *       ->items([
 *           ["id" => "home", "title" => "Home", "icon" => "home"],
 *           ["id" => "search", "title" => "Search", "icon" => "search"],
 *           ["id" => "profile", "title" => "Profile", "icon" => "person"],
 *       ])
 *       ->selectedItem("home")
 *       ->onItemSelected("onBottomNavSelected");
 */
class BottomNavBar extends Component {
    private $navItems = [];
    
    /**
     * Set navigation items (3-5 items recommended)
     * @param array $items Array of ["id" => "", "title" => "", "icon" => ""]
     */
    public function items(array $items) {
        $this->navItems = $items;
        return $this;
    }
    
    /**
     * Set initially selected item
     */
    public function selectedItem(string $itemId) {
        $this->attributes['selectedItem'] = $itemId;
        return $this;
    }
    
    /**
     * Set item selection handler
     */
    public function onItemSelected(string $handler) {
        $this->attributes['onItemSelected'] = $handler;
        return $this;
    }
    
    /**
     * Set background color
     */
    public function backgroundColor(string $color) {
        $this->attributes['backgroundColor'] = $color;
        return $this;
    }
    
    /**
     * Set selected item color
     */
    public function selectedColor(string $color) {
        $this->attributes['selectedColor'] = $color;
        return $this;
    }
    
    /**
     * Set unselected item color
     */
    public function unselectedColor(string $color) {
        $this->attributes['unselectedColor'] = $color;
        return $this;
    }
    
    /**
     * Show/hide item labels
     */
    public function showLabels(bool $show) {
        $this->attributes['showLabels'] = $show;
        return $this;
    }
    
    public function toArray() {
        $data = parent::toArray();
        $data['type'] = 'BottomNavBar';
        $data['items'] = $this->navItems;
        return $data;
    }
    
    // =========================================================================
    // ACTION METHODS
    // =========================================================================
    
    public function _setSelectedItem(string $itemId): array {
        return ["action" => "bottom_nav_select", "target" => $this->getId(), "itemId" => $itemId];
    }
    
    public function _getSelectedItem($default = null): ?string {
        return $this->_get("selectedItem", $default);
    }
    
    public function _setBadge(string $itemId, $count): array {
        return ["action" => "bottom_nav_badge", "target" => $this->getId(), "itemId" => $itemId, "count" => $count];
    }
    
    public function _clearBadge(string $itemId): array {
        return ["action" => "bottom_nav_badge", "target" => $this->getId(), "itemId" => $itemId, "count" => 0];
    }
}

/**
 * Helper function to create a standard app layout with drawer.
 * 
 * @param array $config Configuration array:
 *   - 'title': App bar title
 *   - 'drawerItems': Navigation drawer items
 *   - 'bottomNavItems': Bottom navigation items (optional)
 *   - 'content': Main content view
 *   - 'onDrawerItemSelected': Handler for drawer selection
 *   - 'onBottomNavSelected': Handler for bottom nav selection
 */
function appWithDrawer(array $config) {
    $title = $config['title'] ?? 'App';
    $drawerItems = $config['drawerItems'] ?? [];
    $content = $config['content'] ?? new VerticalLayout([]);
    $onDrawerItem = $config['onDrawerItemSelected'] ?? 'onDrawerItemSelected';
    
    // Create app bar
    $appBar = (new TopAppBar())
        ->id("app_bar")
        ->title($title)
        ->navigationIcon("menu")
        ->onNavigationClick("onToggleDrawer");
    
    // Create drawer content
    $drawer = (new NavigationDrawer())
        ->id("nav_drawer")
        ->items($drawerItems)
        ->onItemSelected($onDrawerItem);
    
    // Main content with app bar
    $mainLayout = new VerticalLayout([
        $appBar,
        $content
    ]);
    
    // Add bottom nav if specified
    if (!empty($config['bottomNavItems'])) {
        $bottomNav = (new BottomNavBar())
            ->id("bottom_nav")
            ->items($config['bottomNavItems'])
            ->onItemSelected($config['onBottomNavSelected'] ?? 'onBottomNavSelected');
        
        // Wrap content to have bottom nav at bottom
        $mainLayout = new VerticalLayout([
            $appBar,
            (new VerticalLayout([$content]))->weight(1),
            $bottomNav
        ]);
    }
    
    return (new DrawerLayout())
        ->id("main_drawer")
        ->drawer($drawer)
        ->content($mainLayout)
        ->marginTop(30); // Add top margin to avoid status bar
}

// =============================================================================
// VIEW STATE - Shared file approach for getting view properties
// =============================================================================

/**
 * Path to the shared view state file.
 * Java writes this file before calling PHP.
 */
function getViewStateFile(): string {
    return dirname(__FILE__) . '/view_state.json';
}

/**
 * Read the current view state from the shared file.
 * @return array Associative array of view states: [viewId => [property => value, ...], ...]
 */
function readViewState(): array {
    $file = getViewStateFile();
    if (!file_exists($file)) {
        return [];
    }
    $content = file_get_contents($file);
    return json_decode($content, true) ?? [];
}

/**
 * Get a view property value directly from the shared state file.
 * This is SYNCHRONOUS - the value is already available.
 * 
 * @param string $viewId The view ID
 * @param string $property The property name (e.g., "text", "checked")
 * @param mixed $default Default value if property not found
 * @return mixed The property value or default
 * 
 * Usage:
 *   $text = getViewProperty("my_input", "text");
 *   $isChecked = getViewProperty("my_checkbox", "checked", false);
 */
function getViewProperty(string $viewId, string $property, $default = null) {
    $state = readViewState();
    return $state[$viewId][$property] ?? $default;
}

/**
 * Get all properties of a view.
 * @param string $viewId The view ID
 * @return array All properties of the view, or empty array
 */
function getViewState(string $viewId): array {
    $state = readViewState();
    return $state[$viewId] ?? [];
}

/**
 * Update a view's attributes (returns action for Java to process).
 */
function updateView(string $viewId, array $attributes): array {
    return [
        "action" => "update",
        "target" => $viewId,
        "attributes" => $attributes
    ];
}

/**
 * Update multiple views at once.
 * @param array $updates [viewId => [prop => value, ...], ...]
 * @return array Action array for Java
 * 
 * Usage:
 *   return updateMany([
 *       "counter" => ["text" => "Count: 5"],
 *       "btn" => ["tag" => "5", "enabled" => true]
 *   ]);
 */
function updateMany(array $updates): array {
    return [
        "action" => "update_many",
        "updates" => $updates
    ];
}

/**
 * Combine multiple action calls into one batch.
 * Takes the return values of _set* methods and merges them.
 * 
 * @param array ...$actions Individual action arrays from _set* methods
 * @return array Combined update_many action
 * 
 * Usage:
 *   return batch(
 *       $counterLabel->_setText("Count: 5"),
 *       $incrementBtn->_setTag("5"),
 *       $statusLabel->_setTextColor("#00ff00")
 *   );
 */
function batch(...$actions): array {
    $updates = [];
    $nonUpdateActions = [];
    
    foreach ($actions as $action) {
        if (isset($action['target']) && isset($action['attributes'])) {
            $viewId = $action['target'];
            if (!isset($updates[$viewId])) {
                $updates[$viewId] = [];
            }
            $updates[$viewId] = array_merge($updates[$viewId], $action['attributes']);
        } else {
            $nonUpdateActions[] = $action;
        }
    }
    
    // If we have mixed actions, return a batch
    if (!empty($nonUpdateActions) && !empty($updates)) {
        return [
            "action" => "batch",
            "actions" => array_merge(
                [updateMany($updates)],
                $nonUpdateActions
            )
        ];
    }
    
    if (!empty($nonUpdateActions)) {
        return [
            "action" => "batch",
            "actions" => $nonUpdateActions
        ];
    }
    
    return updateMany($updates);
}

// =============================================================================
// DIALOG / SNACKBAR / POPUP ACTIONS
// =============================================================================

/**
 * Show a Snackbar message (Material Design bottom notification).
 * 
 * @param string $message Message text
 * @param string|null $actionText Button text (e.g., "UNDO")
 * @param string|null $actionCallback PHP method to call when action clicked
 * @param int $duration Duration: 0=short, 1=long, 2=indefinite
 * @return array Action
 * 
 * Usage:
 *   return snackbar("Item deleted", "UNDO", "onUndoDelete");
 */
function snackbar(string $message, ?string $actionText = null, ?string $actionCallback = null, int $duration = 0): array {
    $action = [
        "action" => "SNACKBAR",
        "message" => $message,
        "duration" => $duration
    ];
    if ($actionText) $action["actionText"] = $actionText;
    if ($actionCallback) $action["actionCallback"] = $actionCallback;
    return $action;
}

/**
 * Show a confirmation dialog with Yes/No buttons.
 * 
 * @param string $title Dialog title
 * @param string $message Dialog message
 * @param string $onYes PHP method for positive button
 * @param string|null $onNo PHP method for negative button
 * @param string $yesText Positive button text
 * @param string $noText Negative button text
 * @return array Action
 * 
 * Usage:
 *   return confirmDialog("Delete?", "Are you sure?", "onConfirmDelete", "onCancel");
 */
function dialog(string $title, string $message, string $onYes, ?string $onNo = null, string $yesText = "OK", string $noText = "Cancel"): array {
    $action = [
        "action" => "DIALOG",
        "title" => $title,
        "message" => $message,
        "positiveText" => $yesText,
        "positiveCallback" => $onYes,
    ];
    if ($onNo) {
        $action["negativeText"] = $noText;
        $action["negativeCallback"] = $onNo;
    }
    return $action;
}

/**
 * Show a list dialog.
 * 
 * @param string $title Dialog title
 * @param array $items List of items to show
 * @param string $onSelect PHP method called with selected index and item
 * @return array Action
 * 
 * Usage:
 *   return listDialog("Choose Color", ["Red", "Green", "Blue"], "onColorSelect");
 */
function listDialog(string $title, array $items, string $onSelect): array {
    return [
        "action" => "LIST_DIALOG",
        "title" => $title,
        "items" => $items,
        "callback" => $onSelect
    ];
}

/**
 * Show a date picker dialog.
 * 
 * @param string $callback PHP method to call with selected date
 * @param string|null $initialDate Initial date (Y-m-d format)
 * @return array Action
 * 
 * Usage:
 *   return datePickerDialog("onDateSelected");
 *   // Callback receives: ['year' => 2026, 'month' => 4, 'day' => 9]
 */
function datePickerDialog(string $callback, ?string $initialDate = null): array {
    $action = [
        "action" => "DATE_PICKER_DIALOG",
        "callback" => $callback
    ];
    if ($initialDate) $action["initialDate"] = $initialDate;
    return $action;
}

/**
 * Show a time picker dialog.
 * 
 * @param string $callback PHP method to call with selected time
 * @param bool $is24Hour Use 24-hour format
 * @return array Action
 * 
 * Usage:
 *   return timePickerDialog("onTimeSelected");
 *   // Callback receives: ['hour' => 14, 'minute' => 30]
 */
function timePickerDialog(string $callback, bool $is24Hour = true): array {
    return [
        "action" => "TIME_PICKER_DIALOG",
        "callback" => $callback,
        "is24Hour" => $is24Hour
    ];
}

/**
 * Show an input dialog (text field in a dialog).
 * 
 * @param string $title Dialog title
 * @param string $hint Input hint
 * @param string $callback PHP method to call with entered text
 * @param string $initialValue Pre-filled value
 * @return array Action
 * 
 * Usage:
 *   return inputDialog("Rename", "Enter new name", "onRename", "current name");
 */
function inputDialog(string $title, string $hint, string $callback, string $initialValue = ""): array {
    return [
        "action" => "INPUT_DIALOG",
        "title" => $title,
        "hint" => $hint,
        "callback" => $callback,
        "initialValue" => $initialValue
    ];
}

/**
 * Show a bottom sheet dialog.
 * 
 * @param Component $content The content to show in the bottom sheet
 * @return array Action
 * 
 * Usage:
 *   return bottomSheet(
 *       (new VerticalLayout([
 *           (new TextView())->text("Options")->textSize(20)->padding(16),
 *           (new Button())->text("Share")->action("onShare"),
 *           (new Button())->text("Delete")->action("onDelete"),
 *       ]))->padding(16)
 *   );
 */
function bottomSheet(Component $content): array {
    return [
        "action" => "BOTTOM_SHEET",
        "content" => $content->toArray()
    ];
}

/**
 * Dismiss any currently open dialog/bottom sheet.
 * 
 * @return array Action
 */
function dismissDialog(): array {
    return ["action" => "DISMISS_DIALOG"];
}

// =============================================================================
// ANIMATION ACTIONS
// =============================================================================

/**
 * Animate a view's properties.
 * 
 * @param string $viewId View ID to animate
 * @param array $properties Target property values
 * @param int $duration Animation duration in ms
 * @param string $interpolator "linear", "accelerate", "decelerate", "overshoot", "bounce"
 * @return array Action
 * 
 * Usage:
 *   return animate("my_view", ["alpha" => 0, "translationY" => -100], 300);
 */
function animate(string $viewId, array $properties, int $duration = 300, string $interpolator = "decelerate"): array {
    return [
        "action" => "ANIMATE",
        "target" => $viewId,
        "properties" => $properties,
        "duration" => $duration,
        "interpolator" => $interpolator
    ];
}

/**
 * Animate multiple views sequentially or in parallel.
 * 
 * @param array $animations Array of animate() results
 * @param bool $sequential True = play one after another, false = all at once
 * @return array Action
 */
function animateSet(array $animations, bool $sequential = false): array {
    return [
        "action" => "ANIMATE_SET",
        "animations" => $animations,
        "sequential" => $sequential
    ];
}

// =============================================================================
// CLIPBOARD / SHARE ACTIONS
// =============================================================================

/**
 * Copy text to clipboard.
 * 
 * @param string $text Text to copy
 * @param string $label Clipboard label
 * @return array Action
 */
function copyToClipboard(string $text, string $label = "Copied"): array {
    return [
        "action" => "CLIPBOARD_COPY",
        "text" => $text,
        "label" => $label
    ];
}

/**
 * Open Android share sheet.
 * 
 * @param string $text Text to share
 * @param string $title Share dialog title
 * @return array Action
 */
function share(string $text, string $title = "Share"): array {
    return [
        "action" => "SHARE",
        "text" => $text,
        "title" => $title
    ];
}

/**
 * Open a URL in external browser.
 * 
 * @param string $url URL to open
 * @return array Action
 */
function openUrl(string $url): array {
    return [
        "action" => "OPEN_URL",
        "url" => $url
    ];
}

/**
 * Scroll a ScrollView to a specific view or position.
 * 
 * @param string $scrollViewId ScrollView ID
 * @param string|int $target View ID to scroll to, or pixel position
 * @param bool $smooth Use smooth scrolling
 * @return array Action
 */
function scrollTo(string $scrollViewId, $target, bool $smooth = true): array {
    return [
        "action" => "SCROLL_TO",
        "target" => $scrollViewId,
        "scrollTarget" => $target,
        "smooth" => $smooth
    ];
}

/**
 * Remove a view from the layout entirely.
 * 
 * @param string $viewId View ID to remove
 * @return array Action
 */
function removeView(string $viewId): array {
    return [
        "action" => "REMOVE_VIEW",
        "target" => $viewId
    ];
}

/**
 * Add a child view to a container dynamically.
 * 
 * @param string $parentId Parent container ID
 * @param Component $child Child component to add
 * @param int $index Insert position (-1 = end)
 * @return array Action
 */
function addView(string $parentId, Component $child, int $index = -1): array {
    return [
        "action" => "ADD_VIEW",
        "target" => $parentId,
        "child" => $child->toArray(),
        "index" => $index
    ];
}

/**
 * Replace all children of a container.
 * 
 * @param string $parentId Parent container ID
 * @param array $children Array of Component objects
 * @return array Action
 */
function replaceChildren(string $parentId, array $children): array {
    return [
        "action" => "REPLACE_CHILDREN",
        "target" => $parentId,
        "children" => array_map(function($c) {
            return ($c instanceof Component) ? $c->toArray() : $c;
        }, $children)
    ];
}

// =============================================================================
// NATIVE / SENSOR / HARDWARE ACTIONS (DroidScript Bridge)
// =============================================================================
//
// These functions return action arrays that Java intercepts in processPhpResponse().
// Java injects JavaScript into DroidScript to access device hardware, then
// delivers the result to your PHP callback method asynchronously.
//
// Flow: PHP returns DS_SENSOR_CALL → Java injects JS → DroidScript reads sensor
//       → JS calls _phpPlugin.OnSensorResult → Java calls your PHP callback
// =============================================================================

/**
 * Generic DroidScript native call via the sensor bridge.
 * 
 * @param string $type Sensor/native type identifier
 * @param string $callback PHP method to receive the result
 * @param array $params Additional parameters for the call
 * @return array Action array
 */
function nativeCall(string $type, string $callback, array $params = []): array {
    return array_merge([
        "action" => "DS_SENSOR_CALL",
        "sensor" => $type,
        "callback" => $callback
    ], $params);
}

// ---- Motion Sensors ----

/**
 * Read accelerometer values.
 * Callback receives: ['x' => float, 'y' => float, 'z' => float]
 */
function readAccelerometer(string $callback): array {
    return nativeCall("accelerometer", $callback);
}

/**
 * Read gyroscope values (rotation rate).
 * Callback receives: ['x' => float, 'y' => float, 'z' => float]
 */
function readGyroscope(string $callback): array {
    return nativeCall("gyroscope", $callback);
}

/**
 * Read gravity sensor values.
 * Callback receives: ['x' => float, 'y' => float, 'z' => float]
 */
function readGravity(string $callback): array {
    return nativeCall("gravity", $callback);
}

/**
 * Read orientation/compass (azimuth, pitch, roll).
 * Callback receives: ['azimuth' => float, 'pitch' => float, 'roll' => float]
 */
function readOrientation(string $callback): array {
    return nativeCall("compass", $callback);
}

/**
 * Read magnetic field sensor.
 * Callback receives: ['x' => float, 'y' => float, 'z' => float]
 */
function readMagneticField(string $callback): array {
    return nativeCall("magneticfield", $callback);
}

// ---- Environment Sensors ----

/**
 * Read ambient light sensor (lux).
 * Callback receives: ['light' => float]
 */
function readLight(string $callback): array {
    return nativeCall("light", $callback);
}

/**
 * Read proximity sensor.
 * Callback receives: ['distance' => float, 'near' => bool]
 */
function readProximity(string $callback): array {
    return nativeCall("proximity", $callback);
}

/**
 * Read barometric pressure sensor (hPa).
 * Callback receives: ['pressure' => float]
 */
function readPressure(string $callback): array {
    return nativeCall("pressure", $callback);
}

/**
 * Read relative humidity sensor (%).
 * Callback receives: ['humidity' => float]
 */
function readHumidity(string $callback): array {
    return nativeCall("humidity", $callback);
}

/**
 * Read ambient temperature sensor (°C).
 * Callback receives: ['temperature' => float]
 */
function readTemperature(string $callback): array {
    return nativeCall("temperature", $callback);
}

/**
 * Read step counter since last reboot.
 * Callback receives: ['steps' => int]
 */
function readStepCounter(string $callback): array {
    return nativeCall("stepcounter", $callback);
}

// ---- Location ----

/**
 * Read GPS/network location.
 * Callback receives: ['lat' => float, 'lng' => float, 'altitude' => float, 'speed' => float, 'bearing' => float]
 */
function readLocation(string $callback): array {
    return nativeCall("location", $callback);
}

/**
 * Check if location services are enabled.
 * Callback receives: ['enabled' => bool]
 */
function checkLocationEnabled(string $callback): array {
    return nativeCall("locationenabled", $callback);
}

// ---- Battery & Power ----

/**
 * Read battery status.
 * Callback receives: ['level' => int, 'charging' => bool, 'chargeType' => string]
 */
function readBattery(string $callback): array {
    return nativeCall("battery", $callback);
}

// ---- Device Info ----

/**
 * Get device information.
 * Callback receives: ['model' => string, 'osVersion' => int, 'apiLevel' => int,
 *   'deviceId' => string, 'isTablet' => bool, 'language' => string,
 *   'country' => string, 'appName' => string, 'packageName' => string, 'freeSpace' => float]
 */
function readDeviceInfo(string $callback): array {
    return nativeCall("deviceinfo", $callback);
}

/**
 * Get screen dimensions and density.
 * Callback receives: ['width' => int, 'height' => int, 'density' => float,
 *   'rotation' => int, 'orientation' => string]
 */
function readScreenInfo(string $callback): array {
    return nativeCall("screeninfo", $callback);
}

// ---- Network / Connectivity ----

/**
 * Get WiFi info (SSID, IP, signal).
 * Callback receives: ['ssid' => string, 'ip' => string, 'connected' => bool, 'rssi' => int]
 */
function readWifiInfo(string $callback): array {
    return nativeCall("wifi", $callback);
}

/**
 * Scan for nearby WiFi networks.
 * Callback receives: ['networks' => array]
 */
function scanWifi(string $callback): array {
    return nativeCall("wifiscan", $callback);
}

/**
 * Get Bluetooth status and paired devices.
 * Callback receives: ['enabled' => bool, 'paired' => array]
 */
function readBluetoothInfo(string $callback): array {
    return nativeCall("bluetooth", $callback);
}

/**
 * Discover nearby Bluetooth devices.
 * Callback receives: ['devices' => array]
 */
function discoverBluetooth(string $callback): array {
    return nativeCall("btdiscover", $callback);
}

/**
 * Get network connectivity info.
 * Callback receives: ['connected' => bool, 'type' => string, 'ip' => string, 'mac' => string]
 */
function readNetworkInfo(string $callback): array {
    return nativeCall("networkinfo", $callback);
}

// ---- HTTP Requests ----

/**
 * Make an HTTP request via DroidScript.
 * Callback receives: ['status' => int, 'response' => string]
 */
function httpRequest(string $url, string $callback, string $method = "GET", ?string $body = null, ?string $headers = null): array {
    return nativeCall("http", $callback, [
        "url" => $url,
        "httpMethod" => $method,
        "body" => $body,
        "headers" => $headers
    ]);
}

function httpGet(string $url, string $callback): array {
    return httpRequest($url, $callback, "GET");
}

function httpPost(string $url, string $callback, ?string $body = null): array {
    return httpRequest($url, $callback, "POST", $body);
}

/**
 * Download a file from URL.
 * Callback receives: ['file' => string, 'success' => bool]
 */
function downloadFile(string $url, string $destPath, string $callback): array {
    return nativeCall("download", $callback, [
        "url" => $url,
        "dest" => $destPath
    ]);
}

// ---- Camera & Media ----

/**
 * Take a photo with the device camera.
 * Opens the image chooser with Camera and Internal storage options.
 * Callback receives: ['file' => string] on success, ['error' => 'cancelled'] on cancel
 * Note: The quality parameter is currently ignored.
 */
function takePhoto(string $callback, int $quality = 80): array {
    return nativeCall("camera", $callback, ["quality" => (string)$quality]);
}

/**
 * Play an audio file.
 * Callback receives: ['status' => 'playing', 'file' => string]
 */
function playAudio(string $file, string $callback): array {
    return nativeCall("playaudio", $callback, ["file" => $file]);
}

/**
 * Stop audio playback.
 * Callback receives: ['status' => 'stopped']
 */
function stopAudio(string $callback = "handle_stopaudio"): array {
    return nativeCall("stopaudio", $callback);
}

/**
 * Start recording audio.
 * Callback receives: ['status' => 'recording', 'file' => string]
 */
function recordAudio(string $file, string $callback): array {
    return nativeCall("recordaudio", $callback, ["file" => $file]);
}

/**
 * Stop recording audio.
 * Callback receives: ['status' => 'stopped', 'file' => string]
 */
function stopRecording(string $callback = "handle_stoprecording"): array {
    return nativeCall("stoprecording", $callback);
}

/**
 * Play a system ringtone.
 * @param string $type "notification", "alarm", or "ringtone"
 */
function playRingtone(string $type = "notification"): array {
    return nativeCall("ringtone", "handle_ringtone", ["ringtoneType" => $type]);
}

// ---- Text-to-Speech & Speech Recognition ----

/**
 * Speak text aloud using TTS.
 * Callback receives: ['done' => true]
 */
function textToSpeech(string $text, string $callback = "handle_tts", float $pitch = 1.0, float $rate = 1.0): array {
    return nativeCall("speech", $callback, [
        "text" => $text,
        "pitch" => $pitch,
        "rate" => $rate
    ]);
}

/**
 * Start speech recognition (speech to text).
 * Callback receives: ['text' => string]
 */
function speechRecognition(string $callback): array {
    return nativeCall("speechrecognition", $callback);
}

// ---- Vibration ----

/**
 * Vibrate the device.
 * @param string $pattern Comma-separated vibration pattern in ms: "on,off,on,off..."
 */
function vibrate(string $pattern = "100,50,100"): array {
    return nativeCall("vibrate", "handle_vibrate", ["pattern" => $pattern]);
}

// ---- Volume & Audio ----

/**
 * Get current volume level.
 * Callback receives: ['volume' => int, 'max' => int, 'stream' => string]
 */
function getVolume(string $callback, string $stream = "music"): array {
    return nativeCall("getvolume", $callback, ["stream" => $stream]);
}

/**
 * Set volume level (0-15 typically).
 */
function setVolume(int $level, string $stream = "music"): array {
    return nativeCall("setvolume", "handle_setvolume", [
        "level" => $level,
        "stream" => $stream
    ]);
}

/**
 * Set ringer mode.
 * @param string $mode "normal", "vibrate", or "silent"
 */
function setRingerMode(string $mode): array {
    return nativeCall("setringermode", "handle_setringermode", ["mode" => $mode]);
}

// ---- Screen ----

/**
 * Set screen brightness (0.0 to 1.0).
 */
function setScreenBrightness(float $level): array {
    return nativeCall("setbrightness", "handle_setbrightness", ["level" => $level]);
}

/**
 * Prevent/allow the screen from locking.
 */
function preventScreenLock(bool $prevent = true): array {
    return nativeCall("preventscreenlock", "handle_preventscreenlock", ["prevent" => $prevent]);
}

// ---- Clipboard ----

/**
 * Copy text to clipboard.
 */
function setClipboard(string $text): array {
    return nativeCall("clipboard_set", "handle_clipboard", ["text" => $text]);
}

/**
 * Get text from clipboard.
 * Callback receives: ['text' => string]
 */
function getClipboard(string $callback): array {
    return nativeCall("clipboard_get", $callback);
}

// ---- SMS & Phone ----

/**
 * Send an SMS message.
 * Callback receives: ['sent' => bool, 'phone' => string]
 */
function sendSms(string $phone, string $message, string $callback = "handle_sms"): array {
    return nativeCall("sms", $callback, [
        "phone" => $phone,
        "message" => $message
    ]);
}

/**
 * Make a phone call.
 */
function phoneCall(string $number): array {
    return nativeCall("phonecall", "handle_phonecall", ["number" => $number]);
}

// ---- Notifications ----

/**
 * Show a system notification.
 * Callback receives: ['shown' => true]
 */
function showNotification(string $title, string $body, string $callback = "handle_notification"): array {
    return nativeCall("notification", $callback, [
        "title" => $title,
        "body" => $body
    ]);
}

// ---- Barcode / QR Code ----

/**
 * Scan a barcode or QR code using the camera.
 * Callback receives: ['code' => string]
 */
function scanQrCode(string $callback): array {
    return nativeCall("barcode", $callback);
}

// ---- Encryption / Hashing ----

/**
 * Encrypt text with a password (AES).
 * Callback receives: ['result' => string]
 */
function encryptText(string $text, string $password, string $callback): array {
    return nativeCall("encrypt", $callback, [
        "text" => $text,
        "password" => $password
    ]);
}

/**
 * Decrypt text with a password (AES).
 * Callback receives: ['result' => string]
 */
function decryptText(string $text, string $password, string $callback): array {
    return nativeCall("decrypt", $callback, [
        "text" => $text,
        "password" => $password
    ]);
}

/**
 * Hash text with the given algorithm.
 * @param string $algorithm "MD5", "SHA1", "SHA256", "SHA512"
 * Callback receives: ['result' => string]
 */
function hashText(string $text, string $algorithm, string $callback): array {
    return nativeCall("hash", $callback, [
        "text" => $text,
        "algorithm" => $algorithm
    ]);
}

// ---- Flashlight ----

/**
 * Toggle the camera flashlight on/off.
 */
function flashlight(bool $on = true): array {
    return nativeCall("flashlight", "handle_flashlight", ["on" => $on]);
}

// ---- Email ----

/**
 * Send an email (opens email intent).
 */
function sendEmail(string $recipient, string $subject, string $body, string $callback = "handle_email", ?string $attachment = null): array {
    return nativeCall("sendemail", $callback, [
        "recipient" => $recipient,
        "subject" => $subject,
        "body" => $body,
        "attachment" => $attachment
    ]);
}

// ---- File System (via DroidScript) ----

/**
 * Read a file from device storage.
 * Callback receives: ['content' => string, 'path' => string]
 */
function readNativeFile(string $path, string $callback): array {
    return nativeCall("readfile", $callback, ["path" => $path]);
}

/**
 * Write content to a file on device storage.
 * Callback receives: ['success' => bool, 'path' => string]
 */
function writeNativeFile(string $path, string $content, string $callback = "handle_writefile"): array {
    return nativeCall("writefile", $callback, [
        "path" => $path,
        "content" => $content
    ]);
}

/**
 * List files in a device folder.
 * Callback receives: ['files' => array, 'path' => string]
 */
function listNativeFolder(string $path, string $callback): array {
    return nativeCall("listfolder", $callback, ["path" => $path]);
}

/**
 * Check if a file exists on device storage.
 * Callback receives: ['exists' => bool, 'path' => string]
 */
function nativeFileExists(string $path, string $callback): array {
    return nativeCall("fileexists", $callback, ["path" => $path]);
}

// ---- Intent / App Launch ----

/**
 * Open another app by package name.
 */
function openApp(string $packageName, string $callback = "handle_openapp"): array {
    return nativeCall("openapp", $callback, ["package" => $packageName]);
}

/**
 * Send an Android intent.
 */
function sendIntent(string $action, string $callback, ?string $type = null, ?string $uri = null, ?string $extras = null): array {
    return nativeCall("intent", $callback, [
        "intentAction" => $action,
        "type" => $type,
        "uri" => $uri,
        "extras" => $extras
    ]);
}
?>