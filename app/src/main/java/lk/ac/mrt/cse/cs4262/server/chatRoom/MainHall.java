package lk.ac.mrt.cse.cs4262.server.chatRoom;

public final class MainHall extends Room {

    private static MainHall instance = null;

    private MainHall(String name){
        super(name);
    }

    public static synchronized MainHall getInstance(String name){
        if(instance == null){
            instance = new MainHall(name);
        }
        return instance;
    }
}
