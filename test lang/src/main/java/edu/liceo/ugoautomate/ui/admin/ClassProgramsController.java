package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.ClassOffering;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.Page;
import edu.liceo.ugoautomate.ui.common.Tables;
import edu.liceo.ugoautomate.ui.common.Views;
import edu.liceo.ugoautomate.ui.common.Views.View;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import java.util.List;

public class ClassProgramsController implements Page {
    private static final String SELECT_HINT = "Select a class in the list first.";

    private final AppContext ctx;
    private boolean loadingTerms;

    @FXML private ComboBox<String> termFilter;
    @FXML private Label count;
    @FXML private TableView<ClassOffering> table;

    public ClassProgramsController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        Tables.setup(table, "No classes in this term yet. Press \"Add class\" to create one.");
        Tables.column(table, "Code", ClassOffering::getSubjectCode);
        Tables.column(table, "Subject", ClassOffering::getSubjectTitle).setMinWidth(180);
        Tables.column(table, "Units", ClassOffering::getUnits);
        Tables.column(table, "Section", ClassOffering::getSection);
        Tables.column(table, "Schedule", ClassOffering::getScheduleText).setMinWidth(170);
        Tables.column(table, "Room", ClassOffering::getRoom);
        Tables.column(table, "Instructor", ClassOffering::getFacultyName);
        Tables.column(table, "Students", ClassOffering::getEnrolledCount);
        Tables.onDoubleClick(table, this::openRoster);
    }

    @Override
    public void onShow() {
        refresh();
    }

    @FXML
    private void refresh() {
        String keep = termFilter.getValue();
        Async.run(table, () -> ctx.classPrograms().terms(), terms -> {
            loadingTerms = true;
            termFilter.getItems().setAll(terms);
            termFilter.setValue(keep != null && terms.contains(keep) ? keep : terms.get(0));
            loadingTerms = false;
            loadClasses();
        });
    }

    @FXML
    private void loadClasses() {
        String term = termFilter.getValue();
        if (loadingTerms || term == null) {
            return;
        }
        Async.run(table, () -> ctx.classPrograms().listClasses(term), rows -> {
            Tables.setRows(table, rows);
            int units = rows.stream().mapToInt(ClassOffering::getUnits).sum();
            count.setText(rows.size() + " class(es), " + units + " unit(s)");
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

    private void openEditor(ClassOffering offering) {
        View<ClassEditController> view = Views.load(ctx, "admin/ClassEdit.fxml");
        view.controller().setClass(offering, termFilter.getValue(), List.copyOf(table.getItems()));
        Views.showDialog(table, offering == null ? "Add Class" : "Edit Class", view.root());
        if (view.controller().isSaved()) {
            refresh();
        }
    }

    @FXML
    private void students() {
        Tables.selected(table, SELECT_HINT).ifPresent(this::openRoster);
    }

    private void openRoster(ClassOffering offering) {
        View<ClassRosterController> view = Views.load(ctx, "admin/ClassRoster.fxml");
        view.controller().setClass(offering);
        Views.showDialog(table, "Students - " + offering, view.root());
        loadClasses();
    }

    @FXML
    private void delete() {
        Tables.selected(table, SELECT_HINT).ifPresent(offering -> {
            if (!Dialogs.confirm(table, "Delete " + offering.getSubjectCode() + " (" + offering.getSection() + ")?\n"
                    + "Its " + offering.getEnrolledCount() + " enrolled student(s) will be removed from it.")) {
                return;
            }
            Async.run(table, () -> {
                ctx.classPrograms().delete(offering.getId());
                return Boolean.TRUE;
            }, ok -> loadClasses());
        });
    }
}
