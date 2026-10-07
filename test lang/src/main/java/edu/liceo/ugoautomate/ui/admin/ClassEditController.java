package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.CampusLocation;
import edu.liceo.ugoautomate.model.ClassOffering;
import edu.liceo.ugoautomate.model.Faculty;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Choice;
import edu.liceo.ugoautomate.ui.common.TimeSpinner;
import edu.liceo.ugoautomate.ui.common.Views;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ClassEditController {
    private final AppContext ctx;
    private final Map<DayOfWeek, CheckBox> dayBoxes = new EnumMap<>(DayOfWeek.class);
    private ClassOffering original;
    private boolean saved;

    @FXML private Label heading;
    @FXML private TextField term;
    @FXML private TextField subjectCode;
    @FXML private TextField subjectTitle;
    @FXML private Spinner<Integer> units;
    @FXML private ComboBox<String> section;
    @FXML private ComboBox<Choice<Long>> instructor;
    @FXML private FlowPane days;
    @FXML private TimeSpinner startTime;
    @FXML private TimeSpinner endTime;
    @FXML private ComboBox<String> room;

    public ClassEditController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        units.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, 3));
        for (DayOfWeek day : EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY)) {
            CheckBox box = new CheckBox(day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            dayBoxes.put(day, box);
            days.getChildren().add(box);
        }
        startTime.setTime(LocalTime.of(8, 0));
        endTime.setTime(LocalTime.of(9, 0));
    }

    public void setClass(ClassOffering offering, String defaultTerm, List<ClassOffering> termClasses) {
        this.original = offering;
        heading.setText(offering == null ? "Add Class" : "Edit Class");
        termClasses.stream().map(ClassOffering::getSection).distinct().sorted().forEach(section.getItems()::add);
        term.setText(offering == null ? Objects.toString(defaultTerm, "") : offering.getTerm());
        if (offering != null) {
            subjectCode.setText(offering.getSubjectCode());
            subjectTitle.setText(offering.getSubjectTitle());
            units.getValueFactory().setValue(offering.getUnits());
            section.setValue(offering.getSection());
            section.getEditor().setText(offering.getSection());
            offering.getDays().forEach(d -> {
                if (dayBoxes.containsKey(d)) {
                    dayBoxes.get(d).setSelected(true);
                }
            });
            startTime.setTime(offering.getStartTime());
            endTime.setTime(offering.getEndTime());
            room.setValue(offering.getRoom());
            room.getEditor().setText(Objects.toString(offering.getRoom(), ""));
        }

        instructor.getItems().setAll(new Choice<>("TBA (to be announced)", null));
        instructor.getSelectionModel().selectFirst();
        Long selectedFaculty = offering == null ? null : offering.getFacultyId();
        record Options(List<Faculty> faculty, List<CampusLocation> locations) {
        }
        Async.run(term, () -> new Options(ctx.faculty().listAll(), ctx.locations().listAll()), options -> {
            for (Faculty f : options.faculty()) {
                if (f.isActive() || Objects.equals(f.getId(), selectedFaculty)) {
                    instructor.getItems().add(new Choice<>(f.getFullName() + " - " + f.getDepartment(), f.getId()));
                }
            }
            Choice.select(instructor, selectedFaculty);
            options.locations().stream().map(CampusLocation::getName).forEach(room.getItems()::add);
        });
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void save() {
        ClassOffering c = new ClassOffering();
        if (original != null) {
            c.setId(original.getId());
        }
        c.setTerm(term.getText());
        c.setSubjectCode(subjectCode.getText());
        c.setSubjectTitle(subjectTitle.getText());
        units.commitValue();
        c.setUnits(units.getValue());
        c.setSection(section.getEditor().getText());
        c.setFacultyId(Choice.selectedValue(instructor));
        Set<DayOfWeek> chosen = EnumSet.noneOf(DayOfWeek.class);
        dayBoxes.forEach((day, box) -> {
            if (box.isSelected()) {
                chosen.add(day);
            }
        });
        c.setDays(chosen);
        c.setStartTime(startTime.getTime());
        c.setEndTime(endTime.getTime());
        c.setRoom(room.getEditor().getText());
        Async.run(term, () -> original == null ? ctx.classPrograms().create(c) : ctx.classPrograms().update(c), result -> {
            saved = true;
            Views.close(term);
        });
    }

    @FXML
    private void cancel() {
        Views.close(term);
    }
}
