<?php
require_once '../simple.php';

class App {
    function index() {
        return page("Page Builder", [
            card("User Info", [
                label("John Doe", ['bold' => true, 'size' => 18]),
                label("john@email.com", ['color' => '#888888']),
                spacer(10),
                button("Edit Profile", "onEdit"),
            ]),
            spacer(15),
            card("Statistics", [
                row([
                    label("Posts: 42", ['size' => 14]),
                    label("Followers: 1,234", ['size' => 14]),
                ]),
            ]),
            spacer(15),
            card("Settings", [
                checkbox("notifications", "Enable notifications"),
                checkbox("darkMode", "Dark mode"),
            ]),
        ], ['padding' => 20]);
    }
    
    function onEdit($p) {
        return toast("Edit profile clicked!");
    }
}
