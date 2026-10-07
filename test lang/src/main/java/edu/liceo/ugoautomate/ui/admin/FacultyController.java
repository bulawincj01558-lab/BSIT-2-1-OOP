package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.Faculty;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.Page;
import edu.liceo.ugoautomate.ui.common.Tables;
import edu.liceo.ugoautomate.ui.common.Views;
import edu.liceo.ugoautomate.ui.common.Views.View;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

public class FacultyController implements Page {
    private static final String SELECT_HINT = "Select a faculty member in the list first.";

    private final AppContext ctx;

    @FXML private Label count;
    @FXML private TableView<Faculty> table;

    public FacultyController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        Tables.setup(table, "No faculty yet. Press \"Add faculty\" to add one.");
        Tables.column(table, "Name", f -> f.getLastName() + ", " + f.getFirstName());
        Tables.column(table, "Department", Faculty::getDepartment);
        Tables.column(table, "Email", Faculty::getEmail);
        Tables.column(table, "Status", f -> f.isActive() ? "Active" : "Deactivated");
        Tables.onDoubleClick(table, this::openEditor);
    }

    @Override
    public void onShow() {
        reload();
    }

    private void reload() {
        Async.run(table, () -> ctx.faculty().listAll(), rows -> {
            Tables.setRows(table, rows);
            count.setText(rows.size() + " faculty member(s)");
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

    private void openEditor(Faculty faculty) {
        View<FacultyEditController> view = Views.load(ctx, "admin/FacultyEdit.fxml");
        view.controller().setFaculty(faculty, table.getItems());
        Views.showDialog(table, faculty == null ? "Add Faculty" : "Edit Faculty", view.root());
        if (view.controller().isSaved()) {
            reload();
        }
    }

    @FXML
    private void toggleActive() {
        Tables.selected(table, SELECT_HINT).ifPresent(faculty -> {
            Faculty changed = new Faculty();
            changed.setId(faculty.getId());
            changed.setFirstName(faculty.getFirstName());
            changed.setLastName(faculty.getLastName());
            changed.setDepartment(faculty.getDepartment());
            changed.setEmail(faculty.getEmail());
            changed.setActive(!faculty.isActive());
            Async.run(table, () -> ctx.faculty().update(changed), saved -> reload());
        });
    }

    @FXML
    private void delete() {
        Tables.selected(table, SELECT_HINT).ifPresent(faculty -> {
            if (!Dialogs.confirm(table, "Permanently delete " + faculty.getFullName() + " and all of their evaluations?\n"
                    + "To keep the results, deactivate them instead.")) {
                return;
            }
            Async.run(table, () -> {
                ctx.faculty().delete(faculty.getId());
                return Boolean.TRUE;
            }, ok -> reload());
        });
    }
}
