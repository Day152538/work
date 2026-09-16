package com.xuyan.fm.dto;

/**
 * AI 预填的结构化输出目标类型（Java record）。
 *
 * 这是 Spring AI「结构化输出」的核心：
 * ChatClient.call().entity(AiPrefillResponse.class) 内部用 BeanOutputConverter
 * 依据 record 的组件类型自动生成 JSON Schema → 要求模型按 schema 输出 →
 * 再把模型返回的 JSON 反序列化回本对象。手写方案需要自己维护 schema、解析与异常处理。
 *
 * 字段刻意收敛，与 sh_idle_item 真实列一一对应（priceReason 不进库，只用于前端展示定价依据）：
 * - name         → idle_name    (≤60 字)
 * - details      → idle_details (后端截断到 2000 字 + AI 水印)
 * - labelId      → idle_label   (必须命中 sh_type 现有分类，否则后端回退)
 * - suggestPrice → idle_price   (仅"建议价"，用字符串承载再清洗，避免模型输出"320元"导致反序列化失败)
 * - priceReason  → 展示用：一句话说明建议价依据（如"参考平台同分类近期成交 xx 单、均价 xx 元"），可空
 */
public record AiPrefillResponse(String name, String details, Long labelId, String suggestPrice, String priceReason) {
}
