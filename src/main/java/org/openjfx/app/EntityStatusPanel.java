package org.openjfx.app;

import java.util.EnumMap;
import java.util.Map;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class EntityStatusPanel extends VBox {

    // ── Colour palette ────────────────────────────────────────────────────────
    private static final String BG_DARK    = "#1a1a2e";
    private static final String BG_HEADER  = "#0f3460";
    private static final String BG_ROW_ODD = "#1e1e38";
    private static final String TEXT_MAIN  = "#eaeaea";
    private static final String TEXT_MUTED = "#8892a4";

    private static final Map<EntityType, String> SPECIES_COLOR = new EnumMap<>(EntityType.class);
    private static final Map<EntityType, String> SPECIES_ICON  = new EnumMap<>(EntityType.class);

    static {
        SPECIES_COLOR.put(EntityType.RABBIT,   "#a8e6cf");
        SPECIES_COLOR.put(EntityType.WOLF,     "#ff6b6b");
        SPECIES_COLOR.put(EntityType.BEAR,     "#ffb347");
        SPECIES_COLOR.put(EntityType.ELEPHANT, "#87ceeb");
        SPECIES_COLOR.put(EntityType.FISH,     "#6495ed");
        SPECIES_COLOR.put(EntityType.GRASS,    "#90ee90");
        SPECIES_COLOR.put(EntityType.ALGAE,    "#40e0d0");
        SPECIES_COLOR.put(EntityType.ROCK,     "#a0a0a0");
        SPECIES_COLOR.put(EntityType.FRUIT,    "#ffa500");
        SPECIES_COLOR.put(EntityType.WATER,    "#4fc3f7");

        SPECIES_ICON.put(EntityType.RABBIT,   "🐰");
        SPECIES_ICON.put(EntityType.WOLF,     "🐺");
        SPECIES_ICON.put(EntityType.BEAR,     "🐻");
        SPECIES_ICON.put(EntityType.ELEPHANT, "🐘");
        SPECIES_ICON.put(EntityType.FISH,     "🐟");
        SPECIES_ICON.put(EntityType.GRASS,    "🌾");
        SPECIES_ICON.put(EntityType.ALGAE,    "🦠");
        SPECIES_ICON.put(EntityType.ROCK,     "🪨");
        SPECIES_ICON.put(EntityType.FRUIT,    "🍎");
        SPECIES_ICON.put(EntityType.WATER,    "💧");
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private final WorldMap worldMap;
    private EntityType selectedType;

    private final TableView<SummaryRow>  summaryTable = new TableView<>();
    private final ObservableList<SummaryRow>  summaryRows  = FXCollections.observableArrayList();
    private final TableView<DetailRow>   detailTable  = new TableView<>();
    private final ObservableList<DetailRow>   detailRows   = FXCollections.observableArrayList();

    private final Label  headerLabel = new Label();
    private final Button backButton  = new Button("← Quay lại");

    // ── Constructor ───────────────────────────────────────────────────────────
    public EntityStatusPanel(WorldMap worldMap) {
        this.worldMap = worldMap;
        setPrefWidth(400);
        setFillWidth(true);
        setStyle("-fx-background-color: " + BG_DARK + ";");

        buildSummaryTable();
        buildDetailTable();

        getChildren().addAll(buildHeaderBox(), summaryTable, detailTable);
        VBox.setVgrow(summaryTable, Priority.ALWAYS);
        VBox.setVgrow(detailTable, Priority.ALWAYS);

        showSummary();
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private HBox buildHeaderBox() {
        backButton.setStyle(
            "-fx-background-color: #2d2d4e; -fx-text-fill: " + TEXT_MAIN + "; "
            + "-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 10; "
            + "-fx-background-radius: 4;");
        backButton.setOnAction(e -> showSummary());

        headerLabel.setStyle(
            "-fx-text-fill: " + TEXT_MAIN + "; -fx-font-size: 13px; -fx-font-weight: bold;");

        HBox box = new HBox(10, backButton, headerLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10, 12, 10, 12));
        box.setStyle("-fx-background-color: " + BG_HEADER + ";");
        return box;
    }

    // ── Summary table ─────────────────────────────────────────────────────────
    private void buildSummaryTable() {
        summaryTable.setItems(summaryRows);
        summaryTable.setFocusTraversable(false);
        summaryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        summaryTable.setStyle(tableBaseStyle());
        summaryTable.setPlaceholder(styledPlaceholder("Chưa có thực thể nào"));
        summaryTable.setFixedCellSize(38);

        // Species column: icon + colored name
        TableColumn<SummaryRow, SummaryRow> speciesCol = new TableColumn<>("Loài");
        speciesCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        speciesCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(SummaryRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) { setGraphic(null); return; }

                String color = SPECIES_COLOR.getOrDefault(row.type, TEXT_MUTED);
                String icon  = SPECIES_ICON.getOrDefault(row.type, "•");

                Region dot = new Region();
                dot.setMinSize(8, 8);
                dot.setMaxSize(8, 8);
                dot.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4;");

                Label nameLbl = new Label(icon + "  " + friendlyName(row.type));
                nameLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");

                HBox cell = new HBox(8, dot, nameLbl);
                cell.setAlignment(Pos.CENTER_LEFT);
                setGraphic(cell);
                setText(null);
                setStyle("-fx-background-color: transparent;");
            }
        });

        // Count column: badge
        TableColumn<SummaryRow, SummaryRow> countCol = new TableColumn<>("Số lượng");
        countCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        countCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(SummaryRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) { setGraphic(null); return; }

                String color = SPECIES_COLOR.getOrDefault(row.type, TEXT_MUTED);
                Label badge = new Label(String.valueOf(row.count));
                badge.setStyle(
                    "-fx-background-color: " + color + "33; "  // 20% opacity
                    + "-fx-text-fill: " + color + "; "
                    + "-fx-font-weight: bold; -fx-font-size: 12px; "
                    + "-fx-padding: 2 10; -fx-background-radius: 10;");

                HBox cell = new HBox(badge);
                cell.setAlignment(Pos.CENTER_LEFT);
                setGraphic(cell);
                setText(null);
                setStyle("-fx-background-color: transparent;");
            }
        });

        summaryTable.getColumns().add(speciesCol);
        summaryTable.getColumns().add(countCol);

        // Click row → open detail
        summaryTable.setRowFactory(tv -> {
            javafx.scene.control.TableRow<SummaryRow> row = new javafx.scene.control.TableRow<>() {
                @Override
                protected void updateItem(SummaryRow item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("-fx-background-color: transparent;");
                    } else {
                        int idx = getIndex();
                        setStyle("-fx-background-color: " + (idx % 2 == 0 ? BG_DARK : BG_ROW_ODD) + ";");
                    }
                }
            };
            row.setCursor(javafx.scene.Cursor.HAND);
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty()) showDetail(row.getItem().type);
            });
            return row;
        });
    }

    // ── Detail table ──────────────────────────────────────────────────────────
    private void buildDetailTable() {
        detailTable.setItems(detailRows);
        detailTable.setFocusTraversable(false);
        detailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        detailTable.setStyle(tableBaseStyle());
        detailTable.setPlaceholder(styledPlaceholder("Không có cá thể nào"));
        detailTable.setFixedCellSize(36);

        TableColumn<DetailRow, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().id));
        idCol.setCellFactory(col -> styledTextCell(TEXT_MUTED, 10));
        idCol.setMaxWidth(52);
        idCol.setMinWidth(52);

        TableColumn<DetailRow, Double> healthCol = makeBarColumn(
            "❤ Máu", 120, row -> row.healthFrac, "#48bb78", "#ecc94b", "#fc8181");
        TableColumn<DetailRow, Double> hungerCol = makeBarColumn(
            "🍖 Đói", 100, row -> row.hungerFrac, "#48bb78", "#ecc94b", "#fc8181");
        TableColumn<DetailRow, Double> thirstCol = makeBarColumn(
            "💧 Khát", 100, row -> row.thirstFrac, "#4fc3f7", "#ecc94b", "#fc8181");

        detailTable.getColumns().add(idCol);
        detailTable.getColumns().add(healthCol);
        detailTable.getColumns().add(hungerCol);
        detailTable.getColumns().add(thirstCol);

        detailTable.setRowFactory(tv -> {
            javafx.scene.control.TableRow<DetailRow> row = new javafx.scene.control.TableRow<>() {
                @Override
                protected void updateItem(DetailRow item, boolean empty) {
                    super.updateItem(item, empty);
                    int idx = getIndex();
                    setStyle("-fx-background-color: " + (empty ? "transparent" :
                        (idx % 2 == 0 ? BG_DARK : BG_ROW_ODD)) + ";");
                }
            };
            return row;
        });
    }

    // Makes a bar-chart column where 0.0=empty, 1.0=full.
    // Color transitions: high → midColor at 0.5, midColor → lowColor at 0.25.
    private TableColumn<DetailRow, Double> makeBarColumn(
            String title, double prefWidth,
            java.util.function.Function<DetailRow, Double> getter,
            String highColor, String midColor, String lowColor) {

        TableColumn<DetailRow, Double> col = new TableColumn<>(title);
        col.setPrefWidth(prefWidth);
        col.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(getter.apply(data.getValue())));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setStyle("-fx-background-color: transparent;");
                if (empty || value == null) { setGraphic(null); return; }

                double frac  = Math.max(0, Math.min(1, value));
                String color = frac > 0.5 ? highColor : frac > 0.25 ? midColor : lowColor;

                // Track (background)
                Region track = new Region();
                track.setPrefSize(prefWidth - 24, 10);
                track.setMaxSize(prefWidth - 24, 10);
                track.setStyle("-fx-background-color: #2a2a44; -fx-background-radius: 5;");

                // Fill
                Region fill = new Region();
                fill.setPrefSize((prefWidth - 24) * frac, 10);
                fill.setMaxSize((prefWidth - 24) * frac, 10);
                fill.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 5;");

                StackPane bar = new StackPane(track, fill);
                StackPane.setAlignment(fill, Pos.CENTER_LEFT);
                bar.setMaxWidth(prefWidth - 24);
                bar.setMinWidth(prefWidth - 24);

                Label pct = new Label(String.format("%d%%", Math.round(frac * 100)));
                pct.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 9px; -fx-min-width: 28; -fx-alignment: center-right;");

                HBox cell = new HBox(5, bar, pct);
                cell.setAlignment(Pos.CENTER_LEFT);
                setGraphic(cell);
                setText(null);
            }
        });
        return col;
    }

    private static <T> TableCell<T, String> styledTextCell(String textColor, int fontSize) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: " + fontSize + "px; "
                    + "-fx-background-color: transparent;");
            }
        };
    }

    // ── Data refresh ──────────────────────────────────────────────────────────
    public void refreshData() {
        if (selectedType == null) refreshSummary();
        else refreshDetail();
    }

    private void refreshSummary() {
        Map<EntityType, Integer> counts = new EnumMap<>(EntityType.class);
        for (Entity entity : worldMap.getEntities()) {
            if (entity.getType() != null) counts.merge(entity.getType(), 1, Integer::sum);
        }
        summaryRows.clear();
        for (Map.Entry<EntityType, Integer> e : counts.entrySet())
            summaryRows.add(new SummaryRow(e.getKey(), e.getValue()));
        summaryRows.sort((a, b) -> Integer.compare(b.count, a.count));
    }

    private void refreshDetail() {
        detailRows.clear();
        if (selectedType == null) return;
        for (Entity entity : worldMap.getEntities()) {
            if (entity.getType() != selectedType) continue;
            if (entity instanceof LivingEntity living) {
                double healthFrac = living.getMaxHealth() > 0
                    ? living.getHealth() / living.getMaxHealth() : 0;
                detailRows.add(new DetailRow(
                    String.valueOf(living.getId()),
                    healthFrac,
                    living.getHunger() / 100.0,
                    living.getThirst() / 100.0));
            }
        }
        detailRows.sort((a, b) -> {
            try { return Integer.compare(Integer.parseInt(a.id), Integer.parseInt(b.id)); }
            catch (NumberFormatException ex) { return a.id.compareTo(b.id); }
        });
    }

    // ── View switching ────────────────────────────────────────────────────────
    private void showSummary() {
        selectedType = null;
        headerLabel.setText("📊  Tổng quan các loài");
        backButton.setVisible(false);
        backButton.setManaged(false);
        summaryTable.setVisible(true);
        summaryTable.setManaged(true);
        detailTable.setVisible(false);
        detailTable.setManaged(false);
        refreshSummary();
    }

    private void showDetail(EntityType type) {
        selectedType = type;
        String icon = SPECIES_ICON.getOrDefault(type, "");
        headerLabel.setText(icon + "  Chi tiết: " + friendlyName(type));
        backButton.setVisible(true);
        backButton.setManaged(true);
        summaryTable.setVisible(false);
        summaryTable.setManaged(false);
        detailTable.setVisible(true);
        detailTable.setManaged(true);
        refreshDetail();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static String tableBaseStyle() {
        return "-fx-control-inner-background: " + BG_DARK + "; "
            + "-fx-font-family: 'Segoe UI', 'SF Pro Text', system; "
            + "-fx-font-size: 12px; "
            + "-fx-table-header-border-color: #2d2d4e; "
            + "-fx-faint-focus-color: transparent; "
            + "-fx-focus-color: transparent;";
    }

    private static Label styledPlaceholder(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 12px;");
        return lbl;
    }

    private static String friendlyName(EntityType type) {
        return switch (type) {
            case RABBIT   -> "Thỏ";
            case WOLF     -> "Sói";
            case BEAR     -> "Gấu";
            case ELEPHANT -> "Voi";
            case FISH     -> "Cá";
            case GRASS    -> "Cỏ";
            case ALGAE    -> "Tảo";
            case ROCK     -> "Đá";
            case FRUIT    -> "Quả";
            case WATER    -> "Nước";
            default       -> type.toString();
        };
    }

    // ── Data models ───────────────────────────────────────────────────────────
    public static class SummaryRow {
        public final EntityType type;
        public final int count;
        public SummaryRow(EntityType type, int count) { this.type = type; this.count = count; }
    }

    public static class DetailRow {
        public final String id;
        public final double healthFrac;  // 0..1
        public final double hungerFrac;  // 0..1
        public final double thirstFrac;  // 0..1
        public DetailRow(String id, double healthFrac, double hungerFrac, double thirstFrac) {
            this.id         = id;
            this.healthFrac = healthFrac;
            this.hungerFrac = hungerFrac;
            this.thirstFrac = thirstFrac;
        }
    }
}
