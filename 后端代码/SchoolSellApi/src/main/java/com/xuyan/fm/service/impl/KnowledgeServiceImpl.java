package com.xuyan.fm.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuyan.fm.dao.KnowledgeDao;
import com.xuyan.fm.dao.NoticeDao;
import com.xuyan.fm.model.KnowledgeChunk;
import com.xuyan.fm.model.NoticeModel;
import com.xuyan.fm.service.KnowledgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * RAG 检索增强服务。
 *
 * <p>流程：
 * <ol>
 *   <li>知识库重建：从公告表 + 平台规则文本拉取 → 切块 → 调 DashScope text-embedding-v3
 *       生成向量 → 存 knowledge_chunk 表</li>
 *   <li>检索：用户提问 → embedding → 与库中所有 chunk 计算余弦相似度 → 取 top-k</li>
 *   <li>注入：AiChatServiceImpl 把检索到的知识片段注入系统提示词，模型基于真实知识回答，
 *       并标注来源（公告/规则），从根本上减少幻觉</li>
 * </ol>
 *
 * <p>向量存储：MySQL TEXT 列存逗号分隔 float（1024 维），知识量小（几百条），
 * 全量余弦计算完全可行；未来数据量大可迁移 Milvus/pgvector。
 */
@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeServiceImpl.class);

    private static final String EMBEDDING_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";
    private static final String EMBEDDING_MODEL = "text-embedding-v3";
    /** 单次 embedding 批量上限（DashScope 限制 texts 数组长度） */
    private static final int EMBED_BATCH = 10;
    /** 检索 top-k */
    private static final int DEFAULT_TOP_K = 3;
    /** 相似度阈值：低于此值的检索结果不注入（避免不相关内容干扰） */
    private static final double SIMILARITY_THRESHOLD = 0.35;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Resource
    private KnowledgeDao knowledgeDao;
    @Resource
    private NoticeDao noticeDao;
    @Resource
    private ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    // ==================== 检索 ====================

    @Override
    public List<KnowledgeChunk> retrieve(String query, int topK) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<KnowledgeChunk> all = knowledgeDao.selectAll();
        if (all.isEmpty()) {
            return List.of();
        }
        // 1. 查询向量化
        float[] qVec;
        try {
            qVec = embedOne(query);
        } catch (Exception e) {
            log.warn("RAG 查询 embedding 失败，跳过检索: {}", e.getMessage());
            return List.of();
        }
        if (qVec == null) {
            return List.of();
        }
        // 2. 全量余弦相似度计算（知识量小，几百条，O(n) 完全可行）
        List<ScoredChunk> scored = new ArrayList<>();
        for (KnowledgeChunk chunk : all) {
            if (chunk.getEmbedding() == null || chunk.getEmbedding().isBlank()) {
                continue;
            }
            float[] cVec = parseVector(chunk.getEmbedding());
            if (cVec == null || cVec.length != qVec.length) {
                continue;
            }
            double sim = cosineSimilarity(qVec, cVec);
            if (sim >= SIMILARITY_THRESHOLD) {
                scored.add(new ScoredChunk(chunk, sim));
            }
        }
        // 3. 按相似度降序取 top-k
        scored.sort(Comparator.comparingDouble((ScoredChunk s) -> s.score).reversed());
        int k = Math.max(1, Math.min(topK, scored.size()));
        List<KnowledgeChunk> result = new ArrayList<>(k);
        for (int i = 0; i < k; i++) {
            result.add(scored.get(i).chunk);
        }
        return result;
    }

    @Override
    public List<KnowledgeChunk> retrieve(String query) {
        return retrieve(query, DEFAULT_TOP_K);
    }

    // ==================== 知识库重建 ====================

    @Override
    public int rebuild() {
        if (apiKey == null || apiKey.isBlank() || "sk-no-key-configured".equals(apiKey)) {
            log.warn("RAG 重建跳过：未配置 API Key");
            return 0;
        }
        List<KnowledgeChunk> chunks = new ArrayList<>();

        // 1. 公告：每条公告一个 chunk
        try {
            List<NoticeModel> notices = noticeDao.getAllNotices();
            for (NoticeModel n : notices) {
                if (n.getContent() == null || n.getContent().isBlank()) continue;
                KnowledgeChunk c = new KnowledgeChunk();
                c.setSourceType("notice");
                c.setSourceId(String.valueOf(n.getId()));
                c.setTitle("平台公告 #" + n.getId());
                c.setContent(n.getContent().trim());
                chunks.add(c);
            }
        } catch (Exception e) {
            log.warn("RAG 加载公告失败: {}", e.getMessage());
        }

        // 2. 平台规则：硬编码规则文本，按段落切块
        for (String[] rule : PLATFORM_RULES) {
            KnowledgeChunk c = new KnowledgeChunk();
            c.setSourceType("rule");
            c.setSourceId(rule[0]);
            c.setTitle(rule[0]);
            c.setContent(rule[1]);
            chunks.add(c);
        }

        if (chunks.isEmpty()) {
            log.info("RAG 重建：无知识内容");
            return 0;
        }

        // 3. 清空旧库 + 批量 embedding + 入库
        knowledgeDao.deleteAll();
        int inserted = 0;
        for (int i = 0; i < chunks.size(); i += EMBED_BATCH) {
            List<KnowledgeChunk> batch = chunks.subList(i, Math.min(i + EMBED_BATCH, chunks.size()));
            try {
                List<float[]> vectors = embedBatch(batch.stream().map(KnowledgeChunk::getContent).toList());
                for (int j = 0; j < batch.size(); j++) {
                    if (j < vectors.size() && vectors.get(j) != null) {
                        batch.get(j).setEmbedding(vectorToString(vectors.get(j)));
                    }
                    knowledgeDao.insert(batch.get(j));
                    inserted++;
                }
            } catch (Exception e) {
                log.warn("RAG 批量 embedding 失败（批次 {}-{}）: {}", i, i + batch.size(), e.getMessage());
            }
        }
        log.info("RAG 知识库重建完成：共 {} 条", inserted);
        return inserted;
    }

    @Override
    public int count() {
        return knowledgeDao.count();
    }

    // ==================== embedding 调用 ====================

    private float[] embedOne(String text) throws Exception {
        List<float[]> list = embedBatch(List.of(text));
        return list.isEmpty() ? null : list.get(0);
    }

    private List<float[]> embedBatch(List<String> texts) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // DashScope text-embedding-v3 请求体：{"model":..., "input":{"texts":[...]}}
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "model", EMBEDDING_MODEL,
                "input", java.util.Map.of("texts", texts)
        ));

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(EMBEDDING_URL, entity, String.class);

        List<float[]> result = new ArrayList<>();
        JsonNode root = objectMapper.readTree(resp.getBody());
        JsonNode embeddings = root.path("output").path("embeddings");
        if (embeddings.isArray()) {
            // 按 text_index 排序，保证与输入顺序一致
            float[][] arr = new float[texts.size()][];
            for (JsonNode e : embeddings) {
                int idx = e.path("text_index").asInt();
                JsonNode vec = e.path("embedding");
                if (vec.isArray() && idx >= 0 && idx < texts.size()) {
                    float[] f = new float[vec.size()];
                    for (int i = 0; i < vec.size(); i++) {
                        f[i] = (float) vec.get(i).asDouble();
                    }
                    arr[idx] = f;
                }
            }
            for (float[] f : arr) result.add(f);
        }
        return result;
    }

    // ==================== 工具方法 ====================

    private static float[] parseVector(String s) {
        try {
            String[] parts = s.split(",");
            float[] v = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                v[i] = Float.parseFloat(parts[i].trim());
            }
            return v;
        } catch (Exception e) {
            return null;
        }
    }

    private static String vectorToString(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 8);
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.toString();
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0 : dot / denom;
    }

    private record ScoredChunk(KnowledgeChunk chunk, double score) {}

    // ==================== 平台规则知识库（硬编码） ====================
    // 每条：[标题, 内容]
    private static final String[][] PLATFORM_RULES = {
            {"平台介绍", "校园跳蚤市场是面向在校大学生的二手闲置交易平台，支持发布闲置、搜索购买、在线沟通、订单管理等功能。平台致力于让校园闲置物品流通起来，帮助学生省钱、环保。"},
            {"发布规则", "发布商品必须真实描述物品成色、功能状态和瑕疵；禁止发布违禁品（烟酒、药品、武器、盗版软件、假冒伪劣商品等）；禁止发布与校园二手交易无关的内容；商品图片必须是实物图，禁止盗图；价格应合理，禁止恶意标价引流。"},
            {"交易提醒", "交易建议当面验货后再付款，仔细检查物品功能和外观；谨防诈骗，不要私下转账给陌生人，不要点击不明链接；平台不承担私下交易的风险；如遇到纠纷可联系平台客服处理。"},
            {"会员权益", "平台 VIP 会员享受以下权益：购买商品享受 8 折优惠；昵称显示炫彩渐变特效；优先展示发布的商品；专属客服通道。会员开通后即时生效，有效期内可重复享受。"},
            {"账号安全", "请妥善保管账号密码，不要将账号借给他人使用；建议使用强密码并定期更换；如发现账号异常请立即修改密码并联系客服；平台不会以任何理由索要你的密码。"},
            {"订单流程", "下单流程：浏览商品 → 立即购买 → 确认订单 → 联系卖家 → 当面交易 → 确认收货。订单状态包括：待付款、待发货、待收货、已完成、已取消。下单后商品库存自动扣减，取消订单库存自动恢复。"},
            {"商品状态", "商品状态分为：在售（可购买）、下架（卖家主动下架）、待审核（新发布等待管理员审核）、违规（被管理员判定违规下架）。只有在售状态的商品可以下单购买。"},
            {"违规处理", "发布违禁品、虚假描述、恶意骚扰等行为，平台管理员有权下架商品、封禁账号。被判定违规的商品会显示违规原因，用户可申诉。账号封禁期间无法登录和交易。"},
    };
}
