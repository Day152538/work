package com.xuyan.fm.controller;

import com.xuyan.fm.service.KnowledgeService;
import com.xuyan.fm.vo.ResultVo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * RAG 知识库管理接口（仅管理员）。
 * 路径 /admin/knowledge/** 由 AuthInterceptor 校验 admin 角色 JWT。
 */
@RestController
@RequestMapping("/admin/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    /**
     * 重建知识库：从公告表 + 平台规则文本拉取 → 切块 → embedding → 入库。
     * 公告新增/修改后调用此接口刷新向量索引。
     */
    @PostMapping("/rebuild")
    public ResultVo rebuild() {
        int count = knowledgeService.rebuild();
        Map<String, Object> data = new HashMap<>();
        data.put("chunkCount", count);
        return ResultVo.success(data);
    }

    /** 查看知识库条目数 */
    @PostMapping("/count")
    public ResultVo count() {
        Map<String, Object> data = new HashMap<>();
        data.put("chunkCount", knowledgeService.count());
        return ResultVo.success(data);
    }
}
