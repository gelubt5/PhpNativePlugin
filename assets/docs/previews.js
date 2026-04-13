// PhpNativePlugin Documentation - Helper Functions

// Copy code to clipboard
function copyCode(btn) {
    const codeBlock = btn.closest('.code-block');
    const code = codeBlock.querySelector('pre').textContent;
    navigator.clipboard.writeText(code).then(() => {
        btn.textContent = 'Copied!';
        setTimeout(() => btn.textContent = 'Copy', 2000);
    });
}

// Example file mapping
const exampleFiles = {
    'hello-world': '01_hello_world.php',
    'page-example': '02_page_cards.php',
    'card-example': '02_page_cards.php',
    'label-example': '03_ui_elements.php',
    'button-example': '03_ui_elements.php',
    'input-example': '03_ui_elements.php',
    'checkbox-example': '03_ui_elements.php',
    'sensor-example': '08_sensors.php',
    'login-example': '05_login_form.php',
    'fluent-example': '10_fluent_api.php',
    'counter-example': '04_counter.php',
    'layout-example': '10_fluent_api.php',
    'listview-example': '06_todo_list.php',
    'nav-example': '07_navigation.php',
    'topappbar-example': '07_navigation.php',
    'bottomnav-example': '09_bottom_nav.php'
};

// Show preview modal with run instructions
function showPreview(id) {
    const modal = document.getElementById('previewModal');
    const screen = document.getElementById('previewScreen');
    const exampleFile = exampleFiles[id] || id + '.php';
    
    screen.innerHTML = `
        <div style="padding: 20px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;">
            <div style="font-size: 48px; text-align: center; margin-bottom: 15px;">📱</div>
            <h3 style="color: #4ec9b0; text-align: center; margin-bottom: 15px; font-size: 18px;">
                Run on Android Device
            </h3>
            <p style="color: #888; text-align: center; margin-bottom: 20px; font-size: 13px;">
                This example runs as native Android UI through Java.
            </p>
            
            <div style="background: #252526; border-radius: 8px; padding: 15px; margin-bottom: 15px;">
                <div style="color: #888; font-size: 11px; margin-bottom: 5px;">EXAMPLE FILE</div>
                <div style="color: #4ec9b0; font-family: monospace; font-size: 14px;">
                    docs_examples/${exampleFile}
                </div>
            </div>
            
            <div style="color: #e0e0e0; font-size: 13px; line-height: 1.7;">
                <div style="display: flex; gap: 10px; padding: 8px 0; border-bottom: 1px solid #333;">
                    <span style="color: #4ec9b0; font-weight: bold;">1.</span>
                    <span>Install PhpNativePlugin in DroidScript</span>
                </div>
                <div style="display: flex; gap: 10px; padding: 8px 0; border-bottom: 1px solid #333;">
                    <span style="color: #4ec9b0; font-weight: bold;">2.</span>
                    <span>Copy DocRunner.js to DroidScript</span>
                </div>
                <div style="display: flex; gap: 10px; padding: 8px 0; border-bottom: 1px solid #333;">
                    <span style="color: #4ec9b0; font-weight: bold;">3.</span>
                    <span>Copy docs_examples/ folder</span>
                </div>
                <div style="display: flex; gap: 10px; padding: 8px 0;">
                    <span style="color: #4ec9b0; font-weight: bold;">4.</span>
                    <span>Run and select the example</span>
                </div>
            </div>
            
            <div style="text-align: center; margin-top: 20px;">
                <button onclick="closePreview()" style="
                    background: #4ec9b0;
                    color: #1e1e1e;
                    border: none;
                    padding: 10px 30px;
                    border-radius: 20px;
                    font-weight: bold;
                    cursor: pointer;
                ">Got it</button>
            </div>
        </div>
    `;
    
    modal.classList.add('active');
}

// Close preview modal
function closePreview() {
    document.getElementById('previewModal').classList.remove('active');
}

// Close on overlay click
document.addEventListener('DOMContentLoaded', function() {
    const modal = document.getElementById('previewModal');
    if (modal) {
        modal.addEventListener('click', function(e) {
            if (e.target === modal) {
                closePreview();
            }
        });
    }
});
