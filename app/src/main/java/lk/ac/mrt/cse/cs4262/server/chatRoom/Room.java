package lk.ac.mrt.cse.cs4262.server.chatRoom;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lk.ac.mrt.cse.cs4262.server.client.Client;

public class Room {

    private String name;
    private List<Client> clientList;
    private ExecutorService threadPool;

    public Room(String name) {
        this.name = name;
        this.clientList = new ArrayList<>();
        this.threadPool = Executors.newCachedThreadPool();
    }

    public String getRoomName() {
        return name;
    }

    public void addClientToRoom(Client client) {
        clientList.add(client);
    }

    public void removeClientFromRoom(Client client) {
        clientList.remove(client);
    }

    public void broadcast(Client sender, String message) {
        threadPool.execute(new BroadcastMessage(sender, message));
    }

    private class BroadcastMessage implements Runnable {
        private Client sender;
        private String message;

        public BroadcastMessage(Client sender, String message) {
            this.sender = sender;
            this.message = message;
        }

        @Override
        public void run() {
            for (Client client : clientList) {
                if (!client.equals(sender)) {
                    client.send(message);
                }
            }
        }
    }
}
