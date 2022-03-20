package lk.ac.mrt.cse.cs4262.server.leaderElector.state;

import lk.ac.mrt.cse.cs4262.server.leaderElector.EventConstants;
import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElector;

public class SomeoneStartState extends LeaderElectorState{

    public SomeoneStartState(LeaderElector leaderElector) {
        super(leaderElector);
    }

    private void waitForNominationOrCoordinationMessage() throws InterruptedException{
        Thread.sleep(EventConstants.TIME_INTERVAL_T4);
        if(getLeaderElector().getLeaderElectorState() instanceof SomeoneStartState){
            dispatchEvent(EventConstants.T4_EXPIRED);
        }
    }

    @Override
    public void dispatchEvent(String event) throws InterruptedException {
        switch(event){
            case EventConstants.RECEIVE_NOMINATION:
                getLeaderElector().setLeaderElectorState(new LeaderState(getLeaderElector()));
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.SEND_COORDINATOR);
                break;

            case EventConstants.T4_EXPIRED:
                getLeaderElector().setLeaderElectorState(new StartUpState(getLeaderElector()));
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.START);
                break;
            
            case EventConstants.RECEIVE_ELECTION:
                waitForNominationOrCoordinationMessage();
                break;
            
            case EventConstants.RECEIVE_COORDINATOR:
                getLeaderElector().setLeaderElectorState(new NotLeaderState(getLeaderElector()));
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.RECEIVE_COORDINATOR);
                break;

            default:
                break;
        }
    }
    
}
