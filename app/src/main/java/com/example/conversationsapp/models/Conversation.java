package com.example.conversationsapp.models;

import com.google.firebase.Timestamp;

import java.io.Serializable;
import java.util.ArrayList;

public class Conversation implements Serializable {
    @Override
    public String toString() {
        return "Conversation{" +
                "messages=" + messages +
                ", newMessages=" + newMessages +
                ", conversationId='" + conversationId + '\'' +
                ", title='" + title + '\'' +
                ", receiverId='" + receiverId + '\'' +
                ", senderId='" + senderId + '\'' +
                '}';
    }

    ArrayList<Message> messages = new ArrayList<>();
    boolean newMessages;
    String conversationId, title, receiverId, senderId;

    Timestamp date;

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public Conversation(){

    }

    public ArrayList<Message> getMessages() {
        return messages;
    }

    public void setMessages(ArrayList<Message> messages) {
        this.messages = messages;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public void setNewMessages(boolean newMessages) {
        this.newMessages = newMessages;
    }
    public Message getMostRecentMessage(){
        Message toReturn = null;
        for(int i = 0; i < messages.size()-1; i++){
            if(messages.get(i).getSentAt().compareTo(messages.get(i+1).getSentAt()) > 0){
                toReturn = messages.get(i);
            }
        }
        return toReturn;
    }

    public boolean isNewMessages() {
        return newMessages;
    }

}
