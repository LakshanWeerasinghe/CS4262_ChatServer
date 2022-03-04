package lk.ac.mrt.cse.cs4262.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.ac.mrt.cse.cs4262.server.chatRoom.Room;

public class Store {

    private static Store instance = null;
    private List<String> localClients;
    private List<String> localTmpClients;
    private final Object localClientsLock = new Object();
    private Map<String, String> localRooms;
    private List<String> localTmpRooms;
    private final Object localRoomsLock = new Object();
    private Map<String, Room> managedRooms;

    private Store(){
        this.localClients = new ArrayList<>();
        this.localTmpClients = new ArrayList<>();
        this.localRooms = new HashMap<>();
        this.localTmpRooms = new ArrayList<>();
        this.managedRooms = new HashMap<>();
    }

    public static synchronized Store getInstance(){
        if(instance == null){
            instance = new Store();
        }
        return instance;
    }

    public boolean clientIdentityExist(String identity){
        synchronized(localClientsLock){
            if(localClients.contains(identity) || localTmpClients.contains(identity)){
                return true;
            }
            else{
                localTmpClients.add(identity);
                return false;
            }
        }
    }    

    public void addClient(String identity){
        synchronized(localClientsLock){
            localClients.add(identity);
            localTmpClients.remove(identity);
        }
    }

    public void removeClientIdentityFromTmp(String identity){
        synchronized(localClientsLock){
            localTmpClients.remove(identity);
        }
    }

    public boolean roomIDExist(String roomID) {
        synchronized(localRoomsLock) {
            if (localRooms.containsKey(roomID) || localTmpRooms.contains(roomID))
                return true;
            else {
                localTmpRooms.add(roomID);
                return false;
            }
        }
    }

    public void addRoom(String roomID, String serverName, Room room) {
        synchronized(localRoomsLock) {
            localRooms.put(roomID, serverName);
            managedRooms.put(roomID, room);
            localTmpRooms.remove(roomID);
        }
    }

    public void removeRoomIDFromTmp(String roomID) {
        synchronized(localRoomsLock) {
            localTmpRooms.remove(roomID);
        }
    }

    public Room getManagedRoom(String roomID) {
        return this.managedRooms.get(roomID);
    }
}
