package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.CampusLocation;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.DataTable;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.ListTableModel;
import edu.liceo.ugoautomate.ui.common.Refreshable;
import edu.liceo.ugoautomate.ui.common.UiTheme;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Optional;

import static edu.liceo.ugoautomate.ui.common.ListTableModel.column;

/**
 * Administrator management of campus locations (F-5.2).
 */
public class LocationsPanel extends JPanel implements Refreshable {

    private final AppContext ctx;
    private final DataTable<CampusLocation> table = new DataTable<>(new ListTableModel<>(
            column("Name", CampusLocation::getName),
            column("Type", CampusLocation::getType),
            column("Building", CampusLocation::getBuilding),
            column("Floor", CampusLocation::getFloor),
            column("Description", CampusLocation::getDescription)));
    private List<CampusLocation> current = List.of();

    public LocationsPanel(AppContext ctx) {
        super(new BorderLayout());
        this.ctx = ctx;

        JButton add = UiTheme.primaryButton("Add Location");
        add.addActionListener(e -> {
            if (LocationEditDialog.open(this, ctx, null, current)) {
                refresh();
            }
        });
        JButton edit = new JButton("Edit");
        edit.addActionListener(e -> editSelected());
        table.onDoubleClick(this::editSelected);
        JButton delete = UiTheme.dangerButton("Delete");
        delete.addActionListener(e -> deleteSelected());

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.add(table.getComponent(), BorderLayout.CENTER);
        body.add(UiTheme.buttonRow(add, edit, delete), BorderLayout.SOUTH);
        add(UiTheme.page(AdminDashboard.LOCATIONS,
                "Add, update, or remove the buildings, offices, facilities, and landmarks shown in the Campus Navigator.",
                body), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Async.run(this, ctx.locations()::listAll, locations -> {
            current = locations;
            table.setRows(locations);
        });
    }

    private void editSelected() {
        Optional<CampusLocation> selected = table.getSelected();
        if (selected.isEmpty()) {
            Dialogs.info(this, "Select a location first.");
            return;
        }
        if (LocationEditDialog.open(this, ctx, selected.get(), current)) {
            refresh();
        }
    }

    private void deleteSelected() {
        Optional<CampusLocation> selected = table.getSelected();
        if (selected.isEmpty()) {
            Dialogs.info(this, "Select a location first.");
            return;
        }
        if (!Dialogs.confirm(this, "Remove \"" + selected.get().getName() + "\" from the Campus Navigator?")) {
            return;
        }
        Async.run(this, () -> {
            ctx.locations().remove(selected.get().getId());
            return null;
        }, ignored -> refresh());
    }
}
