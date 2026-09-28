package edu.liceo.ugoautomate;

import edu.liceo.ugoautomate.ui.LoginFrame;
import edu.liceo.ugoautomate.ui.common.UiTheme;
import edu.liceo.ugoautomate.util.AppConfig;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Application entry point: installs the look and feel, initializes the local
 * database and services, then shows the login screen.
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UiTheme.install();
            AppContext ctx;
            try {
                ctx = AppContext.create(AppConfig.databasePath());
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(null,
                        "Liceo U Go Automate could not start.\n\n" + e.getMessage()
                                + "\n\nDatabase location: " + AppConfig.databasePath(),
                        AppConfig.APP_NAME, JOptionPane.ERROR_MESSAGE);
                System.exit(1);
                return;
            }
            Runtime.getRuntime().addShutdownHook(new Thread(ctx::close, "ugo-shutdown"));
            new LoginFrame(ctx).setVisible(true);
        });
    }
}
