package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MyObjectInputStream;
import cn.edu.sustech.cs209.chatting.common.MyObjectOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import javafx.application.Platform;

public class Client {

private Socket socket;
static final int port = 9998;
static final String host = "127.0.0.1";

private ClientThread clientThread;

private Controller controller;

public Client(Controller controller) {
    try {
        socket = new Socket(host, port);
        setController(controller);
        clientThread = new ClientThread(socket, controller);
        new Thread(clientThread).start();
    } catch (IOException e) {
        e.printStackTrace();
    }
}

public void setController(Controller controller) {
    this.controller = controller;
}

public ClientThread getClientThread() {
    return clientThread;
}

public static class ClientThread implements Runnable {

    boolean isConnected;
    boolean stop = true;
    Socket socket;
    Controller controller;
    MyObjectInputStream ois;
    MyObjectOutputStream oos;

    public ClientThread(Socket socket, Controller controller) {
        this.socket = socket;
        this.controller = controller;
    }

    @Override
    public synchronized void run() {
        try {
            while (socket.isConnected() && stop) {
                ois = new MyObjectInputStream(socket.getInputStream());
                Message message = (Message) ois.readObject();
                if (message != null) {
                    switch (message.getType()) {
                        case "SUCCESS":
                            isConnected = true;
                            break;
                        case "FAIL":
                            isConnected = false;
                            break;
                        case "ONLINEUSER":
                            ArrayList<String> onlineUsers = message.getOnlineUsers();
                            onlineUsers.remove(controller.username);
                            controller.setOnlineUsers(onlineUsers);

                            Platform.runLater(() -> {
                                controller.setCurrentOnlineCntText(onlineUsers.size() + 1);
                            });
                            break;
                        case "CREATE":
                            Platform.runLater(
                                () -> controller.createChat(message.getGroupName()));
                            break;
                        case "MSG":
                            Platform.runLater(() -> {
                                controller.addMessage(message);
                            });
                            break;
                    }
                    System.out.println(message.getType());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> controller.alertMessage("Server close!"));
        }
    }

    public void send(Message message) {
        try {
            oos = new MyObjectOutputStream(socket.getOutputStream());
            oos.writeObject(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendCreateChat(ArrayList<String> sendTo, String groupName) {
        Message message = new Message();
        message.setType("CREATE");
        message.setSendTo(sendTo);
        message.setGroupName(groupName);
        send(message);
    }

    public void sendMSG(String sendBy, ArrayList<String> sendTo, String data,
        String groupName) {
        sendTo.remove(controller.username);
        sendTo.removeIf(o -> !controller.onlineUsers.contains(o));
        Message message = new Message();
        message.setType("MSG");
        message.setSentBy(sendBy);
        message.setSendTo(sendTo);
        message.setData(data);
        message.setGroupName(groupName);
        send(message);
    }

    public void connect() {
        Message message = new Message();
        message.setType("LOGIN");
        message.setSentBy(controller.username);
        send(message);
    }
}
}
