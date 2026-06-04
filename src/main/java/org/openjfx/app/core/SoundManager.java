package org.openjfx.app.core;

import javafx.scene.media.AudioClip;

public final class SoundManager {
    private static final String EAT_MEAT_SOUND = "/sounds/anthit.mp3";
    private static final String FISH_SWIM_SOUND = "/sounds/ca.wav";

    private static AudioClip eatMeatClip;
    private static AudioClip fishSwimClip;

    private SoundManager() {
    }

    public static void playEatMeat() {
        AudioClip clip = getEatMeatClip();
        if (clip != null) {
            clip.play();
        }
    }

    public static void setFishSwimming(boolean swimming) {
        AudioClip clip = getFishSwimClip();
        if (clip == null) return;
        if (swimming) {
            if (!clip.isPlaying()) {
                clip.play();
            }
        } else if (clip.isPlaying()) {
            clip.stop();
        }
    }

    private static AudioClip getEatMeatClip() {
        if (eatMeatClip != null) return eatMeatClip;
        try {
            var resource = SoundManager.class.getResource(EAT_MEAT_SOUND);
            if (resource == null) return null;
            eatMeatClip = new AudioClip(resource.toExternalForm());
            return eatMeatClip;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static AudioClip getFishSwimClip() {
        if (fishSwimClip != null) return fishSwimClip;
        try {
            var resource = SoundManager.class.getResource(FISH_SWIM_SOUND);
            if (resource == null) return null;
            fishSwimClip = new AudioClip(resource.toExternalForm());
            fishSwimClip.setCycleCount(AudioClip.INDEFINITE);
            fishSwimClip.setVolume(0.45);
            return fishSwimClip;
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
