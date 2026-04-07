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
// STANDARD COMPONENTS
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

// Material component with full path
class SwitchMaterial extends Component {
    public function toArray() {
        $data = parent::toArray();
        $data['type'] = "com.google.android.material.switchmaterial.SwitchMaterial";
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
class CardView extends VerticalLayout {}

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
    foreach ($actions as $action) {
        if (isset($action['target']) && isset($action['attributes'])) {
            $viewId = $action['target'];
            if (!isset($updates[$viewId])) {
                $updates[$viewId] = [];
            }
            $updates[$viewId] = array_merge($updates[$viewId], $action['attributes']);
        }
    }
    return updateMany($updates);
}
?>