package lk.ac.mrt.cse.cs4262.server.client.command;

import lk.ac.mrt.cse.cs4262.server.Store;
import lk.ac.mrt.cse.cs4262.server.chatRoom.Room;
import lk.ac.mrt.cse.cs4262.server.client.Client;

public class CreateRoomHandler {
    private Store store;

    public CreateRoomHandler(Store store) {
        this.store = store;
    }

    public Room handleCreateRoom(String roomID, Client client) {
        Room room = null;
        if (!store.roomIDExist(roomID) && client.getOwnedRoom().equals("")) {
            // call to leader room handler component
            boolean roomIDExists = false;
            if (!roomIDExists) {
                room = new Room(roomID);
                store.addRoom(roomID, client.getConnectedServerName(), room);
            }
        }
        store.removeRoomIDFromTmp(roomID);
        return room;
    }
}
