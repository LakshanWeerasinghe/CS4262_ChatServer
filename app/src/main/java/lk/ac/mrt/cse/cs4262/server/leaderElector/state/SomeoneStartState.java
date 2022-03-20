package lk.ac.mrt.cse.cs4262.server.leaderElector.state;

import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElector;

public class SomeoneStartState extends LeaderElectorState{

    public SomeoneStartState(LeaderElector leaderElector) {
        super(leaderElector);
    }

    @Override
    public void dispatchEvent(String event) {
        
    }
    
}
