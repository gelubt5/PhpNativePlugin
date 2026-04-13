<?php
require_once '../ui_core.php';

class App {
    function index() {
        // Navigation drawer
        $drawer = (new NavigationDrawer())
            ->setHeader("My App", "user@example.com")
            ->addItem("home", "Home", "goHome")
            ->addItem("settings", "Settings", "goSettings")
            ->addItem("info", "About", "goAbout");
        
        // Top app bar
        $topBar = (new TopAppBar("Home"))
            ->setId("top_bar")
            ->setBackground('#1976D2')
            ->setNavigationIcon("menu")
            ->setOnNavigationClick("toggleDrawer")
            ->addAction("search", "onSearch")
            ->addAction("more_vert", "onMenu");
        
        // Main content
        $content = (new VerticalLayout())
            ->setPadding([20, 20, 20, 20])
            ->addChild(
                (new TextView("Welcome!"))
                    ->setTextSize(24)
                    ->setBold(true)
                    ->setTextColor('#4ec9b0')
            )
            ->addChild(
                (new TextView("Tap the menu icon (☰) to open the navigation drawer."))
                    ->setTextColor('#888888')
                    ->setPadding([0, 10, 0, 20])
            )
            ->addChild(
                (new CardView())
                    ->setBackground('#2d2d30')
                    ->setCornerRadius(12)
                    ->setPadding([16, 16, 16, 16])
                    ->addChild(
                        (new VerticalLayout())
                            ->addChild((new TextView("Quick Actions"))->setBold(true)->setTextSize(18))
                            ->addChild((new Button("Open Drawer"))->setOnClick("toggleDrawer"))
                            ->addChild((new Button("Search"))->setOnClick("onSearch"))
                    )
            );
        
        // Combine into DrawerLayout
        return (new DrawerLayout())
            ->setId("main_drawer")
            ->setDrawer($drawer)
            ->setContent(
                (new VerticalLayout())
                    ->addChild($topBar)
                    ->addChild($content)
            );
    }
    
    function toggleDrawer($p) {
        return drawerToggle("main_drawer");
    }
    
    function onSearch($p) {
        return toast("Search clicked");
    }
    
    function onMenu($p) {
        return toast("Menu clicked");
    }
    
    function goHome($p) {
        return batch([
            drawerClose("main_drawer"),
            updateView("top_bar", ["title" => "Home"]),
            toast("Home selected")
        ]);
    }
    
    function goSettings($p) {
        return batch([
            drawerClose("main_drawer"),
            updateView("top_bar", ["title" => "Settings"]),
            toast("Settings selected")
        ]);
    }
    
    function goAbout($p) {
        return batch([
            drawerClose("main_drawer"),
            updateView("top_bar", ["title" => "About"]),
            toast("About selected")
        ]);
    }
}
