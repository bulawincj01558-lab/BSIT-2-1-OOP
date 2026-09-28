package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.AttendanceSession;
import edu.liceo.ugoautomate.model.CampusLocation;
import edu.liceo.ugoautomate.model.SessionType;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.FormBuilder;
import edu.liceo.ugoautomate.ui.common.SwingDates;
import edu.liceo.ugoautomate.ui.common.UiTheme;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Create/edit form for an attendance session.
 */
public final class SessionEditDialog extends JDialog {

    private final AppContext ctx;
    private final AttendanceSession existing;
    private final JTextField title = new JTextField(26);
    private final JComboBox<SessionType> type = new JComboBox<>(SessionType.values());
    private final JComboBox<String> venue = new JComboBox<>();
    private final JSpinner start;
    private final JSpinner end;
    private final JButton save = UiTheme.primaryButton("Save Session");
    private boolean saved;

    private SessionEditDialog(Window owner, AppContext ctx, AttendanceSession existing) {
        super(owner, existing == null ? "New Attendance Session" : "Edit Attendance Session",
                ModalityType.APPLICATION_MODAL);
        this.ctx = ctx;
        this.existing = existing;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        LocalDateTime defaultStart = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(1);
        start = SwingDates.dateTimeSpinner(existing == null ? defaultStart : existing.getStartTime());
        end = SwingDates.dateTimeSpinner(existing == null ? defaultStart.plusMinutes(90) : existing.getEndTime());
        venue.setEditable(true);
        title.putClientProperty("JTextField.placeholderText", "e.g. IT 213 - Object-Oriented Programming");

        if (existing != null) {
            title.setText(existing.getTitle());
            type.setSelectedItem(existing.getSessionType());
        }

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> submit());

        JPanel form = new FormBuilder()
                .add("Title *", title)
                .add("Type *", type)
                .add("Venue", venue)
                .add("Start *", start)
                .add("End *", end)
                .addFull(UiTheme.subtitle("Students can check in from " + AttendanceSession.EARLY_CHECK_IN_MINUTES
                        + " minutes before the start until the end time."))
                .build();

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(UiTheme.padding(20));
        content.add(UiTheme.title(getTitle()), BorderLayout.NORTH);
        content.add(form, BorderLayout.CENTER);
        content.add(UiTheme.buttonRow(save, cancel), BorderLayout.SOUTH);
        setContentPane(UiTheme.dialogBody(content));
        getRootPane().setDefaultButton(save);
        UiTheme.fitToScreen(this, owner);

        Async.run(this, ctx.locations()::listAll, locations -> {
            venue.addItem("");
            locations.stream().map(CampusLocation::getName).sorted().forEach(venue::addItem);
            venue.setSelectedItem(existing == null || existing.getVenue() == null ? "" : existing.getVenue());
        });
    }

    /** @return true if the session was saved */
    public static boolean open(Component parent, AppContext ctx, AttendanceSession existing) {
        Window owner = parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent);
        SessionEditDialog dialog = new SessionEditDialog(owner, ctx, existing);
        dialog.setVisible(true);
        return dialog.saved;
    }

    private void submit() {
        String t = title.getText();
        SessionType st = (SessionType) type.getSelectedItem();
        Object v = venue.getEditor().getItem();
        String venueValue = v == null ? null : v.toString();
        LocalDateTime s = SwingDates.getDateTime(start);
        LocalDateTime e = SwingDates.getDateTime(end);
        save.setEnabled(false);
        Async.run(this, () -> existing == null
                        ? ctx.attendance().createSession(t, st, venueValue, s, e)
                        : ctx.attendance().updateSession(existing.getId(), t, st, venueValue, s, e),
                ignored -> {
                    saved = true;
                    dispose();
                }, () -> save.setEnabled(true));
    }
}
