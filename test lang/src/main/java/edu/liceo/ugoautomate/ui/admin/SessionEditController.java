package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.AttendanceSession;
import edu.liceo.ugoautomate.model.CampusLocation;
import edu.liceo.ugoautomate.model.SessionType;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Choice;
import edu.liceo.ugoautomate.ui.common.TimeSpinner;
import edu.liceo.ugoautomate.ui.common.Views;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.LocalTime;

public class SessionEditController {
    private final AppContext ctx;
    private AttendanceSession original;
    private boolean saved;

    @FXML private Label heading;
    @FXML private TextField title;
    @FXML private ComboBox<SessionType> type;
    @FXML private ComboBox<Choice<Long>> locationBox;
    @FXML private DatePicker date;
    @FXML private TimeSpinner startTime;
    @FXML private TimeSpinner endTime;

    public SessionEditController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        type.getItems().setAll(SessionType.values());
        type.getSelectionModel().selectFirst();
    }

    public void setSession(AttendanceSession session) {
        this.original = session;
        heading.setText(session == null ? "New Attendance Session" : "Edit Attendance Session");
        LocalTime nextHour = LocalTime.now().withMinute(0).withSecond(0).withNano(0).plusHours(1);
        date.setValue(session == null ? LocalDate.now() : session.getStartsAt().toLocalDate());
        startTime.setTime(session == null ? nextHour : session.getStartsAt().toLocalTime());
        endTime.setTime(session == null ? nextHour.plusMinutes(90) : session.getEndsAt().toLocalTime());
        if (session != null) {
            title.setText(session.getTitle());
            type.setValue(session.getType());
        }
        locationBox.getItems().setAll(new Choice<>("(No location)", null));
        locationBox.getSelectionModel().selectFirst();
        Long selectedId = session == null ? null : session.getLocationId();
        Async.run(title, () -> ctx.locations().listAll(), all -> {
            for (CampusLocation l : all) {
                locationBox.getItems().add(new Choice<>(l.toString(), l.getId()));
            }
            Choice.select(locationBox, selectedId);
        });
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void save() {
        AttendanceSession s = new AttendanceSession();
        if (original != null) {
            s.setId(original.getId());
        }
        s.setTitle(title.getText());
        s.setType(type.getValue());
        s.setLocationId(Choice.selectedValue(locationBox));
        LocalDate day = date.getValue();
        s.setStartsAt(day == null ? null : day.atTime(startTime.getTime()));
        s.setEndsAt(day == null ? null : day.atTime(endTime.getTime()));
        Async.run(title, () -> original == null ? ctx.attendance().createSession(s) : ctx.attendance().updateSession(s),
                result -> {
                    saved = true;
                    Views.close(title);
                });
    }

    @FXML
    private void cancel() {
        Views.close(title);
    }
}
