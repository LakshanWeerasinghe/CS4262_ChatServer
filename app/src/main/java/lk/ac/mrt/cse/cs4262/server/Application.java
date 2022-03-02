package lk.ac.mrt.cse.cs4262.server;


public class Application {

    public static void main(String[] args){

        Server server = new Server(6000);
        server.listen();
    }
    
}
