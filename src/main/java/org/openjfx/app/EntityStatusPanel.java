package org.openjfx.app;

import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class EntityStatusPanel extends TableView<EntityStatusPanel.EntityStatusRow> {

    private final WorldMap worldMap;
    private final ObservableList<EntityStatusRow> rows = FXCollections.observableArrayList();

    public EntityStatusPanel(WorldMap worldMap) {
        this.worldMap = worldMap;

        setItems(rows);
        setPrefWidth(400);
        setFocusTraversable(false);

        TableColumn<EntityStatusRow, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().id));

        TableColumn<EntityStatusRow, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().type));

        TableColumn<EntityStatusRow, String> healthCol = new TableColumn<>("Health");
        healthCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().health));

        TableColumn<EntityStatusRow, String> hungerCol = new TableColumn<>("Hunger");
        hungerCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().hunger));

        TableColumn<EntityStatusRow, String> thirstCol = new TableColumn<>("Thirst");
        thirstCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().thirst));

        getColumns().add(idCol);
        getColumns().add(typeCol);
        getColumns().add(healthCol);
        getColumns().add(hungerCol);
        getColumns().add(thirstCol);

        setStyle(
            "-fx-control-inner-background: #1e1e1e;" +
            "-fx-font-family: 'Consolas', 'Monospaced';" +
            "-fx-font-size: 13px;"
        );
    }

    public void refreshData() {
        rows.clear();

        for (Entity entity : worldMap.getEntities()) {
            if (entity instanceof LivingEntity living) {
                rows.add(new EntityStatusRow(
                    String.valueOf(living.getId()),
                    living.getType().toString(),
                    format(living.getHealth()),
                    format(living.getHunger()),
                    format(living.getThirst())
                ));
            }
        }
    }

    private String format(double value) {
        return String.format("%.1f", value);
    }

    public static class EntityStatusRow {
        public final String id;
        public final String type;
        public final String health;
        public final String hunger;
        public final String thirst;

        public EntityStatusRow(String id, String type, String health, String hunger, String thirst) {
            this.id = id;
            this.type = type;
            this.health = health;
            this.hunger = hunger;
            this.thirst = thirst;
        }
    }
}