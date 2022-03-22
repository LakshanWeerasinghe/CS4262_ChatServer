package lk.ac.mrt.cse.cs4262.server.leaderElector.state;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lk.ac.mrt.cse.cs4262.server.SystemState;
import lk.ac.mrt.cse.cs4262.server.coordinator.CoordinatorConnector;
import lk.ac.mrt.cse.cs4262.server.leaderElector.EventConstants;
import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElectionHandler;
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


    private void sendElectionMessages(){
        getLeaderElector().setElectionAnswerMap(new HashMap<>());
        getLeaderElector().setNoOneAnswered(true);
        SystemState.getInstance().getSystemConfigMap().values().forEach(
            x -> {
                if(x.getPriority() > getLeaderElector().getMyConfig().getPriority() 
                        && x.getName() != getLeaderElector().getMyConfig().getName()){
                    try {
                        CoordinatorConnector highPriorityServerConnector = 
                                                new CoordinatorConnector(x.getHostIp(), x.getCoordinatorPort(), true)
                                                .setConnectingServerName(x.getName());
                        Thread highPriorityServerConnectorThread = new Thread(highPriorityServerConnector);
                        highPriorityServerConnector.sendMessage(electionMsg);
                        highPriorityServerConnectorThread.start();
                        getLeaderElector().getElectionAnswerMap().put(x.getName(), false);
                    } catch (IOException e) {
                        log.error("error sending election msg to server {}", x.getName());
                        log.error("error is {}", e.getMessage());
                    }                    
                }
            }
        );
    }

    @Override
    public void dispatchEvent(String event, LeaderElectionHandler owner) throws InterruptedException {
        switch(event){
            case EventConstants.START:
                sendElectionMessages();
                Thread.sleep(EventConstants.TIME_INTERVAL_T2);
                if(getLeaderElector().isNoOneAnswered()){
                    owner.setCoordinatingServerName(getLeaderElector().getMyConfig().getName());
                    dispatchEvent(EventConstants.T2_EXPIRED, owner);
                }else{
                    dispatchEvent(EventConstants.RECEIVE_ANSWER, owner);
                }
                if(getLeaderElector().getLeaderElectorState() instanceof StartUpState){
                    dispatchEvent(EventConstants.START, owner);
                }
                break;

            case EventConstants.T2_EXPIRED:
                getLeaderElector().setLeaderElectorState(new LeaderState(getLeaderElector()), owner);
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.SEND_COORDINATOR, owner);
                break;
            
            case EventConstants.RECEIVE_ANSWER:
                getLeaderElector().setLeaderElectorState(new ChoosingState(getLeaderElector()), owner);
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.SEND_NOMINATION, owner);
                break;

            case EventConstants.RECEIVE_ELECTION:
                getLeaderElector().setLeaderElectorState(new SomeoneStartState(getLeaderElector()), owner);
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.RECEIVE_ELECTION, owner);
                break;

            case EventConstants.RECEIVE_COORDINATOR:
                getLeaderElector().setLeaderElectorState(new NotLeaderState(getLeaderElector()), owner);
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.RECEIVE_COORDINATOR, owner);
                break;
                
            default:
                break;
        }
        
    }

    @Override
    public String toString() {
        return "Startup State";
    }
    
}
