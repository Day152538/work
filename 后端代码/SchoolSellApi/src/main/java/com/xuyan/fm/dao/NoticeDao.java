package com.xuyan.fm.dao;

import com.xuyan.fm.model.NoticeModel;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface NoticeDao {

    @Select("SELECT * FROM sh_notice")
    List<NoticeModel> getAllNotices();

    @Select("SELECT * FROM sh_notice WHERE id = #{id}")
    NoticeModel getNoticeById(Long id);

    @Insert("INSERT INTO sh_notice text VALUES (#{text})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void createNotice(NoticeModel notice);

    @Update("UPDATE sh_notice SET text = #{text} WHERE id = #{id}")
    void updateNotice(NoticeModel notice);

    @Delete("DELETE FROM sh_notice WHERE id = #{id}")
    void deleteNotice(Long id);
}