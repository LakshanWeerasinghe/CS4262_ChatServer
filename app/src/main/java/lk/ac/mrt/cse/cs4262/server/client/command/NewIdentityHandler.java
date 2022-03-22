package lk.ac.mrt.cse.cs4262.server.client.command;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import lk.ac.mrt.cse.cs4262.server.Store;
import lk.ac.mrt.cse.cs4262.server.SystemState;
import lk.ac.mrt.cse.cs4262.server.chatRoom.MainHall;
import lk.ac.mrt.cse.cs4262.server.client.Client;
import lk.ac.mrt.cse.cs4262.server.coordinator.CoordinatorConnector;
import lk.ac.mrt.cse.cs4262.server.leader.LeaderIndentityHandler;
import lk.ac.mrt.cse.cs4262.server.util.Util;

public class NewIdentityHandler {

    private Store store;
    private MainHall mainHall;
    
    public NewIdentityHandler(Store store, MainHall mainHall){
        this.store = store;
        this.mainHall = mainHall;
    }

    public String handleNewIdentity(String identity, Client client){
        boolean identityExists = true;
        Map<String, Object> map = new HashMap<>();
        map.put("type", "newidentity");

        if(!checkValidity(identity)) {
            map.put("approved", "false");
        } else {
            // check whether the identity exists in the local or tmp client lists
            if(store.clientIdentityExist(identity)){
                map.put("approved", "false");
            }
            else{
                // if this is not the leader, ask the leader whether identity exists
                SystemState s = SystemState.getInstance();
                if (!client.getConnectedServerName().equals(s.getLeader())) {
                    try {
                        CoordinatorConnector cc = new CoordinatorConnector(
                                s.getLeaderConfig().getHostIp(),
                                s.getLeaderConfig().getCoordinatorPort(),
                                true
                        );
                        Map<String, Object> leaderMap = new HashMap<>();
                        leaderMap.put("type", "checkidentityexist");
                        leaderMap.put("identity", identity);
                        leaderMap.put("serverid", client.getConnectedServerName());
                        cc.sendMessage(Util.getJsonString(leaderMap));

                        Boolean b = (Boolean) cc.handleMessage().get("exist");
                        identityExists = b.booleanValue();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else { // if this is the leader, ask from other servers whether the identity exists
                    identityExists = LeaderIndentityHandler.getInstance().askIndentityExist(identity, client.getConnectedServerName());
                }

                if (identityExists){
                    store.removeClientIdentityFromTmp(identity);
                    map.put("approved", "false");
                }
                else{
                    store.addClient(identity);
                    store.removeClientIdentityFromTmp(identity);
                    client.setClientIdentifier(identity);
                    client.setRoom(mainHall);
                    mainHall.addClientToRoom(client);
                    map.put("approved", "true");
                }
            }
        }
        return Util.getJsonString(map);
    }

    public Boolean checkValidity(String identity) {
        if(identity.length() < 3 || identity.length() > 16) {
            return false;
        } else if(!identity.matches("^[a-zA-Z0-9]*$")) {
            return false;
        } else if(!String.valueOf(identity.charAt(0)).matches("^[a-zA-Z0-9]*$")) {
            return false;
        } else {
            return true;
        }
    }


}