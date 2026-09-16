package com.xuyan.fm.model;

import java.io.Serializable;

public class UserMessageModel  implements Serializable {
    private Long id;
    private String userMessage;
    private Long userId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "UserMessageModel{" +
                "id=" + id +
                ", userMessage='" + userMessage + '\'' +
                ", userId=" + userId +
                '}';
    }
}