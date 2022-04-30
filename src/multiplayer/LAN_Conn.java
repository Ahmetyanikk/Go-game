package multiplayer;

import game.Model;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
//bağlantıyı illüstre eden bir arayüz.
public abstract class LAN_Conn extends Thread {
    Model model;
    boolean isConnected = false;
    Socket socket;
// giden mesajı gösteren b
    public void send(int b) throws IOException{
        System.out.println("LAN_Conn: Going to send draw...");
        socket.getOutputStream().write(b);
        System.out.println("LAN_Conn: Sent draw");
    }
// ya mesaj döner yada hata anlamında -1
    public int receive() throws IOException, InterruptedException{
        InputStream stream = socket.getInputStream();
        
        //veriye ulaşabildi mi?
        while (stream.available() == 0){
        	Thread.sleep(100);
        }
    	//gelen veriyi okuyup döndürür
    	return stream.read();
    }

// bağlantı kurulur
    public abstract void init(String server_addr) throws IOException;
// bağlantıyı kapatır 
    public void terminate() throws IOException {
        if (socket != null)
            socket.close();
    }

    public boolean isConnected() {
        return isConnected;
    }
}
