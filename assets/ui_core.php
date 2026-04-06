<?php
// ui_core.php

abstract class Component {
    protected $attributes = [];

    public function __call($name, $arguments) {
        $this->attributes[$name] = isset($arguments[0]) ? $arguments[0] : null;
        return $this;
    }

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
}

// Componente standard
class TextView extends Component {}
class Button extends Component {}
class CheckBox extends Component {}
class EditText extends Component {}
class ImageView extends Component {}

// Componentă specială care necesită path complet în Android
class SwitchMaterial extends Component {
    public function toArray() {
        $data = parent::toArray();
        $data['type'] = "com.google.android.material.switchmaterial.SwitchMaterial";
        return $data;
    }
}

class VerticalLayout extends Component {
    private $children;

    public function __construct($children = []) {
        $this->children = $children;
    }

    public function toArray() {
        $data = parent::toArray();
        $data["children"] = array_map(function($child) {
            return ($child instanceof Component) ? $child->toArray() : $child;
        }, $this->children);
        return $data;
    }
}

// =========================================================================
// View Property Helpers
// =========================================================================

/**
 * Request a view property value from Java.
 * Returns an action that Java will process and call back with the result.
 *
 * @param string $viewId The view ID to get property from
 * @param string $property The property name (e.g., "text", "url", "checked", "progress")
 * @param string $callback The PHP method to call with the result
 * @return array Action array for Java to process
 *
 * Example usage:
 *   return getViewProperty("my_webview", "url", "handleUrl");
 *
 * The callback will receive: ["viewId" => "my_webview", "property" => "url", "value" => "https://..."]
 */
function getViewProperty(string $viewId, string $property, string $callback): array {
    return [
        "action" => "GET_VIEW_PROPERTY",
        "viewId" => $viewId,
        "property" => $property,
        "callback" => $callback
    ];
}

/**
 * Request multiple view properties at once.
 *
 * @param array $requests Array of ["viewId" => ..., "property" => ...]
 * @param string $callback The PHP method to call with all results
 * @return array Action array for Java to process
 *
 * Example usage:
 *   return getViewProperties([
 *       ["viewId" => "txt_name", "property" => "text"],
 *       ["viewId" => "chk_agree", "property" => "checked"],
 *   ], "handleFormData");
 */
function getViewProperties(array $requests, string $callback): array {
    return [
        "action" => "GET_VIEW_PROPERTIES",
        "requests" => $requests,
        "callback" => $callback
    ];
}

/**
 * Update a view's attributes.
 *
 * @param string $viewId The view ID to update
 * @param array $attributes Key-value pairs of attributes
 * @return array Update action array
 */
function updateView(string $viewId, array $attributes): array {
    return [
        "action" => "update",
        "target" => $viewId,
        "attributes" => $attributes
    ];
}
?>