package lk.ac.mrt.cse.cs4262.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import lk.ac.mrt.cse.cs4262.server.chatRoom.MainHall;
import lk.ac.mrt.cse.cs4262.server.client.Client;
import lk.ac.mrt.cse.cs4262.server.client.command.CreateRoomHandler;
import lk.ac.mrt.cse.cs4262.server.client.command.NewIdentityHandler;

public class Server {
    
    private String serverName;
    private ServerSocket serverSocket;
    private Store store;
    private MainHall mainHall;
    private NewIdentityHandler newIdentityHandler;
    private CreateRoomHandler createRoomHandler;

    public Server(int port, String serverName){
        try {
            this.serverName = serverName;
            this.serverSocket = new ServerSocket(port);
            System.out.println("Server " + serverName + " opened on port " + String.valueOf(port));
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
        store.addRoom(mainHall.getRoomName(), serverName, mainHall);
    }

    public MainHall getMainHall(){
        return mainHall;
    }

    public String getServerName() {
        return this.serverName;
    }

    public void setCreateRoomHandler(CreateRoomHandler createRoomHandler) {
        this.createRoomHandler = createRoomHandler;
    }

    public CreateRoomHandler getCreateRoomHandler() {
        return this.createRoomHandler;
    }
}
