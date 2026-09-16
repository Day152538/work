package com.xuyan.fm.service.impl;

import com.xuyan.fm.dao.FavoriteDao;
import com.xuyan.fm.dao.IdleItemDao;
import com.xuyan.fm.model.FavoriteModel;
import com.xuyan.fm.model.IdleItemModel;
import com.xuyan.fm.service.FavoriteService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Resource
    private FavoriteDao favoriteDao;

    @Resource
    private IdleItemDao idleItemDao;

    /**
     * 新增收藏
     * @param favoriteModel
     * @return
     */
    public boolean addFavorite(FavoriteModel favoriteModel){
        return favoriteDao.insert(favoriteModel)==1;
    }

    /**
     * 删除收藏（带属主条件，只能删自己的）
     */
    public boolean deleteFavorite(Long userId, Long id){
        return favoriteDao.deleteByIdAndUser(id, userId)==1;
    }

    /**
     * 判断用户是否收藏某个闲置
     * user_id建索引
     * @param userId
     * @param idleId
     * @return
     */
    public Integer isFavorite(Long userId,Long idleId){
        return favoriteDao.checkFavorite(userId,idleId);
    }

    /**
     * 查询一个用户的所有收藏
     * 关联查询，没有用join，通过where in查询关联的闲置信息
     * @param userId
     * @return
     */
    public List<FavoriteModel> getAllFavorite(Long userId){
        List<FavoriteModel> list=favoriteDao.getMyFavorite(userId);
        if(list.size()>0){
            List<Long> idleIdList=new ArrayList<>();
            for(FavoriteModel i:list){
                idleIdList.add(i.getIdleId());
            }
            List<IdleItemModel> idleItemModelList=idleItemDao.findIdleByList(idleIdList);
            Map<Long,IdleItemModel> map=new HashMap<>();
            for(IdleItemModel idle:idleItemModelList){
                map.put(idle.getId(),idle);
            }
            for(FavoriteModel i:list){
                i.setIdleItem(map.get(i.getIdleId()));
            }
        }
        return list;
    }
}
