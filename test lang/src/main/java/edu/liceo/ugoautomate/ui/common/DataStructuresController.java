package edu.liceo.ugoautomate.ui.common;

import com.google.zxing.common.BitMatrix;
import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.ds.BTreeModel;
import edu.liceo.ugoautomate.ds.Complexity;
import edu.liceo.ugoautomate.ds.ComplexityBenchmark;
import edu.liceo.ugoautomate.ds.MemoryEstimator;
import edu.liceo.ugoautomate.ds.StructureCatalog;
import edu.liceo.ugoautomate.ds.StructureInfo;
import edu.liceo.ugoautomate.model.CampusLocation;
import edu.liceo.ugoautomate.ui.common.Views.View;
import edu.liceo.ugoautomate.util.QrCodes;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public class DataStructuresController implements Page {
    private static final String SAMPLE_TOKEN =
            "UGO1.ATTENDANCE.1.1790000000.1790003600.AbCdEfGhIjKl.0123456789abcdefghijklmnopqrstuvwxyzABCDEFG";

    private final AppContext ctx;
    private Supplier<List<String>> navigationHistory = List::of;
    private Supplier<List<String>> pageNames = List::of;
    private ScanHistory scanHistory = new ScanHistory();

    @FXML private ToggleButton structuresTab;
    @FXML private ToggleButton benchmarkTab;
    @FXML private VBox structuresPanel;
    @FXML private VBox benchmarkPanel;
    @FXML private FlowPane chips;
    @FXML private Label title;
    @FXML private Label javaType;
    @FXML private Label usedIn;
    @FXML private Label liveLabel;
    @FXML private ScrollPane diagramScroll;
    @FXML private DsDiagram diagram;
    @FXML private Label allocation;
    @FXML private TableView<MemoryEstimator.Line> memoryTable;
    @FXML private Label memoryTotal;
    @FXML private Label memoryNote;
    @FXML private TableView<Complexity> complexityTable;
    @FXML private Label spaceLabel;
    @FXML private TableView<ComplexityBenchmark.Result> benchmarkTable;
    @FXML private Label benchmarkStatus;
    @FXML private Button runBenchmark;

    private StructureInfo selected = StructureCatalog.all().get(0);
    private List<CampusLocation> locations = List.of();

    public DataStructuresController(AppContext ctx) {
        this.ctx = ctx;
    }

    public static View<DataStructuresController> create(AppContext ctx, DashboardController dashboard) {
        View<DataStructuresController> view = Views.load(ctx, "common/DataStructures.fxml");
        DataStructuresController c = view.controller();
        c.navigationHistory = dashboard::navigationHistory;
        c.pageNames = dashboard::pageNames;
        c.scanHistory = dashboard.scanHistory();
        return view;
    }

    @FXML
    private void initialize() {
        Tabs.link(new ToggleButton[]{structuresTab, benchmarkTab}, new Node[]{structuresPanel, benchmarkPanel});

        diagram.heightProperty().addListener((obs, old, height) -> {
            diagramScroll.setPrefViewportHeight(height.doubleValue());
            diagramScroll.setMinViewportHeight(height.doubleValue());
        });

        ToggleGroup group = new ToggleGroup();
        for (StructureInfo info : StructureCatalog.all()) {
            ToggleButton chip = new ToggleButton(info.title());
            chip.getStyleClass().add("chip");
            chip.setToggleGroup(group);
            chip.setSelected(info == selected);
            chip.setOnAction(e -> {
                chip.setSelected(true);
                selected = info;
                render();
            });
            chips.getChildren().add(chip);
        }

        Tables.setup(memoryTable, "");
        Tables.column(memoryTable, "Part", MemoryEstimator.Line::part);
        Tables.column(memoryTable, "Bytes", l -> l.bytes() == 0 ? "" : MemoryEstimator.format(l.bytes()));

        Tables.setup(complexityTable, "");
        Tables.column(complexityTable, "Operation", Complexity::operation);
        Tables.column(complexityTable, "Best", Complexity::best);
        Tables.column(complexityTable, "Average", Complexity::average);
        Tables.column(complexityTable, "Worst", Complexity::worst);

        Tables.setup(benchmarkTable, "Press \"Run measurement\" to measure this computer.");
        Tables.column(benchmarkTable, "Operation", ComplexityBenchmark.Result::operation);
        Tables.column(benchmarkTable, "Structure", ComplexityBenchmark.Result::structure);
        Tables.column(benchmarkTable, "Big-O", ComplexityBenchmark.Result::bigO);
        Tables.column(benchmarkTable, "n = 1,000", r -> formatNanos(r.nanosPerOp()[0]));
        Tables.column(benchmarkTable, "n = 10,000", r -> formatNanos(r.nanosPerOp()[1]));
        Tables.column(benchmarkTable, "n = 100,000", r -> formatNanos(r.nanosPerOp()[2]));
        Tables.column(benchmarkTable, "Growth (100k ÷ 1k)",
                r -> String.format("× %.1f", r.nanosPerOp()[2] / (double) r.nanosPerOp()[0]));
    }

    @Override
    public void onShow() {
        Async.run(diagram, () -> ctx.locations().listAll(), loaded -> {
            locations = loaded;
            render();
        });
    }

    private void render() {
        StructureInfo info = selected;
        title.setText(info.title());
        javaType.setText("Type: " + info.javaType());
        usedIn.setText(info.usedIn());
        allocation.setText(info.allocation());
        Tables.setRows(complexityTable, info.operations());
        Tables.fitRows(complexityTable, info.operations().size());
        spaceLabel.setText("Space complexity: " + info.spaceComplexity());

        List<String> names = locations.stream().map(CampusLocation::getName).toList();
        MemoryEstimator.Report report;
        switch (info.kind()) {
            case ARRAY -> {
                BitMatrix matrix = QrCodes.moduleMatrix(SAMPLE_TOKEN);
                diagram.showBitGrid(matrix);
                liveLabel.setText("Live: a QR code for a " + SAMPLE_TOKEN.length() + "-character token is "
                        + matrix.getWidth() + " × " + matrix.getHeight() + " modules");
                report = MemoryEstimator.bitMatrix(matrix.getWidth(), matrix.getHeight());
            }
            case DYNAMIC_ARRAY -> {
                int capacity = MemoryEstimator.arrayListCapacity(names.size());
                diagram.showCells(names, capacity);
                liveLabel.setText("Live: " + names.size() + " campus locations loaded from the database (capacity " + capacity + ")");
                report = MemoryEstimator.arrayList(names.size());
            }
            case HASH_TABLE -> {
                int buckets = MemoryEstimator.hashMapCapacity(names.size());
                diagram.showHashTable(names, buckets);
                liveLabel.setText("Live: " + names.size() + " location names hashed into " + buckets + " buckets");
                report = MemoryEstimator.hashMap(names.size());
            }
            case STACK -> {
                List<String> stack = navigationHistory.get();
                diagram.showStack(stack);
                liveLabel.setText("Live: your navigation stack holds " + stack.size() + " page(s)");
                report = MemoryEstimator.arrayDeque(stack.size(), "pages");
            }
            case QUEUE -> {
                List<String> queue = scanHistory.oldestFirst();
                diagram.showQueue(queue);
                liveLabel.setText("Live: " + queue.size() + " of " + ScanHistory.CAPACITY + " recent scan results");
                report = MemoryEstimator.arrayDeque(queue.size(), "scan results");
            }
            case DOUBLY_LINKED_LIST -> {
                List<String> pages = pageNames.get();
                diagram.showLinkedList(pages);
                liveLabel.setText("Live: this dashboard's " + pages.size() + " pages in menu order");
                report = MemoryEstimator.linkedHashMap(pages.size());
            }
            case B_TREE -> {
                List<String> sorted = new ArrayList<>(names);
                sorted.sort(Comparator.comparing(String::toLowerCase));
                diagram.showBTree(BTreeModel.build(sorted, 4));
                int averageLength = (int) Math.round(names.stream().mapToInt(String::length).average().orElse(20));
                liveLabel.setText("Live: index idx_locations_name over " + names.size() + " location names (drawn with order 4)");
                report = MemoryEstimator.bTreeIndex(names.size(), averageLength);
            }
            default -> throw new IllegalStateException("Unknown structure " + info.kind());
        }
        Tables.setRows(memoryTable, report.lines());
        Tables.fitRows(memoryTable, report.lines().size());
        memoryTotal.setText("Total: " + MemoryEstimator.format(report.totalBytes()) + " (" + report.totalBytes() + " bytes)");
        memoryNote.setText(report.note());
    }

    @FXML
    private void runBenchmark() {
        runBenchmark.setDisable(true);
        benchmarkStatus.setText("Measuring... (about 3 seconds)");
        Async.run(runBenchmark, ComplexityBenchmark::run, results -> {
            Tables.setRows(benchmarkTable, results);
            benchmarkStatus.setText("Done. Times are averages per operation.");
        }, () -> runBenchmark.setDisable(false));
    }

    private static String formatNanos(long nanos) {
        if (nanos < 1_000) {
            return nanos + " ns";
        }
        if (nanos < 1_000_000) {
            return String.format("%.1f µs", nanos / 1_000.0);
        }
        return String.format("%.2f ms", nanos / 1_000_000.0);
    }
}
