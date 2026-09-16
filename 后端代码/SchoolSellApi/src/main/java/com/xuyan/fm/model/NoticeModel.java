package com.xuyan.fm.model;

public class NoticeModel {
    /**
     * 自增主键
     */
    private Long id;
    /**
     * 留言内容
     */
    private String text;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return text;
    }

    public void setContent(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "NoticeModel{" +
                "id=" + id +
                ", text='" + text + '\'' +
                '}';
    }
}
