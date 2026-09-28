package edu.liceo.ugoautomate.ui.common;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.CampusLocation;
import edu.liceo.ugoautomate.model.LocationType;
import edu.liceo.ugoautomate.service.LocationService;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

/**
 * Campus Navigator (F-2.1 - F-2.3): an organized, searchable directory of
 * buildings, offices, facilities, and landmarks with a details card for the
 * selected location.
 * <p>
 * On wide screens the list and details sit side by side; on narrow or
 * portrait screens they stack vertically.
 */
public class CampusNavigatorPanel extends JPanel implements Refreshable {

    private static final String ALL_TYPES = "All types";
    private static final int STACK_BELOW_WIDTH = 700;

    private final AppContext ctx;
    private final JTextField searchField = new JTextField(22);
    private final JComboBox<Object> typeFilter = new JComboBox<>();
    private final DefaultListModel<CampusLocation> listModel = new DefaultListModel<>();
    private final JList<CampusLocation> resultList = new JList<>(listModel);
    private final JLabel resultCount = new JLabel();
    private final JSplitPane split;

    private final JLabel detailName = new JLabel("Select a location");
    private final JLabel detailType = new JLabel("-");
    private final JLabel detailBuilding = new JLabel("-");
    private final JLabel detailFloor = new JLabel("-");
    private final JTextArea detailDescription = UiTheme.paragraph("Choose a building, office, or facility from the list.");

    private List<CampusLocation> allLocations = List.of();

    public CampusNavigatorPanel(AppContext ctx) {
        super(new BorderLayout());
        this.ctx = ctx;

        typeFilter.addItem(ALL_TYPES);
        for (LocationType type : LocationType.values()) {
            typeFilter.addItem(type);
        }
        searchField.putClientProperty("JTextField.placeholderText", "Search a building, office, or facility...");
        searchField.putClientProperty("JTextField.showClearButton", true);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
        typeFilter.addActionListener(e -> applyFilter());

        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof CampusLocation l) {
                    setText("<html><b>" + escape(l.getName()) + "</b><br><small>" + l.getType().getDisplayName()
                            + (l.getBuilding() != null && l.getType() != LocationType.BUILDING
                            ? " - " + escape(l.getBuilding()) : "") + "</small></html>");
                    setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
                }
                return this;
            }
        });
        resultList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && resultList.getSelectedValue() != null) {
                showDetails(resultList.getSelectedValue());
            }
        });

        JPanel searchBar = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        searchBar.add(new JLabel("Search:"));
        searchBar.add(searchField);
        searchBar.add(new JLabel("Type:"));
        searchBar.add(typeFilter);
        resultCount.setForeground(UiTheme.TEXT_MUTED);
        searchBar.add(resultCount);

        JScrollPane listScroll = new JScrollPane(resultList);
        listScroll.setPreferredSize(new Dimension(320, 380));
        listScroll.setMinimumSize(new Dimension(200, 160));
        JPanel details = buildDetailsCard();
        details.setMinimumSize(new Dimension(200, 160));

        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, details);
        split.setResizeWeight(0.45);
        split.setBorder(null);
        split.setContinuousLayout(true);

        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.add(searchBar, BorderLayout.NORTH);
        body.add(split, BorderLayout.CENTER);
        add(UiTheme.page("Campus Navigator",
                "Search for a building, office, or facility and select it to see where it is and what it offers.",
                body), BorderLayout.CENTER);

        Responsive.onResize(this, this::updateOrientation);
    }

    @Override
    public void refresh() {
        Async.run(this, ctx.locations()::listAll, locations -> {
            allLocations = locations;
            applyFilter();
        });
    }

    /** Stacks list and details vertically on narrow screens, side by side otherwise. */
    private void updateOrientation() {
        int wanted = getWidth() > 0 && getWidth() < STACK_BELOW_WIDTH
                ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT;
        if (split.getOrientation() != wanted) {
            split.setOrientation(wanted);
            split.setDividerLocation(0.5);
        }
    }

    private JPanel buildDetailsCard() {
        detailName.setFont(detailName.getFont().deriveFont(Font.BOLD, 18f));
        detailName.setForeground(UiTheme.PRIMARY_DARK);

        JPanel form = new FormBuilder()
                .addFull(detailName)
                .add("Type", detailType)
                .add("Building", detailBuilding)
                .add("Floor", detailFloor)
                .addFull(new JLabel("Description"))
                .addFull(detailDescription)
                .build();
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(form, BorderLayout.NORTH);
        return UiTheme.card("Location Details", holder);
    }

    private void applyFilter() {
        Object typeItem = typeFilter.getSelectedItem();
        LocationType type = typeItem instanceof LocationType t ? t : null;
        List<CampusLocation> filtered = LocationService.filter(allLocations, searchField.getText(), type);

        CampusLocation previouslySelected = resultList.getSelectedValue();
        listModel.clear();
        filtered.forEach(listModel::addElement);
        resultCount.setText(filtered.size() + " of " + allLocations.size() + " locations");
        if (previouslySelected != null && listModel.contains(previouslySelected)) {
            resultList.setSelectedValue(previouslySelected, true);
        } else if (filtered.size() == 1) {
            resultList.setSelectedIndex(0);
        }
    }

    private void showDetails(CampusLocation l) {
        detailName.setText(l.getName());
        detailType.setText(l.getType().getDisplayName());
        detailBuilding.setText(l.getBuilding() == null ? "-" : l.getBuilding());
        detailFloor.setText(l.getFloor() == null ? "-" : l.getFloor());
        detailDescription.setText(l.getDescription() == null ? "" : l.getDescription());
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
