package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.EntryLog;
import edu.liceo.ugoautomate.model.GuestVisit;
import edu.liceo.ugoautomate.service.GuestVisitService;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.DataTable;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.DisplayTime;
import edu.liceo.ugoautomate.ui.common.ListTableModel;
import edu.liceo.ugoautomate.ui.common.Refreshable;
import edu.liceo.ugoautomate.ui.common.SwingDates;
import edu.liceo.ugoautomate.ui.common.UiTheme;
import edu.liceo.ugoautomate.ui.common.WrapLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.Optional;

import static edu.liceo.ugoautomate.ui.common.ListTableModel.column;

/**
 * Campus entry logs and guest visit logs (F-5.4).
 */
public class EntryLogsPanel extends JPanel implements Refreshable {

    private final AppContext ctx;

    private final JSpinner fromDate = SwingDates.dateSpinner(LocalDate.now());
    private final JSpinner toDate = SwingDates.dateSpinner(LocalDate.now());
    private final DataTable<EntryLog> entries = new DataTable<>(new ListTableModel<>(
            column("Entry Time", DisplayTime.class, e -> DisplayTime.of(e.getEntryTime())),
            column("Type", EntryLog::getEntrantType),
            column("Name", EntryLog::getEntrantName),
            column("Student ID / Contact", EntryLog::getIdentifier),
            column("Details", EntryLog::getDetails),
            column("Verified By", EntryLog::getMethod),
            column("Gate Staff", EntryLog::getVerifiedByName)));

    private final JSpinner visitDate = SwingDates.dateSpinner(LocalDate.now());
    private final JCheckBox allRecent = new JCheckBox("Show latest " + GuestVisitService.RECENT_LIMIT + " instead");
    private final DataTable<GuestVisit> visits = new DataTable<>(new ListTableModel<>(
            column("Visit Date", DisplayTime.class, v -> DisplayTime.of(v.getVisitDate())),
            column("Guest", GuestVisit::getGuestName),
            column("Contact", GuestVisit::getGuestContact),
            column("Purpose", GuestVisit::getPurpose),
            column("Visiting", GuestVisit::getPersonToVisit),
            column("Status", GuestVisit::getStatus),
            column("Registered", DisplayTime.class, v -> DisplayTime.of(v.getRegisteredAt())),
            column("Checked In", DisplayTime.class, v -> DisplayTime.of(v.getCheckedInAt()))));

    public EntryLogsPanel(AppContext ctx) {
        super(new BorderLayout());
        this.ctx = ctx;

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Campus Entry Logs", entriesTab());
        tabs.addTab("Guest Visit Logs", visitsTab());
        add(UiTheme.page(AdminDashboard.LOGS, "Verified campus entries and guest visit registrations.", tabs),
                BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        loadEntries();
        loadVisits();
    }

    private JPanel entriesTab() {
        JButton show = UiTheme.primaryButton("Show");
        show.addActionListener(e -> loadEntries());
        JPanel filter = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        filter.add(new JLabel("From"));
        filter.add(fromDate);
        filter.add(new JLabel("To"));
        filter.add(toDate);
        filter.add(show);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(UiTheme.padding(10));
        panel.add(filter, BorderLayout.NORTH);
        panel.add(entries.getComponent(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel visitsTab() {
        JButton show = UiTheme.primaryButton("Show");
        show.addActionListener(e -> loadVisits());
        allRecent.addActionListener(e -> {
            visitDate.setEnabled(!allRecent.isSelected());
            loadVisits();
        });
        JButton cancel = UiTheme.dangerButton("Cancel Visit");
        cancel.addActionListener(e -> cancelVisit());

        JPanel filter = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        filter.add(new JLabel("Visit date"));
        filter.add(visitDate);
        filter.add(show);
        filter.add(allRecent);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(UiTheme.padding(10));
        panel.add(filter, BorderLayout.NORTH);
        panel.add(visits.getComponent(), BorderLayout.CENTER);
        panel.add(UiTheme.buttonRow(cancel), BorderLayout.SOUTH);
        return panel;
    }

    private void loadEntries() {
        LocalDate from = SwingDates.getDate(fromDate);
        LocalDate to = SwingDates.getDate(toDate);
        Async.run(this, () -> ctx.entries().entryLogs(from, to), entries::setRows);
    }

    private void loadVisits() {
        LocalDate date = allRecent.isSelected() ? null : SwingDates.getDate(visitDate);
        Async.run(this, () -> ctx.visits().listVisits(date), visits::setRows);
    }

    private void cancelVisit() {
        Optional<GuestVisit> selected = visits.getSelected();
        if (selected.isEmpty()) {
            Dialogs.info(this, "Select a visit first.");
            return;
        }
        if (!Dialogs.confirm(this, "Cancel the visit of " + selected.get().getGuestName() + "?")) {
            return;
        }
        Async.run(this, () -> {
            ctx.visits().cancelVisit(selected.get().getId());
            return null;
        }, ignored -> loadVisits());
    }
}
