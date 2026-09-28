package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.Guest;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.ui.RegisterGuestDialog;
import edu.liceo.ugoautomate.ui.RegisterStudentDialog;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.DataTable;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.DisplayTime;
import edu.liceo.ugoautomate.ui.common.FormBuilder;
import edu.liceo.ugoautomate.ui.common.ListTableModel;
import edu.liceo.ugoautomate.ui.common.Refreshable;
import edu.liceo.ugoautomate.ui.common.UiTheme;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.util.Optional;

import static edu.liceo.ugoautomate.ui.common.ListTableModel.column;

/**
 * Administrator management of student and guest accounts (F-5.1).
 */
public class AccountsPanel extends JPanel implements Refreshable {

    private final AppContext ctx;

    private final DataTable<Student> students = new DataTable<>(new ListTableModel<>(
            column("Student ID", Student::getStudentNumber),
            column("Full Name", Student::getFullName),
            column("Course", Student::getCourse),
            column("Year", Integer.class, Student::getYearLevel),
            column("Email", Student::getEmail),
            column("Contact", Student::getContactNumber),
            column("Status", s -> s.isActive() ? "Active" : "Deactivated")));

    private final DataTable<Guest> guests = new DataTable<>(new ListTableModel<>(
            column("Full Name", Guest::getFullName),
            column("Username / Email", Guest::getUsername),
            column("Contact", Guest::getContactNumber),
            column("Purpose of Visit", Guest::getPurposeOfVisit),
            column("Person / Office to Visit", Guest::getPersonToVisit),
            column("Status", g -> g.isActive() ? "Active" : "Deactivated"),
            column("Registered", DisplayTime.class, g -> DisplayTime.of(g.getCreatedAt()))));

    public AccountsPanel(AppContext ctx) {
        super(new BorderLayout());
        this.ctx = ctx;

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Students", studentsTab());
        tabs.addTab("Guests", guestsTab());
        add(UiTheme.page(AdminDashboard.ACCOUNTS, "View, add, edit, deactivate, or remove student and guest accounts.",
                tabs), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Async.run(this, ctx.accounts()::listStudents, students::setRows);
        Async.run(this, ctx.accounts()::listGuests, guests::setRows);
    }

    private JPanel studentsTab() {
        JButton add = UiTheme.primaryButton("Add Student");
        add.addActionListener(e -> {
            if (RegisterStudentDialog.open(this, ctx, "Add Student")) {
                refresh();
            }
        });
        JButton edit = new JButton("Edit");
        edit.addActionListener(e -> students.getSelected().ifPresentOrElse(this::editStudent, this::selectFirst));
        students.onDoubleClick(() -> students.getSelected().ifPresent(this::editStudent));
        return tab(students, add, edit, commonButtons(students));
    }

    private JPanel guestsTab() {
        JButton add = UiTheme.primaryButton("Add Guest");
        add.addActionListener(e -> {
            if (RegisterGuestDialog.open(this, ctx, "Add Guest")) {
                refresh();
            }
        });
        JButton edit = new JButton("Edit");
        edit.addActionListener(e -> guests.getSelected().ifPresentOrElse(this::editGuest, this::selectFirst));
        guests.onDoubleClick(() -> guests.getSelected().ifPresent(this::editGuest));
        return tab(guests, add, edit, commonButtons(guests));
    }

    private JButton[] commonButtons(DataTable<? extends User> table) {
        JButton toggle = new JButton("Activate / Deactivate");
        toggle.addActionListener(e -> selected(table).ifPresent(this::toggleActive));
        JButton reset = new JButton("Reset Password");
        reset.addActionListener(e -> selected(table).ifPresent(this::resetPassword));
        JButton delete = UiTheme.dangerButton("Delete");
        delete.addActionListener(e -> selected(table).ifPresent(this::delete));
        return new JButton[]{toggle, reset, delete};
    }

    private static JPanel tab(DataTable<?> table, JButton add, JButton edit, JButton[] others) {
        JPanel buttons = UiTheme.buttonRow(add, edit, others[0], others[1], others[2]);
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(UiTheme.padding(10));
        panel.add(table.getComponent(), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private Optional<User> selected(DataTable<? extends User> table) {
        Optional<User> user = table.getSelected().map(User.class::cast);
        if (user.isEmpty()) {
            selectFirst();
        }
        return user;
    }

    private void selectFirst() {
        Dialogs.info(this, "Select an account first.");
    }

    private void editStudent(Student student) {
        if (AccountEditDialog.editStudent(this, ctx, student)) {
            refresh();
        }
    }

    private void editGuest(Guest guest) {
        if (AccountEditDialog.editGuest(this, ctx, guest)) {
            refresh();
        }
    }

    private void toggleActive(User user) {
        boolean activate = !user.isActive();
        String action = activate ? "activate" : "deactivate";
        if (!Dialogs.confirm(this, "Do you want to " + action + " the account of " + user.getFullName() + "?")) {
            return;
        }
        Async.run(this, () -> {
            ctx.accounts().setActive(user.getId(), activate);
            return null;
        }, ignored -> refresh());
    }

    private void resetPassword(User user) {
        JPasswordField pw = new JPasswordField(20);
        JPasswordField confirm = new JPasswordField(20);
        JPanel form = new FormBuilder()
                .addFull(UiTheme.subtitle("Set a new password for " + user.getFullName() + "."))
                .add("New password", pw)
                .add("Confirm password", confirm)
                .build();
        int choice = JOptionPane.showConfirmDialog(this, form, "Reset Password",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        char[] p1 = pw.getPassword();
        char[] p2 = confirm.getPassword();
        Async.run(this, () -> {
            ctx.accounts().resetPassword(user.getId(), p1, p2);
            return null;
        }, ignored -> Dialogs.info(this, "The password for " + user.getFullName() + " has been reset."));
    }

    private void delete(User user) {
        if (!Dialogs.confirm(this, "Permanently delete the account of " + user.getFullName() + "?\n\n"
                + "Their attendance records and visit registrations will also be removed.\n"
                + "Entry logs keep the recorded name. Consider deactivating instead.")) {
            return;
        }
        Async.run(this, () -> {
            ctx.accounts().deleteAccount(user.getId());
            return null;
        }, ignored -> refresh());
    }
}
