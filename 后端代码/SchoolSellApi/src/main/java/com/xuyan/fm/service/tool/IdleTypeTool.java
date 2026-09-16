package com.xuyan.fm.service.tool;

import com.xuyan.fm.dao.OrderDao;
import com.xuyan.fm.dao.TypeDao;
import com.xuyan.fm.model.TypeModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * AI 发布助手的「工具」（Function Calling）。
 *
 * <p>两个工具分别演示两种“让模型连自己的数据”：
 * <ol>
 *   <li>listSupportedTypes：分类枚举不进 Prompt，模型需要时实时查库（对应实现计划 §5.3）；</li>
 *   <li>queryRecentDealPrices：给建议价提供“平台真实成交行情”作依据，
 *       而不是让模型拍脑袋定价（2026-09 新增的行情估价器）。</li>
 * </ol>
 *
 * Spring AI 启动时扫描本类中标注 @Tool 的方法，自动生成 function schema 随请求发给模型；
 * 模型返回 tool_calls 后框架在本进程内执行方法、把结果回灌给模型，再让其继续生成最终回复。
 */
@Component
public class IdleTypeTool {

    private final TypeDao typeDao;
    private final OrderDao orderDao;

    public IdleTypeTool(TypeDao typeDao, OrderDao orderDao) {
        this.typeDao = typeDao;
        this.orderDao = orderDao;
    }

    /**
     * 查询平台当前支持的闲置商品分类。
     *
     * @return 分类数组，每项含 id（分类编号）与 name（分类名称）
     */
    @Tool(description = "查询平台当前支持的闲置商品分类列表。返回分类数组，每项包含 id（数字编号）与 name（分类名称）。"
            + "请先调用本工具拿到分类列表，再根据图片中识别出的商品，从中选择最匹配的一个分类 id。")
    public List<TypeModel> listSupportedTypes() {
        return typeDao.listAll();
    }

    /**
     * 查询某个分类下平台“真实成交”的行情（取自已支付、未取消订单）。
     *
     * @param labelId 分类 id（商品分类编号）
     * @return 一段人类可读的行情描述：成交笔数、最低/平均/最高价
     */
    @Tool(description = "查询平台内某个分类近期的真实成交行情（数据来自已支付订单，已取消的不算）。"
            + "入参 labelId 是商品分类编号。返回该分类的成交笔数、最低价、平均价、最高价，"
            + "用于给商品建议售价提供依据，而不是凭空估价。")
    public String queryRecentDealPrices(Long labelId) {
        if (labelId == null) {
            return "未提供分类 id，无法查询成交行情。";
        }
        Map<String, Object> stat = orderDao.selectRecentDealStatsByLabel(labelId.intValue());
        Number cnt = asNumber(getIgnoreCase(stat, "cnt"));
        if (cnt == null || cnt.intValue() <= 0) {
            return "平台内该分类（id=" + labelId + "）暂无已支付成交样本，无法给出行情区间，价格仅供卖家参考。";
        }
        String min = money(getIgnoreCase(stat, "minPrice"));
        String avg = money(getIgnoreCase(stat, "avgPrice"));
        String max = money(getIgnoreCase(stat, "maxPrice"));
        return String.format("分类 id=%d 近期真实成交：共 %s 单；最低 %s 元、平均 %s 元、最高 %s 元（数据取自平台已支付订单）。",
                labelId, cnt, min, avg, max);
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
