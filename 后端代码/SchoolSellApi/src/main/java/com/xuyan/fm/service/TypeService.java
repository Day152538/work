package com.xuyan.fm.service;

import com.xuyan.fm.model.TypeModel;

import java.util.List;

public interface TypeService {

    List<TypeModel> listByCondition(int begin, int nums);

    void addType(TypeModel typeModel);

    void updateType(TypeModel typeModel);

    void deleteType(Long id);
}
