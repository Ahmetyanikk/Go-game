package multiplayer;

import game.Model;

import java.io.IOException;
import java.net.Socket;


public class Client extends LAN_Conn{
    public Client(Model model){
        this.model = model;
    }//constructor
    public void init(String server_addr) throws IOException {
        socket = new Socket(server_addr, Server.SERVER_PORT);
        System.out.println("The client (Black)\n------------------\n");
        System.out.println("Client: Connected to server: "
                + socket.getRemoteSocketAddress());
        isConnected = true;
    }//init
}
