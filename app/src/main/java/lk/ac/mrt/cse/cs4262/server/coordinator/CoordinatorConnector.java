package lk.ac.mrt.cse.cs4262.server.coordinator;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
        this.socket = new Socket(ip, port);
    }

    public CoordinatorConnector(String ip, int port, boolean createBuffers) throws IOException{
        this.socket = new Socket(ip, port);
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
        try {
            this.coordinatorInputBuffer = new BufferedReader(new InputStreamReader(
                    this.socket.getInputStream(), "utf-8"));
        } catch (IOException e) {
            log.error("error occored while creating input buffer");
            log.error("error is {}", e.getMessage());
        }
    }

    private void createOutputBuffer(Socket coordinatorSocket){
        try {
            this.coordinatorOutputBuffer = new DataOutputStream(this.socket.getOutputStream());
        } catch (Exception e) {
            log.error("error occored while creating output buffer");
            log.error("error is {}", e.getMessage());
        }
    }
    

    public void sendMessage(String message) {
        log.info("send a message {} to coordinator", message);
        try {
            coordinatorOutputBuffer.write((message + "\n").getBytes("UTF-8"));
            coordinatorOutputBuffer.flush();
        } catch (IOException e) {
           log.error("error occred while sending the message");
           log.error("error is {}", e.getMessage());
        }
    }

  
    @Override
    public void run() {
        log.info("start to listen for coordinator messages");
        handleMessage();
    }

    public Map<String, Object> handleMessage(){
        try {
            while (!this.socket.isClosed()) {
                String bufferedMessage = CoordinatorConnector.this.coordinatorInputBuffer.readLine();
                if (this.gson == null) {
                    this.gson = new Gson();
                }
        
                if (bufferedMessage != null) {
                    JsonObject jsonObject = this.gson.fromJson(bufferedMessage, JsonObject.class);
        
                    String messageType = jsonObject.get("type").getAsString();
                    log.info("recived message {} from {}", jsonObject, connectingServerName);
        
                    Map<String, Object> map = new HashMap<>();
                    switch (messageType) {
                        case "roomexist":
                            boolean roomIDExists = jsonObject.get("exist").getAsBoolean();
                            map.put("exist", roomIDExists);                            
                            break;

                        case "heartbeatsuccess":
                            String serverName = jsonObject.get("serverid").getAsString();
                            HeartbeatMonitor.getInstance().acknowledge(serverName);
                            break;

                        case "coordinator":
                            map.put("coodinator", "answer");
                            break;

                        case "answer":
                            LeaderElector.getInstance().updateElectionAnswerMap(connectingServerName);
                            break;
                            
                        case "identityexist":
                            boolean identityExist = jsonObject.get("exist").getAsBoolean();
                            map.put("exist", identityExist);
                            break;

                        case "view":
                            JsonArray liverServersJsonArray = jsonObject.getAsJsonArray("liveServerNames");
                            JsonObject allRoomsJsonObject = jsonObject.getAsJsonObject("allRooms");
                            List<String> liveServerNames = new ArrayList<>();
                            Map<String, String> allRoomsMap = new HashMap<>();
                            for (JsonElement e : liverServersJsonArray) {
                                liveServerNames.add(e.getAsString());
                            }
                            for (Map.Entry<String,JsonElement> entry : allRoomsJsonObject.entrySet()) {
                                allRoomsMap.put(entry.getKey(), entry.getValue().getAsString());
                            }
                            map.put("liveServerNames", liveServerNames);
                            map.put("allRooms", allRoomsMap);
                            break;
                       
                        default:
                            break;
                    }
                    return map;
                }
            }
        } catch (SocketException e) {
            log.error("error socket connection interupted");
            log.error("error is {}", e.getMessage());
        } catch (IOException e) {
            log.error("error is {}", e.getMessage());
        }finally{
            close();
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
