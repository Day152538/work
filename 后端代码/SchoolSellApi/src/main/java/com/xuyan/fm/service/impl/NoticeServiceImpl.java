package com.xuyan.fm.service.impl;

import com.xuyan.fm.dao.NoticeDao;
import com.xuyan.fm.model.NoticeModel;
import com.xuyan.fm.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeServiceImpl implements NoticeService {

    @Autowired
    private NoticeDao noticeDao;

    @Override
    public List<NoticeModel> getAllNotices() {
        return noticeDao.getAllNotices();
    }

    @Override
    public NoticeModel getNoticeById(Long id) {
        return noticeDao.getNoticeById(id);
    }

    @Override
    public NoticeModel createNotice(NoticeModel notice) {
        noticeDao.createNotice(notice);
        return notice;
    }

    @Override
    public NoticeModel updateNotice(Long id, NoticeModel notice) {
        notice.setId(id);
        noticeDao.updateNotice(notice);
        return notice;
    }

    @Override
    public void deleteNotice(Long id) {
        noticeDao.deleteNotice(id);
    }
}