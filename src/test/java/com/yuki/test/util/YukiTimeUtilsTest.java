package com.yuki.test.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YukiTimeUtilsTest {

    @Test
    void returnsOfficialJlptExamLimitMinutes() {
        assertThat(YukiTimeUtils.getExamLimitMinutes("N1")).isEqualTo(175);
        assertThat(YukiTimeUtils.getExamLimitMinutes("n2")).isEqualTo(165);
        assertThat(YukiTimeUtils.getExamLimitMinutes(" N3 ")).isEqualTo(140);
        assertThat(YukiTimeUtils.getExamLimitMinutes("N4")).isEqualTo(125);
        assertThat(YukiTimeUtils.getExamLimitMinutes("N5")).isEqualTo(105);
    }

    @Test
    void rejectsUnsupportedLevel() {
        assertThatThrownBy(() -> YukiTimeUtils.getExamLimitMinutes("N6"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported JLPT level");
    }
}
