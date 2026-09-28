package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.CampusLocation;
import edu.liceo.ugoautomate.model.LocationType;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.FormBuilder;
import edu.liceo.ugoautomate.ui.common.UiTheme;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.util.List;
import java.util.Objects;

/**
 * Add/edit form for a campus location (F-5.2).
 */
public final class LocationEditDialog extends JDialog {

    private final AppContext ctx;
    private final CampusLocation existing;

    private final JTextField name = new JTextField(24);
    private final JComboBox<LocationType> type = new JComboBox<>(LocationType.values());
    private final JComboBox<String> building = new JComboBox<>();
    private final JTextField floor = new JTextField(24);
    private final JTextArea description = new JTextArea(5, 24);
    private final JButton save = UiTheme.primaryButton("Save Location");
    private boolean saved;

    private LocationEditDialog(Window owner, AppContext ctx, CampusLocation existing, List<CampusLocation> all) {
        super(owner, existing == null ? "Add Location" : "Edit Location", ModalityType.APPLICATION_MODAL);
        this.ctx = ctx;
        this.existing = existing;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        building.setEditable(true);
        building.addItem("");
        all.stream().filter(l -> l.getType() == LocationType.BUILDING).map(CampusLocation::getName)
                .sorted().forEach(building::addItem);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);

        if (existing != null) {
            name.setText(existing.getName());
            type.setSelectedItem(existing.getType());
            building.setSelectedItem(Objects.toString(existing.getBuilding(), ""));
            floor.setText(Objects.toString(existing.getFloor(), ""));
            description.setText(Objects.toString(existing.getDescription(), ""));
        }

        JPanel form = new FormBuilder()
                .add("Name *", name)
                .add("Type *", type)
                .add("Building", building)
                .add("Floor", floor)
                .add("Description", new JScrollPane(description))
                .build();

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> submit());

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(UiTheme.padding(20));
        content.add(UiTheme.title(getTitle()), BorderLayout.NORTH);
        content.add(form, BorderLayout.CENTER);
        content.add(UiTheme.buttonRow(save, cancel), BorderLayout.SOUTH);
        setContentPane(UiTheme.dialogBody(content));
        UiTheme.fitToScreen(this, owner);
    }

    /** @return true if the location was saved */
    public static boolean open(Component parent, AppContext ctx, CampusLocation existing, List<CampusLocation> all) {
        Window owner = parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent);
        LocationEditDialog dialog = new LocationEditDialog(owner, ctx, existing, all);
        dialog.setVisible(true);
        return dialog.saved;
    }

    private void submit() {
        CampusLocation l = new CampusLocation();
        if (existing != null) {
            l.setId(existing.getId());
        }
        l.setName(name.getText());
        l.setType((LocationType) type.getSelectedItem());
        Object buildingValue = building.getEditor().getItem();
        l.setBuilding(buildingValue == null ? null : buildingValue.toString());
        l.setFloor(floor.getText());
        l.setDescription(description.getText());

        save.setEnabled(false);
        Async.run(this, () -> existing == null ? ctx.locations().add(l) : ctx.locations().update(l), ignored -> {
            saved = true;
            dispose();
        }, () -> save.setEnabled(true));
    }
}
