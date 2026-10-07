package edu.liceo.ugoautomate.ui.guest;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.Guest;
import edu.liceo.ugoautomate.model.GuestVisit;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.DisplayTime;
import edu.liceo.ugoautomate.ui.common.Page;
import edu.liceo.ugoautomate.ui.common.QrDialogController;
import edu.liceo.ugoautomate.ui.common.Responsive;
import edu.liceo.ugoautomate.ui.common.Tables;
import edu.liceo.ugoautomate.util.AppConfig;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;

public class GuestVisitsController implements Page {
    private static final String SELECT_HINT = "Select a visit in the list first.";

    private final AppContext ctx;

    @FXML private GridPane body;
    @FXML private TableView<GuestVisit> table;
    @FXML private TextField purpose;
    @FXML private TextField personToVisit;
    @FXML private DatePicker visitDate;
    @FXML private Label aheadNote;

    public GuestVisitsController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        Responsive.split(body, 900, 0.58);
        visitDate.setValue(LocalDate.now());
        aheadNote.setText("You can register up to " + AppConfig.MAX_VISIT_DAYS_AHEAD + " days ahead.");

        Tables.setup(table, "No visits registered yet.");
        Tables.column(table, "Visit date", GuestVisit::getVisitDate, DisplayTime::format);
        Tables.column(table, "Purpose", GuestVisit::getPurpose);
        Tables.column(table, "Visiting", GuestVisit::getPersonToVisit);
        Tables.column(table, "Status", GuestVisit::getStatus);
        Tables.column(table, "Checked in", GuestVisit::getCheckedInAt, DisplayTime::format);
        Tables.onDoubleClick(table, this::showPass);
    }

    @Override
    public void onShow() {
        ctx.session().current().ifPresent(user -> {
            if (user instanceof Guest g && purpose.getText().isBlank()) {
                purpose.setText(g.getPurposeOfVisit());
                personToVisit.setText(g.getPersonToVisit());
            }
        });
        reload();
    }

    @FXML
    private void reload() {
        Async.run(table, () -> ctx.guestVisits().myVisits(), rows -> Tables.setRows(table, rows));
    }

    @FXML
    private void register() {
        String purposeText = purpose.getText();
        String personText = personToVisit.getText();
        LocalDate date = visitDate.getValue();
        Async.run(table, () -> ctx.guestVisits().registerVisit(purposeText, personText, date), visit -> {
            reload();
            Dialogs.info(table, "Visit registered for " + DisplayTime.format(visit.getVisitDate())
                    + ".\nYour guest pass will open now. Show it at the gate on that day.");
            showPass(visit);
        });
    }

    @FXML
    private void showPass() {
        Tables.selected(table, SELECT_HINT).ifPresent(this::showPass);
    }

    private void showPass(GuestVisit visit) {
        Async.run(table, () -> ctx.guestVisits().issueGuestPass(visit.getId()), token ->
                QrDialogController.show(ctx, table, "Guest Pass", token,
                        "Guest pass - " + DisplayTime.format(visit.getVisitDate()),
                        "guest-pass-" + visit.getVisitDate() + ".png"));
    }

    @FXML
    private void cancel() {
        Tables.selected(table, SELECT_HINT).ifPresent(visit -> {
            if (!Dialogs.confirm(table, "Cancel your visit on " + DisplayTime.format(visit.getVisitDate()) + "?")) {
                return;
            }
            Async.run(table, () -> {
                ctx.guestVisits().cancelVisit(visit.getId());
                return Boolean.TRUE;
            }, ok -> reload());
        });
    }
}
