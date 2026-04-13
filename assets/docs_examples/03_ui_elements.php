<?php
require_once '../simple.php';

class App {
    function index() {
        return page("UI Elements", [
            label("Labels", ['size' => 20, 'bold' => true, 'color' => '#4ec9b0']),
            label("Simple text label"),
            label("Colored text", ['color' => '#FF9800']),
            label("Centered text", ['center' => true]),
            
            divider(),
            spacer(10),
            
            label("Buttons", ['size' => 20, 'bold' => true, 'color' => '#4ec9b0']),
            button("Default Button", "onDefault"),
            button("Success", "onSuccess", ['color' => '#4CAF50']),
            button("Danger", "onDanger", ['color' => '#f44336']),
            button("Custom", "onCustom", ['color' => '#9C27B0', 'textColor' => '#ffffff']),
            
            divider(),
            spacer(10),
            
            label("Inputs", ['size' => 20, 'bold' => true, 'color' => '#4ec9b0']),
            input("email", "Enter your email"),
            input("password", "Password", ['password' => true]),
            input("age", "Your age", ['number' => true]),
        ]);
    }
    
    function onDefault($p) { return toast("Default clicked"); }
    function onSuccess($p) { return toast("Success!"); }
    function onDanger($p) { return toast("Danger!"); }
    function onCustom($p) { return toast("Custom clicked"); }
}
