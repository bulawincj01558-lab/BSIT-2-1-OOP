package edu.liceo.ugoautomate.ui.common;

import edu.liceo.ugoautomate.util.AppConfig;
import edu.liceo.ugoautomate.util.AppException;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Standard message and confirmation dialogs.
 */
public final class Dialogs {

    private Dialogs() {
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, AppConfig.APP_NAME, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, AppConfig.APP_NAME, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Shows an error. Expected application errors show their own message;
     * unexpected ones show a generic message plus the technical detail.
     */
    public static void error(Component parent, Throwable t) {
        if (t instanceof AppException) {
            error(parent, t.getMessage());
        } else {
            error(parent, "Something went wrong. Please try again.\n\nDetails: " + t);
        }
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, AppConfig.APP_NAME,
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }
}
