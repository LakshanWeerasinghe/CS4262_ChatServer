package lk.ac.mrt.cse.cs4262.server.leaderElector.state;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lk.ac.mrt.cse.cs4262.server.SystemState;
import lk.ac.mrt.cse.cs4262.server.coordinator.CoordinatorConnector;
import lk.ac.mrt.cse.cs4262.server.leaderElector.EventConstants;
import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElector;
import lk.ac.mrt.cse.cs4262.server.util.Util;

public class StartUpState extends LeaderElectorState{
    
    private static final Logger log = LoggerFactory.getLogger(StartUpState.class);

    private String electionMsg;

    public StartUpState(LeaderElector leaderElector) {
        super(leaderElector);
        buildElectionMsg();
    }

    private void buildElectionMsg(){
        Map<String, Object> electionMsgMap = new HashMap<>();
        electionMsgMap.put("type", "election");
        electionMsg = Util.getJsonString(electionMsgMap);
    }


    public void sendElectionMessages(){
        getLeaderElector().setElectionAnswerMap(new HashMap<>());
        SystemState.getInstance().getSystemConfigMap().values().forEach(
            x -> {
                if(x.getPriority() > getLeaderElector().getMyConfig().getPriority()){
                    try {
                        getLeaderElector().getElectionAnswerMap().put(x.getName(), false);
                        CoordinatorConnector highPriorityServerConnector = 
                                                new CoordinatorConnector(x.getHostIp(), x.getCoordinatorPort(), true)
                                                .setConnectingServerName(x.getName());
                        Thread highPriorityServerConnectorThread = new Thread(highPriorityServerConnector);
                        highPriorityServerConnector.sendMessage(electionMsg);
                        highPriorityServerConnectorThread.start();
                    } catch (IOException e) {
                        log.error("error sending election msg to server {}", x.getName());
                        log.error("error is {}", e.getMessage());
                    }                    
                }
            }
        );
    }

    @Override
    public void dispatchEvent(String event) {
        switch(event){
            case EventConstants.T2_EXPIRED:
                getLeaderElector().setLeaderElectorState(new LeaderState(getLeaderElector()));
                break;
            
            case EventConstants.RECEIVE_ANSWER:
                getLeaderElector().setLeaderElectorState(new ChoosingState(getLeaderElector()));
                break;

            case EventConstants.RECEIVE_ELECTION:
                getLeaderElector().setLeaderElectorState(new SomeoneStartState(getLeaderElector()));
                break;
                
            default:
                break;
        }
        
    }
    
}
