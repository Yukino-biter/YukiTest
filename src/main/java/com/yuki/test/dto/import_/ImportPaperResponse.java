package com.yuki.test.dto.import_;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImportPaperResponse {
    private Long paperId;
    private int mainCount;
    private int itemCount;
}
