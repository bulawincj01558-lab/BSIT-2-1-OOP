package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.CampusLocation;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.Page;
import edu.liceo.ugoautomate.ui.common.Tables;
import edu.liceo.ugoautomate.ui.common.Views;
import edu.liceo.ugoautomate.ui.common.Views.View;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;

public class LocationsController implements Page {
    private static final String SELECT_HINT = "Select a location in the list first.";

    private final AppContext ctx;
    private List<CampusLocation> all = List.of();

    @FXML private TextField search;
    @FXML private Label count;
    @FXML private TableView<CampusLocation> table;

    public LocationsController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        Tables.setup(table, "No locations match the search.");
        Tables.column(table, "Name", CampusLocation::getName);
        Tables.column(table, "Type", CampusLocation::getType);
        Tables.column(table, "Building", CampusLocation::getBuilding);
        Tables.column(table, "Floor", CampusLocation::getFloor);
        Tables.column(table, "Description", CampusLocation::getDescription);
        Tables.onDoubleClick(table, this::openEditor);
    }

    @Override
    public void onShow() {
        reload();
    }

    @FXML
    private void reload() {
        String text = search.getText();
        Async.run(table, () -> {
            all = ctx.locations().listAll();
            return ctx.locations().search(text, null);
        }, results -> {
            Tables.setRows(table, results);
            count.setText(results.size() + " location(s)");
        });
    }

    @FXML
    private void add() {
        openEditor(null);
    }

    @FXML
    private void edit() {
        Tables.selected(table, SELECT_HINT).ifPresent(this::openEditor);
    }

    private void openEditor(CampusLocation location) {
        View<LocationEditController> view = Views.load(ctx, "admin/LocationEdit.fxml");
        view.controller().setLocation(location, all);
        Views.showDialog(table, location == null ? "Add Location" : "Edit Location", view.root());
        if (view.controller().isSaved()) {
            reload();
        }
    }

    @FXML
    private void delete() {
        Tables.selected(table, SELECT_HINT).ifPresent(location -> {
            if (!Dialogs.confirm(table, "Delete \"" + location.getName() + "\" from the campus map?")) {
                return;
            }
            Async.run(table, () -> {
                ctx.locations().delete(location.getId());
                return Boolean.TRUE;
            }, ok -> reload());
        });
    }
}
