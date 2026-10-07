package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.CampusLocation;
import edu.liceo.ugoautomate.model.LocationType;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Views;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Objects;

public class LocationEditController {
    private final AppContext ctx;
    private CampusLocation original;
    private boolean saved;

    @FXML private Label heading;
    @FXML private TextField name;
    @FXML private ComboBox<LocationType> type;
    @FXML private ComboBox<String> building;
    @FXML private TextField floor;
    @FXML private TextArea description;

    public LocationEditController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        type.getItems().setAll(LocationType.values());
        type.getSelectionModel().selectFirst();
    }

    public void setLocation(CampusLocation location, List<CampusLocation> allLocations) {
        this.original = location;
        heading.setText(location == null ? "Add Location" : "Edit Location");
        building.getItems().setAll("");
        allLocations.stream()
                .filter(l -> l.getType() == LocationType.BUILDING)
                .map(CampusLocation::getName)
                .distinct()
                .forEach(building.getItems()::add);
        if (location != null) {
            name.setText(location.getName());
            type.setValue(location.getType());
            String buildingName = Objects.toString(location.getBuilding(), "");
            building.setValue(buildingName);
            building.getEditor().setText(buildingName);
            floor.setText(Objects.toString(location.getFloor(), ""));
            description.setText(Objects.toString(location.getDescription(), ""));
        }
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void save() {
        CampusLocation l = new CampusLocation();
        if (original != null) {
            l.setId(original.getId());
        }
        l.setName(name.getText());
        l.setType(type.getValue());
        l.setBuilding(building.getEditor().getText());
        l.setFloor(floor.getText());
        l.setDescription(description.getText());
        Async.run(name, () -> original == null ? ctx.locations().create(l) : ctx.locations().update(l), result -> {
            saved = true;
            Views.close(name);
        });
    }

    @FXML
    private void cancel() {
        Views.close(name);
    }
}
