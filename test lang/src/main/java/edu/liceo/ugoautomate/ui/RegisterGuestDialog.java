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
 * Guest registration form (F-1.2) capturing the information required before
 * campus entry: name, contact, purpose of visit, and person/office to visit.
 */
public class RegisterGuestDialog extends JDialog {

    private final AppContext ctx;
    private final JTextField fullName = new JTextField(24);
    private final JTextField contact = new JTextField(24);
    private final JTextField email = new JTextField(24);
    private final JTextField purpose = new JTextField(24);
    private final JComboBox<String> personToVisit = new JComboBox<>();
    private final JPasswordField password = new JPasswordField(24);
    private final JPasswordField confirm = new JPasswordField(24);
    private final JButton register = UiTheme.primaryButton("Register");
    private boolean registered;

    private RegisterGuestDialog(Window owner, AppContext ctx, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.ctx = ctx;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        personToVisit.setEditable(true);
        email.putClientProperty("JTextField.placeholderText", "You will use this to log in");
        purpose.putClientProperty("JTextField.placeholderText", "e.g. Enrollment inquiry, meeting, delivery of documents");

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        register.addActionListener(e -> submit());

        JPanel form = new FormBuilder()
                .addFull(UiTheme.title(title))
                .add("Full name *", fullName)
                .add("Contact number *", contact)
                .add("Email *", email)
                .add("Purpose of visit *", purpose)
                .add("Person / office to visit *", personToVisit)
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

        Async.run(this, ctx.locations()::officeNames, offices -> {
            offices.forEach(personToVisit::addItem);
            personToVisit.setSelectedItem("");
        });
    }

    /** @return true if a guest was registered */
    public static boolean open(Component parent, AppContext ctx) {
        return open(parent, ctx, "Guest Registration");
    }

    public static boolean open(Component parent, AppContext ctx, String title) {
        Window owner = parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent);
        RegisterGuestDialog dialog = new RegisterGuestDialog(owner, ctx, title);
        dialog.setVisible(true);
        return dialog.registered;
    }

    private void submit() {
        String name = fullName.getText();
        String phone = contact.getText();
        String mail = email.getText();
        String visitPurpose = purpose.getText();
        Object person = personToVisit.getEditor().getItem();
        String personValue = person == null ? "" : person.toString();
        char[] pw = password.getPassword();
        char[] pw2 = confirm.getPassword();
        register.setEnabled(false);
        Async.run(this, () -> ctx.auth().registerGuest(name, phone, mail, visitPurpose, personValue, pw, pw2),
                guest -> {
                    registered = true;
                    Dialogs.info(this, "Registration successful.\n\nLog in with " + guest.getUsername()
                            + ", then register your visit to get a visit pass for the gate.");
                    dispose();
                }, () -> register.setEnabled(true));
    }
}
