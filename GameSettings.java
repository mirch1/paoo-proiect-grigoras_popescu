package PaooGame;

public class GameSettings {
    private static boolean musicEnabled = true;
    private static boolean soundEnabled = true;

    public static boolean isMusicEnabled() { return musicEnabled; }
    public static void setMusicEnabled(boolean musicEnabled) { GameSettings.musicEnabled = musicEnabled; }
    public static boolean isSoundEnabled() { return soundEnabled; }
    public static void setSoundEnabled(boolean soundEnabled) { GameSettings.soundEnabled = soundEnabled; }
}
