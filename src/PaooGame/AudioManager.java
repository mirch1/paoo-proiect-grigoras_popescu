package PaooGame;

import PaooGame.Exceptions.AudioLoadException;

import javax.sound.sampled.*;
import java.io.File;

/*! \class AudioManager
    \brief Gestioneaza muzica de fundal si efectele sonore ale jocului.

    \details
    Clasa foloseste sablonul Singleton, deoarece avem nevoie de un singur
    manager audio global in tot jocul. Astfel evitam situatia in care mai multe
    melodii ruleaza simultan fara control.

    Muzica si efectele sonore sunt redate folosind Clip din javax.sound.sampled.
    Pentru proiect, este recomandat ca fisierele audio sa fie .wav,
    PCM signed, 16-bit, 44100 Hz.
 */
public class AudioManager {

    /*! \brief Instanta unica a clasei AudioManager. */
    private static AudioManager instance;

    /*! \brief Melodia curenta de fundal. */
    private Clip currentMusic;

    /*! \brief Calea melodiei care ruleaza in prezent. */
    private String currentMusicPath;

    /*! \brief Ultima melodie ceruta de joc. Este folosita pentru reluare dupa reactivarea muzicii. */
    private String lastRequestedMusicPath;

    /*! \brief Flag care indica daca muzica este activata. */
    private boolean musicEnabled = true;

    /*! \brief Flag care indica daca efectele sonore sunt activate. */
    private boolean soundEffectsEnabled = true;

    /*! \brief Volumul muzicii, in decibeli. Valori negative inseamna mai incet. */
    private float musicVolume = -12.0f;

    /*! \brief Volumul efectelor sonore, in decibeli. */
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

    // =========================================================================
    // MUZICA DE FUNDAL
    // =========================================================================

    /*! \fn public void playMusic(String path)
        \brief Porneste o melodie de fundal in loop.

        \details
        Daca aceeasi melodie ruleaza deja, nu o repornim.
        Daca ruleaza alta melodie, o oprim si pornim noua melodie.
        Daca fisierul audio lipseste sau are format invalid, eroarea este tratata
        prin AudioLoadException, fara sa opreasca jocul.

        \param path Calea catre fisierul audio.
     */
    public void playMusic(String path) {
        /*
         * Retinem mereu ultima melodie ceruta.
         * Astfel, daca muzica este oprita din Settings si apoi repornita,
         * putem relua automat tema curenta.
         */
        lastRequestedMusicPath = path;

        if (!musicEnabled) {
            return;
        }

        try {
            /*
             * Daca aceeasi melodie ruleaza deja, nu o incarcam din nou.
             * Astfel evitam restartarea inutila a soundtrack-ului la fiecare update.
             */
            if (currentMusic != null
                    && path.equals(currentMusicPath)
                    && currentMusic.isRunning()) {
                return;
            }

            /// Oprim melodia veche inainte sa pornim una noua.
            stopMusic();

            /// Incarcam clipul audio folosind metoda care arunca AudioLoadException.
            currentMusic = loadClip(path);

            /// Aplicam volumul pentru muzica.
            setClipVolume(currentMusic, musicVolume);

            /// Muzica de fundal ruleaza continuu.
            currentMusic.loop(Clip.LOOP_CONTINUOUSLY);
            currentMusic.start();

            currentMusicPath = path;

        } catch (AudioLoadException e) {
            /*
             * Audio-ul nu este critic pentru rularea jocului.
             * Daca lipseste o melodie sau formatul este incompatibil,
             * afisam mesajul si continuam jocul.
             */
            System.out.println(e.getMessage());

            currentMusic = null;
            currentMusicPath = null;
        }
    }

    /*! \fn public void stopMusic()
        \brief Opreste muzica de fundal curenta.
     */
    public void stopMusic() {
        if (currentMusic != null) {
            if (currentMusic.isRunning()) {
                currentMusic.stop();
            }

            currentMusic.close();
            currentMusic = null;
            currentMusicPath = null;
        }
    }

    // =========================================================================
    // EFECTE SONORE
    // =========================================================================

    /*! \fn public void playSoundEffect(String path)
        \brief Reda un efect sonor scurt o singura data.

        \details
        Efectele sonore sunt incarcate in clipuri separate, ca sa poata fi redate
        peste muzica de fundal. Dupa terminare, clipul este inchis automat.

        \param path Calea catre fisierul audio.
     */
    public void playSoundEffect(String path) {
        if (!soundEffectsEnabled) {
            return;
        }

        try {
            /// Incarcam efectul sonor.
            Clip effectClip = loadClip(path);

            /// Aplicam volumul pentru efecte.
            setClipVolume(effectClip, soundVolume);

            /*
             * Cand efectul sonor se termina, inchidem Clip-ul.
             * Altfel, multe efecte scurte pot ramane in memorie.
             */
            effectClip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    effectClip.close();
                }
            });

            /// Pornim efectul sonor.
            effectClip.start();

        } catch (AudioLoadException e) {
            /*
             * Daca un efect sonor lipseste, jocul nu trebuie sa se opreasca.
             * Afisam doar mesajul pentru debugging.
             */
            System.out.println(e.getMessage());
        }
    }

    // =========================================================================
    // INCARCARE AUDIO
    // =========================================================================

    /*! \fn private Clip loadClip(String path)
        \brief Incarca un fisier audio si il transforma intr-un Clip redabil.

        \details
        Aceasta metoda centralizeaza incarcarea audio.
        Daca fisierul lipseste sau formatul nu este suportat, arunca
        AudioLoadException, o exceptie specifica proiectului.

        \param path Calea catre fisierul audio.
        \return Clip-ul audio incarcat.
     */
    private Clip loadClip(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new AudioLoadException("Cale audio invalida.");
        }

        File audioFile = new File(path);

        if (!audioFile.exists() || !audioFile.isFile()) {
            throw new AudioLoadException("Fisier audio lipsa: " + path);
        }

        try {
            AudioInputStream audioInputStream = getCompatibleAudioInputStream(audioFile);

            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);

            audioInputStream.close();

            return clip;

        } catch (Exception e) {
            throw new AudioLoadException(
                    "Eroare la incarcarea fisierului audio: " + path,
                    e
            );
        }
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

        int channels = originalFormat.getChannels();

        /*
         * In mod normal, fisierele audio au 1 canal mono sau 2 canale stereo.
         * Daca Java nu poate detecta numarul de canale, folosim stereo ca fallback.
         */
        if (channels <= 0) {
            channels = 2;
        }

        AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100.0f,
                16,
                channels,
                channels * 2,
                44100.0f,
                false
        );

        return AudioSystem.getAudioInputStream(targetFormat, originalStream);
    }

    // =========================================================================
    // VOLUM
    // =========================================================================

    /*! \fn private void setClipVolume(Clip clip, float volume)
        \brief Seteaza volumul unui clip audio.

        \details
        Volumul este limitat intre valorile permise de FloatControl,
        ca sa evitam exceptii daca trimitem o valoare prea mare sau prea mica.

        \param clip Clip-ul audio.
        \param volume Volumul in decibeli.
     */
    private void setClipVolume(Clip clip, float volume) {
        if (clip == null) {
            return;
        }

        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl =
                    (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

            float min = gainControl.getMinimum();
            float max = gainControl.getMaximum();

            float safeVolume = Math.max(min, Math.min(max, volume));

            gainControl.setValue(safeVolume);
        }
    }

    // =========================================================================
    // SETARI AUDIO
    // =========================================================================

    /*! \fn public void setMusicEnabled(boolean musicEnabled)
        \brief Activeaza sau dezactiveaza muzica.
     */

    /*! \fn public void setMusicEnabled(boolean musicEnabled)
    \brief Activeaza sau dezactiveaza muzica.
 */
    public void setMusicEnabled(boolean musicEnabled) {
        this.musicEnabled = musicEnabled;

        if (!musicEnabled) {
            stopMusic();
        } else {
            /*
             * Daca muzica a fost repornita din Settings, reluam ultima tema ceruta.
             * De exemplu: menu_theme, dungeon_theme, battle_theme etc.
             */
            if (lastRequestedMusicPath != null) {
                playMusic(lastRequestedMusicPath);
            }
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
}

