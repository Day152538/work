package com.xuyan.fm.dao;

import com.xuyan.fm.model.AiChatMessage;
import com.xuyan.fm.model.AiUserMemory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI 对话持久化 + 用户长期记忆 DAO（ai_chat_message / ai_user_memory）。
 *
 * 归属安全：所有“取消息”都带 user_id 条件，用户只能读到自己会话里的消息。
 */
@Mapper
public interface AiChatDao {

    @Insert("insert into ai_chat_message (conversation_id, user_id, role, content, create_time) "
            + "values (#{conversationId}, #{userId}, #{role}, #{content}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMessage(AiChatMessage message);

    /** 某会话最近 N 条（按 id 倒序取回，调用方自行反转为升序） */
    @Select("select id, conversation_id as conversationId, user_id as userId, role, content, create_time as createTime "
            + "from ai_chat_message where conversation_id = #{conversationId} and user_id = #{userId} "
            + "order by id desc limit #{limit}")
    List<AiChatMessage> recentMessages(@Param("conversationId") String conversationId,
                                       @Param("userId") long userId,
                                       @Param("limit") int limit);

    /** 该用户自某条消息以来新增的消息数（用于判断是否需要增量摘要） */
    @Select("select count(*) from ai_chat_message where user_id = #{userId} and id > #{fromId}")
    int countNewMessages(@Param("userId") long userId, @Param("fromId") long fromId);

    /** 取该用户自某条以来的新消息（升序，供摘要） */
    @Select("select id, conversation_id as conversationId, user_id as userId, role, content, create_time as createTime "
            + "from ai_chat_message where user_id = #{userId} and id > #{fromId} "
            + "order by id asc limit #{limit}")
    List<AiChatMessage> newMessages(@Param("userId") long userId,
                                    @Param("fromId") long fromId,
                                    @Param("limit") int limit);

    /** 该用户消息最大 id */
    @Select("select max(id) from ai_chat_message where user_id = #{userId}")
    Long maxMessageId(@Param("userId") long userId);

    @Select("select user_id as userId, summary, summarized_upto_id as summarizedUptoId, update_time as updateTime "
            + "from ai_user_memory where user_id = #{userId}")
    AiUserMemory findMemory(@Param("userId") long userId);

    @Insert("insert into ai_user_memory (user_id, summary, summarized_upto_id, update_time) "
            + "values (#{userId}, #{summary}, #{summarizedUptoId}, now()) "
            + "on duplicate key update summary = #{summary}, summarized_upto_id = #{summarizedUptoId}, update_time = now()")
    int upsertMemory(AiUserMemory memory);
}
