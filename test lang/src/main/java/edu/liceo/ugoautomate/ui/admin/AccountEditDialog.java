package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.Guest;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.FormBuilder;
import edu.liceo.ugoautomate.ui.common.UiTheme;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.util.concurrent.Callable;

/**
 * Administrator edit form for a student or guest account.
 */
public final class AccountEditDialog extends JDialog {

    private boolean saved;

    private AccountEditDialog(Window owner, String title, JPanel form, JButton save) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(UiTheme.padding(20));
        content.add(UiTheme.title(title), BorderLayout.NORTH);
        content.add(form, BorderLayout.CENTER);
        content.add(UiTheme.buttonRow(save, cancel), BorderLayout.SOUTH);
        setContentPane(UiTheme.dialogBody(content));
        getRootPane().setDefaultButton(save);
        UiTheme.fitToScreen(this, owner);
    }

    /** @return true if changes were saved */
    public static boolean editStudent(Component parent, AppContext ctx, Student student) {
        JTextField name = new JTextField(student.getFullName(), 24);
        JTextField email = new JTextField(nz(student.getEmail()), 24);
        JTextField contact = new JTextField(nz(student.getContactNumber()), 24);
        JTextField course = new JTextField(student.getCourse(), 24);
        JComboBox<Integer> year = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6});
        year.setSelectedItem(student.getYearLevel());

        JPanel form = new FormBuilder()
                .add("Student ID", new JLabel(student.getStudentNumber()))
                .add("Full name", name)
                .add("Email", email)
                .add("Contact number", contact)
                .add("Course", course)
                .add("Year level", year)
                .build();
        return show(parent, "Edit Student", form, () -> {
            ctx.accounts().updateStudent(student.getId(), name.getText(), email.getText(), contact.getText(),
                    course.getText(), (Integer) year.getSelectedItem());
            return null;
        });
    }

    /** @return true if changes were saved */
    public static boolean editGuest(Component parent, AppContext ctx, Guest guest) {
        JTextField name = new JTextField(guest.getFullName(), 24);
        JTextField email = new JTextField(nz(guest.getEmail()), 24);
        JTextField contact = new JTextField(nz(guest.getContactNumber()), 24);
        JTextField purpose = new JTextField(guest.getPurposeOfVisit(), 24);
        JTextField person = new JTextField(guest.getPersonToVisit(), 24);

        JPanel form = new FormBuilder()
                .add("Username", new JLabel(guest.getUsername()))
                .add("Full name", name)
                .add("Email", email)
                .add("Contact number", contact)
                .add("Purpose of visit", purpose)
                .add("Person / office to visit", person)
                .build();
        return show(parent, "Edit Guest", form, () -> {
            ctx.accounts().updateGuest(guest.getId(), name.getText(), email.getText(), contact.getText(),
                    purpose.getText(), person.getText());
            return null;
        });
    }

    private static boolean show(Component parent, String title, JPanel form, Callable<Void> saveAction) {
        Window owner = parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent);
        JButton save = UiTheme.primaryButton("Save Changes");
        AccountEditDialog dialog = new AccountEditDialog(owner, title, form, save);
        save.addActionListener(e -> {
            save.setEnabled(false);
            Async.run(dialog, saveAction, ignored -> {
                dialog.saved = true;
                dialog.dispose();
            }, () -> save.setEnabled(true));
        });
        dialog.setVisible(true);
        return dialog.saved;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
