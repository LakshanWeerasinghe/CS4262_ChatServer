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
import lk.ac.mrt.cse.cs4262.server.objects.ServerConfigObj;
import lk.ac.mrt.cse.cs4262.server.util.Util;

public class ChoosingState extends LeaderElectorState{

    private static final Logger log = LoggerFactory.getLogger(ChoosingState.class);

    private ServerConfigObj highestPriorityServer = null;
    private String nominationMsg;

    public ChoosingState(LeaderElector leaderElector) {
        super(leaderElector);
        buildNominationMsg();
    }

    private void buildNominationMsg(){
        Map<String, Object> nominationMsgMap = new HashMap<>();
        nominationMsgMap.put("type", "nomination");
        nominationMsg = Util.getJsonString(nominationMsgMap);
    }

    private void findHighestPriorityServer(LeaderElectionHandler owner) throws InterruptedException{
        if(getLeaderElector().getElectionAnswerMap().keySet().size() > 0){
            for (String key : getLeaderElector().getElectionAnswerMap().keySet()) {
                if(getLeaderElector().getElectionAnswerMap().get(key)){
                    ServerConfigObj serverConfig = SystemState.getInstance().getServerConfig(key);
                    if(highestPriorityServer == null){
                        highestPriorityServer = serverConfig;
                        continue;
                    }
                    if(serverConfig.getPriority() > highestPriorityServer.getPriority()){
                        highestPriorityServer = serverConfig;
                    }
                }
            }
            getLeaderElector().getElectionAnswerMap().remove(highestPriorityServer.getName());
        }
        else{
            highestPriorityServer = null;
            dispatchEvent(EventConstants.T3_EXPIRED, owner);
        }
        
    }

    private void sendNominationMessage(LeaderElectionHandler owner) throws InterruptedException{
        try {
            CoordinatorConnector nominationConnector = 
                new CoordinatorConnector(highestPriorityServer.getHostIp(), 
                                            highestPriorityServer.getCoordinatorPort())
                .createOutputBuffer()
                .createInputBuffer();
                nominationConnector.sendMessage(nominationMsg);
                Thread.sleep(EventConstants.TIME_INTERVAL_T3);
                Map<String, Object> answer =  nominationConnector.handleMessage(); // handle coodiator Message
                nominationConnector.close();
                if(answer != null && answer.containsKey("coordinator")){
                    owner.setCoordinatingServerName(highestPriorityServer.getName());
                    dispatchEvent(EventConstants.RECEIVE_COORDINATOR, owner);
                }
                else{
                    highestPriorityServer = null;
                    findHighestPriorityServer(owner);
                    if(highestPriorityServer != null){
                        sendNominationMessage(owner);
                    }else{
                        dispatchEvent(EventConstants.SEND_NOMINATION, owner);
                    }
                }

        } catch (IOException e) {
            log.error("error sending nomination msg to server {}", highestPriorityServer.getName());
            log.error("error is {}", e.getMessage());
            dispatchEvent(EventConstants.SEND_NOMINATION, owner);
        }
    }

    @Override
    public void dispatchEvent(String event, LeaderElectionHandler owner) throws InterruptedException {
        switch(event){
            case EventConstants.RECEIVE_COORDINATOR:
                getLeaderElector().setLeaderElectorState(new NotLeaderState(getLeaderElector()), owner);
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.RECEIVE_COORDINATOR, owner);
                break;
            
            case EventConstants.T3_EXPIRED:
                getLeaderElector().setLeaderElectorState(new StartUpState(getLeaderElector()), owner);
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.START, owner);
                break;

            case EventConstants.RECEIVE_ELECTION:
                getLeaderElector().setLeaderElectorState(new SomeoneStartState(getLeaderElector()), owner);
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.RECEIVE_ELECTION, owner);
                break;

            case EventConstants.SEND_NOMINATION:
                findHighestPriorityServer(owner);
                sendNominationMessage(owner);
                break;
                
            default:
                break;
        }
        
    }

    @Override
    public String toString() {
        return "Choosing State";
    }
    
    
}
