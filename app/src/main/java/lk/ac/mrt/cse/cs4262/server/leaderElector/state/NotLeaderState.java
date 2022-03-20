package lk.ac.mrt.cse.cs4262.server.leaderElector.state;

import lk.ac.mrt.cse.cs4262.server.leaderElector.EventConstants;
import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElector;

public class NotLeaderState extends LeaderElectorState{

    public NotLeaderState(LeaderElector leaderElector) {
        super(leaderElector);
    }

    private void updateLeader(){

    }

    @Override
    public void dispatchEvent(String event) throws InterruptedException {
        switch(event){
            case EventConstants.RECEIVE_ELECTION:
                getLeaderElector().setLeaderElectorState(new SomeoneStartState(getLeaderElector()));
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.RECEIVE_ELECTION);
                break;

            case EventConstants.RECEIVE_COORDINATOR:
                updateLeader();
                break;

            case EventConstants.T1_EXPIRED:
                StartUpState startUpState = new StartUpState(getLeaderElector());
                getLeaderElector().setLeaderElectorState(startUpState);
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.START);
                break;

            default:
                break;
            
        }
        
    }
    
}
