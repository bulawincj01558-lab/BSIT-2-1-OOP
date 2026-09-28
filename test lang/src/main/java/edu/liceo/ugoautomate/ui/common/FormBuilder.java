package edu.liceo.ugoautomate.ui.common;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Builds consistent two-column label/field forms with GridBagLayout.
 */
public final class FormBuilder {

    private final JPanel panel = new JPanel(new GridBagLayout());
    private int row;

    public FormBuilder() {
        panel.setOpaque(false);
    }

    public FormBuilder add(String label, JComponent field) {
        GridBagConstraints lc = new GridBagConstraints();
        lc.gridx = 0;
        lc.gridy = row;
        lc.anchor = GridBagConstraints.LINE_START;
        lc.insets = new Insets(5, 0, 5, 12);
        JLabel l = new JLabel(label);
        l.setLabelFor(field);
        panel.add(l, lc);

        GridBagConstraints fc = new GridBagConstraints();
        fc.gridx = 1;
        fc.gridy = row;
        fc.weightx = 1;
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.insets = new Insets(5, 0, 5, 0);
        panel.add(field, fc);
        row++;
        return this;
    }

    /** Adds a component spanning both columns. */
    public FormBuilder addFull(JComponent component) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(8, 0, 5, 0);
        panel.add(component, c);
        return this;
    }

    public JPanel build() {
        return panel;
    }
}
