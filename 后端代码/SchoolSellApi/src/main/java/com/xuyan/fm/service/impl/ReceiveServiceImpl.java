package com.xuyan.fm.service.impl;

import com.xuyan.fm.dao.ReceiveDao;
import com.xuyan.fm.model.ReceiveModel;
import com.xuyan.fm.service.ReceiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class ReceiveServiceImpl implements ReceiveService {
    @Autowired
    private ReceiveDao receiveDao;
//    @Override
//    public List<ReceiveModel> getAllAdminMessage() {
//        return receiveDao.getAllAdminMessage();
//    }

    @Override
    public ReceiveModel getAdminMessageById(Long id) {
        return receiveDao.getAdminMessageById(id);
    }

    @Override
    public ReceiveModel createAdminMessage(ReceiveModel receiveModel) {
        receiveDao.createAdminMessage(receiveModel);
        return receiveModel;
    }

    @Override
    public ReceiveModel updateAdminMessage(Long id, ReceiveModel receiveModel) {
        receiveModel.setId(id);
        receiveDao.updateAdminMessage(receiveModel);
        return receiveModel;
    }

    @Override
    public void deleteAdminMessage(Long id) {
        receiveDao.deleteAdminMessage(id);
    }

    @Override
    public List<ReceiveModel> getAllAdminMessage() {
        return receiveDao.getAllAdminMessage();
    }
}
