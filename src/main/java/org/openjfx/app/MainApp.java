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
import javafx.scene.layout.HBox; // Dùng HBox để xếp hàng ngang
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

        // --- PHẦN THÊM MỚI: Thiết lập ListView cho Terminal bên phải ---
        // Tạo danh sách động để chứa các dòng log
        ObservableList<String> logData = FXCollections.observableArrayList();
        
        // Tạo ListView để hiển thị logData lên giao diện
        ListView<String> listView = new ListView<>(logData);
        listView.setPrefWidth(400); // Độ rộng của bảng Terminal bên phải
        listView.setFocusTraversable(false); // Không cho phép focus vào list để tránh lag
        
        // Thêm CSS để ListView trông giống Terminal (Nền tối, chữ xanh/trắng)
        listView.setStyle("-fx-control-inner-background: #1e1e1e; " +
                         "-fx-font-family: 'Consolas', 'Monospaced'; " +
                         "-fx-font-size: 13px;");

        // Đăng ký TerminalLogger với danh sách logData vừa tạo
        // (Lúc này TerminalLogger sẽ dùng constructor có tham số logData)
        worldMap.addObserver(new TerminalLogger(logData));
        // -------------------------------------------------------------

        Rabbit rabbit1 = new Rabbit(new Vector2D(400, 300));
        worldMap.addEntity(rabbit1);

        Wolf wolf1 = new Wolf(
            new Vector2D(400, 300)
        );
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

        // --- THAY ĐỔI: Sử dụng HBox để chia đôi màn hình ---
        // HBox sẽ xếp Canvas bên trái và ListView bên phải
        HBox root = new HBox(canvas, listView); 
        
        // Cập nhật Scene với root mới (HBox)
        // Chiều rộng tổng cộng = WIDTH bản đồ + 300px Terminal
        stage.setScene(new Scene(root, WIDTH + 400, HEIGHT)); 
        stage.setTitle("Project - Ecology Simulation (Map & Terminal)");
        stage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}