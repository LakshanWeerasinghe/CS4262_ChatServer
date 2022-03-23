package lk.ac.mrt.cse.cs4262.server.leaderElector;

public class EventConstants {
    
    public static final String RECEIVE_ANSWER = "RECEIVE_ANSWER";

    public static final String RECEIVE_ELECTION = "RECEIVE_ELECTION";

    public static final String RECEIVE_NOMINATION = "RECEIVE_NOMINATION";
    public static final String SEND_NOMINATION = "SEND_NOMINATION";

    public static final String RECEIVE_COORDINATOR = "RECEIVE_COORDINATOR";
    public static final String SEND_COORDINATOR = "SEND_COORDINATOR";

    public static final String START_RECOVERY = "START_RECOVERY";
    public static final String RECOVER_AS_LEADER = "RECOVER_AS_LEADER";
    public static final String RECOVER_AS_NOT_LEADER = "RECOVER_AS_NOT_LEADER";


    public static final String T1_EXPIRED = "T1_EXPIRED";
    public static final String T2_EXPIRED = "T2_EXPIRED";
    public static final String T3_EXPIRED = "T3_EXPIRED";
    public static final String T4_EXPIRED = "T4_EXPIRED";

    public static final String START = "START";

    public static final Integer TIME_INTERVAL_T1 = 5000;
    public static final Integer TIME_INTERVAL_T2 = 5000;
    public static final Integer TIME_INTERVAL_T3 = 5000;
    public static final Integer TIME_INTERVAL_T4 = 5000;
}
