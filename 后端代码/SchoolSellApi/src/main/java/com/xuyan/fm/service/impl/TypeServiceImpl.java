package com.xuyan.fm.service.impl;

import com.xuyan.fm.dao.TypeDao;
import com.xuyan.fm.model.TypeModel;
import com.xuyan.fm.service.TypeService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class TypeServiceImpl implements TypeService {

    @Resource
    private TypeDao typeDao;

    @Override
    public List<TypeModel> listByCondition(int begin, int nums) {
        return typeDao.listByCondition(begin, nums);
    }


    @Override
    public void addType(TypeModel type) {
        typeDao.addType(type);
    }

    @Override
    public void updateType(TypeModel type) {
        typeDao.updateType(type);
    }

    @Override
    public void deleteType(Long id) {
        typeDao.deleteType(id);
    }
}