package lk.ac.mrt.cse.cs4262.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lk.ac.mrt.cse.cs4262.server.chatRoom.MainHall;
import lk.ac.mrt.cse.cs4262.server.client.Client;
import lk.ac.mrt.cse.cs4262.server.client.command.CreateRoomHandler;
import lk.ac.mrt.cse.cs4262.server.client.command.NewIdentityHandler;
import lk.ac.mrt.cse.cs4262.server.coordinator.CoordinatorConnection;

public class Server {

    private static final Logger log = LoggerFactory.getLogger(Server.class);
    
    private final String serverName;
    private Store store;
    private SystemState systemState;
    private MainHall mainHall;
    private NewIdentityHandler newIdentityHandler;
    private CreateRoomHandler createRoomHandler;
    private ServerSocket coordinatorServerSocket;
    private ServerSocket clientHandlerServerSocket;


    public Server(String serverName){
        this.serverName = serverName;
    }
    

    public Server createCoordinatorServerSocket(int port){
        log.info("start creating coordinator server socket on port {}", port);
        try {
            this.coordinatorServerSocket = new ServerSocket(port);
            log.info("coordinator server socket opened on port {}", port);
        } catch (Exception e) {
            log.error("error occored while creating coordinator server socket");
            log.error("error is {}", e.getMessage());
            System.exit(1);
        }
        return this;
    }


    public Server createClientHandlerServerSocket(int port){
        log.info("start creating client handler server socket on port {}", port);
        try {
            this.clientHandlerServerSocket = new ServerSocket(port);
            log.info("client socket opened on port {}", port);
        } catch (Exception e) {
            log.error("error occored while creating client handler server socket");
            log.error("error is {}", e.getMessage());
            System.exit(1);
        }
        return this;
    }


    public void listen(){
        startListenOnCoordinatorSocket();
        startListenOnClientSocket();
    }

    private void startListenOnCoordinatorSocket(){
        log.info("start to listen for coordinator connections");
        new Thread(new Runnable(){
            @Override
            public void run() {
                while(true){
                    try {
                        Socket coordinationConnectionSocket = coordinatorServerSocket.accept();
                        new Thread( new CoordinatorConnection(coordinationConnectionSocket, Server.this)).start();
                    } catch (IOException e) {
                       log.error("error occored while trying to make a socket connection");
                       log.error("error is {}", e.getMessage());
                    } 
                }
            }
        }).start();
    }

    private void startListenOnClientSocket(){
        log.info("start to listen for client connections");
        new Thread(new Runnable(){
            @Override
            public void run() {
                while(true){
                    try {
                        Socket client = clientHandlerServerSocket.accept();
                        Thread clientThread = new Thread(new Client(client, Server.this));
                        clientThread.start();
                    } catch (IOException e) {
                        log.error("error occored while trying to make a socket connection");
                       log.error("error is {}", e.getMessage());
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

    public void setSystemState(SystemState systemState){
        this.systemState = systemState;
    }

    public SystemState getSystemState(){
        return systemState;
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
