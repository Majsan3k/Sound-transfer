/**
 * A class for record sounds
 */

import javax.sound.sampled.*;
import java.io.*;

public class Recorder implements Runnable {

    private boolean alive;
    private TargetDataLine targetDataLine;
    private ByteArrayOutputStream outputSound;
    private byte[] buffer;
    private Storage storage;

    /**
     * Create new DataLine.Info and checks if the specified audioformat is supported.
     * Opens a TargetDataLine
     * @param audioFormat
     */
    public Recorder (AudioFormat audioFormat) {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
            if(!AudioSystem.isLineSupported(info)){
                System.out.println("Audio system does not support this format");
                return;
            }
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(audioFormat);
            targetDataLine.start();
        } catch (LineUnavailableException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        buffer = new byte[targetDataLine.getBufferSize() / 5];
    }

    /**
     * Return storage, which is the sound that has been recorded
     * @return sound that has been recorded
     */
    public synchronized Storage getStorage() {
        return storage;
    }

    /**
     * Stop recording
     */
    public void stopRecording() {
        alive = false;
    }

    /**
     * Closes ByteArrayOutputStream and TargetDataLine
     */
    private synchronized void close() {
        try {
            outputSound.flush();
            outputSound.close();
            targetDataLine.stop();
            targetDataLine.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Writes new sound to an ByteArrayOutPutStream. When there is no more to record
     * the sound is saved in a new Storage object.
     */
    @Override
    public synchronized void run() {
        alive = true;
        outputSound = new ByteArrayOutputStream();
        while (alive) {
            try {
                int bytes = targetDataLine.read(buffer, 0, buffer.length);
                if (bytes > 0) {
                    outputSound.write(buffer, 0, bytes);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                close();
            }
        }
        storage = new Storage(outputSound);
        close();
    }
}