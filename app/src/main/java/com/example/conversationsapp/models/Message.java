package com.example.conversationsapp.models;

import com.google.firebase.Timestamp;

public class Message {

    String conversationId, title, messageText, mSenderId, mReceiverId, mId;
    Timestamp sentAt;

    public String getmId() {
        return mId;
    }

    public void setmId(String mId) {
        this.mId = mId;
    }

    public Message(){

    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getmSenderId() {
        return mSenderId;
    }

    public void setmSenderId(String mSenderId) {
        this.mSenderId = mSenderId;
    }

    public String getmReceiverId() {
        return mReceiverId;
    }

    public void setmReceiverId(String mReceiverId) {
        this.mReceiverId = mReceiverId;
    }

    public Timestamp getSentAt() {
        return sentAt;
    }

    public void setSentAt(Timestamp sentAt) {
        this.sentAt = sentAt;
    }

    @Override
    public String toString() {
        return "Message{" +
                "conversationId='" + conversationId + '\'' +
                ", title='" + title + '\'' +
                ", messageText='" + messageText + '\'' +
                ", mSenderId='" + mSenderId + '\'' +
                ", mReceiverId='" + mReceiverId + '\'' +
                ", mId='" + mId + '\'' +
                ", sentAt=" + sentAt +
                '}';
    }
}
