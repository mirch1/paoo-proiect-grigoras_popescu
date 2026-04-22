package PaooGame;

import java.awt.image.BufferedImage;

public class Animation {
    private final int speed; // Timpul (în milisecunde) între cadre
    private int index;       // Cadrul curent pe care îl afișăm
    private long lastTime, timer;
    private final BufferedImage[] frames; // Șirul de imagini pentru animație

    // Constructorul primește viteza și cadrele
    public Animation(int speed, BufferedImage[] frames) {
        this.speed = speed;
        this.frames = frames;
        index = 0;
        timer = 0;
        lastTime = System.currentTimeMillis();
    }

    public void tick() {
        // Calculăm cât timp a trecut de la ultimul Update
        timer += System.currentTimeMillis() - lastTime;
        lastTime = System.currentTimeMillis();

        // Dacă a trecut suficient timp, trecem la cadrul următor
        if (timer > speed) {
            index++;
            timer = 0; // Resetăm timer-ul

            // Dacă am ajuns la finalul cadrelor, o luăm de la capăt
            if (index >= frames.length) {
                index = 0;
            }
        }
    }

    // Returnează imaginea care trebuie desenată acum
    public BufferedImage getCurrentFrame() {
        return frames[index];
    }
}