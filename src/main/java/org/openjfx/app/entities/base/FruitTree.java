package org.openjfx.app.entities.base;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;

public class FruitTree extends StaticEntity {
    private boolean hasFruit;
    private double fruitRegrowTime = 20;
    private double fruitTimer = 0;

    private double reproduceTime = 60;
    private double reproduceTimer = 0;

    public FruitTree(Vector2D position) {
        super(position, 12, "FruitTree");
        hasFruit = true;
    }

    public boolean hasFruit() {
        return hasFruit;
    }

    public void harvestFruit() {
        if (hasFruit) {
            hasFruit = false;
            fruitTimer = 0;
        }
    }

    @Override
    public void update(double dt, WorldMap world) {
        // Mọc lại quả
        if (!hasFruit) {
            fruitTimer += dt;
            if (fruitTimer >= fruitRegrowTime) {
                hasFruit = true;
                fruitTimer = 0;
            }
        }

        // Sinh cây con
        reproduceTimer += dt;
        if (reproduceTimer >= reproduceTime) {
            double newX = position.x + (Math.random() * 100 - 50);
            double newY = position.y + (Math.random() * 100 - 50);
            world.addEntity(new FruitTree(new Vector2D(newX, newY)));
            reproduceTimer = 0;
        }
    }
}