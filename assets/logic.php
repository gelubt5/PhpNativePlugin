<?php
// Dezactivăm orice output care nu e JSON
error_reporting(0);
ini_set('display_errors', 0);

require_once dirname(__FILE__) . '/router.php';
require_once dirname(__FILE__) . '/app.php';

// Executăm logica prin router
Router::handle(new MyApp());
?>