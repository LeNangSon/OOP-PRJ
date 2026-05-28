package org.openjfx.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
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
import javafx.stage.Stage;

public class MainApp extends Application {
    private enum SpawnKind {
        RABBIT, WOLF, BEAR, ELEPHANT, FISH,
        GRASS, ALGAE, BUSH, ROCK
    }

    private static final double WIDTH  = 1032;
    private static final double HEIGHT = 576;
    private static final double MIN_ZOOM = 1.0;
    private static final double MAX_ZOOM = 3.0;
    private static final String FIXED_MAP_RESOURCE_PATH    = "/org/openjfx/app/map-final.png";
    private static final String TERRAIN_CSV_RESOURCE_PATH  = "/org/openjfx/app/terrain.csv";
    private static final int    TERRAIN_TILE_SIZE          = 24;

    private static final double[][] WOLF_DEN_CENTERS     = {{372, 60}, {828, 60}};
    private static final int        WOLF_PER_DEN         = 9;
    private static final double     WOLF_DEN_RADIUS      = 55.0;

    private static final double[][] RABBIT_DEN_CENTERS   = {
        {84, 108}, {588, 108}, {972, 204}, {756, 252},
        {324, 276}, {60, 324}, {540, 324}, {876, 420},
    };
    private static final int        RABBIT_PER_DEN       = 6;
    private static final double     RABBIT_DEN_RADIUS    = 40.0;

    private static final double[][] BEAR_DEN_CENTERS     = {
        {252, 60}, {948, 60}, {708, 84}, {468, 132},
        {612, 228}, {876, 276}, {204, 324}, {972, 348}, {708, 372},
    };
    private static final int        BEAR_PER_DEN         = 2;
    private static final double     BEAR_DEN_RADIUS      = 30.0;

    private static final double[][] ELEPHANT_DEN_CENTERS = {{396, 420}};
    private static final int        ELEPHANT_PER_DEN     = 12;
    private static final double     ELEPHANT_DEN_RADIUS  = 70.0;

    private static final int    DEN_PLACEMENT_MAX_ATTEMPTS = 60;
    private static final int    INITIAL_GRASS_COUNT        = 300;

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final String BG_DARK        = "#1a1a2e";
    private static final String BG_PANEL       = "#16213e";
    private static final String BG_TOOLBAR     = "#0f3460";
    private static final String ACCENT_ANIMAL  = "#e94560";
    private static final String ACCENT_PLANT   = "#1a936f";
    private static final String ACCENT_ZOOM    = "#533483";
    private static final String TEXT_MAIN      = "#eaeaea";
    private static final String TEXT_MUTED     = "#8892a4";

    // ── Button styles ─────────────────────────────────────────────────────────
    private static final String BTN_ANIMAL =
        "-fx-background-color: #22223b; -fx-text-fill: " + TEXT_MAIN + "; "
        + "-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 5 11; "
        + "-fx-background-radius: 5;";
    private static final String BTN_ANIMAL_ON =
        "-fx-background-color: " + ACCENT_ANIMAL + "; -fx-text-fill: white; "
        + "-fx-font-size: 11px; -fx-cursor: crosshair; -fx-padding: 5 11; "
        + "-fx-background-radius: 5; -fx-font-weight: bold;";
    private static final String BTN_PLANT =
        "-fx-background-color: #1b3a2f; -fx-text-fill: #a8e6cf; "
        + "-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 5 11; "
        + "-fx-background-radius: 5;";
    private static final String BTN_PLANT_ON =
        "-fx-background-color: " + ACCENT_PLANT + "; -fx-text-fill: white; "
        + "-fx-font-size: 11px; -fx-cursor: crosshair; -fx-padding: 5 11; "
        + "-fx-background-radius: 5; -fx-font-weight: bold;";
    private static final String BTN_ZOOM =
        "-fx-background-color: #2e2040; -fx-text-fill: white; "
        + "-fx-font-size: 13px; -fx-cursor: hand; -fx-font-weight: bold; "
        + "-fx-min-width: 28; -fx-padding: 3 7; -fx-background-radius: 4;";
    private static final String SECTION_LABEL_STYLE =
        "-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 9px; "
        + "-fx-font-weight: bold; -fx-padding: 0 0 3 1;";

    // ── State ─────────────────────────────────────────────────────────────────
    private WorldMap worldMap;
    private Canvas   canvas;
    private SpawnKind pendingSpawnKind;
    private ToggleGroup spawnGroup;
    private Slider  zoomSlider;
    private boolean updatingZoomSlider;
    private double  simulationSpeed = 1.0;
    private boolean paused          = false;
    private ToggleButton logTabBtn;
    private ToggleButton statsTabBtn;
    private final Random spawnRandom = new Random();

    @Override
    public void start(Stage stage) {
        worldMap = new WorldMap(WIDTH, HEIGHT);
        worldMap.setFixedBackgroundImageFromResource(FIXED_MAP_RESOURCE_PATH);
        worldMap.setTerrainGridFromCsvResource(TERRAIN_CSV_RESOURCE_PATH, TERRAIN_TILE_SIZE);
        seedInitialAnimals();

        // ── Log panel ─────────────────────────────────────────────────────────
        ObservableList<String> logData = FXCollections.observableArrayList();
        ListView<String> listView = new ListView<>(logData);
        listView.setFocusTraversable(false);
        listView.setStyle(
            "-fx-control-inner-background: " + BG_DARK + "; "
            + "-fx-font-family: 'Consolas', 'Monospaced'; "
            + "-fx-font-size: 12px; "
            + "-fx-border-color: transparent;");
        VBox.setVgrow(listView, Priority.ALWAYS);

        worldMap.addObserver(new TerminalLogger(logData));
        worldMap.notifyAction("Hệ thống", "đã khởi tạo",
            "48 thỏ (8 ổ), 18 sói (2 ổ), 18 gấu (9 ổ), 12 voi (1 ổ), "
            + INITIAL_GRASS_COUNT + " cỏ");

        // ── Stats panel ───────────────────────────────────────────────────────
        EntityStatusPanel entityStatusPanel = new EntityStatusPanel(worldMap);
        entityStatusPanel.setVisible(false);
        entityStatusPanel.setManaged(false);
        VBox.setVgrow(entityStatusPanel, Priority.ALWAYS);

        // ── Canvas ────────────────────────────────────────────────────────────
        canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        canvas.setFocusTraversable(true);
        setupCanvasInput();

        // ── Animation loop ────────────────────────────────────────────────────
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                gc.clearRect(0, 0, WIDTH, HEIGHT);
                if (!paused) {
                    worldMap.update(0.016 * simulationSpeed);
                }
                worldMap.render(gc);
                if (entityStatusPanel.isVisible()) {
                    entityStatusPanel.refreshData();
                }
            }
        };
        timer.start();

        // ── Layout ────────────────────────────────────────────────────────────
        HBox toolbar   = buildToolbar();
        VBox mapColumn = new VBox(toolbar, canvas);
        VBox.setVgrow(canvas, Priority.ALWAYS);

        VBox sidePanel = buildSidePanel(listView, entityStatusPanel);
        HBox root = new HBox(mapColumn, sidePanel);

        Scene scene = new Scene(root, WIDTH + 400, HEIGHT + 52);
        scene.setFill(javafx.scene.paint.Color.web(BG_DARK));

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.TAB) {
                if (logTabBtn.isSelected()) statsTabBtn.setSelected(true);
                else logTabBtn.setSelected(true);
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                clearPlacementMode();
                event.consume();
            } else if (event.getCode() == KeyCode.SPACE) {
                paused = !paused;
                event.consume();
            }
        });

        stage.setScene(scene);
        stage.setTitle("Ecology Simulation");
        stage.show();
        canvas.requestFocus();
    }

    // ── Side panel with tabs ──────────────────────────────────────────────────
    private VBox buildSidePanel(ListView<String> listView, EntityStatusPanel statsPanel) {
        String tabActive =
            "-fx-background-color: " + BG_PANEL + "; -fx-text-fill: " + TEXT_MAIN + "; "
            + "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 7 0; "
            + "-fx-cursor: hand; -fx-background-radius: 0; "
            + "-fx-border-color: transparent transparent " + ACCENT_ANIMAL + " transparent; "
            + "-fx-border-width: 0 0 2 0;";
        String tabInactive =
            "-fx-background-color: " + BG_DARK + "; -fx-text-fill: " + TEXT_MUTED + "; "
            + "-fx-font-size: 12px; -fx-padding: 7 0; "
            + "-fx-cursor: hand; -fx-background-radius: 0;";

        logTabBtn   = new ToggleButton("📋  Nhật ký");
        statsTabBtn = new ToggleButton("📊  Thống kê");
        ToggleGroup panelGroup = new ToggleGroup();
        logTabBtn.setToggleGroup(panelGroup);
        statsTabBtn.setToggleGroup(panelGroup);
        logTabBtn.setSelected(true);
        logTabBtn.setMaxWidth(Double.MAX_VALUE);
        statsTabBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(logTabBtn, Priority.ALWAYS);
        HBox.setHgrow(statsTabBtn, Priority.ALWAYS);

        logTabBtn.setStyle(tabActive);
        statsTabBtn.setStyle(tabInactive);

        logTabBtn.selectedProperty().addListener((obs, was, now) -> {
            if (!now) return;
            logTabBtn.setStyle(tabActive);
            statsTabBtn.setStyle(tabInactive);
            listView.setVisible(true);
            listView.setManaged(true);
            statsPanel.setVisible(false);
            statsPanel.setManaged(false);
        });
        statsTabBtn.selectedProperty().addListener((obs, was, now) -> {
            if (!now) return;
            statsTabBtn.setStyle(tabActive);
            logTabBtn.setStyle(tabInactive);
            statsPanel.setVisible(true);
            statsPanel.setManaged(true);
            listView.setVisible(false);
            listView.setManaged(false);
            statsPanel.refreshData();
        });

        HBox tabBar = new HBox(logTabBtn, statsTabBtn);
        tabBar.setStyle("-fx-background-color: " + BG_DARK + "; "
            + "-fx-border-color: #2d2d4e; -fx-border-width: 0 0 1 0;");

        VBox panel = new VBox(tabBar, listView, statsPanel);
        panel.setPrefWidth(400);
        panel.setStyle("-fx-background-color: " + BG_DARK + ";");
        return panel;
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────
    private HBox buildToolbar() {
        spawnGroup = new ToggleGroup();

        // Animal buttons
        VBox animalSection = makeSection("🐾  ĐỘNG VẬT",
            new HBox(4,
                createSpawnToggle("🐰 Thỏ",   SpawnKind.RABBIT,   BTN_ANIMAL, BTN_ANIMAL_ON),
                createSpawnToggle("🐺 Sói",   SpawnKind.WOLF,     BTN_ANIMAL, BTN_ANIMAL_ON),
                createSpawnToggle("🐻 Gấu",   SpawnKind.BEAR,     BTN_ANIMAL, BTN_ANIMAL_ON),
                createSpawnToggle("🐘 Voi",   SpawnKind.ELEPHANT, BTN_ANIMAL, BTN_ANIMAL_ON),
                createSpawnToggle("🐟 Cá",    SpawnKind.FISH,     BTN_ANIMAL, BTN_ANIMAL_ON)
            ));

        // Plant buttons
        VBox plantSection = makeSection("🌿  THỰC VẬT",
            new HBox(4,
                createSpawnToggle("🌾 Cỏ",   SpawnKind.GRASS, BTN_PLANT, BTN_PLANT_ON),
                createSpawnToggle("🦠 Tảo",  SpawnKind.ALGAE, BTN_PLANT, BTN_PLANT_ON),
                createSpawnToggle("🌳 Bụi",  SpawnKind.BUSH,  BTN_PLANT, BTN_PLANT_ON),
                createSpawnToggle("🪨 Đá",   SpawnKind.ROCK,  BTN_PLANT, BTN_PLANT_ON)
            ));

        // Speed control
        Label speedValLabel = new Label("×1.0");
        speedValLabel.setStyle("-fx-text-fill: " + TEXT_MAIN + "; -fx-font-size: 11px; -fx-min-width: 30;");

        Slider speedSlider = new Slider(0.25, 3.0, 1.0);
        speedSlider.setPrefWidth(90);
        speedSlider.setMajorTickUnit(0.75);
        speedSlider.setSnapToTicks(false);
        speedSlider.setTooltip(new Tooltip("Tốc độ mô phỏng"));
        speedSlider.setStyle("-fx-control-inner-background: #2e2040;");
        speedSlider.valueProperty().addListener((obs, o, n) -> {
            simulationSpeed = n.doubleValue();
            speedValLabel.setText(String.format("×%.2g", simulationSpeed));
        });

        ToggleButton pauseBtn = new ToggleButton("⏸");
        pauseBtn.setStyle(BTN_ZOOM);
        pauseBtn.setTooltip(new Tooltip("Dừng/Tiếp (Space)"));
        pauseBtn.selectedProperty().addListener((obs, was, now) -> {
            paused = now;
            pauseBtn.setText(now ? "▶" : "⏸");
        });

        VBox speedSection = makeSection("⏱  TỐC ĐỘ",
            new HBox(5, pauseBtn, speedSlider, speedValLabel));

        // Zoom control
        zoomSlider = new Slider(MIN_ZOOM, MAX_ZOOM, MIN_ZOOM);
        zoomSlider.setPrefWidth(100);
        zoomSlider.setTooltip(new Tooltip("Phóng to / thu nhỏ"));
        zoomSlider.setStyle("-fx-control-inner-background: #2e2040;");
        zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (updatingZoomSlider) return;
            zoomAtPoint(newVal.doubleValue(), worldMap.getScale(), WIDTH / 2, HEIGHT / 2);
        });

        Button btnMinus = makeZoomBtn("−");
        Button btnPlus  = makeZoomBtn("+");
        Button btnReset = new Button("↺");
        btnReset.setStyle(BTN_ZOOM + "-fx-text-fill: #ffaaaa;");
        btnReset.setTooltip(new Tooltip("Đặt lại zoom"));

        btnPlus.setOnAction(e  -> applyZoom(worldMap.getScale() + 0.1));
        btnMinus.setOnAction(e -> applyZoom(worldMap.getScale() - 0.1));
        btnReset.setOnAction(e -> {
            worldMap.setScale(MIN_ZOOM);
            worldMap.setOffset(0, 0);
            syncZoomSlider(MIN_ZOOM);
        });

        VBox zoomSection = makeSection("🔍  ZOOM",
            new HBox(5, btnMinus, zoomSlider, btnPlus, btnReset));

        // Assemble toolbar
        Separator sep1 = new Separator(Orientation.VERTICAL);
        Separator sep2 = new Separator(Orientation.VERTICAL);
        Separator sep3 = new Separator(Orientation.VERTICAL);
        sep1.setStyle("-fx-background-color: #2d2d4e;");
        sep2.setStyle("-fx-background-color: #2d2d4e;");
        sep3.setStyle("-fx-background-color: #2d2d4e;");

        HBox rightGroup = new HBox(12, speedSection, sep3, zoomSection);
        rightGroup.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(rightGroup, Priority.ALWAYS);
        rightGroup.setMaxWidth(Double.MAX_VALUE);

        HBox toolbar = new HBox(14, animalSection, sep1, plantSection, sep2, rightGroup);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: " + BG_TOOLBAR + "; -fx-padding: 8 14;");
        return toolbar;
    }

    private VBox makeSection(String title, HBox buttons) {
        Label label = new Label(title);
        label.setStyle(SECTION_LABEL_STYLE);
        buttons.setAlignment(Pos.CENTER_LEFT);
        VBox section = new VBox(3, label, buttons);
        section.setAlignment(Pos.CENTER_LEFT);
        return section;
    }

    private Button makeZoomBtn(String text) {
        Button btn = new Button(text);
        btn.setStyle(BTN_ZOOM);
        return btn;
    }

    private ToggleButton createSpawnToggle(String label, SpawnKind kind,
                                           String normalStyle, String selectedStyle) {
        ToggleButton btn = new ToggleButton(label);
        btn.setToggleGroup(spawnGroup);
        btn.setStyle(normalStyle);
        btn.setTooltip(new Tooltip("Click trên bản đồ để đặt " + label.replaceAll("[^\\p{L} ]", "").trim()));
        btn.selectedProperty().addListener((obs, was, now) -> {
            btn.setStyle(now ? selectedStyle : normalStyle);
            if (now) {
                pendingSpawnKind = kind;
                canvas.setCursor(Cursor.CROSSHAIR);
            } else if (pendingSpawnKind == kind) {
                pendingSpawnKind = null;
                canvas.setCursor(Cursor.DEFAULT);
            }
        });
        return btn;
    }

    // ── Canvas input ──────────────────────────────────────────────────────────
    private void setupCanvasInput() {
        final double[] mouseAnchor = new double[2];
        final double[] lastOffset  = new double[2];

        canvas.setOnMousePressed(e -> {
            mouseAnchor[0] = e.getX();
            mouseAnchor[1] = e.getY();
            lastOffset[0]  = worldMap.getOffsetX();
            lastOffset[1]  = worldMap.getOffsetY();
            canvas.requestFocus();
        });

        canvas.setOnMouseDragged(e -> {
            if (isPlacementMode() || worldMap.getScale() <= MIN_ZOOM) return;
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
                return;
            }
            double oldScale = worldMap.getScale();
            double newScale = oldScale;
            if (e.getButton() == MouseButton.PRIMARY)
                newScale = Math.min(MAX_ZOOM, oldScale + 0.2);
            else if (e.getButton() == MouseButton.SECONDARY)
                newScale = Math.max(MIN_ZOOM, oldScale - 0.2);
            if (newScale != oldScale)
                zoomAtPoint(newScale, oldScale, e.getX(), e.getY());
        });
    }

    private boolean isPlacementMode() { return pendingSpawnKind != null; }

    private void clearPlacementMode() {
        pendingSpawnKind = null;
        canvas.setCursor(Cursor.DEFAULT);
        if (spawnGroup != null) spawnGroup.selectToggle(null);
    }

    private Vector2D screenToWorld(double screenX, double screenY) {
        double scale = worldMap.getScale();
        return new Vector2D(
            (screenX - worldMap.getOffsetX()) / scale,
            (screenY - worldMap.getOffsetY()) / scale);
    }

    // ── Seeding ───────────────────────────────────────────────────────────────
    private void seedInitialAnimals() {
        spawnDens(SpawnKind.RABBIT,   RABBIT_DEN_CENTERS,   RABBIT_PER_DEN,   RABBIT_DEN_RADIUS);
        spawnDens(SpawnKind.WOLF,     WOLF_DEN_CENTERS,     WOLF_PER_DEN,     WOLF_DEN_RADIUS);
        spawnDens(SpawnKind.BEAR,     BEAR_DEN_CENTERS,     BEAR_PER_DEN,     BEAR_DEN_RADIUS);
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
        int cols = (int) Math.ceil(WIDTH  / TERRAIN_TILE_SIZE);
        int rows = (int) Math.ceil(HEIGHT / TERRAIN_TILE_SIZE);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Vector2D center = new Vector2D((c + 0.5) * TERRAIN_TILE_SIZE, (r + 0.5) * TERRAIN_TILE_SIZE);
                if (worldMap.getTerrainAt(center) == TerrainType.LAND) result.add(center);
            }
        }
        return result;
    }

    private void spawnDens(SpawnKind kind, double[][] centers, int perDen, double radius) {
        for (double[] center : centers)
            spawnDen(kind, new Vector2D(center[0], center[1]), perDen, radius);
    }

    private void spawnDen(SpawnKind kind, Vector2D center, int count, double radius) {
        int placed = 0, attempts = 0, maxAttempts = count * DEN_PLACEMENT_MAX_ATTEMPTS;
        while (placed < count && attempts < maxAttempts) {
            attempts++;
            double angle = spawnRandom.nextDouble() * Math.PI * 2;
            double r     = Math.sqrt(spawnRandom.nextDouble()) * radius;
            Vector2D position = new Vector2D(center.x + r * Math.cos(angle), center.y + r * Math.sin(angle));
            LivingEntity entity = createLivingByKind(kind, position);
            if (entity == null || !worldMap.canStandOn(entity, position)) continue;
            worldMap.addEntity(entity);
            placed++;
        }
    }

    // ── Spawn helpers ─────────────────────────────────────────────────────────
    private void spawnAt(SpawnKind kind, Vector2D position) {
        if (kind == null || position == null) return;
        switch (kind) {
            case RABBIT, WOLF, BEAR, ELEPHANT, FISH -> spawnLiving(kind, position);
            case GRASS  -> spawnGrass(position);
            case ALGAE  -> spawnAlgae(position);
            case BUSH   -> spawnTerrainObstacle(position, TerrainType.BUSH, new Bush(position));
            case ROCK   -> spawnTerrainObstacle(position, TerrainType.ROCK, new Rock(position));
        }
    }

    private void spawnLiving(SpawnKind kind, Vector2D position) {
        LivingEntity entity = createLivingByKind(kind, position);
        if (entity == null) return;
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
            worldMap.notifyAction("Hệ thống", "không thể đặt", entity.getClass().getSimpleName() + " tại vị trí này");
            return;
        }
        if (!worldMap.setTerrainAt(position, terrainType)) {
            worldMap.notifyAction("Hệ thống", "không thể đặt", entity.getClass().getSimpleName() + " ngoài bản đồ");
            return;
        }
        addEntityAndLog(entity);
    }

    private void addEntityAndLog(Entity entity) {
        worldMap.addEntity(entity);
        worldMap.notifyAction("Hệ thống", "đã thêm", entity.getClass().getSimpleName() + "#" + entity.getId());
    }

    private LivingEntity createLivingByKind(SpawnKind kind, Vector2D position) {
        return switch (kind) {
            case RABBIT   -> new Rabbit(position);
            case WOLF     -> new Wolf(position);
            case BEAR     -> new Bear(position);
            case ELEPHANT -> new Elephant(position);
            case FISH     -> new Fish(position);
            default       -> null;
        };
    }

    // ── Zoom ──────────────────────────────────────────────────────────────────
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

    public static void main(String[] args) { launch(args); }
}
