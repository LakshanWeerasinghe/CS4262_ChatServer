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

import lk.ac.mrt.cse.cs4262.server.SystemState;
import lk.ac.mrt.cse.cs4262.server.leader.LeaderIndentityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lk.ac.mrt.cse.cs4262.server.Server;
import lk.ac.mrt.cse.cs4262.server.Store;
import lk.ac.mrt.cse.cs4262.server.heartbeat.HeartbeatMonitor;
import lk.ac.mrt.cse.cs4262.server.leader.LeaderRoomHandler;
import lk.ac.mrt.cse.cs4262.server.leaderElector.EventConstants;
import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElectionHandler;
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
        try {
            this.coordinatorInputBuffer = new BufferedReader(new InputStreamReader(
                    this.coordinatorSocket.getInputStream(), "utf-8"));
        } catch (IOException e) {
            log.error("error occored while creating input buffer");
            log.error("error is {}", e.getMessage());
        }
    }

    private void createOutputBuffer(Socket coordinatorSocket){
        try {
            this.coordinatorOutputBuffer = new DataOutputStream(this.coordinatorSocket.getOutputStream());
        } catch (Exception e) {
            log.error("error occored while creating output buffer");
            log.error("error is {}", e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            while (!this.coordinatorSocket.isClosed()) {
                String bufferedMessage = this.coordinatorInputBuffer.readLine();
                
                if (this.gson == null) {
                    this.gson = new Gson();
                }

                if (bufferedMessage != null) {
                    JsonObject recievedMessage = this.gson.fromJson(bufferedMessage, JsonObject.class);

                    log.info("recived message {} from a coordinator server with ip {} and {}", recievedMessage, 
                                coordinatorSocket.getRemoteSocketAddress(), coordinatorSocket.getPort());

                    String messageType = recievedMessage.get("type").getAsString();
                    Map<String, Object> map = new HashMap<>();
                    String roomID, serverName, identity;
                    switch (messageType) {
                        case "checkroomexist":
                            roomID = recievedMessage.get("roomid").getAsString();
                            serverName = recievedMessage.get("serverid").getAsString();
                            boolean roomIDExists = LeaderRoomHandler.getInstance().
                                handleCreateRoom(roomID, serverName);
                            
                            map.put("type", "roomexist");
                            map.put("roomid", roomID);
                            map.put("exist", roomIDExists);
                            send(Util.getJsonString(map));
                            break;

                        case "createroomack":
                            boolean created = recievedMessage.get("created").getAsBoolean();
                            if (created) {
                                roomID = recievedMessage.get("roomid").getAsString();
                                serverName = recievedMessage.get("serverid").getAsString();
                                LeaderRoomHandler.getInstance().
                                    informAboutNewRoom(roomID, serverName);
                            }
                            break;

                        case "roomcreate":
                            roomID = recievedMessage.get("roomid").getAsString();
                            serverName = recievedMessage.get("serverid").getAsString();
                            Store.getInstance().addRoom(roomID, serverName);
                            break;

                        case "heartbeatcheck":
                            map.put("type", "heartbeatsuccess");
                            map.put("serverid", myServerName);
                            send(Util.getJsonString(map));
                            HeartbeatMonitor.getInstance().executeMonitor();
                            break;

                        case "failurenotice":
                            JsonArray jArray = recievedMessage.getAsJsonArray("failed");
                            List<String> failed = new ArrayList<>();
                            for (JsonElement e : jArray) failed.add(e.getAsString());
                            for (String failedServerName : failed) {
                                SystemState.getInstance().getSystemConfigMap().get(failedServerName)
                                        .setIsServerActive(false);
                                Store.getInstance().removeFaildServerDetails(failedServerName);
                            }
                            break;

                        case "election":
                            map.put("type", "answer");
                            send(Util.getJsonString(map));
                            HeartbeatMonitor.getInstance().interuptSubordinateHeartBeatMonitorThread();
                            SystemState.getInstance().setLeader(null);
                            HeartbeatMonitor.getInstance().setSubordinateStarted(false);
                            new LeaderElectionHandler(EventConstants.RECEIVE_ELECTION, null).start();
                            break;
                        
                        case "nomination":
                            HeartbeatMonitor.getInstance().interuptSubordinateHeartBeatMonitorThread();
                            SystemState.getInstance().setLeader(null);
                            HeartbeatMonitor.getInstance().setSubordinateStarted(false);
                            new LeaderElectionHandler(EventConstants.RECEIVE_NOMINATION, null).start();
                            break;
                        
                        case "coordinator":
                            HeartbeatMonitor.getInstance().interuptSubordinateHeartBeatMonitorThread();
                            SystemState.getInstance().setLeader(null);
                            HeartbeatMonitor.getInstance().setSubordinateStarted(false);
                            String leaderServerName = recievedMessage.get("value").getAsString();
                            new LeaderElectionHandler(EventConstants.RECEIVE_COORDINATOR, leaderServerName).start();
                            break;

                        case "checkidentityexist":
                            identity = recievedMessage.get("identity").getAsString();
                            serverName = recievedMessage.get("serverid").getAsString();
                            map.put("type", "identityexist");
                            map.put("identity", identity);
                            // check for the identity in local and tmp client lists
                            if(Store.getInstance().clientIdentityExistAndAddToTmp(identity)){
                                map.put("exist", "true");
                            } else {
                                SystemState s = SystemState.getInstance();
                                if (myServerName.equals(s.getLeader())) {
                                    // if you are the leader, ask from other servers for the identity
                                    Boolean identityExist = LeaderIndentityHandler.getInstance()
                                                                .askIndentityExist(identity, serverName);
                                    if(identityExist) {
                                        map.put("exist", "true");
                                    } else {
                                        map.put("exist", "false");
                                    }
                                } else {
                                    // if you are not the leader, reply the leader with identity not exist
                                    map.put("exist", "false");
                                }
                                Store.getInstance().removeClientIdentityFromTmp(identity);
                            }

                            send(Util.getJsonString(map));
                            break;

                        case "deleteroom":
                            String deleteRoomId = recievedMessage.get("roomid").getAsString();
                            String serverId = recievedMessage.get("serverid").getAsString();
                            // deletes the room from its room list
                            Store.getInstance().deleteRoomIDFromAllAndManaged(deleteRoomId);

                            // if this is the leader, inform other servers that the room is deleted
                            if (myServerName.equals(SystemState.getInstance().getLeader())) {
                                LeaderRoomHandler.getInstance().informAboutDeleteRoom(deleteRoomId, serverId);
                            }
                            break;

                        case "iamup":
                            String recoveredServerName = recievedMessage.get("serverid").getAsString();
                            List<String> activeServerNames = SystemState.getInstance().getActiveServerNameList();
                            map.put("type", "view");
                            map.put("liveServerNames", activeServerNames);
                            map.put("allRooms", Store.getInstance().getAllRooms());
                            send(Util.getJsonString(map));
                            SystemState.getInstance().getSystemConfigMap()
                                .get(recoveredServerName).setIsServerActive(true);
                            HeartbeatMonitor.getInstance().updateChecker(recoveredServerName);
                            break;

                        default:
                            break;
                    }
                }
            }
        } catch (SocketException e) {
            log.error("error socket connection interupted");
            log.error("error is {}", e.getMessage());
        } catch (IOException e) {
            log.error("error is {}", e.getMessage());
        }finally{
            try {
                coordinatorSocket.close();
            } catch (IOException e) {
                log.error("error occored while closing coordinator socket");
                log.error("error is {}", e.getMessage());
            }
        }
    }

    public void send(String message) {
        log.info("send a message {} to coordinator on ip {} and port {}", message, 
                coordinatorSocket.getRemoteSocketAddress(), coordinatorSocket.getPort());
        try {
            coordinatorOutputBuffer.write((message + "\n").getBytes("UTF-8"));
            coordinatorOutputBuffer.flush();
        } catch (IOException e) {
           log.error("error occred while sending the message");
           log.error("error is {}", e.getMessage());
        }
    }

}
