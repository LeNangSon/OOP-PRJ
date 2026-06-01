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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainApp extends Application {
    private static final double SOURCE_MAP_SIZE = 1248.0;
    private static final double WIDTH = 576;
    private static final double HEIGHT = 576;
    private static final double TERMINAL_WIDTH = 400;
    private static final double TOOLBAR_HEIGHT = 40;
    // Dùng chung 1 TMX cho mọi mùa (object zones Ho/VatCan không đổi)
    private static final String SHARED_TMX_PATH = "/org/openjfx/app/all.tmx";
    private static final int    SHARED_TILE_SIZE = 32;

    // Ảnh nền riêng cho từng mùa
    private static final String IMG_SPRING = "/org/openjfx/app/spring.jpg";
    private static final String IMG_SUMMER = "/org/openjfx/app/summer.png";
    private static final String IMG_AUTUMN = "/org/openjfx/app/autumn.png";
    private static final String IMG_WINTER = "/org/openjfx/app/winter.png";

    private static final String BTN_BASE   = "-fx-background-color: rgba(20,20,20,0.55); -fx-text-fill: white; -fx-padding: 4 12; -fx-cursor: hand; -fx-font-size: 12px;";
    private static final String BTN_ACCENT = "-fx-background-color: rgba(20,20,20,0.55); -fx-text-fill: #ffdddd; -fx-padding: 4 12; -fx-cursor: hand; -fx-font-size: 12px;";

    private WorldMap worldMap;
    private AnimalToAdd selectedAnimal = AnimalToAdd.RABBIT;
    private Season currentSeason = Season.SPRING;

    private enum AnimalToAdd {
        RABBIT, WOLF, BEAR, ELEPHANT, FISH
    }

    private enum Season {
        SPRING, SUMMER, AUTUMN, WINTER
    }

    @Override
    public void start(Stage stage) {
        double mapScaleX = WIDTH / SOURCE_MAP_SIZE;
        double mapScaleY = HEIGHT / SOURCE_MAP_SIZE;

        String tmxAbsPath = Paths.get(System.getProperty("user.dir"),
                "src", "main", "resources", "org", "openjfx", "app", "all.tmx")
                .toString();

        worldMap = new WorldMap(WIDTH, HEIGHT);
        worldMap.setFixedBackgroundImageFromResource(IMG_SPRING);
        worldMap.setObjectZonesFromTmxFile(tmxAbsPath, SHARED_TILE_SIZE, mapScaleX, mapScaleY);
        startTmxWatcher(tmxAbsPath, mapScaleX, mapScaleY);

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
        StackPane mapPane = new StackPane(canvas, toolbar);
        StackPane.setAlignment(toolbar, Pos.TOP_LEFT);
        mapPane.setStyle("-fx-background-color: transparent;");

        HBox root = new HBox(mapPane, listView, entityStatusPanel);
        root.setStyle("-fx-background-color: #1a1a1a;");
        Scene scene = new Scene(root, WIDTH + TERMINAL_WIDTH, HEIGHT, Color.rgb(26, 26, 26));

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
        stage.setTitle("Project - Ecology Simulation (Map & Terminal)");
        stage.show();
    }

    private HBox createToolbar() {
        // --- Dropdown chon dong vat ---
        MenuButton animalMenu = new MenuButton("Thêm: Thỏ");
        animalMenu.setStyle(BTN_BASE);
        ToggleGroup animalGroup = new ToggleGroup();
        addAnimalItem(animalMenu, animalGroup, "Thỏ", AnimalToAdd.RABBIT, true);
        addAnimalItem(animalMenu, animalGroup, "Sói", AnimalToAdd.WOLF, false);
        addAnimalItem(animalMenu, animalGroup, "Gấu", AnimalToAdd.BEAR, false);
        addAnimalItem(animalMenu, animalGroup, "Voi", AnimalToAdd.ELEPHANT, false);
        addAnimalItem(animalMenu, animalGroup, "Cá", AnimalToAdd.FISH, false);

        // --- Nut zoom ---
        Button btnPlus = createToolbarButton("+");
        Button btnMinus = createToolbarButton("-");
        Button btnReset = createToolbarButton("Đặt lại");
        btnReset.setStyle(BTN_ACCENT);

        btnPlus.setOnAction(e -> zoomAtPoint(Math.min(3.0, worldMap.getScale() + 0.1), worldMap.getScale(), WIDTH / 2, HEIGHT / 2));
        btnMinus.setOnAction(e -> zoomAtPoint(Math.max(1.0, worldMap.getScale() - 0.1), worldMap.getScale(), WIDTH / 2, HEIGHT / 2));
        btnReset.setOnAction(e -> {
            worldMap.setScale(1.0);
            worldMap.setOffset(0, 0);
        });

        // --- Dropdown chon mua ---
        MenuButton seasonMenu = new MenuButton("Mùa: Xuân");
        seasonMenu.setStyle(BTN_BASE);
        ToggleGroup seasonGroup = new ToggleGroup();
        addSeasonItem(seasonMenu, seasonGroup, "Xuân", Season.SPRING, true, false);
        addSeasonItem(seasonMenu, seasonGroup, "Hạ",   Season.SUMMER, false, false);
        addSeasonItem(seasonMenu, seasonGroup, "Thu",  Season.AUTUMN, false, false);
        addSeasonItem(seasonMenu, seasonGroup, "Đông", Season.WINTER, false, false);

        HBox toolbar = new HBox(6, animalMenu, btnPlus, btnMinus, btnReset, seasonMenu);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6));
        toolbar.setMinHeight(TOOLBAR_HEIGHT);
        toolbar.setMaxHeight(TOOLBAR_HEIGHT);
        toolbar.setPrefHeight(TOOLBAR_HEIGHT);
        toolbar.setStyle("-fx-background-color: transparent;");
        return toolbar;
    }

    private void addAnimalItem(MenuButton menu, ToggleGroup group, String label, AnimalToAdd animal, boolean selected) {
        RadioMenuItem item = new RadioMenuItem(label);
        item.setToggleGroup(group);
        item.setSelected(selected);
        item.setOnAction(e -> {
            selectedAnimal = animal;
            menu.setText("Thêm: " + label);
        });
        menu.getItems().add(item);
    }

    private void addSeasonItem(MenuButton menu, ToggleGroup group, String label, Season season, boolean selected, boolean disabled) {
        RadioMenuItem item = new RadioMenuItem(label);
        item.setToggleGroup(group);
        item.setSelected(selected);
        item.setDisable(disabled);
        item.setOnAction(e -> {
            menu.setText("Mùa: " + label);
            switchToSeason(season);
        });
        menu.getItems().add(item);
    }

    private void switchToSeason(Season season) {
        if (currentSeason == season) return;
        currentSeason = season;
        switch (season) {
            case SPRING: worldMap.setFixedBackgroundImageFromResource(IMG_SPRING); break;
            case SUMMER: worldMap.setFixedBackgroundImageFromResource(IMG_SUMMER); break;
            case AUTUMN: worldMap.setFixedBackgroundImageFromResource(IMG_AUTUMN); break;
            case WINTER: worldMap.setFixedBackgroundImageFromResource(IMG_WINTER); break;
        }
    }

    private Button createToolbarButton(String text) {
        Button button = new Button(text);
        button.setStyle(BTN_BASE);
        return button;
    }

    private void addSelectedAnimalAt(double screenX, double screenY) {
        Vector2D worldPosition = screenToWorld(screenX, screenY);
        Entity entity = createSelectedAnimal(worldPosition);
        if (!(entity instanceof LivingEntity)) return;

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
            case RABBIT:   return new Rabbit(position);
            case WOLF:     return new Wolf(position);
            case BEAR:     return new Bear(position);
            case ELEPHANT: return new Elephant(position);
            case FISH:     return new Fish(position);
            default:       return null;
        }
    }

    private void zoomAtPoint(double newScale, double oldScale, double cx, double cy) {
        if (oldScale == newScale) return;
        double ox = cx - (cx - worldMap.getOffsetX()) * (newScale / oldScale);
        double oy = cy - (cy - worldMap.getOffsetY()) * (newScale / oldScale);
        worldMap.setScale(newScale);
        worldMap.setOffset(ox, oy);
    }

    private void startTmxWatcher(String absolutePath, double scaleX, double scaleY) {
        Thread watcher = new Thread(() -> {
            try {
                Path path = Paths.get(absolutePath);
                long[] lastModified = { Files.getLastModifiedTime(path).toMillis() };
                while (true) {
                    Thread.sleep(1000);
                    long current = Files.getLastModifiedTime(path).toMillis();
                    if (current != lastModified[0]) {
                        lastModified[0] = current;
                        Platform.runLater(() -> {
                            try {
                                worldMap.setObjectZonesFromTmxFile(absolutePath, SHARED_TILE_SIZE, scaleX, scaleY);
                            } catch (Exception e) {
                                System.err.println("TMX reload failed: " + e.getMessage());
                            }
                        });
                    }
                }
            } catch (Exception e) {
                System.err.println("TMX watcher error: " + e.getMessage());
            }
        });
        watcher.setDaemon(true);
        watcher.start();
    }

    private Vector2D mapPosition(double sourceX, double sourceY) {
        return new Vector2D(sourceX * WIDTH / SOURCE_MAP_SIZE, sourceY * HEIGHT / SOURCE_MAP_SIZE);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
