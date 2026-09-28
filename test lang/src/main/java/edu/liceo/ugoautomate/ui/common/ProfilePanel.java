package edu.liceo.ugoautomate.ui.common;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.Administrator;
import edu.liceo.ugoautomate.model.Guest;
import edu.liceo.ugoautomate.model.Role;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.service.ProfileUpdate;
import edu.liceo.ugoautomate.util.DateTimeUtil;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;

/**
 * Profile viewing and updating (F-1.4) plus password change. Only the fields
 * the user's role may edit are enabled; the service enforces the same rules.
 */
public class ProfilePanel extends JPanel implements Refreshable {

    private final AppContext ctx;
    private final Role role;

    private final JTextField username = readOnlyField();
    private final JTextField roleField = readOnlyField();
    private final JTextField memberSince = readOnlyField();
    private final JTextField studentNumber = readOnlyField();
    private final JTextField fullName = new JTextField(24);
    private final JTextField email = new JTextField(24);
    private final JTextField contact = new JTextField(24);
    private final JTextField course = new JTextField(24);
    private final JComboBox<Integer> yearLevel = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6});
    private final JTextField purpose = new JTextField(24);
    private final JTextField personToVisit = new JTextField(24);
    private final JTextField office = new JTextField(24);

    private final JPasswordField currentPassword = new JPasswordField(20);
    private final JPasswordField newPassword = new JPasswordField(20);
    private final JPasswordField confirmPassword = new JPasswordField(20);

    public ProfilePanel(AppContext ctx) {
        super(new BorderLayout());
        this.ctx = ctx;
        this.role = ctx.session().currentUser().orElseThrow().getRole();

        FormBuilder profileForm = new FormBuilder()
                .add("Username", username)
                .add("Account type", roleField);
        if (role == Role.STUDENT) {
            profileForm.add("Student ID", studentNumber);
            fullName.setEditable(false);
        }
        profileForm.add("Full name", fullName)
                .add("Email", email)
                .add("Contact number", contact);
        switch (role) {
            case STUDENT -> profileForm.add("Course", course).add("Year level", yearLevel);
            case GUEST -> profileForm.add("Default purpose of visit", purpose)
                    .add("Default person / office to visit", personToVisit);
            case ADMIN -> profileForm.add("Office", office);
        }
        profileForm.add("Member since", memberSince);

        JButton save = UiTheme.primaryButton("Save Profile");
        save.addActionListener(e -> saveProfile(save));
        profileForm.addFull(UiTheme.buttonRow(save));
        if (role == Role.STUDENT) {
            JLabel note = UiTheme.subtitle("Name and student ID changes must be requested from the administrator.");
            profileForm.addFull(note);
        }

        JButton change = UiTheme.primaryButton("Change Password");
        change.addActionListener(e -> changePassword(change));
        JPanel passwordForm = new FormBuilder()
                .add("Current password", currentPassword)
                .add("New password", newPassword)
                .add("Confirm new password", confirmPassword)
                .addFull(UiTheme.subtitle("At least 8 characters with a letter and a number."))
                .addFull(UiTheme.buttonRow(change))
                .build();

        JPanel passwordHolder = new JPanel(new BorderLayout());
        passwordHolder.add(UiTheme.card("Change Password", passwordForm), BorderLayout.NORTH);

        JPanel columns = new JPanel(new ResponsiveGridLayout(380, 2, 16, 16));
        columns.add(UiTheme.card("Profile Information", profileForm.build()));
        columns.add(passwordHolder);

        JPanel holder = new JPanel(new BorderLayout());
        holder.add(columns, BorderLayout.NORTH);
        add(UiTheme.page("My Profile", "View your account and update the details you are allowed to change.", holder),
                BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Async.run(this, ctx.profiles()::getMyProfile, this::populate);
    }

    private void populate(User user) {
        username.setText(user.getUsername());
        roleField.setText(user.getRole().getDisplayName());
        memberSince.setText(DateTimeUtil.format(user.getCreatedAt()));
        fullName.setText(user.getFullName());
        email.setText(nz(user.getEmail()));
        contact.setText(nz(user.getContactNumber()));
        if (user instanceof Student s) {
            studentNumber.setText(s.getStudentNumber());
            course.setText(s.getCourse());
            yearLevel.setSelectedItem(s.getYearLevel());
        } else if (user instanceof Guest g) {
            purpose.setText(g.getPurposeOfVisit());
            personToVisit.setText(g.getPersonToVisit());
        } else if (user instanceof Administrator a) {
            office.setText(a.getOffice());
        }
    }

    private void saveProfile(JButton button) {
        ProfileUpdate update = new ProfileUpdate()
                .setEmail(email.getText())
                .setContactNumber(contact.getText());
        switch (role) {
            case STUDENT -> update.setCourse(course.getText()).setYearLevel((Integer) yearLevel.getSelectedItem());
            case GUEST -> update.setFullName(fullName.getText())
                    .setPurposeOfVisit(purpose.getText())
                    .setPersonToVisit(personToVisit.getText());
            case ADMIN -> update.setFullName(fullName.getText()).setOffice(office.getText());
        }
        button.setEnabled(false);
        Async.run(this, () -> ctx.profiles().updateMyProfile(update), user -> {
            populate(user);
            Dialogs.info(this, "Your profile has been updated.");
        }, () -> button.setEnabled(true));
    }

    private void changePassword(JButton button) {
        char[] current = currentPassword.getPassword();
        char[] next = newPassword.getPassword();
        char[] confirm = confirmPassword.getPassword();
        button.setEnabled(false);
        Async.run(this, () -> {
            ctx.auth().changePassword(current, next, confirm);
            return null;
        }, ignored -> {
            currentPassword.setText("");
            newPassword.setText("");
            confirmPassword.setText("");
            Dialogs.info(this, "Your password has been changed.");
        }, () -> button.setEnabled(true));
    }

    private static JTextField readOnlyField() {
        JTextField field = new JTextField(24);
        field.setEditable(false);
        return field;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
