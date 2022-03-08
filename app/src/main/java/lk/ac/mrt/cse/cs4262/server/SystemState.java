package lk.ac.mrt.cse.cs4262.server;

import java.util.Map;
import java.util.function.Predicate;

import lk.ac.mrt.cse.cs4262.server.objects.ServerConfigObj;

public class SystemState {

    private static SystemState instance = null;
    private String leader;
    private Map<String, ServerConfigObj> systemConfigMap;
    
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
        this.leader = leader;
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
   
}
