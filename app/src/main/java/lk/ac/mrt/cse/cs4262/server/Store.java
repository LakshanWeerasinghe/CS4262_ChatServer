package lk.ac.mrt.cse.cs4262.server;

import java.util.ArrayList;
import java.util.List;

public class Store {

    private static Store instance = null;
    private List<String> localClients;
    private List<String> localTmpClients;
    private final Object localClientsLock = new Object();

    private Store(){
        this.localClients = new ArrayList<>();
        this.localTmpClients = new ArrayList<>();
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
}
