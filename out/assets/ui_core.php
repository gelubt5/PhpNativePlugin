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
?>