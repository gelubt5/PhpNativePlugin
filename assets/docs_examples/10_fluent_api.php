<?php
require_once '../ui_core.php';

class App {
    function index() {
        // Demonstrate the fluent API for building UIs
        
        $title = (new TextView("Fluent API Demo"))
            ->setTextSize(28)
            ->setBold(true)
            ->setTextColor('#4ec9b0')
            ->setGravity('center')
            ->setPadding([0, 0, 0, 20]);
        
        $description = (new TextView("This example demonstrates the fluent/chained API of ui_core.php"))
            ->setTextColor('#888888')
            ->setGravity('center');
        
        // Styled card with nested content
        $card = (new CardView())
            ->setBackground('#2d2d30')
            ->setCornerRadius(16)
            ->setElevation(8)
            ->setPadding([20, 20, 20, 20])
            ->setMargin(20)
            ->addChild(
                (new VerticalLayout())
                    ->addChild(
                        (new TextView("Styled Card"))
                            ->setTextSize(20)
                            ->setBold(true)
                            ->setTextColor('#ffffff')
                    )
                    ->addChild(
                        (new TextView("Cards can contain any nested components."))
                            ->setTextColor('#aaaaaa')
                            ->setPadding([0, 8, 0, 16])
                    )
                    ->addChild(
                        (new HorizontalLayout())
                            ->addChild(
                                (new Button("Cancel"))
                                    ->setBackground('#607D8B')
                                    ->setWeight(1)
                                    ->setOnClick("onCancel")
                            )
                            ->addChild((new TextView(""))->setWeight(0.2))
                            ->addChild(
                                (new Button("OK"))
                                    ->setBackground('#4CAF50')
                                    ->setWeight(1)
                                    ->setOnClick("onOk")
                            )
                    )
            );
        
        // Form inputs with styling
        $form = (new VerticalLayout())
            ->setPadding([20, 20, 20, 20])
            ->addChild(
                (new EditText())
                    ->setId("name_input")
                    ->setHint("Enter your name")
                    ->setBackground('#2d2d30')
                    ->setCornerRadius(8)
                    ->setPadding([12, 12, 12, 12])
            )
            ->addChild((new TextView(""))->setPadding([0, 5, 0, 5]))
            ->addChild(
                (new EditText())
                    ->setId("email_input")
                    ->setHint("Enter your email")
                    ->setBackground('#2d2d30')
                    ->setCornerRadius(8)
                    ->setPadding([12, 12, 12, 12])
            )
            ->addChild((new TextView(""))->setPadding([0, 10, 0, 10]))
            ->addChild(
                (new Button("Submit"))
                    ->setBackground('#1976D2')
                    ->setTextColor('#ffffff')
                    ->setCornerRadius(25)
                    ->setOnClick("onSubmit")
            );
        
        return (new ScrollView())
            ->addChild(
                (new VerticalLayout())
                    ->setBackground('#1e1e1e')
                    ->setPadding([20, 30, 20, 30])
                    ->addChild($title)
                    ->addChild($description)
                    ->addChild($card)
                    ->addChild($form)
            );
    }
    
    function onCancel($p) {
        return toast("Cancelled");
    }
    
    function onOk($p) {
        return toast("OK clicked!");
    }
    
    function onSubmit($p) {
        $name = getViewProperty("name_input", "_getText");
        $email = getViewProperty("email_input", "_getText");
        
        if (empty($name) || empty($email)) {
            return toast("Please fill all fields");
        }
        
        return toast("Submitted: $name ($email)");
    }
}
