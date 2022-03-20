package lk.ac.mrt.cse.cs4262.server;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import lk.ac.mrt.cse.cs4262.server.objects.ServerConfigObj;

public class SystemState {

    private static SystemState instance = null;
    private final Object leaderUpdatingLock = new Object();

    private String leader;
    private Map<String, ServerConfigObj> systemConfigMap;
    private AtomicBoolean isLeaderElected = new AtomicBoolean(true);
    
    private SystemState(){
        this.leader = null;
    }

    public static synchronized SystemState getInstance(){
        if(instance == null){
            instance = new SystemState();
        }
        return instance;
    }

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        synchronized(leaderUpdatingLock){
            this.leader = leader;
        }
    }

    public Map<String, ServerConfigObj> getSystemConfigMap() {
        return systemConfigMap;
    }

    public void setSystemConfigMap(Map<String, ServerConfigObj> systemConfigMap) {
        this.systemConfigMap = systemConfigMap;
    }

    public boolean allServerActive(){
        return systemConfigMap.values().stream().allMatch(x -> x.getIsServerActive());
    }

    public ServerConfigObj getLeaderConfig() {
        if (systemConfigMap != null) return systemConfigMap.get(leader);
        return null;
    }

    public String getIPOfServer(String serverName) {
        if (systemConfigMap.containsKey(serverName)) return systemConfigMap.get(serverName).getHostIp();
        return null;
    }

    public int getCoordinatorPortOfServer(String serverName) {
        if (systemConfigMap.containsKey(serverName)) return systemConfigMap.get(serverName).getCoordinatorPort();
        return -1;
    }

    public ServerConfigObj getServerConfig(String serverName){
        return this.systemConfigMap.get(serverName);
    }
   
    public boolean isLeaderElected(){
        return isLeaderElected.get();
    }

    public void updateLeaderElectionStatus(boolean value){
        isLeaderElected.set(value);
    }
}
