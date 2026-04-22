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
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class MainApp extends Application {
    private WorldMap worldMap;
    private final double WIDTH = 1032;
    private final double HEIGHT = 576;
    private static final String FIXED_MAP_RESOURCE_PATH = "/org/openjfx/app/map-final.png";
    private static final String TERRAIN_CSV_RESOURCE_PATH = "/org/openjfx/app/terrain.csv";
    private static final int TERRAIN_TILE_SIZE = 24;

    @Override
    public void start(Stage stage) {
        // 1. Khởi tạo WorldMap
        worldMap = new WorldMap(WIDTH, HEIGHT);
        worldMap.setFixedBackgroundImageFromResource(FIXED_MAP_RESOURCE_PATH);
        worldMap.setTerrainGridFromCsvResource(TERRAIN_CSV_RESOURCE_PATH, TERRAIN_TILE_SIZE);

        // --- THIẾT LẬP TERMINAL LOG ---
        ObservableList<String> logEntries = FXCollections.observableArrayList();
        
        // THÊM 2 DÒNG LOG KIỂM TRA TẠI ĐÂY
        logEntries.add(">> Hệ thống mô phỏng sinh học đã khởi động.");
        logEntries.add(">> Đang theo dõi thực thể trên bản đồ...");

        ListView<String> logView = new ListView<>(logEntries);
        logView.setPrefWidth(300);
        logView.setFocusTraversable(false);
        
        logView.setStyle("-fx-font-family: 'Consolas'; " +
                         "-fx-font-size: 12; " +
                         "-fx-control-inner-background: #1e1e1e; " +
                         "-fx-text-fill: #00FF00;"); // Chuyển sang màu xanh lá cho giống terminal

        TerminalLogger terminalLogger = new TerminalLogger(logEntries);
        worldMap.addObserver(terminalLogger);

        // --- THÊM CÁC THỰC THỂ ---
        Rabbit rabbit1 = new Rabbit(new Vector2D(400, 300));
        worldMap.addEntity(rabbit1);

        Wolf wolf1 = new Wolf(new Vector2D(450, 350)); 
        worldMap.addEntity(wolf1);

        // Vẽ
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // 3. Vòng lặp mô phỏng
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                gc.clearRect(0, 0, WIDTH, HEIGHT);
                worldMap.update(0.016); 
                worldMap.render(gc);
            }
        };
        timer.start();

        // 4. Thiết lập giao diện
        HBox root = new HBox(); 
        root.getChildren().addAll(canvas, logView);

        stage.setScene(new Scene(root));
        stage.setTitle("HUST Bio-Simulation Project - Terminal Active");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}