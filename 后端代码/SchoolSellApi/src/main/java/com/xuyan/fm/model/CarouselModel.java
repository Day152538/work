package com.xuyan.fm.model;

import lombok.Data;

/**
 * 首页轮播图实体。
 * 整改点：移除 mybatis-plus 注解（@TableName/@TableId/@TableField），
 * 该依赖已从 pom 中删除（全项目仅此一处引用），统一使用 MyBatis 原生映射。
 * 手写 getter/setter 改为 Lombok @Data 精简。
 */
@Data
public class CarouselModel {

    private Long id;

    /** 对应的商品 */
    private Long goodId;

    /** 轮播顺序 */
    private Integer showOrder;

    /** 非数据库字段：商品名称（联查填充，MyBatis resultMap 中不映射，Jackson 正常序列化） */
    private String goodName;

    /** 非数据库字段：商品图片（联查填充） */
    private String imgs;
}
