package com.xuyan.fm.dao;

import com.xuyan.fm.model.TypeModel;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TypeDao {

    List<TypeModel> listByCondition(@Param("begin") int begin, @Param("nums") int nums);

    @Insert("INSERT INTO sh_type (name) VALUES (#{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void addType(TypeModel type);
    @Update("UPDATE sh_type SET name = #{name} WHERE id = #{id}")
    void updateType(TypeModel type);
    @Delete("DELETE FROM sh_type WHERE id = #{id}")
    void deleteType(@Param("id") Long id);

    /**
     * 查询全部分类（供 AI 发布助手的 @Tool 工具调用：把类型表暴露给模型自主选择分类）。
     * 相比 listByCondition 的分页语义，这里是一次性取全量小表（分类数量通常 < 30）。
     */
    @Select("SELECT id, name FROM sh_type ORDER BY id")
    List<TypeModel> listAll();
}