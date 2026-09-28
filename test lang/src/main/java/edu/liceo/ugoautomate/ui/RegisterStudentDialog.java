package edu.liceo.ugoautomate.ui;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.FormBuilder;
import edu.liceo.ugoautomate.ui.common.UiTheme;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;

/**
 * Student registration form (F-1.1). Used from the login screen and by
 * administrators adding students.
 */
public class RegisterStudentDialog extends JDialog {

    /** Fixed-size array (16 elements, 80 bytes of references): O(1) access by index. */
    static final String[] COURSES = {
            "BSIT", "BSCS", "BSIS", "BSN", "BSA", "BSBA", "BSHM", "BSTM", "BSEd", "BEEd",
            "BSCrim", "BSPsych", "AB Comm", "BS Pharmacy", "BS MedTech", "BS Civil Eng"
    };

    private final AppContext ctx;
    private final JTextField studentNumber = new JTextField(22);
    private final JTextField fullName = new JTextField(22);
    private final JComboBox<String> course = new JComboBox<>(COURSES);
    private final JComboBox<Integer> yearLevel = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6});
    private final JTextField email = new JTextField(22);
    private final JTextField contact = new JTextField(22);
    private final JPasswordField password = new JPasswordField(22);
    private final JPasswordField confirm = new JPasswordField(22);
    private final JButton register = UiTheme.primaryButton("Register");
    private boolean registered;

    private RegisterStudentDialog(Window owner, AppContext ctx, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.ctx = ctx;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        course.setEditable(true);
        studentNumber.putClientProperty("JTextField.placeholderText", "e.g. 2025-00123");

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        register.addActionListener(e -> submit());

        JPanel form = new FormBuilder()
                .addFull(UiTheme.title(title))
                .add("Student ID *", studentNumber)
                .add("Full name *", fullName)
                .add("Course *", course)
                .add("Year level *", yearLevel)
                .add("Email *", email)
                .add("Contact number", contact)
                .add("Password *", password)
                .add("Confirm password *", confirm)
                .addFull(UiTheme.subtitle("Password: at least 8 characters with a letter and a number."))
                .addFull(UiTheme.buttonRow(register, cancel))
                .build();
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(UiTheme.padding(20));
        content.add(form, BorderLayout.CENTER);
        setContentPane(UiTheme.dialogBody(content));
        getRootPane().setDefaultButton(register);
        UiTheme.fitToScreen(this, owner);
    }

    /** @return true if a student was registered */
    public static boolean open(Component parent, AppContext ctx) {
        return open(parent, ctx, "Student Registration");
    }

    public static boolean open(Component parent, AppContext ctx, String title) {
        Window owner = parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent);
        RegisterStudentDialog dialog = new RegisterStudentDialog(owner, ctx, title);
        dialog.setVisible(true);
        return dialog.registered;
    }

    private void submit() {
        String number = studentNumber.getText();
        String name = fullName.getText();
        String courseValue = String.valueOf(course.getSelectedItem());
        int year = (Integer) yearLevel.getSelectedItem();
        String mail = email.getText();
        String phone = contact.getText();
        char[] pw = password.getPassword();
        char[] pw2 = confirm.getPassword();
        register.setEnabled(false);
        Async.run(this, () -> ctx.auth().registerStudent(number, name, courseValue, year, mail, phone, pw, pw2),
                student -> {
                    registered = true;
                    Dialogs.info(this, "Registration successful.\n\n" + student.getFullName()
                            + " can now log in using student ID " + student.getStudentNumber() + ".");
                    dispose();
                }, () -> register.setEnabled(true));
    }
}
