package com.xuyan.fm.service.tool;

import com.xuyan.fm.common.context.UserContext;
import com.xuyan.fm.dao.FavoriteDao;
import com.xuyan.fm.dao.IdleItemDao;
import com.xuyan.fm.dao.NoticeDao;
import com.xuyan.fm.dao.OrderDao;
import com.xuyan.fm.dao.TypeDao;
import com.xuyan.fm.model.FavoriteModel;
import com.xuyan.fm.model.IdleItemModel;
import com.xuyan.fm.model.NoticeModel;
import com.xuyan.fm.model.OrderModel;
import com.xuyan.fm.model.TypeModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 对话助手可调用的「平台数据工具」。
 *
 * <p>与发布助手（IdleTypeTool）分开，语义面向“对话/查询”：用户问“有没有便宜手机”
 * “我发布了几件”“书桌最近卖多少钱”，模型会自主调用下面工具查真实数据后回答。
 *
 * <p>联网搜索为“预留位”：默认关闭（ai.web-search.enabled=false，见 application.yml）。
 * 将来接入搜索服务时，新增一个 WebSearchClient 实现并在 Service 里注册成 @Tool 即可，
 * 平台工具与联网工具互不影响。
 */
@Component
public class AiChatTool {

    private final TypeDao typeDao;
    private final IdleItemDao idleItemDao;
    private final OrderDao orderDao;
    private final NoticeDao noticeDao;
    private final FavoriteDao favoriteDao;

    public AiChatTool(TypeDao typeDao, IdleItemDao idleItemDao, OrderDao orderDao,
                      NoticeDao noticeDao, FavoriteDao favoriteDao) {
        this.typeDao = typeDao;
        this.idleItemDao = idleItemDao;
        this.orderDao = orderDao;
        this.noticeDao = noticeDao;
        this.favoriteDao = favoriteDao;
    }

    /** 平台分类列表（id: 名称） */
    @Tool(description = "查询平台当前支持的闲置商品分类列表，返回每项 id 与 name。")
    public String listCategories() {
        List<TypeModel> all = typeDao.listAll();
        if (all == null || all.isEmpty()) {
            return "（平台暂无分类）";
        }
        StringBuilder sb = new StringBuilder();
        for (TypeModel t : all) {
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append(t.getId()).append(": ").append(t.getName());
        }
        return sb.toString();
    }

    /**
     * 按关键字 + 分类 + 价格区间搜索“正在出售”的商品
     * （labelId / minPrice / maxPrice 均可选，只传部分条件也能搜）
     */
    @Tool(description = "按关键字（可选）、分类 id（可选 labelId）、价格区间（可选 minPrice/maxPrice）搜索平台当前在售的闲置商品（最多返回 5 件）。返回每件的 id、名称、价格、发货地、销量、库存。")
    public String searchOnSaleItems(String keyword,
                                    @ToolParam(required = false) Integer labelId,
                                    @ToolParam(required = false) BigDecimal minPrice,
                                    @ToolParam(required = false) BigDecimal maxPrice) {
        String kw = keyword == null ? "" : keyword.trim();
        List<IdleItemModel> list = idleItemDao.findOnSaleByFilter(kw, labelId, minPrice, maxPrice, 0, 5);
        if (list == null || list.isEmpty()) {
            return "没有找到符合条件的在售商品。";
        }
        StringBuilder sb = new StringBuilder();
        for (IdleItemModel i : list) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append("id=").append(i.getId())
                    .append(" 名称=").append(i.getIdleName())
                    .append(" 价格=").append(i.getIdlePrice())
                    .append(" 发货地=").append(nullToEmpty(i.getIdlePlace()))
                    .append(" 销量=").append(i.getSalesCount() == null ? 0 : i.getSalesCount())
                    .append(" 库存=").append(i.getStock() == null ? 0 : i.getStock());
        }
        return sb.toString();
    }

    /** 当前登录用户自己发布的所有商品 */
    @Tool(description = "查询当前登录用户自己发布的所有商品（含下架/在售），返回 id、名称、状态、价格、库存、销量。")
    public String searchMyPublishedItems() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return "尚未登录，无法查询你的商品。";
        }
        List<IdleItemModel> list = idleItemDao.getAllIdleItem(userId);
        if (list == null || list.isEmpty()) {
            return "你还没有发布过商品。";
        }
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (IdleItemModel i : list) {
            if (n++ >= 10) {
                break;
            }
            if (sb.length() > 0) {
                sb.append("\n");
            }
            String status = statusText(i.getIdleStatus());
            sb.append("id=").append(i.getId())
                    .append(" 名称=").append(i.getIdleName())
                    .append(" 状态=").append(status)
                    .append(" 价格=").append(i.getIdlePrice())
                    .append(" 库存=").append(i.getStock() == null ? 0 : i.getStock())
                    .append(" 销量=").append(i.getSalesCount() == null ? 0 : i.getSalesCount());
        }
        return sb.toString();
    }

    /** 某个分类的真实成交行情（来自已支付订单） */
    @Tool(description = "查询平台内某个分类（id 数字）近期的真实成交行情，返回成交笔数与最低/平均/最高价；没有成交则如实说明。")
    public String recentDealStats(Integer labelId) {
        if (labelId == null) {
            return "需要提供分类 id。可先用 listCategories 拿到分类列表。";
        }
        Map<String, Object> stat = orderDao.selectRecentDealStatsByLabel(labelId);
        Number cnt = asNumber(getIgnoreCase(stat, "cnt"));
        if (cnt == null || cnt.intValue() <= 0) {
            return "平台内该分类（id=" + labelId + "）暂无已支付成交样本。";
        }
        return "分类 id=" + labelId + " 近期真实成交：" + cnt + " 单；最低 "
                + money(getIgnoreCase(stat, "minPrice")) + " 元、平均 "
                + money(getIgnoreCase(stat, "avgPrice")) + " 元、最高 "
                + money(getIgnoreCase(stat, "maxPrice")) + " 元。";
    }

    /** 当前登录用户自己的商品统计 + 卖出情况 */
    @Tool(description = "查询当前登录用户自己的商品统计与卖出情况：在售/待审核/违规/已下架数量、已支付卖出订单笔数与累计成交金额。")
    public String getMyStatistics() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return "尚未登录，无法查询统计。";
        }
        List<IdleItemModel> items = idleItemDao.getAllIdleItem(userId);
        int onSale = 0, pending = 0, off = 0, violated = 0;
        for (IdleItemModel i : items) {
            Byte s = i.getIdleStatus();
            if (s == null) {
                continue;
            }
            if (s == 1) {
                onSale++;
            } else if (s == 2) {
                off++;
            } else if (s == 3) {
                pending++;
            } else if (s == 4) {
                violated++;
            }
        }
        int soldCount = 0;
        BigDecimal soldAmount = BigDecimal.ZERO;
        List<Long> idleIds = items.stream().map(IdleItemModel::getId).collect(Collectors.toList());
        if (!idleIds.isEmpty()) {
            List<OrderModel> orders = orderDao.findOrderByIdleIdList(idleIds);
            for (OrderModel o : orders) {
                if (o.getPaymentStatus() != null && o.getPaymentStatus().intValue() == 1) {
                    soldCount++;
                    if (o.getOrderPrice() != null) {
                        soldAmount = soldAmount.add(o.getOrderPrice());
                    }
                }
            }
        }
        return "你的商品统计：在售 " + onSale + " 件、待审核 " + pending + " 件、违规 " + violated
                + " 件、已下架 " + off + " 件；已支付卖出 " + soldCount + " 单，累计成交金额 "
                + soldAmount + " 元。";
    }

    /** 当前登录用户自己的订单概览（买入 + 卖出） */
    @Tool(description = "查询当前登录用户自己的订单概览：我买到的订单笔数、我卖出的订单笔数（只返回本人数据）。")
    public String getMyOrders() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return "尚未登录，无法查询订单。";
        }
        List<OrderModel> bought = orderDao.getMyOrder(userId);
        List<IdleItemModel> items = idleItemDao.getAllIdleItem(userId);
        List<Long> idleIds = items.stream().map(IdleItemModel::getId).collect(Collectors.toList());
        List<OrderModel> sold = idleIds.isEmpty() ? List.of() : orderDao.findOrderByIdleIdList(idleIds);
        return "你的订单概览：买入（作为买家）" + (bought == null ? 0 : bought.size())
                + " 笔；卖出（你的商品被下单）" + (sold == null ? 0 : sold.size()) + " 笔。";
    }

    /** 平台公告列表 */
    @Tool(description = "查询平台当前发布的重要公告/通知，返回公告内容列表（最多 5 条）。")
    public String getNotices() {
        List<NoticeModel> notices = noticeDao.getAllNotices();
        if (notices == null || notices.isEmpty()) {
            return "平台当前暂无公告。";
        }
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (NoticeModel no : notices) {
            if (n++ >= 5) {
                break;
            }
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append("- ").append(nullToEmpty(no.getContent()));
        }
        return "平台当前公告：\n" + sb;
    }

    /** 当前登录用户自己收藏的商品 */
    @Tool(description = "查询当前登录用户自己收藏的商品，返回每件收藏商品 id、名称与价格（只返回本人数据）。")
    public String getMyFavorites() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return "尚未登录，无法查询收藏。";
        }
        List<FavoriteModel> favs = favoriteDao.getMyFavorite(userId);
        if (favs == null || favs.isEmpty()) {
            return "你还没有收藏任何商品。";
        }
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (FavoriteModel f : favs) {
            IdleItemModel item = f.getIdleItem();
            if (item == null) {
                continue;
            }
            if (n++ >= 10) {
                break;
            }
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append("id=").append(item.getId())
                    .append(" 名称=").append(item.getIdleName())
                    .append(" 价格=").append(item.getIdlePrice());
        }
        return "你的收藏：\n" + sb;
    }

    private static String statusText(Byte status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 1: return "在售";
            case 2: return "下架/售罄";
            case 3: return "待审核";
            case 4: return "违规";
            default: return "状态" + status;
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static Object getIgnoreCase(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
                return e.getValue();
            }
        }
        return null;
    }

    private static String money(Object value) {
        return value == null ? "0" : String.valueOf(value);
    }

    private static Number asNumber(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
