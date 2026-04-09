package org.openjfx.app;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.movable.Rabbit;
import org.openjfx.app.entities.movable.Wolf;
import org.openjfx.app.entities.staticobjs.Bush;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainApp extends Application {
    private WorldMap worldMap;
    private final double WIDTH = 1024;
    private final double HEIGHT = 576;
    private static final String FIXED_MAP_RESOURCE_PATH = "/org/openjfx/app/map-final.png";

    @Override
    public void start(Stage stage) {
        //Khởi tạo WorldMap (Sau điều chỉnh thì sửa ở trên)
        worldMap = new WorldMap(WIDTH, HEIGHT);
        worldMap.setFixedBackgroundImageFromResource(FIXED_MAP_RESOURCE_PATH);

        // --- BƯỚC THÊM VOI ---
        // Khởi tạo con voi tại tọa độ (400, 300) - chính giữa màn hình
        // Kích thước size = 80.0, máu = 100, đói = 5.0, khát = 6.0
        Rabbit rabbit1 = new Rabbit(
            new Vector2D(400, 300)
        );
        worldMap.addEntity(rabbit1);

        Wolf wolf1 = new Wolf(
            new Vector2D(500, 300)
        );
        worldMap.addEntity(wolf1);

        Bush bush1 = new Bush(
            new Vector2D(600, 300)
        );
        worldMap.addEntity(bush1);


        

        //vẽ
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        // 3. Vòng lặp mô phỏng (Chạy 60 khung hình/giây)
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Xóa màn hình, cập nhật vị trí và vẽ lại
                gc.clearRect(0, 0, WIDTH, HEIGHT);
                worldMap.update(0.016); // Giả sử dt = 1/60s
                worldMap.render(gc);
            }
        };
        timer.start();
        //Thiết lập giao diện
        StackPane root = new StackPane(canvas);
        stage.setScene(new Scene(root));
        stage.setTitle("Project");
        stage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}