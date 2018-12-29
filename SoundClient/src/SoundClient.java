//Byggde egen server till denna uppgift. Ligger i mappen Server. Den måste startas innan programmet kan köras.

/**
 * Client for sending, receiving, playing and pausing sounds
 */

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class SoundClient extends JFrame implements Runnable{

    private static AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000.0F, 16, 1, 2, 8000.0F, false);
    private AudioInputStream audioInputStream = null;
    private ObjectOutputStream objOut;
    private Recorder recorder;
    private Clip line = null;
    private Socket socket;

    private JButton recordBtn, stopBtn, pauseBtn, resumeBtn;
    private boolean playing, recording, paused;

    /**
     * Starts GUI to user. Creates new socket and ObjectOutPutStream on that socket.
     * @param host socket host
     * @param port socket port
     */

    public SoundClient(String host, int port){
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        add(recordBtn = new JButton("Record"));
        add(stopBtn = new JButton("Stop&Send"));
        add(pauseBtn = new JButton("Pause"));
        add(resumeBtn = new JButton("Resume"));
        add(pauseBtn);
        add(resumeBtn);
        setBtnState();

        recordBtn.addActionListener(e -> record());
        stopBtn.addActionListener(e -> {
            recording = false;
            playing = true;
            recorder.stopRecording();
            sendSound(recorder.getStorage());
            setBtnState();
        });
        pauseBtn.addActionListener(e -> {
            line.stop();
            paused = true;
            setBtnState();});
        resumeBtn.addActionListener(e -> {
            paused = false;
            line.start();
            line.loop(line.LOOP_CONTINUOUSLY);
            setBtnState();
        });

        setVisible(true);
        pack();

        try {
            socket = new Socket(host, port);
            objOut = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.out.println("Couldn't connect. " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Start new thread of the class Record for recording sound
     */
    private void record(){
        clear();
        recording = true;
        paused = false;
        playing = false;
        setBtnState();
        recorder = new Recorder(audioFormat);
        new Thread(recorder).start();
    }

    /**
     * Change state of the buttons. The state decides from if the program are recording, playing,
     * or paused.
     */
    private void setBtnState(){
        recordBtn.setEnabled(!recording);
        stopBtn.setEnabled(recording);
        pauseBtn.setEnabled(playing & !paused);
        resumeBtn.setEnabled(paused);
    }

    /**
     * Send sound throw ObjectOutPutStream
     * @param storage sound to be sent
     */
    private synchronized void sendSound(Storage storage) {
        clear();
        try{
            objOut.flush();
            objOut.writeObject(storage);
            objOut.flush();
        } catch (IOException e) {
            System.out.println("Problem while sending sound " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Stop Clip and close audioInputStream
     */
    private synchronized void clear(){
        if(line != null){
            line.flush();
            line.stop();
            line.close();
            if(audioInputStream != null) {
                try {
                    audioInputStream.close();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    /**
     * Writes the storage's data to at ByteArrayOutPutStream and transform it to a byteArray.
     * Then create a ByteArrayInputStream from the byteArray.
     * Creates a new DataLine info with a Clip class and specified audioFormat.
     * Open line with the audioinputstream, starts it and put in on a loop.
     * @param storage the sound that should be played
     */
    private synchronized void playSound(Storage storage){
        clear();
        recording = false;
        playing = true;
        paused = false;
        setBtnState();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(storage.getData(), 0, storage.getData().length);
            byte[] byteArray = out.toByteArray();
            ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(byteArray);
            audioInputStream = new AudioInputStream(arrayInputStream, audioFormat, byteArray.length);
            DataLine.Info info = new DataLine.Info(Clip.class, audioFormat);
            line = (Clip) AudioSystem.getLine(info);
            line.open(audioInputStream);
            line.start();
            line.loop(Clip.LOOP_CONTINUOUSLY);
        } catch(LineUnavailableException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch(Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Listens for new sounds to come in through ObjectInputStream
     */
    @Override
    public void run()  {
        ObjectInputStream objIn = null;
        try {
            objIn = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        Object sound;
        try {
            while((sound = objIn.readObject()) != null){
                playSound((Storage) sound);
            }
        }catch(SocketException s){
            close();
            System.out.println("You lost connection");
            System.exit(1);
        }catch (IOException e) {
            System.out.println("IO exception: " + e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        close();
    }

    /**
     * Closes socket, ObjectOutPutStream, Clip and AudioInputStream.
     */
    private void close(){
        try {
            socket.close();
            objOut.close();
            line.close();
            audioInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts the program. Uses host 127.0.0.1 and port 2000 if nothing else is specified by user.
     * Host should be specified at index 0 and host at index 1.
     * Closes program if more than 2 arguments is specified
     * @param args
     */

    public static void main(String args[]){
        int port = 2000;
        String host = "127.0.0.1";

        if(args.length > 0){
            host = args[0];
        }
        if(args.length > 1){
            port = Integer.parseInt(args[1]);
        }
        new Thread(new SoundClient(host, port)).start();
    }
}
