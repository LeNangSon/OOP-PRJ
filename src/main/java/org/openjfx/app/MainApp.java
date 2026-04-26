package org.openjfx.app;

import org.openjfx.app.core.TerminalLogger;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.movable.Rabbit;
import org.openjfx.app.entities.movable.Wolf;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {
    private WorldMap worldMap;
    private final double WIDTH = 1032;
    private final double HEIGHT = 576;
    private static final String FIXED_MAP_RESOURCE_PATH = "/org/openjfx/app/map-final.png";
    private static final String TERRAIN_CSV_RESOURCE_PATH = "/org/openjfx/app/terrain.csv";
    private static final int TERRAIN_TILE_SIZE = 24;

    private HBox menuBox;

    @Override
    public void start(Stage stage) {
        worldMap = new WorldMap(WIDTH, HEIGHT);
        worldMap.setFixedBackgroundImageFromResource(FIXED_MAP_RESOURCE_PATH);
        worldMap.setTerrainGridFromCsvResource(TERRAIN_CSV_RESOURCE_PATH, TERRAIN_TILE_SIZE);

        ObservableList<String> logData = FXCollections.observableArrayList();
        ListView<String> listView = new ListView<>(logData);
        EntityStatusPanel entityStatusPanel = new EntityStatusPanel(worldMap);
        entityStatusPanel.setVisible(false);
        entityStatusPanel.setManaged(false);
        listView.setPrefWidth(400);
        listView.setFocusTraversable(false);

        listView.setStyle("-fx-control-inner-background: #1e1e1e; " +
                "-fx-font-family: 'Consolas', 'Monospaced'; " +
                "-fx-font-size: 13px;");

        worldMap.addObserver(new TerminalLogger(logData));
        worldMap.addEntity(new Rabbit(new Vector2D(410, 350)));
        worldMap.addEntity(new Rabbit(new Vector2D(450, 350)));
        worldMap.addEntity(new Rabbit(new Vector2D(400, 350)));
        worldMap.addEntity(new Rabbit(new Vector2D(430, 350)));
        worldMap.addEntity(new Wolf(new Vector2D(500, 300)));

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        canvas.setFocusTraversable(true);

        final double[] mouseAnchor = new double[2];
        final double[] lastOffset = new double[2];

        // ===== SPAWN DATA =====
        final double[] spawnPos = new double[2];

        ContextMenu spawnMenu = new ContextMenu();
        MenuItem addRabbit = new MenuItem("Spawn Rabbit");
        MenuItem addWolf = new MenuItem("Spawn Wolf");

        spawnMenu.getItems().addAll(addRabbit, addWolf);

        addRabbit.setOnAction(e -> {
            worldMap.addEntity(new Rabbit(new Vector2D(spawnPos[0], spawnPos[1])));
        });

        addWolf.setOnAction(e -> {
            worldMap.addEntity(new Wolf(new Vector2D(spawnPos[0], spawnPos[1])));
        });

        canvas.setOnMousePressed(e -> {
            if (menuBox.isVisible()) {
                menuBox.setVisible(false);
                menuBox.setManaged(false);
            }
            mouseAnchor[0] = e.getX();
            mouseAnchor[1] = e.getY();
            lastOffset[0] = worldMap.getOffsetX();
            lastOffset[1] = worldMap.getOffsetY();
            canvas.requestFocus();
        });

        canvas.setOnMouseDragged(e -> {
            if (worldMap.getScale() > 1.0) {
                double dx = e.getX() - mouseAnchor[0];
                double dy = e.getY() - mouseAnchor[1];
                worldMap.setOffset(lastOffset[0] + dx, lastOffset[1] + dy);
            }
        });

        // ===== FIX CHUỘT =====
        canvas.setOnMouseClicked(e -> {
            double oldScale = worldMap.getScale();
            double newScale = oldScale;

            if (e.getButton() == MouseButton.PRIMARY)
                newScale = Math.min(3.0, oldScale + 0.2);
            else if (e.getButton() == MouseButton.MIDDLE)
                newScale = Math.max(1.0, oldScale - 0.2);

            if (newScale != oldScale) {
                zoomAtPoint(newScale, oldScale, e.getX(), e.getY());
            }
        });

        // ===== RIGHT CLICK MENU =====
        canvas.setOnContextMenuRequested(e -> {
            double worldX = (e.getX() - worldMap.getOffsetX()) / worldMap.getScale();
            double worldY = (e.getY() - worldMap.getOffsetY()) / worldMap.getScale();

            spawnPos[0] = worldX;
            spawnPos[1] = worldY;

            spawnMenu.show(canvas, e.getScreenX(), e.getScreenY());
        });

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                gc.clearRect(0, 0, WIDTH, HEIGHT);
                worldMap.update(0.016);
                worldMap.render(gc);
                if (entityStatusPanel.isVisible()) {
                    entityStatusPanel.refreshData();
                }
            }
        };
        timer.start();

        // ===== MENU BAR =====
        MenuBar menuBar = new MenuBar();
        Menu menuFile = new Menu("File");
        MenuItem resetItem = new MenuItem("Reset Map");
        MenuItem exitItem = new MenuItem("Exit");

        resetItem.setOnAction(e -> {
            worldMap.setScale(1.0);
            worldMap.setOffset(0, 0);
        });

        exitItem.setOnAction(e -> stage.close());
        menuFile.getItems().addAll(resetItem, exitItem);

        menuBar.getMenus().add(menuFile);

        // ===== UI ZOOM =====
        Button btnSearch = new Button("🔍");
        Button btnPlus = new Button("+");
        Button btnMinus = new Button("-");
        Button btnReset = new Button("Đặt lại");

        btnPlus.setOnAction(e -> zoomAtPoint(Math.min(3.0, worldMap.getScale() + 0.1), worldMap.getScale(), WIDTH/2, HEIGHT/2));
        btnMinus.setOnAction(e -> zoomAtPoint(Math.max(1.0, worldMap.getScale() - 0.1), worldMap.getScale(), WIDTH/2, HEIGHT/2));
        btnReset.setOnAction(e -> { worldMap.setScale(1.0); worldMap.setOffset(0, 0); });

        menuBox = new HBox(6, btnPlus, btnMinus, btnReset);
        menuBox.setVisible(false);

        btnSearch.setOnAction(e -> {
            boolean isVisible = menuBox.isVisible();
            menuBox.setVisible(!isVisible);
            menuBox.setManaged(!isVisible);
        });

        HBox zoomContainer = new HBox(8, btnSearch, menuBox);
        zoomContainer.setAlignment(Pos.TOP_CENTER);
        zoomContainer.setPadding(new Insets(10, 0, 0, 0));
        zoomContainer.setPickOnBounds(false);

        StackPane canvasPane = new StackPane(canvas, zoomContainer);
        canvasPane.setAlignment(Pos.TOP_CENTER);

        HBox root = new HBox(canvasPane, listView, entityStatusPanel);
        VBox layout = new VBox(menuBar, root);

        Scene scene = new Scene(layout, WIDTH + 400, HEIGHT + 25);

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.TAB) {
                boolean showStats = !entityStatusPanel.isVisible();
                entityStatusPanel.setVisible(showStats);
                entityStatusPanel.setManaged(showStats);
                listView.setVisible(!showStats);
                listView.setManaged(!showStats);

                if (showStats) entityStatusPanel.refreshData();
                event.consume();
            }
        });

        stage.setScene(scene);
        stage.setTitle("Ecology Simulation");
        stage.show();
    }

    private void zoomAtPoint(double newScale, double oldScale, double cx, double cy) {
        if (oldScale == newScale) return;
        double ox = cx - (cx - worldMap.getOffsetX()) * (newScale / oldScale);
        double oy = cy - (cy - worldMap.getOffsetY()) * (newScale / oldScale);
        worldMap.setScale(newScale);
        worldMap.setOffset(ox, oy);
    }

    public static void main(String[] args) {
        launch(args);
    }
}