package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MyObjectInputStream;
import cn.edu.sustech.cs209.chatting.common.MyObjectOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {
    private static HashMap<String,Socket> userNameToSocket = new HashMap<>();
    private static ArrayList<String> OnlineUsers = new ArrayList<>();
    private static final int port = 9998;

    public static void main(String[] args) throws IOException {
        System.out.println("Starting server");

        final ServerSocket serverSocket = new ServerSocket(port);
        try{
            while (true){
                Socket socket = serverSocket.accept();
                new Thread(new ServerThread(socket)).start();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static class ServerThread implements Runnable{
        private Socket socket;
        private String name;
        private MyObjectOutputStream oos;
        private MyObjectInputStream ois;
        public ServerThread(Socket socket) {
            this.socket = socket;
            try{
                oos = new MyObjectOutputStream(socket.getOutputStream());
                ois = new MyObjectInputStream(socket.getInputStream());
            } catch (Exception e){
                System.out.println("Connect Wrong");
                e.printStackTrace();
            }
        }
        @Override
        public synchronized void run(){
            try{
                while (socket.isConnected()){
                    Message message = (Message) ois.readObject();
                    if (message != null){
                        switch (message.getType()){
                            case "LOGIN":
                                connect(message.getSentBy());
                                break;
                            case "LOGOUT":
                                disconnect(message.getSentBy());
                                break;
                            case "MSG":
                            case "CREATE":
                                sendMessage(message);
                                break;
                        }
                        System.out.println(message.getType());
                    }
                }
                System.out.println(name);
                userNameToSocket.remove(name);
                OnlineUsers.remove(name);
                sendOnlineUsers();
            } catch (Exception e){
                e.printStackTrace();
            } finally {
                userNameToSocket.remove(name);
                OnlineUsers.remove(name);
                sendOnlineUsers();
                try {
                    oos.close();
                    ois.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        public void send(Message message, Socket socket){
            try {
                MyObjectOutputStream oos = new MyObjectOutputStream(socket.getOutputStream());
                oos.writeObject(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void sendMessage(Message message){
            ArrayList<String> toUsers = message.getSendTo();
            for (String to: toUsers){
                Socket temp = userNameToSocket.get(to);
                send(message,temp);
            }
        }
        public void sendOnlineUsers(){
            Message message = new Message();
            message.setType("ONLINEUSER");
            message.setOnlineUsers(OnlineUsers);
            for (HashMap.Entry<String, Socket> entry : userNameToSocket.entrySet()){
                send(message, entry.getValue());
            }
        }
        public void connect(String user){
            if (!userNameToSocket.containsKey(user)){
                name = user;
                userNameToSocket.put(user,socket);
                OnlineUsers.add(user);
                Message message = new Message();
                message.setType("SUCCESS");
                message.setSentBy(user);
                send(message,userNameToSocket.get(user));
                sendOnlineUsers();
            } else{
                Message message = new Message();
                message.setType("FAIL");
                message.setSentBy(user);
                send(message,userNameToSocket.get(user));
            }
        }
        public void disconnect(String user){
            if (userNameToSocket.containsKey(user)){
                userNameToSocket.remove(user);
                OnlineUsers.remove(user);
                sendOnlineUsers();
            } else {
                System.out.println("User doesn't exist");
            }
        }
    }
}
