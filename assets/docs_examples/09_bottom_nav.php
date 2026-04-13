<?php
require_once '../ui_core.php';

class App {
    function index() {
        // Top bar
        $topBar = (new TopAppBar("Home"))
            ->setId("top_bar")
            ->setBackground('#1976D2');
        
        // Content area (changes based on selected tab)
        $content = (new VerticalLayout())
            ->setId("content_area")
            ->setBackground('#1e1e1e')
            ->setPadding([20, 20, 20, 20])
            ->setWeight(1)
            ->addChild(
                (new TextView("🏠 Home"))
                    ->setTextSize(32)
                    ->setBold(true)
                    ->setTextColor('#4ec9b0')
            )
            ->addChild(
                (new TextView("Welcome to the app! Use the bottom navigation to explore."))
                    ->setTextColor('#888888')
                    ->setPadding([0, 10, 0, 0])
            );
        
        // Bottom navigation bar
        $bottomNav = (new BottomNavBar())
            ->setId("bottom_nav")
            ->setBackground('#2d2d30')
            ->addItem("home", "Home", "showHome")
            ->addItem("search", "Search", "showSearch")
            ->addItem("favorite", "Favorites", "showFavorites")
            ->addItem("person", "Profile", "showProfile")
            ->setSelectedIndex(0);
        
        return (new VerticalLayout())
            ->addChild($topBar)
            ->addChild($content)
            ->addChild($bottomNav);
    }
    
    function showHome($p) {
        return batch([
            updateView("top_bar", ["title" => "Home"]),
            updateMany([
                ["id" => "content_area", "props" => ["removeAllChildren" => true]]
            ]),
            // Can't dynamically change children easily, so show toast instead
            toast("Home tab selected")
        ]);
    }
    
    function showSearch($p) {
        return batch([
            updateView("top_bar", ["title" => "Search"]),
            toast("Search tab selected")
        ]);
    }
    
    function showFavorites($p) {
        return batch([
            updateView("top_bar", ["title" => "Favorites"]),
            toast("Favorites tab selected")
        ]);
    }
    
    function showProfile($p) {
        return batch([
            updateView("top_bar", ["title" => "Profile"]),
            toast("Profile tab selected")
        ]);
    }
}
