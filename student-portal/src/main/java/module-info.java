module ph.edu.liceo.portal {
    requires javafx.controls;
    requires javafx.fxml;

    opens ph.edu.liceo.portal.controller to javafx.fxml;
    exports ph.edu.liceo.portal;
}