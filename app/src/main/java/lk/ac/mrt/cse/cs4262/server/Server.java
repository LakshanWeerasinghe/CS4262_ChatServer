package lk.ac.mrt.cse.cs4262.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    
    private ServerSocket serverSocket;

    public Server(int port){
        try {
            this.serverSocket = new ServerSocket(port);
            System.out.println("Server socket opened on port " + String.valueOf(port));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void listen(){
        while(true){
            try {
                Socket client = serverSocket.accept();
                Thread clientThread = new Thread(Server.serverReadBuffer(client));
                clientThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        
        }
    }

    public static Runnable serverReadBuffer(Socket socket){
        return new Runnable() {

            BufferedReader socketInputBuffer = null;

            public void run(){

                try {
                    this.socketInputBuffer = new BufferedReader(new InputStreamReader(
                        socket.getInputStream(), "utf-8"));

                    while(true){
                        String msg = this.socketInputBuffer.readLine();
                        System.out.println(msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };
    }
}
