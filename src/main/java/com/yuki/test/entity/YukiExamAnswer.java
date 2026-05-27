package com.yuki.test.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("yuki_exam_answer")
public class YukiExamAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long attemptId;
    private Long questionItemId;
    private String userAnswer;
    private String correctAnswer;
    private Integer isCorrect;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
