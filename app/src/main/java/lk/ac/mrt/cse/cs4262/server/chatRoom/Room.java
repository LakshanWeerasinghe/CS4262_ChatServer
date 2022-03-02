package lk.ac.mrt.cse.cs4262.server.chatRoom;

import java.util.ArrayList;
import java.util.List;

import lk.ac.mrt.cse.cs4262.server.client.Client;

public abstract class Room {
    
    private String name;
    private List<Client> clientList;


    public Room(String name){
        this.name = name;
        this.clientList = new ArrayList<>();
    }

    public String getRoomName(){
        return name;
    }

    public void addClientToRoom(Client client) {
        clientList.add(client);
    }

    public void removeClientFromRoom(Client client) {
        clientList.remove(client);
    }

    public void broadcast(Client sender, String message) {
       for (Client client : clientList) {
           if(!client.equals(sender)){
                client.send(message);
           }
       } 
    }
}

