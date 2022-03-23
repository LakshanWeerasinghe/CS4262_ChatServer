package lk.ac.mrt.cse.cs4262.server.leaderElector.state;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lk.ac.mrt.cse.cs4262.server.Server;
import lk.ac.mrt.cse.cs4262.server.SystemState;
import lk.ac.mrt.cse.cs4262.server.coordinator.CoordinatorConnector;
import lk.ac.mrt.cse.cs4262.server.heartbeat.HeartbeatMonitor;
import lk.ac.mrt.cse.cs4262.server.leaderElector.EventConstants;
import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElectionHandler;
import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElector;
import lk.ac.mrt.cse.cs4262.server.util.Util;

public class LeaderState extends LeaderElectorState{

    private static final Logger log = LoggerFactory.getLogger(LeaderState.class);

    private String coordinatorMsg;

    public LeaderState(LeaderElector leaderElector) {
        super(leaderElector);
        buildCoordinatorMsg();
    }

    private void buildCoordinatorMsg(){
        Map<String, Object> coordinatorMsgMap = new HashMap<>();
        coordinatorMsgMap.put("type", "coordinator");
        coordinatorMsgMap.put("value", getLeaderElector().getMyConfig().getName());
        coordinatorMsg = Util.getJsonString(coordinatorMsgMap);
    }

    private void sendCoordinatorMsg(){
        SystemState.getInstance().getSystemConfigMap().values().forEach(
            x -> {
                if(x.getPriority() < getLeaderElector().getMyConfig().getPriority()){
                    log.info("send coordinator message for server {}", x.getName());
                    try {
                        CoordinatorConnector lowPriorityServerConnector = 
                            new CoordinatorConnector(x.getHostIp(), x.getCoordinatorPort())
                            .createOutputBuffer();
                        lowPriorityServerConnector.sendMessage(coordinatorMsg);
                        lowPriorityServerConnector.close();
                    } catch (IOException e) {
                        log.error("error sending coordinator msg to server {}", x.getName());
                        log.error("error is {}", e.getMessage());
                    }
                }
            }
        );
    }

    @Override
    public void dispatchEvent(String event, LeaderElectionHandler owner) throws InterruptedException {
        switch(event){
            case EventConstants.RECEIVE_ELECTION:
                getLeaderElector().setLeaderElectorState(new SomeoneStartState(getLeaderElector()), owner);
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.RECEIVE_ELECTION, owner);
                break;

            case EventConstants.RECEIVE_COORDINATOR:
                getLeaderElector().setLeaderElectorState(new NotLeaderState(getLeaderElector()), owner);
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.RECEIVE_COORDINATOR, owner);
                break;
            
            case EventConstants.SEND_COORDINATOR:
                SystemState.getInstance().setLeader(owner.getCoordinatingServerName());
                sendCoordinatorMsg();
                HeartbeatMonitor.getInstance().startHeartbeatMonitor(Server.getInstance().getServerName());
                break;

            default:
                break;
        }
        
    }

    @Override
    public String toString() {
        return "Leader State";
    }
    
}
