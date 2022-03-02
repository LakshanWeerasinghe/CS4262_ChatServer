package lk.ac.mrt.cse.cs4262.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import lk.ac.mrt.cse.cs4262.server.chatRoom.MainHall;
import lk.ac.mrt.cse.cs4262.server.client.Client;
import lk.ac.mrt.cse.cs4262.server.client.command.NewIdentityHandler;

public class Server {
    
    private ServerSocket serverSocket;
    private Store store;
    private MainHall mainHall;
    private NewIdentityHandler newIdentityHandler;

    public Server(int port){
        try {
            this.serverSocket = new ServerSocket(port);
            System.out.println("Server socket opened on port " + String.valueOf(port));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void listen(){
        new Thread(new Runnable(){
            @Override
            public void run() {
                while(true){
                    try {
                        Socket client = serverSocket.accept();
                        Thread clientThread = new Thread(new Client(client, Server.this));
                        clientThread.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } 
                }
            }
        }).start();
    }

    public void setStore(Store store){
        this.store = store;
    }

    public Store getStore(){
        return store;
    }

    public void setNewIdentityHandler(NewIdentityHandler newIdentityHandler){
        this.newIdentityHandler = newIdentityHandler;
    }

    public NewIdentityHandler getNewIdentityHandler(){
        return newIdentityHandler;
    }

    public void setMainHall(MainHall mainHall){
        this.mainHall = mainHall;
    }

    public MainHall getMainHall(){
        return mainHall;
    }
}
