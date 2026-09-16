package com.xuyan.fm.dao;

import com.xuyan.fm.model.MessageModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageDao {
    int deleteByPrimaryKey(Long id);

    /** 整改点：带属主条件删除，只能删自己发的留言 */
    int deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    int insert(MessageModel record);

    int insertSelective(MessageModel record);

    MessageModel selectByPrimaryKey(Long id);

    List<MessageModel> getMyMessage(Long userId);

    List<MessageModel> getIdleMessage(Long idleId);

    int updateByPrimaryKeySelective(MessageModel record);

    int updateByPrimaryKey(MessageModel record);
}