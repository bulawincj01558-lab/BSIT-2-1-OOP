package edu.liceo.ugoautomate.ui.student;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.ClassOffering;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Page;
import edu.liceo.ugoautomate.ui.common.Tables;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;

public class ClassProgramController implements Page {
    private static final Comparator<ClassOffering> WEEKLY_ORDER = Comparator
            .comparing((ClassOffering c) -> c.getDays().stream().min(Comparator.naturalOrder()).orElse(DayOfWeek.SUNDAY))
            .thenComparing(ClassOffering::getStartTime);

    private record State(String term, List<ClassOffering> classes) {
    }

    private final AppContext ctx;

    @FXML private Label termLabel;
    @FXML private Label totals;
    @FXML private TableView<ClassOffering> table;

    public ClassProgramController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        Tables.setup(table, "No classes recorded for you this term yet.");
        Tables.column(table, "Code", ClassOffering::getSubjectCode);
        Tables.column(table, "Subject", ClassOffering::getSubjectTitle).setMinWidth(200);
        Tables.column(table, "Units", ClassOffering::getUnits);
        Tables.column(table, "Section", ClassOffering::getSection);
        Tables.column(table, "Schedule", ClassOffering::getScheduleText).setMinWidth(180);
        Tables.column(table, "Room", ClassOffering::getRoom);
        Tables.column(table, "Instructor", ClassOffering::getFacultyName);
    }

    @Override
    public void onShow() {
        reload();
    }

    @FXML
    private void reload() {
        Async.run(table, () -> new State(ctx.classPrograms().currentTerm(), ctx.classPrograms().myClassProgram()), state -> {
            List<ClassOffering> sorted = state.classes().stream().sorted(WEEKLY_ORDER).toList();
            termLabel.setText(state.term());
            int units = sorted.stream().mapToInt(ClassOffering::getUnits).sum();
            totals.setText(sorted.size() + " class(es), " + units + " unit(s) in total");
            Tables.setRows(table, sorted);
        });
    }
}
