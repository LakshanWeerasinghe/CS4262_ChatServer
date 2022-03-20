package lk.ac.mrt.cse.cs4262.server.leaderElector;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lk.ac.mrt.cse.cs4262.server.SystemState;
import lk.ac.mrt.cse.cs4262.server.leaderElector.state.ChoosingState;
import lk.ac.mrt.cse.cs4262.server.leaderElector.state.LeaderElectorState;
import lk.ac.mrt.cse.cs4262.server.leaderElector.state.LeaderState;
import lk.ac.mrt.cse.cs4262.server.leaderElector.state.StartUpState;
import lk.ac.mrt.cse.cs4262.server.objects.ServerConfigObj;

public class LeaderElector {
    
    private static final Logger log = LoggerFactory.getLogger(LeaderElector.class);
    
    private static LeaderElector instance = null;

    private final ServerConfigObj myConfig;

    private final Integer TIME_INTERVAL_T2 = 5000;
    private final Integer TIME_INTERVAL_T4 = 5000;


    private Map<String, Boolean> electionAnswerMap = null;
    private Boolean noOneAnswered = true;

    private LeaderElectorState leaderElectorState;

    private Thread currentThread = null;

    private LeaderElector(String serverName){
        this.myConfig = SystemState.getInstance().getServerConfig(serverName);
        this.leaderElectorState = new StartUpState(this);
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

    public void interuptThread(){
        currentThread.interrupt();
        currentThread = null;
    }


    public void startElection() throws InterruptedException{
        StartUpState startUpState = (StartUpState)leaderElectorState;
        startUpState.sendElectionMessages();
        Thread.sleep(TIME_INTERVAL_T2);
        if(noOneAnswered){
            SystemState.getInstance().setLeader(myConfig.getName());
            leaderElectorState.dispatchEvent(EventConstants.T2_EXPIRED);
            LeaderState leaderState = (LeaderState)leaderElectorState;
            leaderState.sendCoordinatorMsg();
            return;
        }else{
            leaderElectorState.dispatchEvent(EventConstants.RECEIVE_ANSWER);
            ChoosingState choosingState = (ChoosingState)leaderElectorState;
            choosingState.findHighestPriorityServer();
            choosingState.sendNominationMessage();
        }
        if(leaderElectorState instanceof StartUpState){
            startElection();
        }
    }

    public void waitForNominationOrCoordinationMsg() throws InterruptedException{
        Thread.sleep(TIME_INTERVAL_T4);
    }

    public Map<String, Boolean> getElectionAnswerMap() {
        return electionAnswerMap;
    }

    public void setElectionAnswerMap(Map<String, Boolean> electionAnswerMap) {
        this.electionAnswerMap = electionAnswerMap;
    }

    public Boolean getNoOneAnswered() {
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
