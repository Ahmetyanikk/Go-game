package multiplayer;

import game.Model;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;

import javax.swing.SwingWorker;


public class Server extends LAN_Conn{
    //Server portunu hazır giriyorum
    //Port değerini hazır girme yeri olmadığı için  değeri burdan veriyorum
    public static final int SERVER_PORT = 10001;
    
    private ServerSocket serverSocket;
    
    public Server(Model model) {
        this.model = model;
    }
    // bir client'ın bağlanmasını bekliyor
    public void run() {
        try {
            socket = serverSocket.accept();
            isConnected = true;
            System.out.println("The server (White)\n------------------\n");
            System.out.println("Server: Client connected!");
            model.setChanged1();
            //burada diğer oyuncunun hamle yapmasını bekliyeceğimizden dolayı, swingWorker kullandım.SwingWorker thread'in bu iş için başarılı olduğu için kullandım
            SwingWorker worker = new SwingWorker(){
                @Override
                protected Object doInBackground() throws Exception {
                    model.receive();
                    return null;
                }
            };
            worker.execute();
            model.notifyObservers2(UpdateMessages.CLIENT_CONNECTED);
            System.out.println("Server: View notified");
        } catch (SocketException se) {
            System.out.println("Socket closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }//run
// 49. satırda bağlanıyor
    public void init(String not_used) throws IOException {
        serverSocket = new ServerSocket(SERVER_PORT);
    }
// bağlantıyı kapatıyor
    public void terminate() throws IOException{
        serverSocket.close();
        super.terminate();
    }
}
