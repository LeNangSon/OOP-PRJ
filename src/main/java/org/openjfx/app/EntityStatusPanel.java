package org.openjfx.app;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openjfx.app.core.DeathCause;
import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class EntityStatusPanel extends VBox {

    private static final String DARK_STYLE =
            "-fx-control-inner-background: #1e1e1e;"
            + "-fx-font-family: 'Consolas', 'Monospaced';"
            + "-fx-font-size: 13px;";

    private final WorldMap worldMap;

    private final TableView<SummaryRow> summaryTable = new TableView<>();
    private final ObservableList<SummaryRow> summaryRows = FXCollections.observableArrayList();

    private final TableView<DetailRow> detailTable = new TableView<>();
    private final ObservableList<DetailRow> detailRows = FXCollections.observableArrayList();

    private final Label headerLabel = new Label("Tổng quan các loài");
    private final Button backButton = new Button("← Quay lại");

    private EntityType selectedType;

    public EntityStatusPanel(WorldMap worldMap) {
        this.worldMap = worldMap;
        setPrefWidth(400);
        setFillWidth(true);
        setStyle("-fx-background-color: #1e1e1e;");

        buildHeader();
        buildSummaryTable();
        buildDetailTable();

        getChildren().addAll(buildHeaderBox(), summaryTable, detailTable);
        VBox.setVgrow(summaryTable, Priority.ALWAYS);
        VBox.setVgrow(detailTable, Priority.ALWAYS);

        showSummary();
    }

    private HBox buildHeaderBox() {
        backButton.setStyle(
                "-fx-background-color: #3d3d3d; -fx-text-fill: white; -fx-font-size: 11px;"
                + " -fx-cursor: hand; -fx-padding: 4 8;");
        backButton.setOnAction(e -> showSummary());

        headerLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 14px; -fx-font-weight: bold;");

        HBox box = new HBox(8, backButton, headerLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(8, 10, 8, 10));
        box.setStyle("-fx-background-color: #2a2a2a;");
        return box;
    }

    private void buildHeader() {}

    private void buildSummaryTable() {
        summaryTable.setItems(summaryRows);
        summaryTable.setFocusTraversable(false);
        summaryTable.setStyle(DARK_STYLE);
        summaryTable.setPlaceholder(new Label("Chưa có thực thể nào"));

        TableColumn<SummaryRow, String> typeCol = new TableColumn<>("Loài");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().type.toString()));
        typeCol.setPrefWidth(95);

        TableColumn<SummaryRow, String> aliveCol = new TableColumn<>("Sống");
        aliveCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().alive)));
        aliveCol.setPrefWidth(55);

        TableColumn<SummaryRow, String> hungerDeathCol = new TableColumn<>("Đói");
        hungerDeathCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().diedHunger)));
        hungerDeathCol.setPrefWidth(55);

        TableColumn<SummaryRow, String> thirstDeathCol = new TableColumn<>("Khát");
        thirstDeathCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().diedThirst)));
        thirstDeathCol.setPrefWidth(55);

        TableColumn<SummaryRow, String> predationDeathCol = new TableColumn<>("Bị săn");
        predationDeathCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().diedPredation)));
        predationDeathCol.setPrefWidth(65);

        summaryTable.getColumns().add(typeCol);
        summaryTable.getColumns().add(aliveCol);
        summaryTable.getColumns().add(hungerDeathCol);
        summaryTable.getColumns().add(thirstDeathCol);
        summaryTable.getColumns().add(predationDeathCol);

        summaryTable.setRowFactory(tv -> {
            javafx.scene.control.TableRow<SummaryRow> row = new javafx.scene.control.TableRow<>();
            row.setCursor(javafx.scene.Cursor.HAND);
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() >= 1) {
                    showDetail(row.getItem().type);
                }
            });
            return row;
        });
    }

    private void buildDetailTable() {
        detailTable.setItems(detailRows);
        detailTable.setFocusTraversable(false);
        detailTable.setStyle(DARK_STYLE);
        detailTable.setPlaceholder(new Label("Không có cá thể nào"));

        TableColumn<DetailRow, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().id));
        idCol.setPrefWidth(60);

        TableColumn<DetailRow, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().type));
        typeCol.setPrefWidth(80);

        TableColumn<DetailRow, String> healthCol = new TableColumn<>("Health");
        healthCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().health));

        TableColumn<DetailRow, String> hungerCol = new TableColumn<>("Hunger");
        hungerCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().hunger));

        TableColumn<DetailRow, String> thirstCol = new TableColumn<>("Thirst");
        thirstCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().thirst));

        detailTable.getColumns().add(idCol);
        detailTable.getColumns().add(typeCol);
        detailTable.getColumns().add(healthCol);
        detailTable.getColumns().add(hungerCol);
        detailTable.getColumns().add(thirstCol);
    }

    public void refreshData() {
        if (selectedType == null) {
            refreshSummary();
        } else {
            refreshDetail();
        }
    }

    private void refreshSummary() {
        Map<EntityType, Integer> aliveCounts = new EnumMap<>(EntityType.class);
        for (Entity entity : worldMap.getEntities()) {
            EntityType type = entity.getType();
            if (type == null) {
                continue;
            }
            aliveCounts.merge(type, 1, Integer::sum);
        }

        // Hiển thị mọi loài có ít nhất 1 con đang sống HOẶC từng có cá thể chết.
        Set<EntityType> allTypes = new HashSet<>(aliveCounts.keySet());
        allTypes.addAll(worldMap.getDeathCountsByType().keySet());

        summaryRows.clear();
        for (EntityType type : allTypes) {
            int alive = aliveCounts.getOrDefault(type, 0);
            int diedHunger = worldMap.getDeathCount(type, DeathCause.HUNGER);
            int diedThirst = worldMap.getDeathCount(type, DeathCause.THIRST);
            int diedPredation = worldMap.getDeathCount(type, DeathCause.PREDATION);
            summaryRows.add(new SummaryRow(type, alive, diedHunger, diedThirst, diedPredation));
        }
        summaryRows.sort((a, b) -> {
            int byAlive = Integer.compare(b.alive, a.alive);
            if (byAlive != 0) return byAlive;
            return a.type.name().compareTo(b.type.name());
        });
    }

    private void refreshDetail() {
        detailRows.clear();
        if (selectedType == null) {
            return;
        }
        for (Entity entity : worldMap.getEntities()) {
            if (entity.getType() != selectedType) {
                continue;
            }
            if (entity instanceof LivingEntity living) {
                detailRows.add(new DetailRow(
                        String.valueOf(living.getId()),
                        living.getType().toString(),
                        format(living.getHealth()),
                        format(living.getHunger()),
                        format(living.getThirst())));
            } else {
                detailRows.add(new DetailRow(
                        String.valueOf(entity.getId()),
                        String.valueOf(entity.getType()),
                        "-", "-", "-"));
            }
        }
        detailRows.sort((a, b) -> {
            try {
                return Integer.compare(Integer.parseInt(a.id), Integer.parseInt(b.id));
            } catch (NumberFormatException ex) {
                return a.id.compareTo(b.id);
            }
        });
    }

    private void showSummary() {
        selectedType = null;
        headerLabel.setText("Tổng quan các loài");
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
        headerLabel.setText("Chi tiết: " + type);
        backButton.setVisible(true);
        backButton.setManaged(true);
        summaryTable.setVisible(false);
        summaryTable.setManaged(false);
        detailTable.setVisible(true);
        detailTable.setManaged(true);
        refreshDetail();
    }

    private String format(double value) {
        return String.format("%.1f", value);
    }

    public static class SummaryRow {
        public final EntityType type;
        public final int alive;
        public final int diedHunger;
        public final int diedThirst;
        public final int diedPredation;

        public SummaryRow(EntityType type, int alive, int diedHunger, int diedThirst, int diedPredation) {
            this.type = type;
            this.alive = alive;
            this.diedHunger = diedHunger;
            this.diedThirst = diedThirst;
            this.diedPredation = diedPredation;
        }
    }

    public static class DetailRow {
        public final String id;
        public final String type;
        public final String health;
        public final String hunger;
        public final String thirst;

        public DetailRow(String id, String type, String health, String hunger, String thirst) {
            this.id = id;
            this.type = type;
            this.health = health;
            this.hunger = hunger;
            this.thirst = thirst;
        }
    }
}
