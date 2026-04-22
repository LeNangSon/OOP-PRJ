package org.openjfx.app.core;

import javafx.application.Platform;
import javafx.collections.ObservableList;

/**
 * Lớp này triển khai GameObserver để nhận các sự kiện từ WorldMap
 * và hiển thị chúng lên ListView trong JavaFX.
 */
public class TerminalLogger implements GameObserver {
    private ObservableList<String> logData;

    public TerminalLogger(ObservableList<String> logData) {
        this.logData = logData;
    }

    @Override
    public void onEntityDeath(String message) {
        addLog("💀 [TỬ VONG] " + message);
    }

    @Override
    public void onActionOccurred(String actor, String action, String target) {
        addLog("⚔️ " + actor + " " + action + " " + target);
    }

    private void addLog(String msg) {
        // Đảm bảo cập nhật UI trên JavaFX Application Thread
        Platform.runLater(() -> {
            logData.add(0, msg); 
            if (logData.size() > 20) {
                logData.remove(logData.size() - 1);
            }
        });
    }
}