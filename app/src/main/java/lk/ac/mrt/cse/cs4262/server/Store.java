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
    private Map<String, String> allRooms;
    private List<String> tmpRooms;
    private final Object roomsLock = new Object();
    private Map<String, Room> managedRooms;

    private Store(){
        this.localClients = new ArrayList<>();
        this.localTmpClients = new ArrayList<>();
        this.allRooms = new HashMap<>(); // all rooms in the whole system
        this.tmpRooms = new ArrayList<>();
        this.managedRooms = new HashMap<>(); // rooms managed by this server
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
        synchronized(roomsLock) {
            if (allRooms.containsKey(roomID) || tmpRooms.contains(roomID))
                return true;
            else {
                tmpRooms.add(roomID);
                return false;
            }
        }
    }

    public void addRoom(String roomID, String serverName) {
        synchronized(roomsLock) {
            allRooms.put(roomID, serverName);
            if (tmpRooms.contains(roomID)) tmpRooms.remove(roomID);
        }
    }

    public void addManagedRoom(String roomID, String serverName, Room room) {
        synchronized(roomsLock) {
            allRooms.put(roomID, serverName);
            managedRooms.put(roomID, room);
            tmpRooms.remove(roomID);
        }
    }

    public void removeRoomIDFromTmp(String roomID) {
        synchronized(roomsLock) {
            tmpRooms.remove(roomID);
        }
    }

    public Room getManagedRoom(String roomID) {
        return this.managedRooms.get(roomID);
    }
}
