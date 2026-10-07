package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.EntryLog;
import edu.liceo.ugoautomate.model.GuestVisit;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.DisplayTime;
import edu.liceo.ugoautomate.ui.common.Page;
import edu.liceo.ugoautomate.ui.common.Tables;
import edu.liceo.ugoautomate.ui.common.Tabs;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

public class EntryLogsController implements Page {
    private static final String SELECT_HINT = "Select a visit in the list first.";

    private final AppContext ctx;

    @FXML private ToggleButton entriesTab;
    @FXML private ToggleButton visitsTab;
    @FXML private VBox entriesPanel;
    @FXML private VBox visitsPanel;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private Label entryCount;
    @FXML private TableView<EntryLog> entryTable;
    @FXML private CheckBox onlyDate;
    @FXML private DatePicker visitDate;
    @FXML private Label visitCount;
    @FXML private TableView<GuestVisit> visitTable;

    public EntryLogsController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        Tabs.link(new ToggleButton[]{entriesTab, visitsTab}, new Node[]{entriesPanel, visitsPanel});
        fromDate.setValue(LocalDate.now());
        toDate.setValue(LocalDate.now());
        visitDate.setValue(LocalDate.now());

        Tables.setup(entryTable, "No entries logged in this date range.");
        Tables.column(entryTable, "Date and time", EntryLog::getLoggedAt, DisplayTime::format);
        Tables.column(entryTable, "Type", EntryLog::getEntrantType);
        Tables.column(entryTable, "Name", EntryLog::getEntrantName);
        Tables.column(entryTable, "Details", EntryLog::getDetails);
        Tables.column(entryTable, "Verified via", EntryLog::getMethod);
        Tables.column(entryTable, "Verified by", EntryLog::getVerifiedByName);

        Tables.setup(visitTable, "No guest visits for this selection.");
        Tables.column(visitTable, "Visit date", GuestVisit::getVisitDate, DisplayTime::format);
        Tables.column(visitTable, "Guest", GuestVisit::getGuestName);
        Tables.column(visitTable, "Contact", GuestVisit::getGuestContact);
        Tables.column(visitTable, "Purpose", GuestVisit::getPurpose);
        Tables.column(visitTable, "Visiting", GuestVisit::getPersonToVisit);
        Tables.column(visitTable, "Status", GuestVisit::getStatus);
        Tables.column(visitTable, "Registered", GuestVisit::getRegisteredAt, DisplayTime::format);
        Tables.column(visitTable, "Checked in", GuestVisit::getCheckedInAt, DisplayTime::format);
        Tables.column(visitTable, "Checked out", GuestVisit::getCheckedOutAt, DisplayTime::format);
    }

    @Override
    public void onShow() {
        loadEntries();
        loadVisits();
    }

    @FXML
    private void loadEntries() {
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();
        Async.run(entryTable, () -> ctx.entries().listEntryLogs(from, to), logs -> {
            Tables.setRows(entryTable, logs);
            entryCount.setText(logs.size() + " entr" + (logs.size() == 1 ? "y" : "ies"));
        });
    }

    @FXML
    private void onlyDateChanged() {
        visitDate.setDisable(!onlyDate.isSelected());
        loadVisits();
    }

    @FXML
    private void loadVisits() {
        LocalDate date = onlyDate.isSelected() ? visitDate.getValue() : null;
        Async.run(visitTable, () -> ctx.guestVisits().listVisits(date), visits -> {
            Tables.setRows(visitTable, visits);
            visitCount.setText(visits.size() + " visit(s)");
        });
    }

    @FXML
    private void checkOut() {
        Tables.selected(visitTable, SELECT_HINT).ifPresent(visit -> Async.run(visitTable, () -> {
            ctx.guestVisits().checkOut(visit.getId());
            return Boolean.TRUE;
        }, ok -> loadVisits()));
    }

    @FXML
    private void cancelVisit() {
        Tables.selected(visitTable, SELECT_HINT).ifPresent(visit -> {
            if (!Dialogs.confirm(visitTable, "Cancel the visit of " + visit.getGuestName() + "?")) {
                return;
            }
            Async.run(visitTable, () -> {
                ctx.guestVisits().cancelVisit(visit.getId());
                return Boolean.TRUE;
            }, ok -> loadVisits());
        });
    }
}
