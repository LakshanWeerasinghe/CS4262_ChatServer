package lk.ac.mrt.cse.cs4262.server.client.command;

import java.io.IOException;
import java.util.*;

import lk.ac.mrt.cse.cs4262.server.Store;
import lk.ac.mrt.cse.cs4262.server.SystemState;
import lk.ac.mrt.cse.cs4262.server.chatRoom.Room;
import lk.ac.mrt.cse.cs4262.server.client.Client;
import lk.ac.mrt.cse.cs4262.server.coordinator.CoordinatorConnector;
import lk.ac.mrt.cse.cs4262.server.leader.LeaderRoomHandler;
import lk.ac.mrt.cse.cs4262.server.objects.ServerConfigObj;
import lk.ac.mrt.cse.cs4262.server.util.Util;

public class RoomHandler {

    public static final int JOIN_DENIED = 100;
    public static final int JOIN_ACCEPTED_OWN = 101;

    public RoomHandler() {
    }

    public static Room handleCreateRoom(String roomID, Client client) {
        Room room = null;
        // first assume roomID exists
        boolean roomIDExists = true;
        // then check if roomID actually exist locally
        if (!Store.getInstance().roomIDExist(roomID) && client.getOwnedRoom().equals("")) {
            // roomID does not exist locally
            roomIDExists = false;
            // if this is not the leader, ask the leader
            SystemState s = SystemState.getInstance();
            if (!client.getConnectedServerName().equals(s.getLeader())) {
                try {
                    CoordinatorConnector cc = new CoordinatorConnector(
                        s.getLeaderConfig().getHostIp(), 
                        s.getLeaderConfig().getCoordinatorPort(),
                        true
                    );

                    Map<String, Object> map = new HashMap<>();
                    map.put("type", "checkroomexist");
                    map.put("roomid", roomID);
                    map.put("serverid", client.getConnectedServerName());
                    cc.sendMessage(Util.getJsonString(map));

                    Boolean b = (Boolean) cc.handleMessage().get("exist");
                    roomIDExists = b.booleanValue();

                    map.clear();
                    map.put("type", "createroomack");

                    if (!roomIDExists) {
                        room = new Room(roomID, client);
                        Store.getInstance().addManagedRoom(roomID, client.getConnectedServerName(), room);
                        map.put("created", true);
                        map.put("roomid", roomID);
                        map.put("serverid", client.getConnectedServerName());                        
                    } else {
                        map.put("created", false);
                    }
                    cc.sendMessage(Util.getJsonString(map));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                room = new Room(roomID, client);
                Store.getInstance().addManagedRoom(roomID, client.getConnectedServerName(), room);
                LeaderRoomHandler.getInstance().informAboutNewRoom(roomID, client.getConnectedServerName());
            }        
        }
        return room;
    }

    public static List<String> getAllRoomsNames() {
        Map<String, String> allRooms = Store.getInstance().getAllRooms();
        Set<String> allRoomsNamesSet = new HashSet<>(allRooms.keySet());

        // adding MainHall rooms of live servers
        List<ServerConfigObj> serverConfigurations = new ArrayList<ServerConfigObj>(SystemState.getInstance().getSystemConfigMap().values());
        List<String> liveServerNames = new ArrayList<String>();
        for (ServerConfigObj serverConfigObj : serverConfigurations) {
            if (serverConfigObj.getIsServerActive() == true) {
                liveServerNames.add(serverConfigObj.getName());
            }
        }
        for (String serverName : liveServerNames) {
            allRoomsNamesSet.add("MainHall-" + serverName);
        }
        return new ArrayList<String>(allRoomsNamesSet);
    }

    public static Map<String, Object> handleJoinRoom(String roomID, Client client) {
        boolean canJoin = false;
        Map<String, Object> map = new HashMap<>();
        // first check if roomID exists
        if (Store.getInstance().getAllRooms().keySet().contains(roomID)) {
            // then check whether the client is not an owner of another room
            if (client.getOwnedRoom().equals("")) {
                canJoin = true;
            }
        }
        if (!canJoin) {
            // Joining denied
            map.put("type", "roomchange");
            map.put("identity", client.getClientIdentifier());
            map.put("former", roomID);
            map.put("roomid", roomID);
            return map;
        } else {
            // change room right away if the room is managed by this server
            if (Store.getInstance().isManagedRoom(roomID)) {
                Room formerRoom = client.getRoom();
                formerRoom.removeClientFromRoom(client);
                Room changedRoom = Store.getInstance().getManagedRoom(roomID);
                changedRoom.addClientToRoom(client);
                client.setRoom(changedRoom);
                
                map.put("type", "roomchange");
                map.put("identity", client.getClientIdentifier());
                map.put("former", formerRoom.getRoomName());
                map.put("roomid", roomID);

                formerRoom.broadcast(client, Util.getJsonString(map));
                changedRoom.broadcast(client, Util.getJsonString(map));

                return map;
            } else {
                // room is in another server
                String serverName = Store.getInstance().getAllRooms().get(roomID);
                String ip = SystemState.getInstance().getIPOfServer(serverName);
                int port = SystemState.getInstance().getClientPortOfServer(serverName);

                Room formerRoom = client.getRoom();
                formerRoom.removeClientFromRoom(client);

                map.put("type", "roomchange");
                map.put("identity", client.getClientIdentifier());
                map.put("former", formerRoom.getRoomName());
                map.put("roomid", roomID);

                formerRoom.broadcast(client, Util.getJsonString(map));
                map.clear();

                map.put("type", "route");
                map.put("roomid", roomID);
                map.put("host", ip);
                map.put("port", String.valueOf(port));

                return map;
            }
        }
    }

    public static Map<String, Object> handleMoveJoin(String roomID, String formerRoomID, Client client) {
        Room room;
        Map<String, Object> map = new HashMap<>();

        map.put("type", "roomchange");
        map.put("identity", client.getClientIdentifier());
        map.put("former", formerRoomID);

        if (Store.getInstance().isManagedRoom(roomID)) {
            room = Store.getInstance().getManagedRoom(roomID);
        } else {
            room = Store.getInstance().getManagedRoom("MainHall-"+client.getConnectedServerName());
        }
        room.addClientToRoom(client);
        client.setRoom(room);

        map.put("roomid", room.getRoomName());

        room.broadcast(client, Util.getJsonString(map));
        
        return map;
    }

}
