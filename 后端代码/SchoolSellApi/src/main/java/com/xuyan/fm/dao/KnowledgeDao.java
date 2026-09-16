package com.xuyan.fm.dao;

import com.xuyan.fm.model.KnowledgeChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeDao {
    int insert(KnowledgeChunk chunk);
    int deleteBySourceType(@Param("sourceType") String sourceType);
    int deleteAll();
    List<KnowledgeChunk> selectAll();
    int count();
}
