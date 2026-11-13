module aco {
    requires javafx.controls;
    exports aco;
    exports aco.model;
    exports aco.colony;
    exports aco.controller;
    exports aco.view to javafx.graphics;
    opens aco.view to javafx.graphics;
}
