package com.xuyan.fm.model;

import java.io.Serializable;


public class ReceiveModel implements Serializable {
    /**
     * 自增主键
     */
    private Long id;
    /**
     * 留言内容
     */
    private String adminMessage;

    private Long userId;


    // Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAdminMessage() {
        return adminMessage;
    }

    public void setAdminMessage(String adminMessage) {
        this.adminMessage = adminMessage;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }




    // toString()方法重写
    @Override
    public String toString() {
        return "ReceiveModel{" +
                "id=" + id +
                ", adminMessage='" + adminMessage + '\'' +
                ", userId=" + userId +
                '}';
    }
}