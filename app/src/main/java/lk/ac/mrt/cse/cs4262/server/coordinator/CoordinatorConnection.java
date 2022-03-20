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

import lk.ac.mrt.cse.cs4262.server.Server;
import lk.ac.mrt.cse.cs4262.server.Store;
import lk.ac.mrt.cse.cs4262.server.heartbeat.HeartbeatMonitor;
import lk.ac.mrt.cse.cs4262.server.leader.LeaderRoomHandler;
import lk.ac.mrt.cse.cs4262.server.leaderElector.EventConstants;
import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElector;
import lk.ac.mrt.cse.cs4262.server.util.Util;

public class CoordinatorConnection implements Runnable{
    
    private static final Logger log = LoggerFactory.getLogger(CoordinatorConnection.class);
    
    private final Socket coordinatorSocket;
    private BufferedReader coordinatorInputBuffer;
    private DataOutputStream coordinatorOutputBuffer;
    private Gson gson;
    private String myServerName;
    
    public CoordinatorConnection(Socket coordinatorSocket, Server server){
        this.coordinatorSocket = coordinatorSocket;
        this.myServerName = server.getServerName();
        createInputBuffer(coordinatorSocket);
        createOutputBuffer(coordinatorSocket);
        this.gson = new Gson();
    }

    private void createInputBuffer(Socket coordinatorSocket){
        log.info("create coordinator input buffer for {}", coordinatorSocket);
        try {
            this.coordinatorInputBuffer = new BufferedReader(new InputStreamReader(
                    this.coordinatorSocket.getInputStream(), "utf-8"));
        } catch (IOException e) {
            log.error("error occored while creating input buffer");
            log.error("error is {}", e.getMessage());
        }
    }

    private void createOutputBuffer(Socket coordinatorSocket){
        log.info("create coordinator output buffer for {}", coordinatorSocket);
        try {
            this.coordinatorOutputBuffer = new DataOutputStream(this.coordinatorSocket.getOutputStream());
        } catch (Exception e) {
            log.error("error occored while creating output buffer");
            log.error("error is {}", e.getMessage());
        }
    }

    @Override
    public void run() {
        boolean done = false;
        try {
            while (!this.coordinatorSocket.isClosed()) {
                String bufferedMessage = this.coordinatorInputBuffer.readLine();
                

                if (this.gson == null) {
                    this.gson = new Gson();
                }

                if (bufferedMessage != null) {
                    JsonObject jsonObject = this.gson.fromJson(bufferedMessage, JsonObject.class);

                    String messageType = jsonObject.get("type").getAsString();
                    System.out.println(jsonObject);

                    Map<String, Object> map = new HashMap<>();
                    String roomID, serverName;
                    switch (messageType) {
                        case "checkroomexist":
                            roomID = jsonObject.get("roomid").getAsString();
                            serverName = jsonObject.get("serverid").getAsString();
                            boolean roomIDExists = LeaderRoomHandler.getInstance().
                                handleCreateRoom(roomID, serverName);
                            
                            map.put("type", "roomexist");
                            map.put("roomid", roomID);
                            map.put("exist", roomIDExists);
                            send(Util.getJsonString(map));
                            break;

                        case "createroomack":
                            boolean created = jsonObject.get("created").getAsBoolean();
                            if (created) {
                                roomID = jsonObject.get("roomid").getAsString();
                                serverName = jsonObject.get("serverid").getAsString();
                                LeaderRoomHandler.getInstance().
                                    informAboutNewRoom(roomID, serverName);
                            }
                            coordinatorSocket.close();
                            break;

                        case "roomcreate":
                            roomID = jsonObject.get("roomid").getAsString();
                            serverName = jsonObject.get("serverid").getAsString();
                            Store.getInstance().addRoom(roomID, serverName);
                            coordinatorSocket.close();
                            break;

                        case "heartbeatcheck":
                            map.put("type", "heartbeatsuccess");
                            map.put("serverid", myServerName);
                            send(Util.getJsonString(map));
                            HeartbeatMonitor.getInstance().executeMonitor();
                            break;

                        case "failurenotice":
                            JsonArray jArray = jsonObject.getAsJsonArray("failed");
                            List<String> failed = new ArrayList<>();
                            for (JsonElement e : jArray) failed.add(e.getAsString());
                            HeartbeatMonitor.getInstance().updateFailedServers(failed);
                            break;

                        case "election":
                            map.put("type", "answer");
                            send(Util.getJsonString(map));
                            try {
                                LeaderElector.getInstance()
                                            .getLeaderElectorState()
                                            .dispatchEvent(EventConstants.RECEIVE_ELECTION);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                            break;
                        
                        case "nomination":
                            try {
                                LeaderElector.getInstance()
                                                .getLeaderElectorState()
                                                .dispatchEvent(EventConstants.RECEIVE_NOMINATION);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                            break;
                        
                        case "coordinator":
                            try {
                                LeaderElector.getInstance()
                                                .getLeaderElectorState()
                                                .dispatchEvent(EventConstants.RECEIVE_COORDINATOR);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                            break;

                        default:
                            break;
                    }
                    done = true;

                }
            }
        } catch (SocketException e) {
            log.error("error socket connection interupted");
            log.error("error is {}", e.getMessage());
        } catch (IOException e) {
            log.error("error is {}", e.getMessage());
        }finally{
            try {
                if (!done) coordinatorSocket.close();
            } catch (IOException e) {
                log.error("error is {}", e.getMessage());
            }
        }
    }


    public void send(String value) {
        log.info("send a message to coordinator message is {}", value);
        try {
            coordinatorOutputBuffer.write((value + "\n").getBytes("UTF-8"));
            coordinatorOutputBuffer.flush();
        } catch (IOException e) {
           log.error("error occred while sending the message");
           log.error("error is {}", e.getMessage());
        }
    }

}
