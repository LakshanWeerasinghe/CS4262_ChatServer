package lk.ac.mrt.cse.cs4262.server.client.command;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import lk.ac.mrt.cse.cs4262.server.Store;
import lk.ac.mrt.cse.cs4262.server.SystemState;
import lk.ac.mrt.cse.cs4262.server.chatRoom.Room;
import lk.ac.mrt.cse.cs4262.server.client.Client;
import lk.ac.mrt.cse.cs4262.server.coordinator.CoordinatorConnector;
import lk.ac.mrt.cse.cs4262.server.leader.LeaderRoomHandler;
import lk.ac.mrt.cse.cs4262.server.util.Util;

public class CreateRoomHandler {

    public CreateRoomHandler() {
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
}
