package lk.ac.mrt.cse.cs4262.server.leaderElector;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lk.ac.mrt.cse.cs4262.server.SystemState;
import lk.ac.mrt.cse.cs4262.server.leaderElector.state.LeaderElectorState;
import lk.ac.mrt.cse.cs4262.server.objects.ServerConfigObj;

public class LeaderElector {
    
    private static final Logger log = LoggerFactory.getLogger(LeaderElector.class);
    
    private static LeaderElector instance = null;

    private final ServerConfigObj myConfig;

    private Map<String, Boolean> electionAnswerMap = null;
    private Boolean noOneAnswered = true;

    private LeaderElectorState leaderElectorState;

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

    public void setLeaderElectorState(LeaderElectorState leaderElectorState) {
        this.leaderElectorState = leaderElectorState;
    }

    public ServerConfigObj getMyConfig() {
        return myConfig;
    }

    public void updateElectionAnswerMap(String key){
        electionAnswerMap.put(key, true);
        noOneAnswered = false;
    }
    
}
