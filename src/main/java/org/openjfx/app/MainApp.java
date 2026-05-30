package org.openjfx.app;

import org.openjfx.app.core.TerminalLogger;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;
import org.openjfx.app.entities.movable.Bear;
import org.openjfx.app.entities.movable.Elephant;
import org.openjfx.app.entities.movable.Fish;
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
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {
    private static final double SOURCE_MAP_SIZE = 1248.0;
    private static final double WIDTH = 576;
    private static final double HEIGHT = 576;
    private static final double TERMINAL_WIDTH = 400;
    private static final double TOOLBAR_HEIGHT = 40;
    private static final String FIXED_MAP_RESOURCE_PATH = "/org/openjfx/app/spring.jpg";
    private static final String SPRING_TMX_RESOURCE_PATH = "/org/openjfx/app/spring.tmx";
    private static final int SPRING_TILE_SIZE = 32;

    private WorldMap worldMap;
    private AnimalToAdd selectedAnimal = AnimalToAdd.RABBIT;

    private enum AnimalToAdd {
        RABBIT, WOLF, BEAR, ELEPHANT, FISH
    }

    @Override
    public void start(Stage stage) {
        double mapScaleX = WIDTH / SOURCE_MAP_SIZE;
        double mapScaleY = HEIGHT / SOURCE_MAP_SIZE;
        worldMap = new WorldMap(WIDTH, HEIGHT);
        worldMap.setFixedBackgroundImageFromResource(FIXED_MAP_RESOURCE_PATH);
        worldMap.setObjectZonesFromTmxResource(SPRING_TMX_RESOURCE_PATH, SPRING_TILE_SIZE, mapScaleX, mapScaleY);

        ObservableList<String> logData = FXCollections.observableArrayList();
        ListView<String> listView = new ListView<>(logData);
        listView.setPrefWidth(TERMINAL_WIDTH);
        listView.setFocusTraversable(false);
        listView.setStyle("-fx-control-inner-background: #1e1e1e; "
                + "-fx-font-family: 'Consolas', 'Monospaced'; "
                + "-fx-font-size: 13px;");

        EntityStatusPanel entityStatusPanel = new EntityStatusPanel(worldMap);
        entityStatusPanel.setVisible(false);
        entityStatusPanel.setManaged(false);

        worldMap.addObserver(new TerminalLogger(logData));
        worldMap.addEntity(new Rabbit(mapPosition(410, 350)));
        worldMap.addEntity(new Rabbit(mapPosition(450, 350)));
        worldMap.addEntity(new Rabbit(mapPosition(400, 350)));
        worldMap.addEntity(new Rabbit(mapPosition(430, 350)));
        worldMap.addEntity(new Wolf(mapPosition(500, 300)));
        worldMap.addEntity(new Fish(mapPosition(170, 120)));

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        canvas.setFocusTraversable(true);

        final double[] mouseAnchor = new double[2];
        final double[] lastOffset = new double[2];

        canvas.setOnMousePressed(e -> {
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
            if (e.getButton() == MouseButton.PRIMARY) {
                addSelectedAnimalAt(e.getX(), e.getY());
            }
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

        HBox toolbar = createToolbar();
        VBox mapPane = new VBox(toolbar, canvas);
        HBox root = new HBox(mapPane, listView, entityStatusPanel);
        Scene scene = new Scene(root, WIDTH + TERMINAL_WIDTH, HEIGHT + TOOLBAR_HEIGHT);

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.TAB) {
                boolean showStats = !entityStatusPanel.isVisible();
                entityStatusPanel.setVisible(showStats);
                entityStatusPanel.setManaged(showStats);
                listView.setVisible(!showStats);
                listView.setManaged(!showStats);

                if (showStats) {
                    entityStatusPanel.refreshData();
                }

                event.consume();
            }
        });

        stage.setScene(scene);
        stage.setTitle("Project - Ecology Simulation (Map & Terminal)");
        stage.show();
    }

    private HBox createToolbar() {
        ToggleGroup animalGroup = new ToggleGroup();
        ToggleButton btnRabbit = createAnimalButton("Thỏ", AnimalToAdd.RABBIT, animalGroup);
        ToggleButton btnWolf = createAnimalButton("Sói", AnimalToAdd.WOLF, animalGroup);
        ToggleButton btnBear = createAnimalButton("Gấu", AnimalToAdd.BEAR, animalGroup);
        ToggleButton btnElephant = createAnimalButton("Voi", AnimalToAdd.ELEPHANT, animalGroup);
        ToggleButton btnFish = createAnimalButton("Cá", AnimalToAdd.FISH, animalGroup);
        btnRabbit.setSelected(true);

        Button btnPlus = createToolbarButton("+");
        Button btnMinus = createToolbarButton("-");
        Button btnReset = createToolbarButton("Đặt lại");
        btnReset.setStyle("-fx-background-color: #4a4a4a; -fx-text-fill: #ffdddd; -fx-padding: 4 10; -fx-cursor: hand;");

        btnPlus.setOnAction(e -> zoomAtPoint(Math.min(3.0, worldMap.getScale() + 0.1), worldMap.getScale(), WIDTH / 2, HEIGHT / 2));
        btnMinus.setOnAction(e -> zoomAtPoint(Math.max(1.0, worldMap.getScale() - 0.1), worldMap.getScale(), WIDTH / 2, HEIGHT / 2));
        btnReset.setOnAction(e -> {
            worldMap.setScale(1.0);
            worldMap.setOffset(0, 0);
        });

        HBox toolbar = new HBox(8, btnRabbit, btnWolf, btnBear, btnElephant, btnFish, btnPlus, btnMinus, btnReset);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6));
        toolbar.setMinHeight(TOOLBAR_HEIGHT);
        toolbar.setStyle("-fx-background-color: #242424;");
        return toolbar;
    }

    private ToggleButton createAnimalButton(String text, AnimalToAdd animal, ToggleGroup group) {
        ToggleButton button = new ToggleButton(text);
        button.setToggleGroup(group);
        button.setStyle("-fx-background-color: #3a3a3a; -fx-text-fill: white; -fx-padding: 4 10; -fx-cursor: hand;");
        button.setOnAction(e -> selectedAnimal = animal);
        return button;
    }

    private Button createToolbarButton(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: #3b3b3b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 10; -fx-cursor: hand;");
        return button;
    }

    private void addSelectedAnimalAt(double screenX, double screenY) {
        Vector2D worldPosition = screenToWorld(screenX, screenY);
        Entity entity = createSelectedAnimal(worldPosition);
        if (!(entity instanceof LivingEntity)) {
            return;
        }

        LivingEntity livingEntity = (LivingEntity) entity;
        if (worldMap.canStandOn(livingEntity, worldPosition)) {
            worldMap.addEntity(livingEntity);
        }
    }

    private Vector2D screenToWorld(double screenX, double screenY) {
        double worldX = (screenX - worldMap.getOffsetX()) / worldMap.getScale();
        double worldY = (screenY - worldMap.getOffsetY()) / worldMap.getScale();
        return new Vector2D(worldX, worldY);
    }

    private Entity createSelectedAnimal(Vector2D position) {
        switch (selectedAnimal) {
            case RABBIT:
                return new Rabbit(position);
            case WOLF:
                return new Wolf(position);
            case BEAR:
                return new Bear(position);
            case ELEPHANT:
                return new Elephant(position);
            case FISH:
                return new Fish(position);
            default:
                return null;
        }
    }

    private void zoomAtPoint(double newScale, double oldScale, double cx, double cy) {
        if (oldScale == newScale) return;
        double ox = cx - (cx - worldMap.getOffsetX()) * (newScale / oldScale);
        double oy = cy - (cy - worldMap.getOffsetY()) * (newScale / oldScale);
        worldMap.setScale(newScale);
        worldMap.setOffset(ox, oy);
    }

    private Vector2D mapPosition(double sourceX, double sourceY) {
        return new Vector2D(sourceX * WIDTH / SOURCE_MAP_SIZE, sourceY * HEIGHT / SOURCE_MAP_SIZE);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
