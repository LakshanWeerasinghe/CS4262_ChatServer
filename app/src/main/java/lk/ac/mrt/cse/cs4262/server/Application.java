package lk.ac.mrt.cse.cs4262.server;

import lk.ac.mrt.cse.cs4262.server.chatRoom.MainHall;
import lk.ac.mrt.cse.cs4262.server.client.command.NewIdentityHandler;

public class Application {

    public static void main(String[] args){

        Server server = new Server(6000);
        server.setStore(Store.getInstance());
        server.setMainHall(MainHall.getInstance("MainHall-s1"));
        server.setNewIdentityHandler(new NewIdentityHandler(Store.getInstance(),
                                        MainHall.getInstance("MainHall-s1")));
        server.listen();
    }
    
}
