package com.xuyan.fm.dao;

import com.xuyan.fm.model.FavoriteModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FavoriteDao {
    int deleteByPrimaryKey(Long id);

    /** 整改点：带属主条件删除，只能删自己的收藏 */
    int deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    int insert(FavoriteModel record);

    int insertSelective(FavoriteModel record);

    FavoriteModel selectByPrimaryKey(Long id);

    List<FavoriteModel> getMyFavorite(Long userId);

    Integer checkFavorite(Long userId,Long idleId);

    int updateByPrimaryKeySelective(FavoriteModel record);

    int updateByPrimaryKey(FavoriteModel record);
}