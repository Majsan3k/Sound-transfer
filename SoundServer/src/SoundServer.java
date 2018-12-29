import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class SoundServer implements Runnable{

    private ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private ServerSocket serverSocket;

    public SoundServer(int port){
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("Couldn't connect. " + e.getMessage());
            System.exit(1);
        }
    }

    public void addClient(ClientHandler ch){
        clientHandlers.add(ch);
    }

    public void removeClient(ClientHandler ch){
        clientHandlers.remove(ch);
    }

    public void broadCast(Object obj) throws IOException{
        for(ClientHandler ch : clientHandlers){
            ch.write(obj);
        }
    }

    @Override
    public void run() {
        try{
            while(true){
                Socket socket = serverSocket.accept();
                System.out.println("Connected" + socket.getInetAddress());
                Thread clientThread = new Thread(new ClientHandler(socket, this));
                clientThread.start();
            }
        }catch (IOException e) {
            System.out.println(e.getMessage());
        }catch(Exception ex)
        {
            System.out.println(ex.getMessage());
        }
    }

    public static void main(String args[]){
        int port = 2000;

        if(args.length > 0){
            port = Integer.parseInt(args[0].toString());
        }

        SoundServer imageServer = new SoundServer(port);
        Thread thread = new Thread(imageServer);
        thread.start();
    }
}
