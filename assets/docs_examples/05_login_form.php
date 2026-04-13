<?php
require_once '../simple.php';

class App {
    function index() {
        return loginForm("onLogin", [
            'title' => 'Welcome Back',
            'userLabel' => 'Email',
            'passLabel' => 'Password', 
            'buttonText' => 'Sign In'
        ]);
    }
    
    function onLogin($p) {
        $email = getViewProperty("username", "_getText");
        $pass = getViewProperty("password", "_getText");
        
        if (empty($email)) {
            return toast("Please enter your email");
        }
        
        if (empty($pass)) {
            return toast("Please enter your password");
        }
        
        if (strlen($pass) < 6) {
            return toast("Password must be at least 6 characters");
        }
        
        // Simulate login
        return batch([
            toast("Logging in as: " . $email),
            goToScreen("dashboard", ['email' => $email])
        ]);
    }
    
    function dashboard($p) {
        return page("Dashboard", [
            label("Welcome!", ['size' => 28, 'bold' => true, 'color' => '#4ec9b0']),
            spacer(10),
            label("Logged in as: " . $p['email'], ['color' => '#888888']),
            spacer(30),
            card("Quick Actions", [
                button("View Profile", "onProfile"),
                button("Settings", "onSettings"),
                button("Logout", "onLogout", ['color' => '#f44336']),
            ]),
        ]);
    }
    
    function onProfile($p) { return toast("Profile clicked"); }
    function onSettings($p) { return toast("Settings clicked"); }
    function onLogout($p) { return goToScreen("index"); }
}
