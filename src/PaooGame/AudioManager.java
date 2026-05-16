package PaooGame;

import javax.sound.sampled.*;
import java.io.File;

/*! \class AudioManager
    \brief Gestioneaza muzica de fundal si efectele sonore ale jocului.

    \details
    Clasa foloseste sablonul Singleton, deoarece avem nevoie de un singur
    manager audio global in tot jocul. Astfel evitam situatia in care mai multe
    melodii ruleaza simultan fara control.

    Muzica este redata folosind Clip din javax.sound.sampled.
    Pentru simplitate, este recomandat sa folosim fisiere .wav.
 */
public class AudioManager {

    /*! \brief Instanta unica a clasei AudioManager. */
    private static AudioManager instance;

    /*! \brief Melodia curenta de fundal. */
    private Clip currentMusic;

    /*! \brief Calea melodiei care ruleaza in prezent. */
    private String currentMusicPath;

    /*! \brief Flag care indica daca muzica este activata. */
    private boolean musicEnabled = true;

    /*! \brief Flag care indica daca efectele sonore sunt activate. */
    private boolean soundEffectsEnabled = true;

    /*! \brief Volumul muzicii, in decibeli. Valori negative inseamna mai incet. */
    private float musicVolume = -12.0f;

    /*! \brief Volumul efectelor sonore. */
    private float soundVolume = -8.0f;

    /*! \brief Constructor privat pentru Singleton. */
    private AudioManager() {
    }

    /*! \fn public static AudioManager getInstance()
        \brief Returneaza instanta unica a managerului audio.
     */
    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }

        return instance;
    }

    /*! \fn public void playMusic(String path)
        \brief Porneste o melodie de fundal in loop.

        \details
        Daca aceeasi melodie ruleaza deja, nu o repornim.
        Daca ruleaza alta melodie, o oprim si pornim noua melodie.

        \param path Calea catre fisierul audio.
     */
    public void playMusic(String path) {
        if (!musicEnabled) {
            return;
        }

        try {
            /// Daca melodia ceruta ruleaza deja, nu facem nimic.
            if (currentMusic != null && path.equals(currentMusicPath) && currentMusic.isRunning()) {
                return;
            }

            /// Oprim melodia veche inainte sa pornim una noua.
            stopMusic();

            File musicFile = new File(path);

            if (!musicFile.exists()) {
                System.out.println("Fisier audio lipsa: " + path);
                return;
            }

            AudioInputStream audioInputStream = getCompatibleAudioInputStream(musicFile);
            currentMusic = AudioSystem.getClip();
            currentMusic.open(audioInputStream);

            setClipVolume(currentMusic, musicVolume);

            currentMusic.loop(Clip.LOOP_CONTINUOUSLY);
            currentMusic.start();

            currentMusicPath = path;

        } catch (Exception e) {
            System.out.println("Eroare la redarea muzicii: " + path);
            e.printStackTrace();
        }
    }

    /*! \fn public void stopMusic()
        \brief Opreste muzica de fundal curenta.
     */
    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic.close();
            currentMusic = null;
            currentMusicPath = null;
        }
    }

    /*! \fn public void playSoundEffect(String path)
        \brief Reda un efect sonor scurt o singura data.

        \param path Calea catre fisierul audio.
     */
    public void playSoundEffect(String path) {
        if (!soundEffectsEnabled) {
            return;
        }

        try {
            File soundFile = new File(path);

            if (!soundFile.exists()) {
                System.out.println("Efect audio lipsa: " + path);
                return;
            }

            AudioInputStream audioInputStream = getCompatibleAudioInputStream(soundFile);

            Clip effectClip = AudioSystem.getClip();
            effectClip.open(audioInputStream);

            setClipVolume(effectClip, soundVolume);

            effectClip.start();

            /*
             * Cand efectul sonor se termina, inchidem Clip-ul.
             * Altfel, multe efecte scurte pot ramane in memorie.
             */
            effectClip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    effectClip.close();
                }
            });

        } catch (Exception e) {
            System.out.println("Eroare la redarea efectului sonor: " + path);
            e.printStackTrace();
        }
    }

    /*! \fn private void setClipVolume(Clip clip, float volume)
        \brief Seteaza volumul unui clip audio.

        \param clip Clip-ul audio.
        \param volume Volumul in decibeli.
     */
    private void setClipVolume(Clip clip, float volume) {
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(volume);
        }
    }

    /*! \fn public void setMusicEnabled(boolean musicEnabled)
        \brief Activeaza sau dezactiveaza muzica.
     */
    public void setMusicEnabled(boolean musicEnabled) {
        this.musicEnabled = musicEnabled;

        if (!musicEnabled) {
            stopMusic();
        }
    }

    /*! \fn public void setSoundEffectsEnabled(boolean soundEffectsEnabled)
        \brief Activeaza sau dezactiveaza efectele sonore.
     */
    public void setSoundEffectsEnabled(boolean soundEffectsEnabled) {
        this.soundEffectsEnabled = soundEffectsEnabled;
    }

    /*! \fn public boolean isMusicEnabled()
        \brief Returneaza daca muzica este activata.
     */
    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    /*! \fn public boolean isSoundEffectsEnabled()
        \brief Returneaza daca efectele sonore sunt activate.
     */
    public boolean isSoundEffectsEnabled() {
        return soundEffectsEnabled;
    }

    /*! \fn private AudioInputStream getCompatibleAudioInputStream(File file)
    \brief Incarca un fisier audio si il converteste intr-un format compatibil cu Clip.

    \details
    Unele fisiere WAV descarcate de pe internet sunt pe 24-bit sau 48kHz,
    iar Java poate refuza sa le redea direct. De aceea convertim fluxul audio
    intr-un format PCM_SIGNED pe 16-bit, 44100 Hz.

    \param file Fisierul audio.
    \return Flux audio compatibil cu javax.sound.sampled.Clip.
 */
    private AudioInputStream getCompatibleAudioInputStream(File file) throws Exception {
        AudioInputStream originalStream = AudioSystem.getAudioInputStream(file);
        AudioFormat originalFormat = originalStream.getFormat();

        AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100.0f,
                16,
                originalFormat.getChannels(),
                originalFormat.getChannels() * 2,
                44100.0f,
                false
        );

        return AudioSystem.getAudioInputStream(targetFormat, originalStream);
    }
}