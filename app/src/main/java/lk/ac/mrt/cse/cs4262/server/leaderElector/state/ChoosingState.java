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
import lk.ac.mrt.cse.cs4262.server.objects.ServerConfigObj;
import lk.ac.mrt.cse.cs4262.server.util.Util;

public class ChoosingState extends LeaderElectorState{

    private static final Logger log = LoggerFactory.getLogger(ChoosingState.class);

    private ServerConfigObj highestPriorityServer = null;
    private String nominationMsg;

    private final Integer TIME_INTERVAL_T3 = 8000;



    public ChoosingState(LeaderElector leaderElector) {
        super(leaderElector);
        buildNominationMsg();
    }

    private void buildNominationMsg(){
        Map<String, Object> nominationMsgMap = new HashMap<>();
        nominationMsgMap.put("type", "nomination");
        nominationMsg = Util.getJsonString(nominationMsgMap);
    }

    private void findHighestPriorityServer(){
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

    private void sendNominationMessage(){
        try {
            CoordinatorConnector nominationConnector = 
                new CoordinatorConnector(highestPriorityServer.getHostIp(), 
                                            highestPriorityServer.getCoordinatorPort())
                .createOutputBuffer()
                .createInputBuffer();
                nominationConnector.sendMessage(nominationMsg);
                Thread.sleep(TIME_INTERVAL_T3);
                Map<String, Object> answer =  nominationConnector.handleMessage(); // handle coodiator Message
                nominationConnector.close();
                if(answer.containsKey("coordinator")){
                    SystemState.getInstance().setLeader(highestPriorityServer.getName());
                    dispatchEvent(EventConstants.RECEIVE_COORDINATOR);
                }
                else{
                    highestPriorityServer = null;
                    findHighestPriorityServer();
                    if(highestPriorityServer != null){
                        sendNominationMessage();
                    }else{
                        dispatchEvent(EventConstants.T3_EXPIRED);
                    }
                }

        } catch (IOException e) {
            log.error("error sending nomination msg to server {}", highestPriorityServer.getName());
            log.error("error is {}", e.getMessage());
        }
        catch (InterruptedException e){
            log.error("error nomination sending thread interupted");
            log.error("error is {}", e.getMessage());
        }
    }

    @Override
    public void dispatchEvent(String event) throws InterruptedException {
        switch(event){
            case EventConstants.RECEIVE_COORDINATOR:
                getLeaderElector().setLeaderElectorState(new NotLeaderState(getLeaderElector()));
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.RECEIVE_COORDINATOR);
                break;
            
            case EventConstants.T3_EXPIRED:
                getLeaderElector().setLeaderElectorState(new StartUpState(getLeaderElector()));
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.START);
                break;

            case EventConstants.RECEIVE_ELECTION:
                getLeaderElector().setLeaderElectorState(new SomeoneStartState(getLeaderElector()));
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.RECEIVE_ELECTION);
                break;

            case EventConstants.SEND_NOMINATION:
                findHighestPriorityServer();
                sendNominationMessage();
                break;
                
            default:
                break;
        }
        
    }
    
}
