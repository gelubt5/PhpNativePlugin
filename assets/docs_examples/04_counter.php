<?php
require_once '../simple.php';

class App {
    function index() {
        return page("Counter Example", [
            label("Tap buttons to change the count:", ['center' => true]),
            spacer(30),
            label("0", ['id' => 'counter', 'size' => 64, 'bold' => true, 'center' => true, 'color' => '#4ec9b0']),
            spacer(30),
            row([
                button("−", "decrement", ['color' => '#f44336']),
                spacer(20),
                button("+", "increment", ['color' => '#4CAF50']),
            ]),
            spacer(30),
            button("Reset", "reset", ['color' => '#607D8B']),
        ]);
    }
    
    function increment($p) {
        $current = (int)getViewProperty("counter", "_getText");
        return updateView("counter", ["text" => (string)($current + 1)]);
    }
    
    function decrement($p) {
        $current = (int)getViewProperty("counter", "_getText");
        return updateView("counter", ["text" => (string)($current - 1)]);
    }
    
    function reset($p) {
        return batch([
            updateView("counter", ["text" => "0"]),
            toast("Counter reset!")
        ]);
    }
}
