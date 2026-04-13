<?php
require_once '../ui_core.php';

class App {
    private $todos = [
        ['title' => 'Buy groceries', 'done' => false],
        ['title' => 'Walk the dog', 'done' => true],
        ['title' => 'Write code', 'done' => false],
        ['title' => 'Read a book', 'done' => false],
    ];
    
    function index() {
        $list = (new ListView())
            ->setId("todo_list")
            ->setItems($this->todos)
            ->setItemLayout(function($item, $index) {
                $textColor = $item['done'] ? '#888888' : '#e0e0e0';
                $icon = $item['done'] ? '✓' : '○';
                
                return (new HorizontalLayout())
                    ->setPadding([16, 12, 16, 12])
                    ->setBackground('#2d2d30')
                    ->addChild(
                        (new TextView($icon))
                            ->setTextSize(20)
                            ->setTextColor($item['done'] ? '#4CAF50' : '#888888')
                    )
                    ->addChild(
                        (new TextView($item['title']))
                            ->setTextColor($textColor)
                            ->setTextSize(16)
                            ->setWeight(1)
                            ->setPadding([12, 0, 0, 0])
                    )
                    ->addChild(
                        (new Button("🗑"))
                            ->setBackground('transparent')
                            ->setOnClick("deleteItem")
                    );
            })
            ->setOnItemClick("toggleItem");
        
        $addRow = (new HorizontalLayout())
            ->setPadding([16, 16, 16, 16])
            ->addChild(
                (new EditText())
                    ->setId("new_todo")
                    ->setHint("Add new task...")
                    ->setWeight(1)
            )
            ->addChild(
                (new Button("+"))
                    ->setBackground('#4CAF50')
                    ->setOnClick("addItem")
            );
        
        return (new VerticalLayout())
            ->setBackground('#1e1e1e')
            ->addChild(
                (new TextView("📝 Todo List"))
                    ->setTextSize(24)
                    ->setBold(true)
                    ->setTextColor('#4ec9b0')
                    ->setPadding([20, 20, 20, 10])
            )
            ->addChild($addRow)
            ->addChild($list);
    }
    
    function addItem($p) {
        $text = getViewProperty("new_todo", "_getText");
        if (empty(trim($text))) {
            return toast("Enter a task first");
        }
        
        return batch([
            listAddItem("todo_list", ['title' => $text, 'done' => false]),
            updateView("new_todo", ["text" => ""])
        ]);
    }
    
    function toggleItem($p) {
        $index = $p['itemIndex'];
        $item = $p['item'];
        $item['done'] = !$item['done'];
        return listUpdateItem("todo_list", $index, $item);
    }
    
    function deleteItem($p) {
        $index = $p['itemIndex'];
        return listDeleteItem("todo_list", $index);
    }
}
