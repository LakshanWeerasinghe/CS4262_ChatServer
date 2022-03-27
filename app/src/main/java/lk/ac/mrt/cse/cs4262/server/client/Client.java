package lk.ac.mrt.cse.cs4262.server.client;

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
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lk.ac.mrt.cse.cs4262.server.Server;
import lk.ac.mrt.cse.cs4262.server.Store;
import lk.ac.mrt.cse.cs4262.server.chatRoom.Room;
import lk.ac.mrt.cse.cs4262.server.client.command.RoomHandler;
import lk.ac.mrt.cse.cs4262.server.client.command.NewIdentityHandler;
import lk.ac.mrt.cse.cs4262.server.util.Util;

public class Client implements Runnable {

    private Socket clientSocket;
    private BufferedReader clientInputBuffer;
    private DataOutputStream clientOutputBuffer;
    private Gson gson;
    private NewIdentityHandler newIdentityHandler;
    private String clientIdentifier;
    private Room room;
    private String connectedServer;
    private String ownedRoom;
    private boolean clientClosed;

    private static final Logger log = LoggerFactory.getLogger(Client.class);

    public Client(Socket clientSocket, Server server) {
        this.clientSocket = clientSocket;
        try {
            this.clientInputBuffer = new BufferedReader(new InputStreamReader(
                    this.clientSocket.getInputStream(), "utf-8"));

        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            this.clientOutputBuffer = new DataOutputStream(this.clientSocket.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.gson = new Gson();
        this.connectedServer = server.getServerName();
        this.newIdentityHandler = server.getNewIdentityHandler();
        this.ownedRoom = "";
        this.clientIdentifier = "";
    }

    @Override
    public void run() {
        try {
            clientClosed = false;
            while (!clientSocket.isClosed()) {
                String bufferedMessage = this.clientInputBuffer.readLine();

                if (this.gson == null) {
                    this.gson = new Gson();
                }

                if (bufferedMessage != null) {
                    JsonObject jsonObject = this.gson.fromJson(bufferedMessage, JsonObject.class);

                    String messageType = jsonObject.get("type").getAsString();
                    log.info("received message {} from {}", jsonObject, clientIdentifier);

                    Map<String, Object> map = new HashMap<>();
                    switch (messageType) {
                        case "newidentity":
                            String identity = jsonObject.get("identity").getAsString();
                            String response = newIdentityHandler.handleNewIdentity(identity, Client.this);
                            
                            send(response);

                            if (this.room != null) {
                                map.put("type", "roomchange");
                                map.put("identity", identity);
                                map.put("former", "");
                                map.put("roomid", this.room.getRoomName());
                                String message = Util.getJsonString(map);

                                room.broadcast(Client.this, message);
                                send(message);
                            }
                            break;

                        case "message":
                            map.put("type", "message");
                            map.put("identity", this.clientIdentifier);
                            map.put("content", jsonObject.get("content").getAsString());
                            room.broadcast(Client.this, Util.getJsonString(map));
                            break;

                        case "createroom":
                            String roomID = jsonObject.get("roomid").getAsString();
                            map.put("type", "createroom");
                            map.put("roomid", roomID);

                            Room newlyCreatedoom = RoomHandler.handleCreateRoom(roomID, Client.this);
                            if (newlyCreatedoom != null) {
                                map.put("approved", "true");
                                response = Util.getJsonString(map);
                                send(response);

                                Map<String, Object> roomChange = new HashMap<>();
                                roomChange.put("type", "roomchange");
                                roomChange.put("identity", this.clientIdentifier);
                                roomChange.put("former", this.room.getRoomName());
                                roomChange.put("roomid", roomID);
                                String roomChangeMessage = Util.getJsonString(roomChange);

                                Room formerRoom = this.room;
                                setRoom(newlyCreatedoom);
                                setOwnedRoom(roomID);
                                formerRoom.removeClientFromRoom(this);
                                newlyCreatedoom.addClientToRoom(this);

                                send(roomChangeMessage);
                                formerRoom.broadcast(Client.this, roomChangeMessage);
                            } else {
                                map.put("approved", "false");
                                response = Util.getJsonString(map);
                                send(response);
                            }
                            break;

                        case "who":
                            List<Client> clientList = this.room.getClientList();
                            List<String> clientNamesList = new ArrayList<String>();
                            for (Client client : clientList) {
                                clientNamesList.add(client.clientIdentifier);
                            }
                            map.put("type", "roomcontents");
                            map.put("roomid", this.room.getRoomName());
                            map.put("identities", clientNamesList);
                            map.put("owner", this.room.getOwner() == null? "" : this.room.getOwner().clientIdentifier);

                            String roomContentMessage = Util.getJsonString(map);
                            send(roomContentMessage);
                            break;

                        case "list":
                            List<String> allRoomsNamesList = RoomHandler.getAllRoomsNames();

                            map.put("type", "roomlist");
                            map.put("rooms", allRoomsNamesList);


                            String roomListMessage = Util.getJsonString(map);
                            send(roomListMessage);
                            break;

                        case "joinroom":
                            String roomName = jsonObject.get("roomid").getAsString();

                            map = RoomHandler.handleJoinRoom(roomName, Client.this);

                            send(Util.getJsonString(map));
                            break;

                        case "movejoin":
                            String newRoom = jsonObject.get("roomid").getAsString();
                            String formerRoom = jsonObject.get("former").getAsString();

                            this.clientIdentifier = jsonObject.get("identity").getAsString();

                            map = RoomHandler.handleMoveJoin(newRoom, formerRoom, Client.this);                            

                            Map<String, Object> serverChangeMap = new HashMap<>();

                            serverChangeMap.put("type", "serverchange");
                            serverChangeMap.put("approved", "true");
                            serverChangeMap.put("serverid", this.connectedServer);

                            send(Util.getJsonString(serverChangeMap));
                            send(Util.getJsonString(map));
                            break;

                        case "deleteroom":
                            String deleteRoomId = jsonObject.get("roomid").getAsString();

                            map.put("type", "deleteroom");
                            map.put("roomid", deleteRoomId);

                            Boolean approved = RoomHandler.handleDeleteRoom(deleteRoomId, Client.this);

                            map.put("approved", approved.toString());

                            String message = Util.getJsonString(map);
                            send(message);
                            break;

                        case "quit":
                            map = closeClient();
                            send(Util.getJsonString(map));
                            clientSocket.close();
                            break;

                        default:
                            break;
                    }

                }
                // detect command or message
                // handle the messge accordingly
            }
        } catch (SocketException e) {
            log.error("Client connection interupted");            
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (!clientClosed) closeClient();
        }

    }

    public void send(String value) {
        try {
            clientOutputBuffer.write((value + "\n").getBytes("UTF-8"));
            clientOutputBuffer.flush();
        } catch (IOException e1) {
            e1.printStackTrace();
            System.exit(1);
        }
    }

    public void setClientIdentifier(String value) {
        this.clientIdentifier = value;
    }

    public String getClientIdentifier() {
        return clientIdentifier;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Room getRoom() {
        return room;
    }

    public String getConnectedServerName() {
        return this.connectedServer;
    }

    public void setOwnedRoom(String roomID) {
        this.ownedRoom = roomID;
    }

    public String getOwnedRoom() {
        return this.ownedRoom;
    }

    private Map<String, Object> closeClient() {
        Store.getInstance().removeClient(this.clientIdentifier);
        Map<String, Object> map = new HashMap<>();

        map.put("type", "roomchange");
        map.put("former", this.room.getRoomName());
        map.put("roomid", "");
        map.put("identity", this.clientIdentifier);

        this.room.broadcast(Client.this, Util.getJsonString(map));
        this.room.removeClientFromRoom(Client.this);

        RoomHandler.handleDeleteRoom(this.room.getRoomName(), Client.this);

        clientClosed = true;

        log.info("Client Closed!");

        return map;
    }
}
