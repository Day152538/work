package com.xuyan.fm.service.impl;

import com.xuyan.fm.common.enums.ErrorMsg;
import com.xuyan.fm.common.exception.BusinessException;
import com.xuyan.fm.dao.ChatDao;
import com.xuyan.fm.dao.IdleItemDao;
import com.xuyan.fm.dao.UserDao;
import com.xuyan.fm.dto.ChatSendRequest;
import com.xuyan.fm.model.IdleItemModel;
import com.xuyan.fm.model.PrivateMessage;
import com.xuyan.fm.model.UserModel;
import com.xuyan.fm.service.ChatService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 买卖双方私信实现（商品级会话）。
 *
 * 安全边界：
 * - 买家（非商品主人）只能与“该商品的卖家”建立会话；
 * - 卖家不能冷启动骚扰任意买家，只能回复“已在本商品上发过咨询”的人；
 * - 一切读取都按 (item_id, 双方) 过滤，保证一方看不到另一方与卖家的其它咨询。
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final int MAX_MESSAGES = 200;
    private static final int CONVERSATION_SCAN = 500;

    private final ChatDao chatDao;
    private final IdleItemDao idleItemDao;
    private final UserDao userDao;

    public ChatServiceImpl(ChatDao chatDao, IdleItemDao idleItemDao, UserDao userDao) {
        this.chatDao = chatDao;
        this.idleItemDao = idleItemDao;
        this.userDao = userDao;
    }

    @Override
    public PrivateMessage send(Long userId, ChatSendRequest request) {
        if (request == null || request.getItemId() == null) {
            throw new BusinessException(ErrorMsg.PARAM_ERROR);
        }
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isEmpty() || content.length() > 500) {
            throw new BusinessException(ErrorMsg.PARAM_ERROR);
        }
        IdleItemModel item = idleItemDao.selectByPrimaryKey(request.getItemId());
        if (item == null || item.getUserId() == null) {
            throw new BusinessException(ErrorMsg.PARAM_ERROR);
        }
        long ownerId = item.getUserId();
        Long partnerId;
        if (userId.equals(ownerId)) {
            // 卖家：只能回复已在本商品上咨询过我的买家
            if (request.getToUserId() == null
                    || chatDao.countThread(request.getItemId(), userId, request.getToUserId()) <= 0) {
                throw new BusinessException(ErrorMsg.NO_PERMISSION);
            }
            partnerId = request.getToUserId();
        } else {
            // 买家：发往该商品卖家（不允许发向其他人）
            if (request.getToUserId() != null && !userId.equals(ownerId)
                    && !request.getToUserId().equals(ownerId)) {
                throw new BusinessException(ErrorMsg.NO_PERMISSION);
            }
            partnerId = ownerId;
        }

        PrivateMessage message = new PrivateMessage();
        message.setFromUser(userId);
        message.setToUser(partnerId);
        message.setOrderId(request.getOrderId());
        message.setItemId(request.getItemId());
        message.setContent(content);
        chatDao.insert(message);
        return message;
    }

    @Override
    public List<PrivateMessage> messages(Long userId, long itemId, long otherUserId, int limit) {
        IdleItemModel item = idleItemDao.selectByPrimaryKey(itemId);
        if (item == null) {
            throw new BusinessException(ErrorMsg.PARAM_ERROR);
        }
        Long ownerId = item.getUserId();
        if (!userId.equals(ownerId) && !Long.valueOf(otherUserId).equals(ownerId)) {
            // 非买家/非卖家视角：只有“我 或 对方”之一是商品主人，且我看的是自己与主人的线程
            throw new BusinessException(ErrorMsg.NO_PERMISSION);
        }
        int n = Math.max(1, Math.min(limit <= 0 ? 100 : limit, MAX_MESSAGES));
        List<PrivateMessage> recent = chatDao.threadMessages(itemId, userId, otherUserId, n);
        chatDao.markRead(itemId, userId);
        List<PrivateMessage> asc = new ArrayList<>(recent.size());
        for (int i = recent.size() - 1; i >= 0; i--) {
            asc.add(recent.get(i));
        }
        return asc;
    }

    @Override
    public List<Map<String, Object>> conversations(Long userId) {
        List<PrivateMessage> recent = chatDao.recentMine(userId, CONVERSATION_SCAN);
        // 按 (itemId, 对方) 聚合，保留最新一条（recent 本身按 id 倒序）
        Map<String, PrivateMessage> latestByThread = new LinkedHashMap<>();
        Map<String, Integer> unreadByThread = new HashMap<>();
        for (PrivateMessage pm : recent) {
            long partner = pm.getFromUser().equals(userId) ? pm.getToUser() : pm.getFromUser();
            String key = pm.getItemId() + ":" + partner;
            latestByThread.putIfAbsent(key, pm);
            if (pm.getToUser().equals(userId)
                    && (pm.getReadFlag() == null || pm.getReadFlag() == 0)) {
                unreadByThread.merge(key, 1, Integer::sum);
            }
        }
        if (latestByThread.isEmpty()) {
            return List.of();
        }

        // 取商品与对方资料
        Set<Long> itemIds = new HashSet<>();
        Set<Long> partnerIds = new HashSet<>();
        for (PrivateMessage pm : latestByThread.values()) {
            itemIds.add(pm.getItemId());
            partnerIds.add(pm.getFromUser().equals(userId) ? pm.getToUser() : pm.getFromUser());
        }
        Map<Long, IdleItemModel> itemMap = new HashMap<>();
        for (IdleItemModel i : idleItemDao.findIdleByList(new ArrayList<>(itemIds))) {
            itemMap.put(i.getId(), i);
        }
        Map<Long, UserModel> userMap = new HashMap<>();
        for (Long pid : partnerIds) {
            UserModel u = userDao.selectByPrimaryKey(pid);
            if (u != null) {
                userMap.put(pid, u);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, PrivateMessage> e : latestByThread.entrySet()) {
            PrivateMessage pm = e.getValue();
            IdleItemModel item = itemMap.get(pm.getItemId());
            long partner = pm.getFromUser().equals(userId) ? pm.getToUser() : pm.getFromUser();
            UserModel u = userMap.get(partner);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemId", pm.getItemId());
            row.put("itemName", item == null ? "（商品已删除）" : item.getIdleName());
            row.put("itemPrice", item == null ? null : item.getIdlePrice());
            row.put("itemImg", firstImage(item));
            row.put("otherUserId", partner);
            row.put("otherNickname", u == null ? "用户 #" + partner : u.getNickname());
            row.put("otherAvatar", u == null ? null : u.getAvatar());
            row.put("lastContent", pm.getContent());
            row.put("lastTime", pm.getCreateTime() == null ? null : pm.getCreateTime().toString());
            row.put("unread", unreadByThread.getOrDefault(e.getKey(), 0));
            result.add(row);
        }
        return result;
    }

    private String firstImage(IdleItemModel item) {
        if (item == null || item.getPictureList() == null) {
            return null;
        }
        String raw = item.getPictureList().trim();
        if (raw.startsWith("[")) {
            int start = raw.indexOf('"');
            int end = raw.indexOf('"', start + 1);
            if (start >= 0 && end > start) {
                String entry = raw.substring(start + 1, end);
                int idx = entry.indexOf("imageName=");
                return idx >= 0 ? entry.substring(idx + "imageName=".length()) : entry;
            }
        }
        return raw;
    }
}
