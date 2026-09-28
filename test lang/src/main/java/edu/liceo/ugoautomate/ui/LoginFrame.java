package edu.liceo.ugoautomate.ui;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.ui.admin.AdminDashboard;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.FormBuilder;
import edu.liceo.ugoautomate.ui.common.Responsive;
import edu.liceo.ugoautomate.ui.common.UiTheme;
import edu.liceo.ugoautomate.ui.guest.GuestDashboard;
import edu.liceo.ugoautomate.ui.student.StudentDashboard;
import edu.liceo.ugoautomate.util.AppConfig;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

/**
 * Sign-in screen (F-1.3). Routes each role to its own dashboard and offers
 * student and guest self-registration.
 */
public class LoginFrame extends JFrame {

    private final AppContext ctx;
    private final JTextField usernameField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);
    private final JButton loginButton = UiTheme.primaryButton("Log In");

    /** Below this width (phones, portrait tablets) the brand panel moves above the form. */
    private static final int STACK_BELOW_WIDTH = 720;

    private final JPanel root = new JPanel();
    private final JPanel brandPanel;
    private final JPanel formPanel;
    private final JLabel appLabel = new JLabel("Liceo U Go");
    private final JLabel automateLabel = new JLabel("Automate");
    private final JLabel taglineLabel =
            new JLabel("<html>Campus navigation, QR attendance,<br>and entry verification in one place.</html>");
    private final JPanel formFields;
    private Boolean stacked;

    public LoginFrame(AppContext ctx) {
        super(AppConfig.APP_NAME + " - Sign In");
        this.ctx = ctx;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(360, 520));
        Responsive.sizeToScreen(this, 920, 580);

        brandPanel = buildBrandPanel();
        formFields = buildForm();
        formPanel = buildFormPanel(formFields);
        setContentPane(root);
        getRootPane().setDefaultButton(loginButton);
        Responsive.onResize(this, this::updateLayout);
    }

    /** Side-by-side on wide windows; brand banner stacked on top of the form on narrow ones. */
    private void updateLayout() {
        boolean nowStacked = getWidth() < STACK_BELOW_WIDTH;
        if (stacked != null && stacked == nowStacked) {
            return;
        }
        stacked = nowStacked;
        root.removeAll();
        if (nowStacked) {
            root.setLayout(new BorderLayout());
            root.add(brandPanel, BorderLayout.NORTH);
            root.add(formPanel, BorderLayout.CENTER);
        } else {
            root.setLayout(new GridLayout(1, 2));
            root.add(brandPanel);
            root.add(formPanel);
        }
        float titleSize = nowStacked ? 28f : 40f;
        appLabel.setFont(appLabel.getFont().deriveFont(Font.BOLD, titleSize));
        automateLabel.setFont(automateLabel.getFont().deriveFont(Font.BOLD, titleSize));
        taglineLabel.setVisible(!nowStacked);
        brandPanel.setBorder(BorderFactory.createEmptyBorder(nowStacked ? 18 : 0, 16, nowStacked ? 18 : 0, 16));
        formFields.setBorder(BorderFactory.createEmptyBorder(16, nowStacked ? 18 : 40, 16, nowStacked ? 18 : 40));
        root.revalidate();
        root.repaint();
    }

    private JPanel buildBrandPanel() {
        JPanel brand = new JPanel(new GridBagLayout());
        brand.setBackground(UiTheme.PRIMARY);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        appLabel.setForeground(Color.WHITE);
        automateLabel.setForeground(UiTheme.ACCENT);
        JLabel uni = new JLabel(AppConfig.UNIVERSITY);
        uni.setFont(uni.getFont().deriveFont(16f));
        uni.setForeground(new Color(0xF3D98B));
        taglineLabel.setForeground(new Color(0xF4E8EA));

        text.add(appLabel);
        text.add(automateLabel);
        text.add(Box.createVerticalStrut(8));
        text.add(uni);
        text.add(Box.createVerticalStrut(20));
        text.add(taglineLabel);
        brand.add(text);
        return brand;
    }

    private JPanel buildForm() {
        JLabel title = UiTheme.title("Sign in");
        JTextArea hint = UiTheme.paragraph("Students: use your student ID. Guests: use your email.");
        hint.setForeground(UiTheme.TEXT_MUTED);

        loginButton.addActionListener(e -> doLogin());

        JButton registerStudent = new JButton("Register as Student");
        registerStudent.addActionListener(e -> RegisterStudentDialog.open(this, ctx));
        JButton registerGuest = new JButton("Register as Guest");
        registerGuest.addActionListener(e -> RegisterGuestDialog.open(this, ctx));

        JPanel form = new FormBuilder()
                .addFull(title)
                .addFull(hint)
                .add("Username", usernameField)
                .add("Password", passwordField)
                .addFull(loginButton)
                .addFull(new JLabel(" "))
                .addFull(UiTheme.subtitle("New to Liceo U Go Automate?"))
                .addFull(UiTheme.buttonRow(registerStudent, registerGuest))
                .build();
        form.setMaximumSize(new Dimension(460, Integer.MAX_VALUE));
        return form;
    }

    /** Centers the form and lets it scroll when the window is short (landscape phones/tablets). */
    private JPanel buildFormPanel(JPanel form) {
        JPanel holder = new JPanel(new GridBagLayout());
        holder.setBackground(Color.WHITE);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        holder.add(form, c);

        JScrollPane scroll = UiTheme.scroll(holder);
        scroll.getViewport().setBackground(Color.WHITE);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    private void doLogin() {
        String username = usernameField.getText();
        char[] password = passwordField.getPassword();
        loginButton.setEnabled(false);
        Async.run(this, () -> ctx.auth().login(username, password), this::openDashboard, () -> {
            loginButton.setEnabled(true);
            passwordField.setText("");
        });
    }

    private void openDashboard(User user) {
        JFrame dashboard = switch (user.getRole()) {
            case STUDENT -> new StudentDashboard(ctx);
            case GUEST -> new GuestDashboard(ctx);
            case ADMIN -> new AdminDashboard(ctx);
        };
        dashboard.setVisible(true);
        dispose();
    }
}
