package lk.ac.mrt.cse.cs4262.server.client.command;

import java.util.HashMap;
import java.util.Map;

import lk.ac.mrt.cse.cs4262.server.Store;
import lk.ac.mrt.cse.cs4262.server.chatRoom.MainHall;
import lk.ac.mrt.cse.cs4262.server.client.Client;
import lk.ac.mrt.cse.cs4262.server.util.Util;

public class NewIdentityHandler {

    private Store store;
    private MainHall mainHall;
    
    public NewIdentityHandler(Store store, MainHall mainHall){
        this.store = store;
        this.mainHall = mainHall;
    }

    public String handleNewIdentity(String identity, Client client){
        Map<String, Object> map = new HashMap<>();
        map.put("type", "newidentity");

        if(store.clientIdentityExist(identity)){
            map.put("approved", "false");
        }
        else{
            // call to the leader client handler component
            boolean isClientIdentityExist = false;
            if(isClientIdentityExist){
                store.removeClientIdentityFromTmp(identity);
                map.put("approved", "false");
            }
            else{
                store.addClient(identity);
                client.setClientIdentifier(identity);
                client.setRoom(mainHall);
                mainHall.addClientToRoom(client);
                map.put("approved", "true");
            }
        }
        return Util.getJsonString(map);
    }


}
