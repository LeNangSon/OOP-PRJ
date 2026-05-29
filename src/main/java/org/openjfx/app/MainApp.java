package org.openjfx.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import org.openjfx.app.core.TerminalLogger;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.strategies.MateStrategy;
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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.stage.Stage;

public class MainApp extends Application {
    private enum SpawnKind {
        RABBIT, WOLF, BEAR, ELEPHANT, FISH,
        GRASS, ALGAE, BUSH, ROCK
    }

    private static final double WIDTH = 1280;
    private static final double HEIGHT = 1280;
    private static final double CANVAS_WIDTH = 960;
    private static final double CANVAS_HEIGHT = 640;
    private static final double MIN_ZOOM = 0.75;
    private static final double MAX_ZOOM = 3.0;
    private static final double SOUND_ZOOM_THRESHOLD = 1.8;
    private static final String FIXED_MAP_RESOURCE_PATH = "/org/openjfx/app/myp.png";
    private static final String TERRAIN_GRASS_CSV_RESOURCE_PATH = "/org/openjfx/app/terrain_Co.csv";
    private static final String TERRAIN_WATER_CSV_RESOURCE_PATH = "/org/openjfx/app/terrain_Song.csv";
    private static final String TERRAIN_ROAD_CSV_RESOURCE_PATH = "/org/openjfx/app/terrain_Cauvaduongdi.csv";
    private static final String WOLF_MATING_SOUND_RESOURCE_PATH =
            "/sounds/soitimbantinh.wav";
    private static final String BEAR_MATING_SOUND_RESOURCE_PATH = "/sounds/gautimbantinh.mp3";
    private static final String WOLF_EATING_SOUND_RESOURCE_PATH = "/sounds/anthit.mp3";
    private static final String ELEPHANT_SOUND_RESOURCE_PATH = "/sounds/voi.mp3";
    private static final String FISH_SWIMMING_SOUND_RESOURCE_PATH = "/sounds/ca.wav";
    private static final int TERRAIN_TILE_SIZE = 16;

    /** Mỗi loài có nhiều "ổ" (den): tâm cố định, thành viên spawn ngẫu nhiên quanh tâm. */
    private static final double[][] WOLF_DEN_CENTERS = {
            {372, 60}, {828, 60}, {60, 228}, {492, 396},
    };
    private static final int WOLF_PER_DEN = 0;
    private static final double WOLF_DEN_RADIUS = 40.0;

    private static final double[][] RABBIT_DEN_CENTERS = {
            {516, 60}, {84, 108}, {876, 132}, {372, 156},
            {660, 156}, {516, 228}, {780, 252}, {300, 300},
            {60, 324}, {636, 324}, {828, 396}, {204, 420},
    };
    private static final int RABBIT_PER_DEN = 0;
    private static final double RABBIT_DEN_RADIUS = 30.0;

    /** Đợt thỏ bổ sung — 5 ổ nhỏ × 3 con = 15 con. */


    private static final double RABBIT_EXTRA_DEN_RADIUS = 25.0;

    private static final double[][] BEAR_DEN_CENTERS = {
            {252, 60}, {948, 60}, {708, 84}, {468, 132},
            {612, 228}, {876, 276}, {204, 324}, {972, 348},
            {708, 372},
    };
    private static final int BEAR_PER_DEN = 0;
    private static final double BEAR_DEN_RADIUS = 30.0;

    private static final double[][] ELEPHANT_DEN_CENTERS = {
            {180, 156}, {780, 156}, {324, 444},
    };
    private static final int ELEPHANT_PER_DEN = 5;
    private static final double ELEPHANT_DEN_RADIUS = 45.0;

    private static final int DEN_PLACEMENT_MAX_ATTEMPTS = 60;

    private static final int INITIAL_GRASS_COUNT = 300;

    private static final String TOOLBAR_STYLE =
            "-fx-background-color: #2a2a2a; -fx-padding: 6 10;";
    private static final String SPAWN_BTN_STYLE =
            "-fx-background-color: #3d3d3d; -fx-text-fill: white; -fx-font-size: 11px; "
                    + "-fx-cursor: hand; -fx-padding: 4 8;";
    private static final String SPAWN_BTN_SELECTED_STYLE =
            "-fx-background-color: #5a8f5a; -fx-text-fill: white; -fx-font-size: 11px; "
                    + "-fx-cursor: crosshair; -fx-padding: 4 8; -fx-font-weight: bold;";

    private WorldMap worldMap;
    private Canvas canvas;
    private SpawnKind pendingSpawnKind;
    private ToggleGroup spawnGroup;
    private Slider zoomSlider;
    private boolean updatingZoomSlider;
    private Clip wolfMatingSoundClip;
    private AudioClip bearMatingSoundClip;
    private AudioClip wolfEatingSoundClip;
    private AudioClip elephantSoundClip;
    private AudioClip fishSwimmingSoundClip;
    private final Random spawnRandom = new Random();

    // Bộ đếm thời gian sinh tồn của hệ sinh thái.
    private Label survivalLabel;
    private double survivalTime;
    private boolean survivalEnded;

    @Override
    public void start(Stage stage) {
        worldMap = new WorldMap(WIDTH, HEIGHT);
        worldMap.setViewportSize(CANVAS_WIDTH, CANVAS_HEIGHT);
        worldMap.setScale(MIN_ZOOM);
        worldMap.setOffset(0, 0);
        worldMap.setFixedBackgroundImageFromResource(FIXED_MAP_RESOURCE_PATH);
        worldMap.setTerrainGridFromLayeredCsvResources(
                TERRAIN_GRASS_CSV_RESOURCE_PATH,
                TERRAIN_WATER_CSV_RESOURCE_PATH,
                TERRAIN_ROAD_CSV_RESOURCE_PATH,
                TERRAIN_TILE_SIZE);
        seedInitialAnimals();

        ObservableList<String> logData = FXCollections.observableArrayList();
        ListView<String> listView = new ListView<>(logData);
        EntityStatusPanel entityStatusPanel = new EntityStatusPanel(worldMap);
        entityStatusPanel.setVisible(false);
        entityStatusPanel.setManaged(false);
        listView.setPrefWidth(400);
        listView.setFocusTraversable(false);
        listView.setStyle("-fx-control-inner-background: #1e1e1e; "
                + "-fx-font-family: 'Consolas', 'Monospaced'; "
                + "-fx-font-size: 13px;");

        worldMap.addObserver(new TerminalLogger(logData));
        worldMap.addObserver(new org.openjfx.app.core.GameObserver() {
            @Override
            public void onEntityDeath(String message) {
            }

            @Override
            public void onActionOccurred(String actor, String action, String target) {
                if (isSoundZoomedIn()
                        && actor != null
                        && (actor.startsWith("Wolf#") || actor.startsWith("Bear#"))
                        && action != null && action.contains("bắt")) {
                    playWolfEatingSound();
                }
            }
        });
        worldMap.notifyAction("Hệ thống", "đã khởi tạo",
                "63 thỏ (12 ổ × 4 + 5 ổ × 3), 16 sói (4 ổ × 4), 18 gấu (9 ổ × 2), "
                        + "12 voi (3 ổ × 4), " + INITIAL_GRASS_COUNT + " cỏ");

        canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        canvas.setFocusTraversable(true);
        setupCanvasInput();

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                gc.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
                worldMap.update(0.016);
                updateWolfMatingSound();
                updateBearMatingSound();
                updateElephantSound();
                updateFishSwimmingSound();
                worldMap.render(gc);
                tickSurvivalClock(0.016);
                if (entityStatusPanel.isVisible()) {
                    entityStatusPanel.refreshData();
                }
            }
        };
        timer.start();

        HBox toolbar = buildToolbar();
        VBox mapColumn = new VBox(toolbar, canvas);
        VBox.setVgrow(canvas, Priority.ALWAYS);

        HBox root = new HBox(mapColumn, listView, entityStatusPanel);
        Scene scene = new Scene(root, CANVAS_WIDTH + 400, CANVAS_HEIGHT + 40);

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
            } else if (event.getCode() == KeyCode.ESCAPE) {
                clearPlacementMode();
                event.consume();
            }
        });

        stage.setScene(scene);
        stage.setTitle("Project - Ecology Simulation (Map & Terminal)");
        stage.show();
        canvas.requestFocus();
    }

    private HBox buildToolbar() {
        spawnGroup = new ToggleGroup();

        ToggleButton btnRabbit = createSpawnToggle("Thỏ", SpawnKind.RABBIT);
        ToggleButton btnWolf = createSpawnToggle("Sói", SpawnKind.WOLF);
        ToggleButton btnBear = createSpawnToggle("Gấu", SpawnKind.BEAR);
        ToggleButton btnElephant = createSpawnToggle("Voi", SpawnKind.ELEPHANT);
        ToggleButton btnFish = createSpawnToggle("Cá", SpawnKind.FISH);

        HBox animalBox = new HBox(4, btnRabbit, btnWolf, btnBear, btnElephant, btnFish);
        animalBox.setAlignment(Pos.CENTER_LEFT);

        ToggleButton btnGrass = createSpawnToggle("Cỏ", SpawnKind.GRASS);
        ToggleButton btnAlgae = createSpawnToggle("Tảo", SpawnKind.ALGAE);
        ToggleButton btnBush = createSpawnToggle("Bụi", SpawnKind.BUSH);
        ToggleButton btnRock = createSpawnToggle("Đá", SpawnKind.ROCK);

        HBox staticBox = new HBox(4, btnGrass, btnAlgae, btnBush, btnRock);
        staticBox.setAlignment(Pos.CENTER_LEFT);

        zoomSlider = new Slider(MIN_ZOOM, MAX_ZOOM, MIN_ZOOM);
        zoomSlider.setPrefWidth(120);
        zoomSlider.setTooltip(new Tooltip("Phóng to / thu nhỏ"));
        zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (updatingZoomSlider) {
                return;
            }
            zoomAtPoint(newVal.doubleValue(), worldMap.getScale(), CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2);
        });

        Button btnPlus = new Button("+");
        Button btnMinus = new Button("−");
        Button btnReset = new Button("Đặt lại");
        styleZoomButton(btnPlus);
        styleZoomButton(btnMinus);
        btnReset.setStyle(
                "-fx-background-color: #444444; -fx-text-fill: #ffaaaa; -fx-background-radius: 4; "
                        + "-fx-padding: 2 8; -fx-cursor: hand; -fx-font-size: 11px;");

        btnPlus.setOnAction(e -> applyZoom(worldMap.getScale() + 0.1));
        btnMinus.setOnAction(e -> applyZoom(worldMap.getScale() - 0.1));
        btnReset.setOnAction(e -> {
            worldMap.setScale(MIN_ZOOM);
            worldMap.setOffset(0, 0);
            syncZoomSlider(MIN_ZOOM);
        });

        HBox zoomBox = new HBox(6, btnMinus, zoomSlider, btnPlus, btnReset);
        zoomBox.setAlignment(Pos.CENTER_RIGHT);

        Label clockIcon = new Label("⏱");
        clockIcon.setStyle("-fx-text-fill: #ffe066; -fx-font-size: 14px;");
        survivalLabel = new Label("00:00");
        survivalLabel.setStyle(
                "-fx-text-fill: #ffe066; -fx-font-size: 14px; -fx-font-weight: bold; "
                        + "-fx-font-family: 'Consolas', 'Monospaced';");
        survivalLabel.setTooltip(new Tooltip("Thời gian hệ sinh thái sinh tồn"));
        HBox timeBox = new HBox(4, clockIcon, survivalLabel);
        timeBox.setAlignment(Pos.CENTER_LEFT);

        HBox toolbar = new HBox(
                8,
                animalBox,
                new Separator(Orientation.VERTICAL),
                staticBox,
                new Separator(Orientation.VERTICAL),
                timeBox,
                new Separator(Orientation.VERTICAL),
                zoomBox);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle(TOOLBAR_STYLE);
        HBox.setHgrow(zoomBox, Priority.ALWAYS);
        zoomBox.setMaxWidth(Double.MAX_VALUE);

        return toolbar;
    }

    private void tickSurvivalClock(double dt) {
        if (survivalEnded) {
            return;
        }
        if (hasAliveMovable()) {
            survivalTime += dt;
            updateSurvivalLabel(false);
        } else {
            survivalEnded = true;
            updateSurvivalLabel(true);
            worldMap.notifyAction("Hệ thống", "kết thúc",
                    "hệ sinh thái sinh tồn được " + formatTime(survivalTime));
        }
    }

    private boolean hasAliveMovable() {
        for (Entity e : worldMap.getEntities()) {
            if (e instanceof LivingEntity living && living.isAlive()) {
                return true;
            }
        }
        return false;
    }

    private void updateSurvivalLabel(boolean ended) {
        if (survivalLabel == null) {
            return;
        }
        String text = formatTime(survivalTime);
        survivalLabel.setText(ended ? text + " (đã kết thúc)" : text);
        survivalLabel.setStyle(
                "-fx-text-fill: " + (ended ? "#ff6b6b" : "#ffe066") + ";"
                        + " -fx-font-size: 14px; -fx-font-weight: bold;"
                        + " -fx-font-family: 'Consolas', 'Monospaced';");
    }

    private String formatTime(double seconds) {
        int total = (int) Math.floor(seconds);
        int hh = total / 3600;
        int mm = (total % 3600) / 60;
        int ss = total % 60;
        if (hh > 0) {
            return String.format("%d:%02d:%02d", hh, mm, ss);
        }
        return String.format("%02d:%02d", mm, ss);
    }

    private ToggleButton createSpawnToggle(String label, SpawnKind kind) {
        ToggleButton btn = new ToggleButton(label);
        btn.setToggleGroup(spawnGroup);
        btn.setStyle(SPAWN_BTN_STYLE);
        btn.setTooltip(new Tooltip("Chọn rồi click trên bản đồ để đặt " + label));
        btn.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                pendingSpawnKind = kind;
                canvas.setCursor(Cursor.CROSSHAIR);
            } else if (pendingSpawnKind == kind) {
                pendingSpawnKind = null;
                canvas.setCursor(Cursor.DEFAULT);
            }
        });
        btn.selectedProperty().addListener((obs, was, now) ->
                btn.setStyle(now ? SPAWN_BTN_SELECTED_STYLE : SPAWN_BTN_STYLE));
        return btn;
    }

    private void styleZoomButton(Button btn) {
        btn.setStyle("-fx-background-color: #3d3d3d; -fx-text-fill: white; -fx-font-size: 13px; "
                + "-fx-cursor: hand; -fx-font-weight: bold; -fx-min-width: 28; -fx-padding: 2 6;");
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
            if (isPlacementMode() || worldMap.getScale() <= MIN_ZOOM) {
                return;
            }
            double dx = e.getX() - mouseAnchor[0];
            double dy = e.getY() - mouseAnchor[1];
            worldMap.setOffset(lastOffset[0] + dx, lastOffset[1] + dy);
        });

        canvas.setOnMouseClicked(e -> {
            if (isPlacementMode()) {
                if (e.getButton() == MouseButton.PRIMARY) {
                    spawnAt(pendingSpawnKind, screenToWorld(e.getX(), e.getY()));
                } else if (e.getButton() == MouseButton.SECONDARY) {
                    clearPlacementMode();
                }
            }
        });
    }

    private boolean isPlacementMode() {
        return pendingSpawnKind != null;
    }

    private void clearPlacementMode() {
        pendingSpawnKind = null;
        canvas.setCursor(Cursor.DEFAULT);
        if (spawnGroup != null) {
            spawnGroup.selectToggle(null);
        }
    }

    private Vector2D screenToWorld(double screenX, double screenY) {
        double scale = worldMap.getScale();
        double wx = (screenX - worldMap.getOffsetX()) / scale;
        double wy = (screenY - worldMap.getOffsetY()) / scale;
        return new Vector2D(wx, wy);
    }

    private void seedInitialAnimals() {
        spawnDens(SpawnKind.RABBIT, RABBIT_DEN_CENTERS, RABBIT_PER_DEN, RABBIT_DEN_RADIUS);

        spawnDens(SpawnKind.WOLF, WOLF_DEN_CENTERS, WOLF_PER_DEN, WOLF_DEN_RADIUS);
        spawnDens(SpawnKind.BEAR, BEAR_DEN_CENTERS, BEAR_PER_DEN, BEAR_DEN_RADIUS);
        spawnDens(SpawnKind.ELEPHANT, ELEPHANT_DEN_CENTERS, ELEPHANT_PER_DEN, ELEPHANT_DEN_RADIUS);
        spawnGrassRandomly(INITIAL_GRASS_COUNT);
    }

    private void spawnGrassRandomly(int count) {
        List<Vector2D> landCenters = collectLandTileCenters();
        Collections.shuffle(landCenters, spawnRandom);
        int toPlace = Math.min(count, landCenters.size());
        for (int i = 0; i < toPlace; i++) {
            worldMap.addEntity(new Grass(landCenters.get(i)));
        }
    }

    private List<Vector2D> collectLandTileCenters() {
        List<Vector2D> result = new ArrayList<>();
        int cols = (int) Math.ceil(WIDTH / TERRAIN_TILE_SIZE);
        int rows = (int) Math.ceil(HEIGHT / TERRAIN_TILE_SIZE);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Vector2D center = new Vector2D(
                        (c + 0.5) * TERRAIN_TILE_SIZE,
                        (r + 0.5) * TERRAIN_TILE_SIZE);
                if (worldMap.getTerrainAt(center) != TerrainType.LAND) {
                    continue;
                }
                if (hasNearbyHazard(center)) {
                    continue;
                }
                result.add(center);
            }
        }
        return result;
    }

    /** Đúng nếu trong vòng 1 ô (8 hướng) có ROCK/WATER/PIT — dùng để cỏ tránh mọc sát đá/sông. */
    private boolean hasNearbyHazard(Vector2D center) {
        int[][] offsets = {
                {-1, -1}, {-1, 0}, {-1, 1},
                {0, -1},           {0, 1},
                {1, -1},  {1, 0},  {1, 1},
        };
        for (int[] o : offsets) {
            Vector2D probe = new Vector2D(
                    center.x + o[1] * TERRAIN_TILE_SIZE,
                    center.y + o[0] * TERRAIN_TILE_SIZE);
            TerrainType t = worldMap.getTerrainAt(probe);
            if (t == TerrainType.ROCK || t == TerrainType.WATER || t == TerrainType.PIT) {
                return true;
            }
        }
        return false;
    }

    private void spawnDens(SpawnKind kind, double[][] centers, int perDen, double radius) {
        for (double[] center : centers) {
            spawnDen(kind, new Vector2D(center[0], center[1]), perDen, radius);
        }
    }

    private void spawnDen(SpawnKind kind, Vector2D center, int count, double radius) {
        int placed = 0;
        int attempts = 0;
        int maxAttempts = count * DEN_PLACEMENT_MAX_ATTEMPTS;
        while (placed < count && attempts < maxAttempts) {
            attempts++;
            // Phân phối uniform trong hình tròn quanh tâm ổ.
            double angle = spawnRandom.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(spawnRandom.nextDouble()) * radius;
            double x = center.x + r * Math.cos(angle);
            double y = center.y + r * Math.sin(angle);
            Vector2D position = new Vector2D(x, y);
            LivingEntity entity = createLivingByKind(kind, position);
            if (entity == null || !worldMap.canStandOn(entity, position)) {
                continue;
            }
            worldMap.addEntity(entity);
            placed++;
        }
    }

    private void spawnAt(SpawnKind kind, Vector2D position) {
        if (kind == null || position == null) {
            return;
        }

        switch (kind) {
            case RABBIT:
            case WOLF:
            case BEAR:
            case ELEPHANT:
            case FISH:
                spawnLiving(kind, position);
                break;
            case GRASS:
                spawnGrass(position);
                break;
            case ALGAE:
                spawnAlgae(position);
                break;
            case BUSH:
                spawnTerrainObstacle(position, TerrainType.BUSH, new Bush(position));
                break;
            case ROCK:
                spawnTerrainObstacle(position, TerrainType.ROCK, new Rock(position));
                break;
            default:
                break;
        }
    }

    private void spawnLiving(SpawnKind kind, Vector2D position) {
        LivingEntity entity = createLivingByKind(kind, position);
        if (entity == null) {
            return;
        }
        if (!worldMap.canStandOn(entity, position)) {
            worldMap.notifyAction("Hệ thống", "không thể đặt", entity.getType() + " tại vị trí này");
            return;
        }
        addEntityAndLog(entity);
    }

    private void spawnGrass(Vector2D position) {
        if (worldMap.getTerrainAt(position) != TerrainType.LAND) {
            worldMap.notifyAction("Hệ thống", "không thể đặt", "Cỏ chỉ đặt trên đất (LAND)");
            return;
        }
        addEntityAndLog(new Grass(position));
    }

    private void spawnAlgae(Vector2D position) {
        if (worldMap.getTerrainAt(position) != TerrainType.WATER) {
            worldMap.notifyAction("Hệ thống", "không thể đặt", "Tảo chỉ đặt trên nước (WATER)");
            return;
        }
        addEntityAndLog(new Algae(position));
    }

    private void spawnTerrainObstacle(Vector2D position, TerrainType terrainType, Entity entity) {
        TerrainType current = worldMap.getTerrainAt(position);
        if (current == TerrainType.WATER || current == TerrainType.PIT) {
            worldMap.notifyAction("Hệ thống", "không thể đặt",
                    entity.getClass().getSimpleName() + " tại vị trí này");
            return;
        }
        if (!worldMap.setTerrainAt(position, terrainType)) {
            worldMap.notifyAction("Hệ thống", "không thể đặt",
                    entity.getClass().getSimpleName() + " ngoài bản đồ");
            return;
        }
        addEntityAndLog(entity);
    }

    private void addEntityAndLog(Entity entity) {
        worldMap.addEntity(entity);
        String name = entity.getClass().getSimpleName() + "#" + entity.getId();
        worldMap.notifyAction("Hệ thống", "đã thêm", name);
    }

    private LivingEntity createLivingByKind(SpawnKind kind, Vector2D position) {
        switch (kind) {
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

    private void applyZoom(double targetScale) {
        double clamped = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, targetScale));
        zoomAtPoint(clamped, worldMap.getScale(), CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2);
    }

    private void zoomAtPoint(double newScale, double oldScale, double cx, double cy) {
        if (oldScale == newScale) {
            return;
        }
        double ox = cx - (cx - worldMap.getOffsetX()) * (newScale / oldScale);
        double oy = cy - (cy - worldMap.getOffsetY()) * (newScale / oldScale);
        worldMap.setScale(newScale);
        worldMap.setOffset(ox, oy);
        syncZoomSlider(newScale);
        updateElephantSound();
    }

    private void syncZoomSlider(double scale) {
        if (zoomSlider == null) {
            return;
        }
        updatingZoomSlider = true;
        zoomSlider.setValue(scale);
        updatingZoomSlider = false;
    }

    private void updateWolfMatingSound() {
        if (isSoundZoomedIn() && hasWolfSearchingMate()) {
            playWolfMatingSound();
        } else {
            stopWolfMatingSound();
        }
    }

    private boolean hasWolfSearchingMate() {
        for (Entity entity : worldMap.getEntities()) {
            if (entity instanceof Wolf
                    && entity instanceof LivingEntity
                    && ((LivingEntity) entity).getMoveStrategy() instanceof MateStrategy) {
                return true;
            }
        }
        return false;
    }

    private void updateBearMatingSound() {
        if (isSoundZoomedIn() && hasBearSearchingMate()) {
            playBearMatingSound();
        } else {
            stopBearMatingSound();
        }
    }

    private boolean hasBearSearchingMate() {
        for (Entity entity : worldMap.getEntities()) {
            if (entity instanceof Bear
                    && entity instanceof LivingEntity
                    && ((LivingEntity) entity).getMoveStrategy() instanceof MateStrategy) {
                return true;
            }
        }
        return false;
    }

    private boolean isSoundZoomedIn() {
        return worldMap != null && worldMap.getScale() >= SOUND_ZOOM_THRESHOLD;
    }

    private void updateElephantSound() {
        if (isSoundZoomedIn() && hasElephant()) {
            playElephantSound();
        } else {
            stopElephantSound();
        }
    }

    private boolean hasElephant() {
        for (Entity entity : worldMap.getEntities()) {
            if (entity instanceof Elephant) {
                return true;
            }
        }
        return false;
    }

    private void updateFishSwimmingSound() {
        if (isSoundZoomedIn() && hasFishSwimmingInWater()) {
            playFishSwimmingSound();
        } else {
            stopFishSwimmingSound();
        }
    }

    private boolean hasFishSwimmingInWater() {
        if (worldMap == null) {
            return false;
        }

        for (Entity entity : worldMap.getEntities()) {
            if (entity instanceof Fish) {
                Fish fish = (Fish) entity;
                if (worldMap.getTerrainAt(fish.getPosition()) == TerrainType.WATER
                        && fish.getVelocity().magnitude() > 0.1) {
                    return true;
                }
            }
        }
        return false;
    }

    private void playWolfMatingSound() {
        Clip clip = getWolfMatingSoundClip();
        if (clip != null && !clip.isRunning()) {
            clip.setFramePosition(0);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    private void stopWolfMatingSound() {
        if (wolfMatingSoundClip != null && wolfMatingSoundClip.isRunning()) {
            wolfMatingSoundClip.stop();
        }
    }

    private Clip getWolfMatingSoundClip() {
        if (wolfMatingSoundClip != null) {
            return wolfMatingSoundClip;
        }

        try {
            var resource = getClass().getResource(WOLF_MATING_SOUND_RESOURCE_PATH);
            if (resource == null) {
                System.err.println("Wolf mating sound not found: " + WOLF_MATING_SOUND_RESOURCE_PATH);
                return null;
            }

            AudioInputStream originalStream = AudioSystem.getAudioInputStream(resource);
            AudioFormat originalFormat = originalStream.getFormat();
            AudioFormat playableFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    originalFormat.getSampleRate(),
                    16,
                    originalFormat.getChannels(),
                    originalFormat.getChannels() * 2,
                    originalFormat.getSampleRate(),
                    false);
            AudioInputStream playableStream = AudioSystem.getAudioInputStream(playableFormat, originalStream);

            wolfMatingSoundClip = AudioSystem.getClip();
            wolfMatingSoundClip.open(playableStream);
            return wolfMatingSoundClip;
        } catch (Exception e) {
            System.err.println("Cannot play wolf mating sound.");
            e.printStackTrace();
            return null;
        }
    }

    private void playBearMatingSound() {
        AudioClip clip = getBearMatingSoundClip();
        if (clip != null && !clip.isPlaying()) {
            clip.play();
        }
    }

    private void stopBearMatingSound() {
        if (bearMatingSoundClip != null && bearMatingSoundClip.isPlaying()) {
            bearMatingSoundClip.stop();
        }
    }

    private AudioClip getBearMatingSoundClip() {
        if (bearMatingSoundClip != null) {
            return bearMatingSoundClip;
        }

        try {
            var resource = getClass().getResource(BEAR_MATING_SOUND_RESOURCE_PATH);
            if (resource == null) {
                System.err.println("Bear mating sound not found: " + BEAR_MATING_SOUND_RESOURCE_PATH);
                return null;
            }
            bearMatingSoundClip = new AudioClip(resource.toExternalForm());
            bearMatingSoundClip.setCycleCount(AudioClip.INDEFINITE);
            bearMatingSoundClip.setVolume(0.8);
            return bearMatingSoundClip;
        } catch (Exception e) {
            System.err.println("Cannot play bear mating sound.");
            e.printStackTrace();
            return null;
        }
    }

    private void playWolfEatingSound() {
        AudioClip clip = getWolfEatingSoundClip();
        if (clip != null) {
            clip.play();
        }
    }

    private AudioClip getWolfEatingSoundClip() {
        if (wolfEatingSoundClip != null) {
            return wolfEatingSoundClip;
        }

        try {
            var resource = getClass().getResource(WOLF_EATING_SOUND_RESOURCE_PATH);
            if (resource == null) {
                System.err.println("Wolf eating sound not found: " + WOLF_EATING_SOUND_RESOURCE_PATH);
                return null;
            }
            wolfEatingSoundClip = new AudioClip(resource.toExternalForm());
            wolfEatingSoundClip.setVolume(0.85);
            return wolfEatingSoundClip;
        } catch (Exception e) {
            System.err.println("Cannot play wolf eating sound.");
            e.printStackTrace();
            return null;
        }
    }

    private void playElephantSound() {
        AudioClip clip = getElephantSoundClip();
        if (clip != null && !clip.isPlaying()) {
            clip.play();
        }
    }

    private void stopElephantSound() {
        if (elephantSoundClip != null && elephantSoundClip.isPlaying()) {
            elephantSoundClip.stop();
        }
    }

    private AudioClip getElephantSoundClip() {
        if (elephantSoundClip != null) {
            return elephantSoundClip;
        }

        try {
            var resource = getClass().getResource(ELEPHANT_SOUND_RESOURCE_PATH);
            if (resource == null) {
                System.err.println("Elephant sound not found: " + ELEPHANT_SOUND_RESOURCE_PATH);
                return null;
            }
            elephantSoundClip = new AudioClip(resource.toExternalForm());
            elephantSoundClip.setCycleCount(AudioClip.INDEFINITE);
            elephantSoundClip.setVolume(0.8);
            return elephantSoundClip;
        } catch (Exception e) {
            System.err.println("Cannot play elephant sound.");
            e.printStackTrace();
            return null;
        }
    }

    private void playFishSwimmingSound() {
        AudioClip clip = getFishSwimmingSoundClip();
        if (clip != null && !clip.isPlaying()) {
            clip.play();
        }
    }

    private void stopFishSwimmingSound() {
        if (fishSwimmingSoundClip != null && fishSwimmingSoundClip.isPlaying()) {
            fishSwimmingSoundClip.stop();
        }
    }

    private AudioClip getFishSwimmingSoundClip() {
        if (fishSwimmingSoundClip != null) {
            return fishSwimmingSoundClip;
        }

        try {
            var resource = getClass().getResource(FISH_SWIMMING_SOUND_RESOURCE_PATH);
            if (resource == null) {
                System.err.println("Fish swimming sound not found: " + FISH_SWIMMING_SOUND_RESOURCE_PATH);
                return null;
            }
            fishSwimmingSoundClip = new AudioClip(resource.toExternalForm());
            fishSwimmingSoundClip.setCycleCount(AudioClip.INDEFINITE);
            fishSwimmingSoundClip.setVolume(0.65);
            return fishSwimmingSoundClip;
        } catch (Exception e) {
            System.err.println("Cannot play fish swimming sound.");
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
