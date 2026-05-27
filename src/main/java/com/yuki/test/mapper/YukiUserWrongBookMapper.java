package com.yuki.test.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuki.test.entity.YukiUserWrongBook;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface YukiUserWrongBookMapper extends BaseMapper<YukiUserWrongBook> {

    @Insert("""
            INSERT INTO yuki_user_wrong_book
                (user_id, question_item_id, user_answer, is_resolved, wrong_count)
            VALUES
                (#{userId}, #{questionItemId}, #{userAnswer}, 0, 1)
            ON DUPLICATE KEY UPDATE
                user_answer = VALUES(user_answer),
                is_resolved = 0,
                wrong_count = wrong_count + 1,
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsertWrongQuestion(@Param("userId") Long userId,
                            @Param("questionItemId") Long questionItemId,
                            @Param("userAnswer") String userAnswer);
}
