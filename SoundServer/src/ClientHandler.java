import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable{

    private SoundServer server;
    private Socket socket;
    private ObjectOutputStream objOut;

    public ClientHandler(Socket clientSocket, SoundServer server){
        this.server = server;
        this.socket = clientSocket;
    }

    public void write(Object obj) throws IOException {
        objOut.writeObject(obj);
        objOut.flush();
    }

    @Override
    public void run() {
        ObjectInputStream inDataFromClient = null;
        try {
            inDataFromClient = new ObjectInputStream(socket.getInputStream());
            objOut = new ObjectOutputStream(socket.getOutputStream());
            server.addClient(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Object obj;
        try {
            while ((obj = inDataFromClient.readObject()) != null) {
                server.broadCast(obj);
            }
            inDataFromClient.close();
            close();
        } catch (IOException e) {
            System.out.println(socket.getInetAddress() + " disconnected");
            close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            close();
        }
    }

    private void close(){
        try {
            socket.close();
            objOut.close();
            server.removeClient(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
