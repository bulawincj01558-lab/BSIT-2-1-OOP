package edu.liceo.ugoautomate.ui.guest;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.Guest;
import edu.liceo.ugoautomate.model.GuestVisit;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.DataTable;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.DisplayTime;
import edu.liceo.ugoautomate.ui.common.FormBuilder;
import edu.liceo.ugoautomate.ui.common.ListTableModel;
import edu.liceo.ugoautomate.ui.common.QrDisplayDialog;
import edu.liceo.ugoautomate.ui.common.Refreshable;
import edu.liceo.ugoautomate.ui.common.UiTheme;
import edu.liceo.ugoautomate.util.DateTimeUtil;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.Optional;

import static edu.liceo.ugoautomate.ui.common.ListTableModel.column;

/**
 * Guest visit registration (F-4.2) and visit passes. Visits are registered for
 * the current day; the pass is shown at the gate for verification.
 */
public class GuestVisitsPanel extends JPanel implements Refreshable {

    private final AppContext ctx;
    private final JTextField purpose = new JTextField(30);
    private final JComboBox<String> personToVisit = new JComboBox<>();
    private final JButton registerButton = UiTheme.primaryButton("Register Visit for Today");
    private final DataTable<GuestVisit> table = new DataTable<>(new ListTableModel<>(
            column("Visit Date", DisplayTime.class, v -> DisplayTime.of(v.getVisitDate())),
            column("Purpose", GuestVisit::getPurpose),
            column("Visiting", GuestVisit::getPersonToVisit),
            column("Status", GuestVisit::getStatus),
            column("Registered", DisplayTime.class, v -> DisplayTime.of(v.getRegisteredAt())),
            column("Checked In", DisplayTime.class, v -> DisplayTime.of(v.getCheckedInAt()))));
    private boolean officesLoaded;

    public GuestVisitsPanel(AppContext ctx) {
        super(new BorderLayout());
        this.ctx = ctx;
        personToVisit.setEditable(true);

        registerButton.addActionListener(e -> registerVisit());
        JPanel form = new FormBuilder()
                .add("Purpose of visit *", purpose)
                .add("Person / office to visit *", personToVisit)
                .addFull(UiTheme.buttonRow(registerButton))
                .build();

        JButton showPass = UiTheme.primaryButton("Show Visit Pass");
        showPass.addActionListener(e -> showPass());
        JButton cancel = UiTheme.dangerButton("Cancel Visit");
        cancel.addActionListener(e -> cancelVisit());
        table.onDoubleClick(this::showPass);

        JPanel list = new JPanel(new BorderLayout(0, 8));
        list.add(table.getComponent(), BorderLayout.CENTER);
        list.add(UiTheme.buttonRow(showPass, cancel), BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.add(UiTheme.card("Register a Visit", form), BorderLayout.NORTH);
        body.add(UiTheme.card("My Visits", list), BorderLayout.CENTER);

        add(UiTheme.page(GuestDashboard.VISITS,
                "Register your visit before campus entry. Each visit pass can be used once, on the day of the visit.",
                body), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Guest guest = (Guest) ctx.session().currentUser().orElseThrow();
        if (purpose.getText().isBlank()) {
            purpose.setText(guest.getPurposeOfVisit());
        }
        if (!officesLoaded) {
            Async.run(this, ctx.locations()::officeNames, offices -> {
                offices.forEach(personToVisit::addItem);
                personToVisit.setSelectedItem(guest.getPersonToVisit());
                officesLoaded = true;
            });
        }
        Async.run(this, ctx.visits()::myVisits, table::setRows);
    }

    private void registerVisit() {
        String visitPurpose = purpose.getText();
        Object person = personToVisit.getEditor().getItem();
        String personValue = person == null ? "" : person.toString();
        registerButton.setEnabled(false);
        Async.run(this, () -> ctx.visits().registerVisit(visitPurpose, personValue), visit -> {
            refresh();
            openPass(visit);
        }, () -> registerButton.setEnabled(true));
    }

    private void showPass() {
        Optional<GuestVisit> selected = table.getSelected();
        if (selected.isEmpty()) {
            Dialogs.info(this, "Select a visit first.");
            return;
        }
        openPass(selected.get());
    }

    private void openPass(GuestVisit visit) {
        Async.run(this, () -> ctx.visits().visitPassPayload(visit.getId()), payload ->
                QrDisplayDialog.show(this, "Visit Pass",
                        visit.getGuestName() + "\nVisiting: " + visit.getPersonToVisit()
                                + "\nValid on " + DateTimeUtil.format(visit.getVisitDate()) + " (one entry)",
                        payload, "visit-pass-" + visit.getId()));
    }

    private void cancelVisit() {
        Optional<GuestVisit> selected = table.getSelected();
        if (selected.isEmpty()) {
            Dialogs.info(this, "Select a visit first.");
            return;
        }
        if (!Dialogs.confirm(this, "Cancel this visit? Its visit pass will no longer work.")) {
            return;
        }
        Async.run(this, () -> {
            ctx.visits().cancelVisit(selected.get().getId());
            return null;
        }, ignored -> refresh());
    }
}
