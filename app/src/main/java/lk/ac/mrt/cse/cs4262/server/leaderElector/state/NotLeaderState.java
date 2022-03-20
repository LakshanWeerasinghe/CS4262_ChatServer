package lk.ac.mrt.cse.cs4262.server.leaderElector.state;

import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElector;

public class NotLeaderState extends LeaderElectorState{

    public NotLeaderState(LeaderElector leaderElector) {
        super(leaderElector);
        //TODO Auto-generated constructor stub
    }

    @Override
    public void dispatchEvent(String event) {
        // TODO Auto-generated method stub
        
    }
    
}
