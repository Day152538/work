package com.xuyan.fm.service.impl;

import com.xuyan.fm.common.utils.RedisUtil;
import com.xuyan.fm.service.IdleItemService;
import com.xuyan.fm.dao.IdleItemDao;
import com.xuyan.fm.dao.UserDao;
import com.xuyan.fm.model.IdleItemModel;
import com.xuyan.fm.model.UserModel;
import com.xuyan.fm.vo.PageVo;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class IdleItemServiceImpl implements IdleItemService {

    @Resource
    private IdleItemDao idleItemDao;

    @Resource
    private UserDao userDao;

    @Resource
    private RedisUtil redisUtil;

    /** 商品详情缓存 TTL：10 分钟（热点商品减少 DB 压力，更新/下架时主动失效） */
    private static final long ITEM_CACHE_TTL_MIN = 10;

    private String itemCacheKey(Long id) {
        return "item:detail:" + id;
    }

    /**
     * 发布闲置
     * 库存/销量字段兜底：库存缺省 1、销量从 0 起（销量只由支付逻辑累加，发布端不可传）。
     * @param idleItemModel
     * @return
     */
    public boolean addIdleItem(IdleItemModel idleItemModel) {
        if (idleItemModel.getStock() == null || idleItemModel.getStock() < 1) {
            idleItemModel.setStock(1);
        }
        // 销量只能由支付逻辑累加，发布时一律从 0 开始，不信任请求体里的值
        idleItemModel.setSalesCount(0);
        return idleItemDao.insert(idleItemModel) == 1;
    }

    /**
     * 查询闲置信息（Cache-Aside 热点缓存）：
     * 先查 Redis，命中直接返回；未命中查 DB 并写缓存（TTL 10 分钟）。
     * Redis 不可用时自动降级直查 DB。更新/下架/删除时主动失效缓存。
     */
    public IdleItemModel getIdleItem(Long id) {
        String key = itemCacheKey(id);
        // 1. 查缓存
        Object cached = redisUtil.get(key);
        if (cached instanceof IdleItemModel item) {
            return item;
        }
        // 2. 缓存未命中（或 Redis 不可用）→ 查 DB
        IdleItemModel idleItemModel = idleItemDao.selectByPrimaryKey(id);
        if (idleItemModel != null) {
            idleItemModel.setUser(userDao.selectByPrimaryKey(idleItemModel.getUserId()));
            // 3. 写缓存（Redis 不可用时 set 返回 false，静默降级）
            redisUtil.set(key, idleItemModel, ITEM_CACHE_TTL_MIN, TimeUnit.MINUTES);
        }
        return idleItemModel;
    }

    /**
     * 查询用户发布的所有闲置
     * user_id建索引
     * @param userId
     * @return
     */
    public List<IdleItemModel> getAllIdelItem(Long userId) {
        return idleItemDao.getAllIdleItem(userId);
    }

    /**
     * 搜索，分页
     * 同时查出闲置发布者的信息
     * @param findValue
     * @param page
     * @param nums
     * @return
     */
    public PageVo<IdleItemModel> findIdleItem(String findValue, int page, int nums) {
        List<IdleItemModel> list=idleItemDao.findIdleItem(findValue, (page - 1) * nums, nums);
        if(list.size()>0){
            List<Long> idList=new ArrayList<>();
            for(IdleItemModel i:list){
                idList.add(i.getUserId());
            }
            List<UserModel> userList=userDao.findUserByList(idList);
            Map<Long,UserModel> map=new HashMap<>();
            for(UserModel user:userList){
                map.put(user.getId(),user);
            }
            for(IdleItemModel i:list){
                i.setUser(map.get(i.getUserId()));
            }
        }
        int count=idleItemDao.countIdleItem(findValue);
        return new PageVo<>(list,count);
    }

    /**
     * 分类查询，分页
     * 同时查出闲置发布者的信息，代码结构与上面的类似，可封装优化，或改为join查询
     * @param idleLabel
     * @param page
     * @param nums
     * @return
     */
    public PageVo<IdleItemModel> findIdleItemByLable(int idleLabel, int page, int nums) {
        List<IdleItemModel> list=idleItemDao.findIdleItemByLable(idleLabel, (page - 1) * nums, nums);
        if(list.size()>0){
            List<Long> idList=new ArrayList<>();
            for(IdleItemModel i:list){
                idList.add(i.getUserId());
            }
            List<UserModel> userList=userDao.findUserByList(idList);
            Map<Long,UserModel> map=new HashMap<>();
            for(UserModel user:userList){
                map.put(user.getId(),user);
            }
            for(IdleItemModel i:list){
                i.setUser(map.get(i.getUserId()));
            }
        }
        int count=idleItemDao.countIdleItemByLable(idleLabel);
        return new PageVo<>(list,count);
    }

    /**
     * 用户更新自己的商品。
     * 整改点：改用带属主条件的 SQL（where id=? and user_id=?），
     * 修复原接口任何登录用户可篡改任意商品的水平越权问题。
     * @param idleItemModel 必须携带 userId（由 Controller 从 JWT 上下文写入）
     * @return
     */
    public boolean updateIdleItem(IdleItemModel idleItemModel){
        if (idleItemModel.getUserId() == null) {
            // 用户操作必须携带属主，无属主的更新一律拒绝
            return false;
        }
        boolean ok = idleItemDao.updateByIdAndUserSelective(idleItemModel)==1;
        if (ok && idleItemModel.getId() != null) {
            redisUtil.delete(itemCacheKey(idleItemModel.getId()));
        }
        return ok;
    }

    /**
     * 管理端更新商品（上下架/审核），不限制属主
     */
    public boolean adminUpdateIdleItem(IdleItemModel idleItemModel){
        boolean ok = idleItemDao.updateByPrimaryKeySelective(idleItemModel)==1;
        if (ok && idleItemModel.getId() != null) {
            redisUtil.delete(itemCacheKey(idleItemModel.getId()));
        }
        return ok;
    }

    public PageVo<IdleItemModel> adminGetIdleList(int status, int page, int nums) {
        List<IdleItemModel> list=idleItemDao.getIdleItemByStatus(status, (page - 1) * nums, nums);
        if(list.size()>0){
            List<Long> idList=new ArrayList<>();
            for(IdleItemModel i:list){
                idList.add(i.getUserId());
            }
            List<UserModel> userList=userDao.findUserByList(idList);
            Map<Long,UserModel> map=new HashMap<>();
            for(UserModel user:userList){
                map.put(user.getId(),user);
            }
            for(IdleItemModel i:list){
                i.setUser(map.get(i.getUserId()));
            }
        }
        int count=idleItemDao.countIdleItemByStatus(status);
        return new PageVo<>(list,count);
    }

    @Override
    public boolean deleteIdleItemById(Long userId, Long id) {
        // 带属主条件删除，用户只能删除自己发布的商品
        boolean ok = idleItemDao.deleteByIdAndUser(id, userId) == 1;
        if (ok) {
            redisUtil.delete(itemCacheKey(id));
        }
        return ok;
    }
}
