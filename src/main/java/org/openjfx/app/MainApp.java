package org.openjfx.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import org.openjfx.app.core.TerminalLogger;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.terrain.TerrainType;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;
import org.openjfx.app.entities.movable.Bear;
import org.openjfx.app.entities.movable.Elephant;
import org.openjfx.app.entities.movable.Fish;
import org.openjfx.app.entities.movable.Rabbit;
import org.openjfx.app.entities.movable.Wolf;
import org.openjfx.app.entities.staticobjs.Algae;
import org.openjfx.app.entities.staticobjs.Bush;
import org.openjfx.app.entities.staticobjs.Grass;
import org.openjfx.app.entities.staticobjs.Rock;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class MainApp extends Application {
    private static final double SOURCE_MAP_SIZE = 1248.0;
    private static final double WIDTH = 576.0;
    private static final double HEIGHT = 576.0;
    private static final double TERMINAL_WIDTH = 400.0;
    private static final double TOOLBAR_HEIGHT = 42.0;
    private static final double MIN_ZOOM = 1.0;
    private static final double MAX_ZOOM = 3.0;
    private static final double SEASON_AUTO_SWITCH_SECONDS = 60.0;
    private static final double SEASON_TRANSITION_SECONDS  = 6.0;

    private static final String SHARED_TMX_RESOURCE = "/org/openjfx/app/all.tmx";
    private static final int SHARED_TILE_SIZE = 32;
    private static final String IMG_SPRING = "/org/openjfx/app/spring.jpg";
    private static final String IMG_SUMMER = "/org/openjfx/app/summer.png";
    private static final String IMG_AUTUMN = "/org/openjfx/app/autumn.png";
    private static final String IMG_WINTER = "/org/openjfx/app/winter.png";

    private static final String TOOLBAR_STYLE = "-fx-background-color: rgba(20,20,20,0.55); -fx-padding: 6 8;";
    private static final String BTN_BASE = "-fx-background-color: rgba(35,35,35,0.72); -fx-text-fill: white; -fx-padding: 4 10; -fx-cursor: hand; -fx-font-size: 12px;";
    private static final String BTN_SELECTED = "-fx-background-color: rgba(80,135,80,0.85); -fx-text-fill: white; -fx-padding: 4 10; -fx-cursor: crosshair; -fx-font-size: 12px; -fx-font-weight: bold;";
    private static final String BTN_ACCENT = "-fx-background-color: rgba(90,45,45,0.78); -fx-text-fill: #ffdddd; -fx-padding: 4 10; -fx-cursor: hand; -fx-font-size: 12px;";

    private enum SpawnKind {
        RABBIT, WOLF, BEAR, ELEPHANT, FISH, GRASS, ALGAE, BUSH, ROCK
    }

    private enum Season {
        SPRING, SUMMER, AUTUMN, WINTER
    }

    private static final double PANEL_REFRESH_INTERVAL = 0.15;
    private double panelRefreshAccum = 0;

    private WorldMap worldMap;
    private Canvas canvas;
    private SpawnKind pendingSpawnKind = SpawnKind.RABBIT;
    private Season currentSeason = Season.SPRING;
    private ToggleGroup spawnGroup;
    private Slider zoomSlider;
    private boolean updatingZoomSlider;
    private final Random random = new Random();
    private Label survivalLabel;
    private MenuButton seasonMenu;
    private ToggleGroup seasonGroup;
    private double seasonElapsedSeconds;
    private boolean isTransitioning    = false;
    private double  transitionElapsed  = 0.0;
    private Season  targetSeason       = null;
    private double survivalTime;
    private boolean survivalEnded;

    @Override
    public void start(Stage stage) {
        double mapScaleX = WIDTH / SOURCE_MAP_SIZE;
        double mapScaleY = HEIGHT / SOURCE_MAP_SIZE;

        worldMap = new WorldMap(WIDTH, HEIGHT);
        worldMap.setFixedBackgroundImageFromResource(IMG_SPRING);
        worldMap.setObjectZonesFromTmxResource(SHARED_TMX_RESOURCE, SHARED_TILE_SIZE, mapScaleX, mapScaleY);

        Path editableTmx = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "org", "openjfx", "app", "all.tmx");
        if (Files.exists(editableTmx)) {
            startTmxWatcher(editableTmx, mapScaleX, mapScaleY);
        }

        ObservableList<String> logData = FXCollections.observableArrayList();
        ListView<String> listView = new ListView<>(logData);
        listView.setPrefWidth(TERMINAL_WIDTH);
        listView.setFocusTraversable(false);
        listView.setStyle("-fx-control-inner-background: #1e1e1e; -fx-font-family: 'Consolas', 'Monospaced'; -fx-font-size: 13px;");
        worldMap.addObserver(new TerminalLogger(logData));

        seedInitialEntities();
        worldMap.notifyAction("Hệ thống", "đã khởi tạo", "map 4 mùa dùng chung all.tmx");

        EntityStatusPanel entityStatusPanel = new EntityStatusPanel(worldMap);
        entityStatusPanel.setVisible(false);
        entityStatusPanel.setManaged(false);

        canvas = new Canvas(WIDTH, HEIGHT);
        canvas.setFocusTraversable(true);
        setupCanvasInput();

        GraphicsContext gc = canvas.getGraphicsContext2D();
        AnimationTimer timer = new AnimationTimer() {
            private long lastNow = 0;

            @Override
            public void handle(long now) {
                // dt theo thời gian thực giữa 2 frame -> chuyển động đều dù FPS dao động.
                double dt = (lastNow == 0) ? 1.0 / 60.0 : (now - lastNow) / 1_000_000_000.0;
                lastNow = now;
                // Kẹp dt để tránh nhảy lớn khi lag/alt-tab gây "teleport" xuyên vật cản.
                dt = Math.min(dt, 0.05);

                gc.clearRect(0, 0, WIDTH, HEIGHT);
                worldMap.update(dt);
                worldMap.render(gc);
                tickSeasonCycle(dt);
                tickSurvivalClock(dt);
                // Refresh panel ~6-7Hz thay vì mỗi frame (rebuild list + sort toàn bộ entity rất nặng).
                panelRefreshAccum += dt;
                if (entityStatusPanel.isVisible() && panelRefreshAccum >= PANEL_REFRESH_INTERVAL) {
                    entityStatusPanel.refreshData();
                    panelRefreshAccum = 0;
                }
            }
        };
        timer.start();

        HBox toolbar = buildToolbar();
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
            } else if (event.getCode() == KeyCode.ESCAPE) {
                clearPlacementMode();
                event.consume();
            }
        });

        stage.setScene(scene);
        stage.setTitle("Project - Ecology Simulation");
        stage.show();
        canvas.requestFocus();
    }

    private HBox buildToolbar() {
        spawnGroup = new ToggleGroup();

        HBox animalBox = new HBox(4,
                createSpawnToggle("Thỏ", SpawnKind.RABBIT),
                createSpawnToggle("Sói", SpawnKind.WOLF),
                createSpawnToggle("Gấu", SpawnKind.BEAR),
                createSpawnToggle("Voi", SpawnKind.ELEPHANT),
                createSpawnToggle("Cá", SpawnKind.FISH));
        animalBox.setAlignment(Pos.CENTER_LEFT);

        HBox staticBox = new HBox(4,
                createSpawnToggle("Cỏ", SpawnKind.GRASS),
                createSpawnToggle("Tảo", SpawnKind.ALGAE),
                createSpawnToggle("Bụi", SpawnKind.BUSH),
                createSpawnToggle("Đá", SpawnKind.ROCK));
        staticBox.setAlignment(Pos.CENTER_LEFT);

        zoomSlider = new Slider(MIN_ZOOM, MAX_ZOOM, MIN_ZOOM);
        zoomSlider.setPrefWidth(110);
        zoomSlider.setTooltip(new Tooltip("Phóng to / thu nhỏ"));
        zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!updatingZoomSlider) {
                zoomAtPoint(newVal.doubleValue(), worldMap.getScale(), WIDTH / 2, HEIGHT / 2);
            }
        });

        Button btnMinus = createToolbarButton("-");
        Button btnPlus = createToolbarButton("+");
        Button btnReset = createToolbarButton("Đặt lại");
        btnReset.setStyle(BTN_ACCENT);
        btnMinus.setOnAction(e -> applyZoom(worldMap.getScale() - 0.1));
        btnPlus.setOnAction(e -> applyZoom(worldMap.getScale() + 0.1));
        btnReset.setOnAction(e -> {
            worldMap.setScale(MIN_ZOOM);
            worldMap.setOffset(0, 0);
            syncZoomSlider(MIN_ZOOM);
        });

        seasonMenu = new MenuButton("Mùa: Xuân");
        seasonMenu.setStyle(BTN_BASE);
        seasonGroup = new ToggleGroup();
        addSeasonItem(seasonMenu, seasonGroup, "Xuân", Season.SPRING, true);
        addSeasonItem(seasonMenu, seasonGroup, "Hạ", Season.SUMMER, false);
        addSeasonItem(seasonMenu, seasonGroup, "Thu", Season.AUTUMN, false);
        addSeasonItem(seasonMenu, seasonGroup, "Đông", Season.WINTER, false);

        survivalLabel = new Label("00:00");
        survivalLabel.setStyle("-fx-text-fill: #ffe066; -fx-font-size: 13px; -fx-font-weight: bold; -fx-font-family: 'Consolas', 'Monospaced';");

        HBox zoomBox = new HBox(6, btnMinus, zoomSlider, btnPlus, btnReset);
        zoomBox.setAlignment(Pos.CENTER_RIGHT);
        HBox toolbar = new HBox(8, animalBox, new Separator(), staticBox, new Separator(), survivalLabel, new Separator(), zoomBox, seasonMenu);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setMinHeight(TOOLBAR_HEIGHT);
        toolbar.setMaxHeight(TOOLBAR_HEIGHT);
        toolbar.setPrefHeight(TOOLBAR_HEIGHT);
        toolbar.setPadding(new Insets(6));
        toolbar.setStyle(TOOLBAR_STYLE);
        HBox.setHgrow(zoomBox, Priority.ALWAYS);
        return toolbar;
    }

    private ToggleButton createSpawnToggle(String label, SpawnKind kind) {
        ToggleButton button = new ToggleButton(label);
        button.setToggleGroup(spawnGroup);
        button.setStyle(kind == pendingSpawnKind ? BTN_SELECTED : BTN_BASE);
        button.setCursor(Cursor.HAND);
        button.setSelected(kind == pendingSpawnKind);
        button.setOnAction(e -> {
            pendingSpawnKind = kind;
            refreshSpawnToggleStyles();
            canvas.requestFocus();
        });
        return button;
    }

    private void refreshSpawnToggleStyles() {
        for (var toggle : spawnGroup.getToggles()) {
            if (toggle instanceof ToggleButton button) {
                button.setStyle(button.isSelected() ? BTN_SELECTED : BTN_BASE);
            }
        }
    }

    private Button createToolbarButton(String text) {
        Button button = new Button(text);
        button.setStyle(BTN_BASE);
        return button;
    }

    private void addSeasonItem(MenuButton menu, ToggleGroup group, String label, Season season, boolean selected) {
        RadioMenuItem item = new RadioMenuItem(label);
        item.setToggleGroup(group);
        item.setUserData(season);
        item.setSelected(selected);
        item.setOnAction(e -> {
            switchToSeason(season);
        });
        menu.getItems().add(item);
    }

    private void switchToSeason(Season season) {
        if (currentSeason == season || isTransitioning) return;
        targetSeason        = season;
        isTransitioning     = true;
        transitionElapsed   = 0.0;
        seasonElapsedSeconds = 0.0;
        javafx.scene.image.Image toImage = loadSeasonImage(season);
        worldMap.beginBackgroundTransition(toImage);
        syncSeasonMenu();
    }

    private javafx.scene.image.Image loadSeasonImage(Season season) {
        String path = switch (season) {
            case SPRING -> IMG_SPRING;
            case SUMMER -> IMG_SUMMER;
            case AUTUMN -> IMG_AUTUMN;
            case WINTER -> IMG_WINTER;
        };
        try {
            var stream = getClass().getResourceAsStream(path);
            if (stream == null) return null;
            var img = new javafx.scene.image.Image(stream);
            return img.isError() ? null : img;
        } catch (Exception e) {
            return null;
        }
    }

    private void tickSeasonCycle(double dt) {
        if (isTransitioning) {
            transitionElapsed += dt;
            double alpha = transitionElapsed / SEASON_TRANSITION_SECONDS;
            worldMap.setTransitionAlpha(alpha);
            if (alpha >= 1.0) {
                worldMap.completeBackgroundTransition();
                currentSeason   = targetSeason;
                targetSeason    = null;
                isTransitioning = false;
                syncSeasonMenu();
            }
            return;
        }
        seasonElapsedSeconds += dt;
        if (seasonElapsedSeconds >= SEASON_AUTO_SWITCH_SECONDS) {
            switchToSeason(nextSeason(currentSeason));
        }
    }

    private Season nextSeason(Season season) {
        return switch (season) {
            case SPRING -> Season.SUMMER;
            case SUMMER -> Season.AUTUMN;
            case AUTUMN -> Season.WINTER;
            case WINTER -> Season.SPRING;
        };
    }

    private void syncSeasonMenu() {
        if (seasonMenu != null) {
            seasonMenu.setText("Mùa: " + seasonLabel(currentSeason));
        }
        if (seasonGroup != null) {
            for (var toggle : seasonGroup.getToggles()) {
                if (toggle instanceof RadioMenuItem item && item.getUserData() == currentSeason) {
                    seasonGroup.selectToggle(item);
                    break;
                }
            }
        }
    }

    private String seasonLabel(Season season) {
        return switch (season) {
            case SPRING -> "Xuân";
            case SUMMER -> "Hạ";
            case AUTUMN -> "Thu";
            case WINTER -> "Đông";
        };
    }

    private void setupCanvasInput() {
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
                worldMap.setOffset(lastOffset[0] + e.getX() - mouseAnchor[0],
                        lastOffset[1] + e.getY() - mouseAnchor[1]);
            }
        });
        canvas.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                addSelectedAt(e.getX(), e.getY());
            }
        });
    }

    private void addSelectedAt(double screenX, double screenY) {
        Vector2D position = screenToWorld(screenX, screenY);
        switch (pendingSpawnKind) {
            case RABBIT, WOLF, BEAR, ELEPHANT, FISH -> spawnLiving(pendingSpawnKind, position);
            case GRASS -> spawnGrass(position);
            case ALGAE -> spawnAlgae(position);
            case BUSH -> spawnTerrainObject(position, new Bush(position));
            case ROCK -> spawnTerrainObject(position, new Rock(position));
        }
    }

    private void spawnLiving(SpawnKind kind, Vector2D position) {
        LivingEntity entity = createLivingByKind(kind, position);
        if (entity == null) return;
        if (!worldMap.canStandOn(entity, position)) {
            worldMap.notifyAction("Hệ thống", "không thể đặt", kind + " tại vị trí này");
            return;
        }
        addEntityAndLog(entity);
    }

    private void spawnGrass(Vector2D position) {
        if (worldMap.getTerrainAt(position) != TerrainType.LAND) {
            worldMap.notifyAction("Hệ thống", "không thể đặt", "Cỏ chỉ đặt trên đất");
            return;
        }
        addEntityAndLog(new Grass(position));
    }

    private void spawnAlgae(Vector2D position) {
        if (worldMap.getTerrainAt(position) != TerrainType.WATER) {
            worldMap.notifyAction("Hệ thống", "không thể đặt", "Tảo chỉ đặt trên nước");
            return;
        }
        addEntityAndLog(new Algae(position));
    }

    private void spawnTerrainObject(Vector2D position, Entity entity) {
        if (!worldMap.isInside(position)) {
            worldMap.notifyAction("Hệ thống", "không thể đặt", entity.getClass().getSimpleName() + " ngoài bản đồ");
            return;
        }
        TerrainType at = worldMap.getTerrainAt(position);
        if (at == TerrainType.WATER) {
            worldMap.notifyAction("Hệ thống", "không thể đặt", entity.getClass().getSimpleName() + " trên nước");
            return;
        }
        if (at == TerrainType.ROCK || at == TerrainType.BUSH) {
            worldMap.notifyAction("Hệ thống", "không thể đặt", entity.getClass().getSimpleName() + " chồng lên vật cản");
            return;
        }
        addEntityAndLog(entity);
    }

    private LivingEntity createLivingByKind(SpawnKind kind, Vector2D position) {
        return switch (kind) {
            case RABBIT -> new Rabbit(position);
            case WOLF -> new Wolf(position);
            case BEAR -> new Bear(position);
            case ELEPHANT -> new Elephant(position);
            case FISH -> new Fish(position);
            default -> null;
        };
    }

    private void addEntityAndLog(Entity entity) {
        worldMap.addEntity(entity);
        worldMap.notifyAction("Hệ thống", "đã thêm", entity.getClass().getSimpleName() + "#" + entity.getId());
    }

    private Vector2D screenToWorld(double screenX, double screenY) {
        return new Vector2D(
                (screenX - worldMap.getOffsetX()) / worldMap.getScale(),
                (screenY - worldMap.getOffsetY()) / worldMap.getScale());
    }

    private Vector2D mapPosition(double sourceX, double sourceY) {
        return new Vector2D(sourceX * WIDTH / SOURCE_MAP_SIZE, sourceY * HEIGHT / SOURCE_MAP_SIZE);
    }

    private void seedInitialEntities() {
        worldMap.addEntity(new Rabbit(mapPosition(410, 350)));
        worldMap.addEntity(new Rabbit(mapPosition(450, 350)));
        worldMap.addEntity(new Rabbit(mapPosition(430, 380)));
        worldMap.addEntity(new Wolf(mapPosition(500, 300)));
        worldMap.addEntity(new Bear(mapPosition(720, 310)));
        worldMap.addEntity(new Elephant(mapPosition(860, 330)));
        worldMap.addEntity(new Fish(mapPosition(170, 120)));

        for (int i = 0; i < 35; i++) {
            Vector2D p = randomLandPosition();
            if (p != null) worldMap.addEntity(new Grass(p));
        }
        for (int i = 0; i < 10; i++) {
            Vector2D p = randomWaterPosition();
            if (p != null) worldMap.addEntity(new Algae(p));
        }
    }

    private Vector2D randomLandPosition() {
        for (int i = 0; i < 80; i++) {
            Vector2D p = new Vector2D(random.nextDouble(WIDTH), random.nextDouble(HEIGHT));
            if (worldMap.getTerrainAt(p) == TerrainType.LAND) return p;
        }
        return null;
    }

    private Vector2D randomWaterPosition() {
        for (int i = 0; i < 120; i++) {
            Vector2D p = new Vector2D(random.nextDouble(WIDTH), random.nextDouble(HEIGHT));
            if (worldMap.getTerrainAt(p) == TerrainType.WATER) return p;
        }
        return null;
    }

    private void tickSurvivalClock(double dt) {
        if (survivalEnded) return;
        if (hasAliveMovable()) {
            survivalTime += dt;
            updateSurvivalLabel(false);
        } else {
            survivalEnded = true;
            updateSurvivalLabel(true);
            worldMap.notifyAction("Hệ thống", "kết thúc", "hệ sinh thái sinh tồn được " + formatTime(survivalTime));
        }
    }

    private boolean hasAliveMovable() {
        for (Entity e : worldMap.getEntities()) {
            if (e instanceof LivingEntity living && living.isAlive()) return true;
        }
        return false;
    }

    private void updateSurvivalLabel(boolean ended) {
        if (survivalLabel == null) return;
        survivalLabel.setText(ended ? formatTime(survivalTime) + " (đã kết thúc)" : formatTime(survivalTime));
    }

    private String formatTime(double seconds) {
        int total = (int) Math.floor(seconds);
        int hh = total / 3600;
        int mm = (total % 3600) / 60;
        int ss = total % 60;
        return hh > 0 ? String.format("%d:%02d:%02d", hh, mm, ss) : String.format("%02d:%02d", mm, ss);
    }

    private void clearPlacementMode() {
        pendingSpawnKind = SpawnKind.RABBIT;
        if (spawnGroup != null && !spawnGroup.getToggles().isEmpty()) {
            spawnGroup.selectToggle(spawnGroup.getToggles().get(0));
            refreshSpawnToggleStyles();
        }
    }

    private void applyZoom(double targetScale) {
        double clamped = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, targetScale));
        zoomAtPoint(clamped, worldMap.getScale(), WIDTH / 2, HEIGHT / 2);
    }

    private void zoomAtPoint(double newScale, double oldScale, double cx, double cy) {
        if (oldScale == newScale) return;
        double ox = cx - (cx - worldMap.getOffsetX()) * (newScale / oldScale);
        double oy = cy - (cy - worldMap.getOffsetY()) * (newScale / oldScale);
        worldMap.setScale(newScale);
        worldMap.setOffset(ox, oy);
        syncZoomSlider(newScale);
    }

    private void syncZoomSlider(double scale) {
        if (zoomSlider == null) return;
        updatingZoomSlider = true;
        zoomSlider.setValue(scale);
        updatingZoomSlider = false;
    }

    private void startTmxWatcher(Path path, double scaleX, double scaleY) {
        Thread watcher = new Thread(() -> {
            try {
                long[] lastModified = { Files.getLastModifiedTime(path).toMillis() };
                while (true) {
                    Thread.sleep(1000);
                    long current = Files.getLastModifiedTime(path).toMillis();
                    if (current != lastModified[0]) {
                        lastModified[0] = current;
                        Platform.runLater(() -> {
                            try {
                                worldMap.setObjectZonesFromTmxFile(path.toString(), SHARED_TILE_SIZE, scaleX, scaleY);
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

    public static void main(String[] args) {
        launch(args);
    }
}
