package lk.ac.mrt.cse.cs4262.server.coordinator;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lk.ac.mrt.cse.cs4262.server.heartbeat.HeartbeatMonitor;
import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElector;

public class CoordinatorConnector implements Runnable{

    private static final Logger log = LoggerFactory.getLogger(CoordinatorConnector.class);

    private Socket socket;
    private BufferedReader coordinatorInputBuffer;
    private DataOutputStream coordinatorOutputBuffer;
    private Gson gson;
    private String connectingServerName;

    public CoordinatorConnector(String ip, int port) throws IOException{
        log.info("start to make a connection with server on ip {} port {}", ip, port);
        this.socket = new Socket(ip, port);
        log.info("successfully connected to the server on ip {} port {}", ip, port);
    }

    public CoordinatorConnector(String ip, int port, boolean createBuffers) throws IOException{
        log.info("start to make a connection with server on ip {} port {}", ip, port);
        this.socket = new Socket(ip, port);
        log.info("successfully connected to the server on ip {} port {}", ip, port);
        if (createBuffers) {
            createInputBuffer(socket);
            createOutputBuffer(socket);
        }
    }

    public CoordinatorConnector createInputBuffer(){
        createInputBuffer(socket);
        return this;
    }

    public CoordinatorConnector createOutputBuffer(){
        createOutputBuffer(socket);
        return this;
    }

    private void createInputBuffer(Socket coordinatorSocket){
        log.info("create coordinator input buffer for {}", coordinatorSocket);
        try {
            this.coordinatorInputBuffer = new BufferedReader(new InputStreamReader(
                    this.socket.getInputStream(), "utf-8"));
        } catch (IOException e) {
            log.error("error occored while creating input buffer");
            log.error("error is {}", e.getMessage());
        }
    }

    private void createOutputBuffer(Socket coordinatorSocket){
        log.info("create coordinator output buffer for {}", coordinatorSocket);
        try {
            this.coordinatorOutputBuffer = new DataOutputStream(this.socket.getOutputStream());
        } catch (Exception e) {
            log.error("error occored while creating output buffer");
            log.error("error is {}", e.getMessage());
        }
    }
    

    public void sendMessage(String value) {
        log.info("send a message to coordinator message is {}", value);
        try {
            coordinatorOutputBuffer.write((value + "\n").getBytes("UTF-8"));
            coordinatorOutputBuffer.flush();
        } catch (IOException e) {
           log.error("error occred while sending the message");
           log.error("error is {}", e.getMessage());
        }
    }

  
    @Override
    public void run() {
        log.info("start to listen for coordinator connections");
        handleMessage();
    }

    public Map<String, Object> handleMessage(){
        boolean done = false;
        try {
            while (!this.socket.isClosed()) {
                String bufferedMessage = CoordinatorConnector.this.coordinatorInputBuffer.readLine();
        
                if (this.gson == null) {
                    this.gson = new Gson();
                }
        
                if (bufferedMessage != null) {
                    JsonObject jsonObject = this.gson.fromJson(bufferedMessage, JsonObject.class);
        
                    String messageType = jsonObject.get("type").getAsString();
        
                    Map<String, Object> map = new HashMap<>();
                    switch (messageType) {
                        case "roomexist":
                            boolean roomIDExists = jsonObject.get("exist").getAsBoolean();
                            map.put("exist", roomIDExists);                            
                            break;

                        case "heartbeatsuccess":
                            String serverName = jsonObject.get("serverid").getAsString();
                            HeartbeatMonitor.getInstance().acknowledge(serverName);
                            socket.close();
                            break;

                        case "coordinator":
                            map.put("coodinator", "answer");
                            break;

                        case "answer":
                            LeaderElector.getInstance().updateElectionAnswerMap(connectingServerName);
                            break;
                       
                        default:
                            break;
                    }
                    done = true;
                    return map;
                }
            }
        } catch (SocketException e) {
            log.error("error socket connection interupted");
            log.error("error is {}", e.getMessage());
        } catch (IOException e) {
            log.error("error is {}", e.getMessage());
        }finally{
            try {
                if (!done) socket.close();
            } catch (IOException e) {
                log.error("error is {}", e.getMessage());
            }
        }
        return null;
    }

    public void close(){
        try {
            socket.close();
        } catch (IOException e) {
            log.error("error when closing the socket");
            log.error("error is {}", e.getMessage());
        }
    }

    public String getConnectingServerName() {
        return connectingServerName;
    }

    public CoordinatorConnector setConnectingServerName(String connectingServerName) {
        this.connectingServerName = connectingServerName;
        return this;
    }

    
}
