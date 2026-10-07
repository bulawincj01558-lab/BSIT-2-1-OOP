package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.ClassOffering;
import edu.liceo.ugoautomate.model.EnrolledStudent;
import edu.liceo.ugoautomate.model.EnrollmentResult;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.StudentFields;
import edu.liceo.ugoautomate.ui.common.Tables;
import edu.liceo.ugoautomate.ui.common.Views;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class ClassRosterController {
    private final AppContext ctx;
    private ClassOffering offering;

    @FXML private Label heading;
    @FXML private Label details;
    @FXML private TextField studentNumber;
    @FXML private ComboBox<String> course;
    @FXML private ComboBox<Integer> yearLevel;
    @FXML private Label count;
    @FXML private TableView<EnrolledStudent> table;

    public ClassRosterController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        StudentFields.setupCourse(course);
        StudentFields.setupYearLevel(yearLevel);
        Tables.setup(table, "No students enrolled yet.");
        Tables.column(table, "Student ID", EnrolledStudent::studentNumber);
        Tables.column(table, "Name", EnrolledStudent::name);
        Tables.column(table, "Course", EnrolledStudent::course);
        Tables.column(table, "Year", EnrolledStudent::yearLevel);
    }

    public void setClass(ClassOffering classOffering) {
        this.offering = classOffering;
        heading.setText(classOffering.getSubjectCode() + " - " + classOffering.getSubjectTitle());
        details.setText(classOffering.getSection() + "  •  " + classOffering.getScheduleText() + "  •  "
                + classOffering.getFacultyName() + "  •  " + classOffering.getTerm());
        reload();
    }

    private void reload() {
        Async.run(table, () -> ctx.classPrograms().roster(offering.getId()), rows -> {
            Tables.setRows(table, rows);
            count.setText(rows.size() + " student(s) enrolled");
        });
    }

    @FXML
    private void enrollOne() {
        String number = studentNumber.getText();
        Async.run(table, () -> ctx.classPrograms().enrollByStudentNumber(offering.getId(), number), student -> {
            studentNumber.clear();
            reload();
        });
    }

    @FXML
    private void enrollCourse() {
        String courseText = StudentFields.courseText(course);
        Integer year = yearLevel.getValue();
        if (!Dialogs.confirm(table, "Enroll every active " + courseText + " year " + year + " student in "
                + offering + "?")) {
            return;
        }
        Async.run(table, () -> ctx.classPrograms().enrollCourse(offering.getId(), courseText, year), result -> {
            reload();
            Dialogs.info(table, summary(result));
        });
    }

    private static String summary(EnrollmentResult result) {
        StringBuilder text = new StringBuilder(result.enrolled() + " student(s) enrolled.");
        if (result.alreadyEnrolled() > 0) {
            text.append("\n").append(result.alreadyEnrolled()).append(" were already in this class.");
        }
        if (!result.conflicts().isEmpty()) {
            text.append("\n\nSkipped because of a schedule conflict:");
            result.conflicts().forEach(c -> text.append("\n• ").append(c));
        }
        return text.toString();
    }

    @FXML
    private void remove() {
        Tables.selected(table, "Select a student in the list first.").ifPresent(student -> {
            if (!Dialogs.confirm(table, "Remove " + student.name() + " from " + offering + "?")) {
                return;
            }
            Async.run(table, () -> {
                ctx.classPrograms().unenroll(offering.getId(), student.userId());
                return Boolean.TRUE;
            }, ok -> reload());
        });
    }

    @FXML
    private void close() {
        Views.close(table);
    }
}
