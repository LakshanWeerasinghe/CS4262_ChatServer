package lk.ac.mrt.cse.cs4262.server.chatRoom;

import java.util.HashMap;
import java.util.Map;

import lk.ac.mrt.cse.cs4262.server.client.Client;
import lk.ac.mrt.cse.cs4262.server.util.Util;

public final class MainHall extends Room {

    private static MainHall instance = null;

    private MainHall(String name){
        super(name);
    }

    public static synchronized MainHall getInstance(String name){
        if(instance == null){
            instance = new MainHall(name);
        }
        return instance;
    }


    @Override
    public void broadcast(Client sender, String identity) {
        Map<String, String> map = new HashMap<>();
        map.put("type", "roomchange");
        map.put("identity", identity);
        map.put("former", "");
        map.put("roomid", getRoomName());

        String message =  Util.getJsonString(map);
        super.broadcast(sender, message);
        sender.send(message);
     }
    
}
