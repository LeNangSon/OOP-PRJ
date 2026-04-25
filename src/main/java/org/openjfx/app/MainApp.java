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
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
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
        listView.setPrefWidth(400);
        listView.setFocusTraversable(false);
        listView.setStyle("-fx-control-inner-background: #1e1e1e; -fx-font-family: 'Consolas';");

        worldMap.addObserver(new TerminalLogger(logData));
        worldMap.addEntity(new Rabbit(new Vector2D(410, 350)));
        worldMap.addEntity(new Rabbit(new Vector2D(420, 350)));
        worldMap.addEntity(new Rabbit(new Vector2D(400, 350)));
        worldMap.addEntity(new Rabbit(new Vector2D(430, 350)));
        worldMap.addEntity(new Wolf(new Vector2D(330, 350)));
        worldMap.addEntity(new Wolf(new Vector2D(550, 200)));

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        canvas.setFocusTraversable(true);

        // --- FIX LỖI LIA CHUỘT (PANNING) ---
        final double[] mouseAnchor = new double[2];
        final double[] lastOffset = new double[2];

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

        canvas.setOnMouseClicked(e -> {
            double oldScale = worldMap.getScale();
            double newScale = oldScale;
            if (e.getButton() == MouseButton.PRIMARY) newScale = Math.min(3.0, oldScale + 0.2);
            else if (e.getButton() == MouseButton.SECONDARY) newScale = Math.max(1.0, oldScale - 0.2);
            
            if (newScale != oldScale) {
                zoomAtPoint(newScale, oldScale, e.getX(), e.getY());
            }
        });

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                gc.clearRect(0, 0, WIDTH, HEIGHT);
                worldMap.update(0.016);
                worldMap.render(gc);
            }
        };
        timer.start();

        // --- GIAO DIỆN SIÊU NHỎ GỌN ---
        Button btnSearch = new Button("🔍");
        btnSearch.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-font-size: 12px; " +
                          "-fx-background-radius: 50; -fx-min-width: 30; -fx-min-height: 30; -fx-cursor: hand;");

        Button btnPlus = new Button("+");
        Button btnMinus = new Button("-");
        Button btnReset = new Button("Đặt lại");
        
        String styleBtn = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 0 4;";
        btnPlus.setStyle(styleBtn);
        btnMinus.setStyle(styleBtn);
        btnReset.setStyle("-fx-background-color: #444444; -fx-text-fill: #ffaaaa; -fx-background-radius: 4; -fx-padding: 1 5; -fx-cursor: hand; -fx-font-size: 10px;");

        menuBox = new HBox(6, btnPlus, btnMinus, btnReset);
        menuBox.setAlignment(Pos.CENTER);
        // Ép khung đen nhỏ xíu
        menuBox.setStyle("-fx-background-color: rgba(20, 20, 20, 0.9); -fx-background-radius: 8; -fx-padding: 2 6;");
        menuBox.setVisible(false);
        menuBox.setManaged(false);
        menuBox.setMaxHeight(26);

        btnSearch.setOnAction(e -> {
            boolean isVisible = menuBox.isVisible();
            menuBox.setVisible(!isVisible);
            menuBox.setManaged(!isVisible);
        });

        btnPlus.setOnAction(e -> zoomAtPoint(Math.min(3.0, worldMap.getScale() + 0.1), worldMap.getScale(), WIDTH/2, HEIGHT/2));
        btnMinus.setOnAction(e -> zoomAtPoint(Math.max(1.0, worldMap.getScale() - 0.1), worldMap.getScale(), WIDTH/2, HEIGHT/2));
        btnReset.setOnAction(e -> { worldMap.setScale(1.0); worldMap.setOffset(0, 0); });

        // --- QUAN TRỌNG: SỬA LỖI CHẶN CHUỘT ---
        HBox zoomContainer = new HBox(8, btnSearch, menuBox);
        zoomContainer.setAlignment(Pos.TOP_CENTER);
        zoomContainer.setPadding(new Insets(10, 0, 0, 0));
        // Chỉ nhận sự kiện lên chính các nút, không chặn vùng trống xung quanh
        zoomContainer.setPickOnBounds(false); 
        zoomContainer.setMaxHeight(40); // Khống chế chiều cao container

        StackPane canvasPane = new StackPane(canvas, zoomContainer);
        canvasPane.setAlignment(Pos.TOP_CENTER);

        HBox root = new HBox(canvasPane, listView);
        stage.setScene(new Scene(root, WIDTH + 400, HEIGHT));
        stage.show();
    }

    private void zoomAtPoint(double newScale, double oldScale, double cx, double cy) {
        if (oldScale == newScale) return;
        double ox = cx - (cx - worldMap.getOffsetX()) * (newScale / oldScale);
        double oy = cy - (cy - worldMap.getOffsetY()) * (newScale / oldScale);
        worldMap.setScale(newScale);
        worldMap.setOffset(ox, oy);
    }

    public static void main(String[] args) { launch(args); }
}