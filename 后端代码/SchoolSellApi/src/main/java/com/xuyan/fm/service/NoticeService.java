package com.xuyan.fm.service;

import com.xuyan.fm.model.NoticeModel;

import java.util.List;

public interface NoticeService {
    List<NoticeModel> getAllNotices();

    NoticeModel getNoticeById(Long id);

    NoticeModel createNotice(NoticeModel noticeModel);

    NoticeModel updateNotice(Long id, NoticeModel noticeModel);

    void deleteNotice(Long id);
}
