package org.openjfx.app.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.openjfx.app.core.strategies.FleeStrategy;
import org.openjfx.app.core.strategies.HunterStrategy;
import org.openjfx.app.core.strategies.WanderStrategy;
import org.openjfx.app.core.terrain.TerrainType;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;
import org.openjfx.app.entities.movable.Bear;
import org.openjfx.app.entities.movable.Elephant;
import org.openjfx.app.entities.movable.Fish;
import org.openjfx.app.entities.movable.Rabbit;
import org.openjfx.app.entities.movable.Wolf;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class WorldMap {

    public static class GridCoordinate {
        private final int row;
        private final int col;
        public GridCoordinate(int row, int col) { this.row = row; this.col = col; }
        public int getRow() { return row; }
        public int getCol() { return col; }
    }

    private final double width;
    private final double height;
    private final List<Entity> entities;
    private TmxObjectZones tmxObjectZones;
    private double objectGridTileSize = 32.0;
    private Image fixedBackgroundImage;
    private final List<GameObserver> observers = new ArrayList<>();

    private final Map<String, Image> imageCache = new HashMap<>();
    private final Map<Integer, String> rabbitDirectionCache = new HashMap<>();
    private final Map<Integer, String> wolfDirectionCache = new HashMap<>();
    private final Map<Integer, String> fishDirectionCache = new HashMap<>();
    private final Map<Integer, String> elephantDirectionCache = new HashMap<>();
    private final Map<Integer, String> bearDirectionCache = new HashMap<>();

    public WorldMap(double width, double height) {
        this.width = width;
        this.height = height;
        this.entities = new ArrayList<>();
    }

    public void addEntity(Entity entity) { entities.add(entity); }

    public void setObjectZonesFromTmxResource(String resourcePath, int tileSize) {
        this.tmxObjectZones = TmxObjectZones.fromResource(resourcePath);
        this.objectGridTileSize = tileSize;
    }

    public void setObjectZonesFromTmxResource(String resourcePath, int tileSize, double scaleX, double scaleY) {
        this.tmxObjectZones = TmxObjectZones.fromResource(resourcePath, scaleX, scaleY);
        this.objectGridTileSize = tileSize * Math.max(scaleX, scaleY);
    }

    public void setObjectZonesFromTmxFile(String absolutePath, int tileSize, double scaleX, double scaleY) {
        this.tmxObjectZones = TmxObjectZones.fromFile(absolutePath, scaleX, scaleY);
        this.objectGridTileSize = tileSize * Math.max(scaleX, scaleY);
    }

    public double getWidth() { return width; }
    public double getHeight() { return height; }

    public TerrainType getTerrainAt(Vector2D position) {
        if (tmxObjectZones != null) {
            if (tmxObjectZones.isObstacle(position)) return TerrainType.ROCK;
            if (tmxObjectZones.isWater(position))    return TerrainType.WATER;
            if (tmxObjectZones.isBush(position))     return TerrainType.BUSH;
        }
        return TerrainType.LAND;
    }

    public GridCoordinate worldToGrid(Vector2D position) {
        if (position == null) return null;
        int col = (int) (position.x / objectGridTileSize);
        int row = (int) (position.y / objectGridTileSize);
        return new GridCoordinate(row, col);
    }

    public Vector2D gridToWorldCenter(int row, int col) {
        if (!isGridInside(row, col)) return null;
        return new Vector2D((col + 0.5) * objectGridTileSize, (row + 0.5) * objectGridTileSize);
    }

    private boolean isGridInside(int row, int col) {
        return row >= 0 && row < getGridRows() && col >= 0 && col < getGridCols();
    }

    private int getGridRows() { return (int) Math.ceil(height / objectGridTileSize); }
    private int getGridCols() { return (int) Math.ceil(width / objectGridTileSize); }

    public Vector2D findNearestTerrainPosition(Vector2D from, TerrainType targetType) {
        if (tmxObjectZones == null || from == null || targetType == null) return null;
        if (targetType == TerrainType.WATER) return tmxObjectZones.findNearestWater(from);
        if (targetType == TerrainType.BUSH)  return tmxObjectZones.findNearestBush(from);
        return null;
    }

    public Vector2D findNearestTerrainPositionInRadius(Vector2D from, TerrainType targetType, double radius) {
        if (tmxObjectZones == null || from == null || targetType == null || radius <= 0) return null;
        if (targetType == TerrainType.WATER) return tmxObjectZones.findNearestWaterInRadius(from, radius);
        if (targetType == TerrainType.BUSH)  return tmxObjectZones.findNearestBushInRadius(from, radius);
        return null;
    }

    // A* pathfinding
    private class AstarNode implements Comparable<AstarNode> {
        int row, col;
        double g;
        double h;
        AstarNode parent;

        public AstarNode(int row, int col) {
            this.row = row;
            this.col = col;
            this.g = Double.MAX_VALUE;
        }

        public double getF() { return g + h; }

        @Override
        public int compareTo(AstarNode other) {
            return Double.compare(this.getF(), other.getF());
        }
    }

    public List<Vector2D> findPathAStar(LivingEntity entity, Vector2D start, Vector2D target) {
        return findPathAStar(entity, start, target, null);
    }

    public List<Vector2D> findPathAStar(LivingEntity entity, Vector2D start, Vector2D target, Set<String> avoidedGridKeys) {
        if (tmxObjectZones == null || entity == null || start == null || target == null) return null;

        GridCoordinate startGrid  = worldToGrid(start);
        GridCoordinate targetGrid = worldToGrid(target);
        if (startGrid == null || targetGrid == null) return null;

        int rows = getGridRows();
        int cols = getGridCols();
        int sRow = startGrid.getRow(),  sCol = startGrid.getCol();
        int tRow = targetGrid.getRow(), tCol = targetGrid.getCol();

        PriorityQueue<AstarNode> openSet = new PriorityQueue<>();
        boolean[][] visited  = new boolean[rows][cols];
        AstarNode[][] allNodes = new AstarNode[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                allNodes[r][c] = new AstarNode(r, c);

        AstarNode startNode  = allNodes[sRow][sCol];
        AstarNode targetNode = allNodes[tRow][tCol];
        startNode.g = 0;
        startNode.h = calculateHeuristic(startNode, targetNode);
        openSet.add(startNode);

        int[][] directions = {{-1,0},{1,0},{0,-1},{0,1},{-1,-1},{-1,1},{1,-1},{1,1}};

        while (!openSet.isEmpty()) {
            AstarNode current = openSet.poll();
            if (current.row == tRow && current.col == tCol) return buildPath(current);

            visited[current.row][current.col] = true;
            for (int[] dir : directions) {
                int nr = current.row + dir[0];
                int nc = current.col + dir[1];
                if (nr < 0 || nc < 0 || nr >= rows || nc >= cols) continue;
                if (visited[nr][nc]) continue;

                Vector2D center = gridToWorldCenter(nr, nc);
                if (center == null) continue;
                boolean isTarget = nr == tRow && nc == tCol;
                if (isTarget ? !canStandAtPoint(entity, center) : !canStandOn(entity, center)) continue;
                if (avoidedGridKeys != null && avoidedGridKeys.contains(gridKey(nr, nc))) continue;

                AstarNode next = allNodes[nr][nc];
                double cost = (dir[0] != 0 && dir[1] != 0) ? Math.sqrt(2) : 1.0;
                double newG = current.g + cost;
                if (newG < next.g) {
                    next.parent = current;
                    next.g = newG;
                    next.h = calculateHeuristic(next, targetNode);
                    openSet.remove(next);
                    openSet.add(next);
                }
            }
        }
        return null;
    }

    private String gridKey(int row, int col) { return row + ":" + col; }

    private List<Vector2D> buildPath(AstarNode node) {
        List<Vector2D> path = new ArrayList<>();
        for (AstarNode cur = node; cur != null; cur = cur.parent)
            path.add(gridToWorldCenter(cur.row, cur.col));
        Collections.reverse(path);
        if (!path.isEmpty()) path.remove(0);
        return path;
    }

    private double calculateHeuristic(AstarNode a, AstarNode b) {
        double dx = a.col - b.col;
        double dy = a.row - b.row;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public boolean canStandOn(LivingEntity entity, Vector2D position) {
        if (entity == null || position == null) return false;
        if (position.x < 0 || position.y < 0 || position.x > width || position.y > height) return false;
        if (collidesWithAnotherLivingEntity(entity, position)) return false;

        double radius   = Math.max(2.0, entity.getSize() * 0.35);
        double diagonal = radius * 0.7;
        Vector2D[] pts = {
            position,
            new Vector2D(position.x + radius,   position.y),
            new Vector2D(position.x - radius,   position.y),
            new Vector2D(position.x,             position.y + radius),
            new Vector2D(position.x,             position.y - radius),
            new Vector2D(position.x + diagonal,  position.y + diagonal),
            new Vector2D(position.x + diagonal,  position.y - diagonal),
            new Vector2D(position.x - diagonal,  position.y + diagonal),
            new Vector2D(position.x - diagonal,  position.y - diagonal)
        };
        for (Vector2D p : pts) {
            if (p.x < 0 || p.y < 0 || p.x > width || p.y > height) return false;
            if (!canEntityStandOnTerrain(entity.getType(), getTerrainAt(p))) return false;
        }
        return true;
    }

    public boolean canStandAtPoint(LivingEntity entity, Vector2D position) {
        return canEntityStandOnTerrain(entity.getType(), getTerrainAt(position));
    }

    private boolean canEntityStandOnTerrain(EntityType entityType, TerrainType terrain) {
        if (terrain == TerrainType.ROCK)  return false;
        if (terrain == TerrainType.WATER) return entityType == EntityType.FISH;
        if (terrain == TerrainType.BUSH)  return entityType == EntityType.RABBIT;
        // LAND: tất cả trừ cá
        return entityType != EntityType.FISH;
    }

    private boolean collidesWithAnotherLivingEntity(LivingEntity movingEntity, Vector2D nextPosition) {
        double movingRadius = Math.max(4.0, movingEntity.getSize() * 0.35);
        for (Entity other : entities) {
            if (other == movingEntity || !(other instanceof LivingEntity)) continue;
            LivingEntity otherLiving = (LivingEntity) other;
            if (!otherLiving.isAlive()) continue;
            double otherRadius  = Math.max(4.0, other.getSize() * 0.35);
            double minDistance  = movingRadius + otherRadius;
            if (RelationManager.isPrey(other.getType(), movingEntity.getType())) minDistance *= 0.35;
            double currentDist = movingEntity.getPosition().distance(other.getPosition());
            double nextDist    = nextPosition.distance(other.getPosition());
            if (currentDist < minDistance && nextDist >= currentDist) continue;
            if (nextDist < minDistance) return true;
        }
        return false;
    }

    public double getInteractionDistance(LivingEntity actor, Entity target) {
        if (actor == null || target == null) return 5.0;
        return Math.max(8.0, (actor.getSize() + target.getSize()) * 0.28);
    }

    public void update(double dt) {
        for (int i = entities.size() - 1; i >= 0; i--) {
            Entity e = entities.get(i);
            e.update(dt, this);
            if (e instanceof LivingEntity && !((LivingEntity) e).isAlive()) entities.remove(i);
        }
    }

    public Entity getEntityById(int id) {
        for (Entity e : entities) if (e.getId() == id) return e;
        return null;
    }

    public List<Entity> getEntities() { return Collections.unmodifiableList(entities); }

    public List<Entity> getNeighbors(Entity owner, double radius) {
        List<Entity> result = new ArrayList<>();
        for (Entity e : entities)
            if (e != owner && owner.getPosition().distance(e.getPosition()) <= radius) result.add(e);
        return result;
    }

    public void addObserver(GameObserver observer) { observers.add(observer); }

    public void notifyAction(String actor, String action, String target) {
        for (GameObserver obs : observers) obs.onActionOccurred(actor, action, target);
    }

    public void broadcastDeath(String message) {
        for (GameObserver obs : observers) obs.onEntityDeath(message);
    }

    // --- Rendering ---

    private double scale   = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;

    public void setScale(double scale) { this.scale = Math.max(1.0, Math.min(3.0, scale)); }
    public double getScale() { return scale; }

    public void setOffset(double x, double y) {
        double sw = width * scale, sh = height * scale;
        offsetX = sw > width  ? Math.min(0, Math.max(x, width  - sw)) : (width  - sw) / 2;
        offsetY = sh > height ? Math.min(0, Math.max(y, height - sh)) : (height - sh) / 2;
    }
    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }

    public void setFixedBackgroundImageFromResource(String resourcePath) {
        try {
            Image image = new Image(getClass().getResourceAsStream(resourcePath));
            fixedBackgroundImage = image.isError() ? null : image;
        } catch (Exception e) { fixedBackgroundImage = null; }
    }

    public void setFixedBackgroundImageFromFile(String absoluteFilePath) {
        try {
            Image image = new Image("file:" + absoluteFilePath);
            fixedBackgroundImage = image.isError() ? null : image;
        } catch (Exception e) { fixedBackgroundImage = null; }
    }

    public void render(GraphicsContext gc) {
        gc.save();
        gc.translate(offsetX, offsetY);
        gc.scale(scale, scale);

        if (fixedBackgroundImage != null) {
            gc.drawImage(fixedBackgroundImage, 0, 0, width, height);
        } else {
            drawGrassBackground(gc);
        }

        for (Entity entity : entities) {
            renderVisionRadius(gc, entity);
            renderEntityWithImage(gc, entity);
            renderWanderDebug(gc, entity);
            renderAStarPathDebug(gc, entity);
        }
        gc.restore();
    }

    private void drawGrassBackground(GraphicsContext gc) {
        int tileSize = 40;
        for (int x = 0; x < width; x += tileSize) {
            for (int y = 0; y < height; y += tileSize) {
                gc.setFill(Color.web((x / tileSize + y / tileSize) % 2 == 0 ? "#90EE90" : "#85e085"));
                gc.fillRect(x, y, tileSize, tileSize);
                gc.setFill(Color.web("#77cc77"));
                gc.fillOval(x + 10, y + 10, 2, 2);
            }
        }
    }

    private void renderVisionRadius(GraphicsContext gc, Entity entity) {
        if (!(entity instanceof LivingEntity)) return;
        LivingEntity le = (LivingEntity) entity;
        if (le.getVisionRadius() <= 0) return;
        double r = le.getVisionRadius();
        gc.save();
        gc.setLineWidth(1.2);
        gc.setStroke(Color.web("#66E0FF", 0.55));
        gc.setFill(Color.web("#66E0FF", 0.08));
        gc.fillOval(le.getPosition().x - r, le.getPosition().y - r, r * 2, r * 2);
        gc.strokeOval(le.getPosition().x - r, le.getPosition().y - r, r * 2, r * 2);
        gc.restore();
    }

    private void renderEntityWithImage(GraphicsContext gc, Entity entity) {
        if (entity instanceof Rabbit)   { renderRabbitWithAnimation(gc, (Rabbit) entity);     return; }
        if (entity instanceof Wolf)     { renderWolfWithAnimation(gc, (Wolf) entity);           return; }
        if (entity instanceof Fish)     { renderFishWithAnimation(gc, (Fish) entity);           return; }
        if (entity instanceof Elephant) { renderElephantWithAnimation(gc, (Elephant) entity);  return; }
        if (entity instanceof Bear)     { renderBearWithAnimation(gc, (Bear) entity);           return; }

        String imagePath = entity.toString().split("\\{")[0];
        try {
            renderImageAtEntity(gc, entity, imagePath);
        } catch (Exception e) {
            gc.setFill(Color.RED);
            gc.fillOval(entity.getPosition().x - 5, entity.getPosition().y - 5, 10, 10);
        }
    }

    private void renderRabbitWithAnimation(GraphicsContext gc, Rabbit r) {
        String dir = getDirection(r.getId(), r.getVelocity(), rabbitDirectionCache, "right");
        renderImageAtEntity(gc, r, "org/openjfx/app/rabbit_walk/rabbit_" + dir + "_" + getWalkFrame(r.getId(), r.getVelocity()) + ".png");
    }

    private void renderWolfWithAnimation(GraphicsContext gc, Wolf w) {
        String dir = getDirection(w.getId(), w.getVelocity(), wolfDirectionCache, "right");
        renderImageAtEntity(gc, w, "org/openjfx/app/wolf_walk/wolf_" + dir + "_" + getWalkFrame(w.getId(), w.getVelocity()) + ".png");
    }

    private void renderFishWithAnimation(GraphicsContext gc, Fish f) {
        String dir = getDirection(f.getId(), f.getVelocity(), fishDirectionCache, "right");
        renderImageAtEntity(gc, f, "org/openjfx/app/fish_swim/fish_" + dir + "_" + getWalkFrame(f.getId(), f.getVelocity()) + ".png");
    }

    private void renderElephantWithAnimation(GraphicsContext gc, Elephant e) {
        String dir = getDirection(e.getId(), e.getVelocity(), elephantDirectionCache, "right");
        renderImageAtEntity(gc, e, "org/openjfx/app/elephant_walk/elephant_" + dir + "_" + getWalkFrame(e.getId(), e.getVelocity()) + ".png");
    }

    private void renderBearWithAnimation(GraphicsContext gc, Bear b) {
        String dir = getDirection(b.getId(), b.getVelocity(), bearDirectionCache, "right");
        renderImageAtEntity(gc, b, "org/openjfx/app/bear_walk/bear_" + dir + "_" + getWalkFrame(b.getId(), b.getVelocity()) + ".png");
    }

    private String getDirection(int id, Vector2D vel, Map<Integer, String> cache, String def) {
        if (vel == null || vel.magnitude() < 0.5) return cache.getOrDefault(id, def);
        String dir = Math.abs(vel.x) >= Math.abs(vel.y)
            ? (vel.x >= 0 ? "right" : "left")
            : (vel.y >= 0 ? "down"  : "up");
        cache.put(id, dir);
        return dir;
    }

    private int getWalkFrame(int id, Vector2D vel) {
        if (vel == null || vel.magnitude() < 0.5) return 0;
        return (int) ((System.nanoTime() / 120_000_000L + id) % 4);
    }

    private void renderImageAtEntity(GraphicsContext gc, Entity entity, String imagePath) {
        try {
            Image img = imageCache.computeIfAbsent(imagePath,
                k -> new Image(getClass().getResourceAsStream("/" + k)));
            double x = entity.getPosition().x - entity.getSize() / 2;
            double y = entity.getPosition().y - entity.getSize() / 2;
            gc.drawImage(img, x, y, entity.getSize(), entity.getSize());
        } catch (Exception e) {
            gc.setFill(Color.RED);
            gc.fillOval(entity.getPosition().x - 5, entity.getPosition().y - 5, 10, 10);
        }
    }

    private void renderWanderDebug(GraphicsContext gc, Entity entity) {
        WanderStrategy.DebugWanderState s = WanderStrategy.getDebugState(entity.getId());
        if (s == null) return;
        double r = s.getWanderRadius();
        gc.save();
        gc.setLineWidth(1.5);
        gc.setStroke(Color.ORANGE);
        gc.strokeOval(s.getCircleCenter().x - r, s.getCircleCenter().y - r, r * 2, r * 2);
        gc.setStroke(Color.YELLOW);
        gc.strokeOval(s.getRandomPoint().x - 5, s.getRandomPoint().y - 5, 10, 10);
        gc.setFill(Color.YELLOW);
        gc.fillOval(s.getRandomPoint().x - 2, s.getRandomPoint().y - 2, 4, 4);
        gc.restore();
    }

    private void renderAStarPathDebug(GraphicsContext gc, Entity entity) {
        List<Vector2D> path = null;
        HunterStrategy.DebugPathState hp = HunterStrategy.getDebugPathState(entity.getId());
        if (hp != null) {
            path = hp.getPath();
        } else {
            FleeStrategy.DebugPathState fp = FleeStrategy.getDebugPathState(entity.getId());
            if (fp != null) path = fp.getPath();
        }
        if (path == null || path.size() < 2) return;

        gc.save();
        gc.setLineWidth(2.0);
        gc.setStroke(Color.web("#00D4FF", 0.85));
        gc.setFill(Color.web("#00D4FF", 0.85));
        Vector2D prev = null;
        for (Vector2D pt : path) {
            if (pt == null) continue;
            if (prev != null) gc.strokeLine(prev.x, prev.y, pt.x, pt.y);
            gc.fillOval(pt.x - 2.5, pt.y - 2.5, 5, 5);
            prev = pt;
        }
        gc.restore();
    }
}
