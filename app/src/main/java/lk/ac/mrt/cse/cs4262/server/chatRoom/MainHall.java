package lk.ac.mrt.cse.cs4262.server.chatRoom;

import lk.ac.mrt.cse.cs4262.server.client.Client;

public final class MainHall extends Room {

    private static MainHall instance = null;

    private MainHall(String name, Client client){
        super(name, client);
    }

    public static synchronized MainHall getInstance(String name, Client client){
        if(instance == null){
            instance = new MainHall(name, client);
        }
        return instance;
    }
}
