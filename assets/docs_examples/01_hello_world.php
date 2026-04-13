<?php
require_once '../simple.php';

class App {
    function index() {
        return page("Hello World", [
            label("Welcome to PhpNativePlugin!", ['size' => 24, 'bold' => true]),
            spacer(20),
            label("This is a native Android app built with PHP."),
            spacer(30),
            button("Click Me", "onClick", ['color' => '#4CAF50']),
        ]);
    }
    
    function onClick($p) {
        return toast("Hello from PHP!");
    }
}
