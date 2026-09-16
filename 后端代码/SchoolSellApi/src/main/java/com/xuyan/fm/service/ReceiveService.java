package com.xuyan.fm.service;


import com.xuyan.fm.model.ReceiveModel;

import java.util.List;

public interface ReceiveService {

    ReceiveModel getAdminMessageById(Long id);

    ReceiveModel createAdminMessage(ReceiveModel receiveModel);

    ReceiveModel updateAdminMessage(Long id, ReceiveModel receiveModel);

    void deleteAdminMessage(Long id);

    List<ReceiveModel> getAllAdminMessage();
}
