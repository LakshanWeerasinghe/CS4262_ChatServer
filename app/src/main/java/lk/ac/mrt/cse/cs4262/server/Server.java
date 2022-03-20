package lk.ac.mrt.cse.cs4262.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lk.ac.mrt.cse.cs4262.server.chatRoom.MainHall;
import lk.ac.mrt.cse.cs4262.server.client.Client;
import lk.ac.mrt.cse.cs4262.server.client.command.NewIdentityHandler;
import lk.ac.mrt.cse.cs4262.server.coordinator.CoordinatorConnection;
import lk.ac.mrt.cse.cs4262.server.heartbeat.HeartbeatMonitor;

public class Server {

    private static final Logger log = LoggerFactory.getLogger(Server.class);
    
    private static Server instance = null;
    
    private final String serverName;
    private Store store;
    private SystemState systemState;
    private MainHall mainHall;
    private NewIdentityHandler newIdentityHandler;
    private ServerSocket coordinatorServerSocket;
    private ServerSocket clientHandlerServerSocket;


    private Server(String serverName){
        this.serverName = serverName;
    }
    
    public static synchronized Server getInstance(String serverName){
        if(instance == null){
            instance = new Server(serverName);
        }
        return instance;
    }

    public static Server getInstance(){
        return instance;
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

    public void startListenOnCoordinatorSocket(){
        new Thread(new Runnable(){
            @Override
            public void run() {

                log.info("start to listen for coordinator connections");
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

    public void startListenOnClientSocket(){
        new Thread(new Runnable(){
            @Override
            public void run() {
                log.info("start to listen for client connections");
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

    public void startHeartbeatMonitor() {
        new Thread (new  Runnable () {
            @Override
            public void run() {
                while(!systemState.allServerActive()){
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                HeartbeatMonitor.getInstance().startHeartbeatMonitor(serverName);                
            }

        }).start();
    }

    public void waitForAllServersToStart(){
        while(!systemState.allServerActive()){
            try {
                log.info("start to sleep since all servers are not active yet.");
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                log.error("thread got interuptted while waiting for all servers start");
                log.error("error is {}", e.getMessage());
            }
        }
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
        store.addManagedRoom(mainHall.getRoomName(), serverName, mainHall);
    }

    public MainHall getMainHall(){
        return mainHall;
    }

    public String getServerName() {
        return this.serverName;
    }
}
