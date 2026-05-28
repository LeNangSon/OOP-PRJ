package org.openjfx.app;

/**
 * Tập trung toàn bộ thông số của các sinh vật.
 * Chỉnh ở đây, không cần mở từng class con vật.
 */
public final class EntityConfig {

    private EntityConfig() {}

    // ─── BASE (dùng chung) ────────────────────────────────────────────────────
    public static final double DRINK_RATE          = 20.0;  // thirst giảm /s khi uống
    public static final double STARVATION_DMG_RATE = 5.0;   // máu mất /s khi đói/khát 100

    // ─── RABBIT ───────────────────────────────────────────────────────────────
    // Speed phải < Bear và Wolf để carnivore có thể săn được.
    // Vision rộng để phản ứng sớm với kẻ thù và tìm cỏ từ xa.
    public static final class Rabbit {
        public static final double SIZE                 = 12.0;
        public static final double HEALTH               = 80.0;
        public static final double HUNGER_RATE          = 3.0;
        public static final double THIRST_RATE          = 4.0;
        public static final double MAX_SPEED            = 38.0;  // Bear(40)>Rabbit>Fish(30)
        public static final double MAX_FORCE            = 40.0;
        public static final double MASS                 = 1.5;
        public static final double WANDER_DISTANCE      = 10.0;
        public static final double WANDER_RADIUS        = 10.0;
        public static final double VISION_RADIUS        = 80.0;
        public static final double MATURE_AGE           = 1.5;
        public static final double REPRODUCE_COOLDOWN   = 5.0;
        public static final double REPRODUCE_HUNGER_COST = 10.0;
    }

    // ─── ELEPHANT ─────────────────────────────────────────────────────────────
    public static final class Elephant {
        public static final double SIZE                 = 40.0;
        public static final double HEALTH               = 350.0;
        public static final double HUNGER_RATE          = 6.0;
        public static final double THIRST_RATE          = 5.0;
        public static final double MAX_SPEED            = 18.0;
        public static final double MAX_FORCE            = 50.0;
        public static final double MASS                 = 12.0;
        public static final double WANDER_DISTANCE      = 30.0;
        public static final double WANDER_RADIUS        = 20.0;
        public static final double VISION_RADIUS        = 90.0;
        public static final double MATURE_AGE           = 20.0;
        public static final double REPRODUCE_COOLDOWN   = 60.0;
        public static final double REPRODUCE_HUNGER_COST = 40.0;
    }

    // ─── WOLF ─────────────────────────────────────────────────────────────────
    // Nhanh nhất map, săn Rabbit. Speed phải > Rabbit(38).
    public static final class Wolf {
        public static final double SIZE                 = 18.0;
        public static final double HEALTH               = 200.0;
        public static final double HUNGER_RATE          = 4.0;
        public static final double THIRST_RATE          = 5.0;
        public static final double MAX_SPEED            = 55.0;  // > Rabbit(38)
        public static final double MAX_FORCE            = 80.0;
        public static final double MASS                 = 3.0;
        public static final double WANDER_DISTANCE      = 30.0;
        public static final double WANDER_RADIUS        = 30.0;
        public static final double VISION_RADIUS        = 60.0;
        public static final double MATURE_AGE           = 10.0;
        public static final double REPRODUCE_COOLDOWN   = 30.0;
        public static final double REPRODUCE_HUNGER_COST = 35.0;
    }

    // ─── BEAR ─────────────────────────────────────────────────────────────────
    // Săn Rabbit và Fish. Speed phải > cả hai: Rabbit(38) và Fish(30).
    public static final class Bear {
        public static final double SIZE                 = 32.0;
        public static final double HEALTH               = 300.0;
        public static final double HUNGER_RATE          = 5.0;
        public static final double THIRST_RATE          = 4.0;
        public static final double MAX_SPEED            = 42.0;  // > Rabbit(38) > Fish(30)
        public static final double MAX_FORCE            = 60.0;
        public static final double MASS                 = 10.0;
        public static final double WANDER_DISTANCE      = 35.0;
        public static final double WANDER_RADIUS        = 35.0;
        public static final double VISION_RADIUS        = 70.0;
        public static final double MATURE_AGE           = 15.0;
        public static final double REPRODUCE_COOLDOWN   = 40.0;
        public static final double REPRODUCE_HUNGER_COST = 35.0;
    }

    // ─── FISH ─────────────────────────────────────────────────────────────────
    // Bị Bear(42) săn. Speed phải < Bear để Bear bắt được.
    public static final class Fish {
        public static final double SIZE                 = 8.0;
        public static final double HEALTH               = 60.0;
        public static final double HUNGER_RATE          = 0.5;
        public static final double THIRST_RATE          = 0.0;
        public static final double MAX_SPEED            = 30.0;  // < Bear(42)
        public static final double MAX_FORCE            = 3.0;
        public static final double MASS                 = 1.0;
        public static final double WANDER_DISTANCE      = 40.0;
        public static final double WANDER_RADIUS        = 25.0;
        public static final double VISION_RADIUS        = 20.0;
        public static final double MATURE_AGE           = 3.0;
        public static final double REPRODUCE_COOLDOWN   = 8.0;
        public static final double REPRODUCE_HUNGER_COST = 15.0;
    }
}
