package org.openjfx.app.core;

import java.util.List;

import org.openjfx.app.entities.base.Entity;

public class World {
    // 1. Các thuộc tính cần thiết để quản lý thế giới
    private List<Entity> entities;
    private double width;
    private double height;

    public World(double width, double height) {
        // Khởi tạo danh sách và kích thước map ở đây
    }

    // 2. Phương thức thêm thực thể (Thỏ, Sói, Cỏ...) vào thế giới
    public void addEntity(Entity entity) {
        // Code thêm entity vào list
    }

    // 3. Vòng lặp chính của thế giới (Gọi mỗi khung hình)
    public void update(double dt) {
        
    }

    // 4. Hàm "Radar" - Cung cấp tầm nhìn cho AI
    public List<Entity> getNeighbors(Entity owner, double radius) {
        
        return null; // Thay bằng list kết quả
    }

    // 5. Hàm xử lý va chạm biên
    private void handleBounds(Entity e) {
        
    }

    // Getter để Renderer có thể lấy danh sách đi vẽ
    public List<Entity> getEntities() {
        return entities;
    }
}