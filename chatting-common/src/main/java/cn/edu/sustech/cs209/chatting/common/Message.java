package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable {
    private Long timestamp;

    private String type;

    private String sentBy;

    private ArrayList<String> sendTo;

    private ArrayList<String> OnlineUsers;

    private String groupName;

    private String data;

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public void setType(String type){
        this.type = type;
    }

    public void setSentBy(String sentBy) {
        this.sentBy = sentBy;
    }

    public void setSendTo(ArrayList<String> sendTo) {
        this.sendTo = sendTo;
    }

    public void setOnlineUsers(ArrayList<String> onlineUsers) {
        OnlineUsers = onlineUsers;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getType(){
        return type;
    }

    public String getSentBy() {
        return sentBy;
    }

    public ArrayList<String> getSendTo() {
        return sendTo;
    }

    public ArrayList<String> getOnlineUsers() {
        return OnlineUsers;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getData() {
        return data;
    }
}
