package lk.ac.mrt.cse.cs4262.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import lk.ac.mrt.cse.cs4262.server.chatRoom.MainHall;
import lk.ac.mrt.cse.cs4262.server.client.command.NewIdentityHandler;

public class Application {

    public static void main(String[] args){

        Properties props = new Properties();
        try (InputStream ins = new FileInputStream("src/main/resources/application.properties")) {
            props.load(ins);
            ins.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Server server = new Server(Integer.parseInt(props.getProperty("port")));
        server.setStore(Store.getInstance());
        server.setMainHall(MainHall.getInstance("MainHall-s1"));
        server.setNewIdentityHandler(new NewIdentityHandler(Store.getInstance(),
                                        MainHall.getInstance("MainHall-s1")));
        server.listen();
    }
    
}
