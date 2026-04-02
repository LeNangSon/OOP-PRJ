package org.openjfx.app;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.movable.Elephant;
import org.openjfx.app.entities.movable.Fish;
import org.openjfx.app.entities.movable.Rabbit;
import org.openjfx.app.entities.staticobjs.Rock;
import org.openjfx.app.environment.Lake;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainApp extends Application {
    private WorldMap worldMap;
    private final double WIDTH = 800;
    private final double HEIGHT = 600;

    @Override
    public void start(Stage stage) {
        //Khởi tạo WorldMap (Sau điều chỉnh thì sửa ở trên)
        worldMap = new WorldMap(WIDTH, HEIGHT);

        // --- BƯỚC THÊM VOI ---
        // Khởi tạo con voi tại tọa độ (400, 300) - chính giữa màn hình
        // Kích thước size = 80.0, máu = 100, đói = 5.0, khát = 6.0
        Elephant elephant = new Elephant(
            new Vector2D(400, 300), 
            80.0, 
            "rect", 
            100.0, 
            5.0, 
            3.0
        );
        worldMap.addEntity(elephant);
        Lake lake = new Lake(new Vector2D(440, 300), 300, 300);
        worldMap.addEntity(lake);

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