package lk.ac.mrt.cse.cs4262.server.leaderElector;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lk.ac.mrt.cse.cs4262.server.SystemState;
import lk.ac.mrt.cse.cs4262.server.leaderElector.state.LeaderElectorState;
import lk.ac.mrt.cse.cs4262.server.objects.ServerConfigObj;

public class LeaderElector {
        
    private static LeaderElector instance = null;
    private static final Object leaderElectorStateLock = new Object();

    private final ServerConfigObj myConfig;

    private Map<String, Boolean> electionAnswerMap = null;
    private Boolean noOneAnswered = true;

    private LeaderElectorState leaderElectorState;

    private LeaderElectionHandler ownerThread = null;

    private LeaderElector(String serverName){
        this.myConfig = SystemState.getInstance().getServerConfig(serverName);
    }

    public static synchronized LeaderElector getInstance(String serverName){
        if(instance == null){
            instance = new LeaderElector(serverName);    
        }
        return instance;
    }

    public static LeaderElector getInstance(){
        return instance;
    }

    public Map<String, Boolean> getElectionAnswerMap() {
        return electionAnswerMap;
    }

    public void setElectionAnswerMap(Map<String, Boolean> electionAnswerMap) {
        this.electionAnswerMap = electionAnswerMap;
    }

    public Boolean isNoOneAnswered() {
        return noOneAnswered;
    }

    public void setNoOneAnswered(Boolean noOneAnswered) {
        this.noOneAnswered = noOneAnswered;
    }

    public LeaderElectorState getLeaderElectorState() {
        return leaderElectorState;
    }

    public void setLeaderElectorState(LeaderElectorState leaderElectorState, LeaderElectionHandler owner) {
        synchronized(leaderElectorStateLock){
            if(owner == null){
                this.leaderElectorState = leaderElectorState;
            }
            else{
                if(owner.equals(ownerThread)){
                    this.leaderElectorState = leaderElectorState;
                }else{
                    if(ownerThread != null){
                        ownerThread.stopThread();
                    }
                    ownerThread = owner;
                    this.leaderElectorState = leaderElectorState;
                }
            }
        }
    }

    public ServerConfigObj getMyConfig() {
        return myConfig;
    }

    public void updateElectionAnswerMap(String key){
        electionAnswerMap.put(key, true);
        noOneAnswered = false;
    }

    public Runnable getOwnerThread() {
        return ownerThread;
    }

    public void setOwnerThread(LeaderElectionHandler ownerThread) {
        this.ownerThread = ownerThread;
    }

    @Override
    public String toString() {
        return "LeaderElector [leaderElectorState=" + leaderElectorState + "]";
    }
        
}
