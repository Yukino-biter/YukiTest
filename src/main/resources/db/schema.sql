CREATE TABLE IF NOT EXISTS yuki_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'User ID',
    username VARCHAR(50) NOT NULL COMMENT 'Username',
    password VARCHAR(255) NOT NULL COMMENT 'Password hash',
    nickname VARCHAR(50) DEFAULT NULL COMMENT 'Nickname',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1 active, 0 disabled',
    api_provider VARCHAR(30) DEFAULT NULL COMMENT 'AI provider, e.g. deepseek/qwen',
    api_key VARCHAR(512) DEFAULT NULL COMMENT 'User-owned API key',
    base_url VARCHAR(255) DEFAULT NULL COMMENT 'Custom AI API base URL',
    api_model VARCHAR(80) DEFAULT NULL COMMENT 'Custom model name',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_yuki_user_username (username),
    CONSTRAINT chk_yuki_user_status CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='YukiTest users';

CREATE TABLE IF NOT EXISTS yuki_paper (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Paper ID',
    level VARCHAR(2) NOT NULL COMMENT 'JLPT level: N1-N5',
    paper_name VARCHAR(100) NOT NULL COMMENT 'Paper name',
    exam_minutes INT NOT NULL COMMENT 'Exam limit in minutes',
    is_published TINYINT NOT NULL DEFAULT 0 COMMENT '1 published, 0 draft',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_yuki_paper_level_published (level, is_published),
    UNIQUE KEY uk_yuki_paper_level_name (level, paper_name),
    CONSTRAINT chk_yuki_paper_level CHECK (level IN ('N1', 'N2', 'N3', 'N4', 'N5')),
    CONSTRAINT chk_yuki_paper_published CHECK (is_published IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='JLPT papers';

CREATE TABLE IF NOT EXISTS yuki_question_main (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Question group ID',
    paper_id BIGINT NOT NULL COMMENT 'Paper ID',
    level VARCHAR(2) NOT NULL COMMENT 'JLPT level: N1-N5',
    section VARCHAR(20) NOT NULL COMMENT 'vocabulary/grammar/reading/listening',
    title VARCHAR(100) DEFAULT NULL COMMENT 'Question group title',
    material MEDIUMTEXT NULL COMMENT 'Reading/listening long material',
    sort_order INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_yuki_question_main_paper (paper_id, section, sort_order),
    KEY idx_yuki_question_main_level_section (level, section),
    CONSTRAINT fk_yuki_question_main_paper
        FOREIGN KEY (paper_id) REFERENCES yuki_paper(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_yuki_question_main_level CHECK (level IN ('N1', 'N2', 'N3', 'N4', 'N5')),
    CONSTRAINT chk_yuki_question_main_section CHECK (section IN ('vocabulary', 'grammar', 'reading', 'listening'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='JLPT main question groups';

CREATE TABLE IF NOT EXISTS yuki_question_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Question item ID',
    main_id BIGINT NOT NULL COMMENT 'Question group ID',
    content TEXT NOT NULL COMMENT 'Question content',
    options JSON NOT NULL COMMENT 'Options JSON, e.g. {"1":"...","2":"..."}',
    correct_answer VARCHAR(1) NOT NULL COMMENT 'Correct answer: 1/2/3/4',
    sort_order INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_yuki_question_item_main (main_id, sort_order),
    CONSTRAINT fk_yuki_question_item_main
        FOREIGN KEY (main_id) REFERENCES yuki_question_main(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_yuki_question_item_answer CHECK (correct_answer IN ('1', '2', '3', '4'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='JLPT question items';

CREATE TABLE IF NOT EXISTS yuki_exam_attempt (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Exam attempt ID',
    user_id BIGINT NOT NULL COMMENT 'User ID',
    paper_id BIGINT NOT NULL COMMENT 'Paper ID',
    total_count INT NOT NULL COMMENT 'Total questions',
    correct_count INT NOT NULL COMMENT 'Correct questions',
    score INT NOT NULL COMMENT 'Score out of 100',
    submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_yuki_exam_attempt_user_time (user_id, submitted_at),
    KEY idx_yuki_exam_attempt_paper (paper_id),
    CONSTRAINT fk_yuki_exam_attempt_user
        FOREIGN KEY (user_id) REFERENCES yuki_user(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_yuki_exam_attempt_paper
        FOREIGN KEY (paper_id) REFERENCES yuki_paper(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Exam attempts';

CREATE TABLE IF NOT EXISTS yuki_exam_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Exam answer ID',
    attempt_id BIGINT NOT NULL COMMENT 'Attempt ID',
    question_item_id BIGINT NOT NULL COMMENT 'Question item ID',
    user_answer VARCHAR(1) NOT NULL COMMENT 'User answer',
    correct_answer VARCHAR(1) NOT NULL COMMENT 'Correct answer',
    is_correct TINYINT NOT NULL COMMENT '1 correct, 0 wrong',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_yuki_exam_answer_attempt_item (attempt_id, question_item_id),
    KEY idx_yuki_exam_answer_item (question_item_id),
    CONSTRAINT fk_yuki_exam_answer_attempt
        FOREIGN KEY (attempt_id) REFERENCES yuki_exam_attempt(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_yuki_exam_answer_item
        FOREIGN KEY (question_item_id) REFERENCES yuki_question_item(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_yuki_exam_answer_user_answer CHECK (user_answer IN ('1', '2', '3', '4')),
    CONSTRAINT chk_yuki_exam_answer_correct_answer CHECK (correct_answer IN ('1', '2', '3', '4')),
    CONSTRAINT chk_yuki_exam_answer_correct CHECK (is_correct IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Exam answer details';

CREATE TABLE IF NOT EXISTS yuki_exam_draft (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Draft ID',
    user_id BIGINT NOT NULL COMMENT 'User ID',
    paper_id BIGINT NOT NULL COMMENT 'Paper ID',
    answers JSON DEFAULT NULL COMMENT 'Draft answers JSON',
    time_left INT DEFAULT NULL COMMENT 'Remaining seconds',
    saved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Last save time',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_yuki_draft_user_paper (user_id, paper_id),
    KEY idx_yuki_draft_user (user_id),
    CONSTRAINT fk_yuki_draft_user
        FOREIGN KEY (user_id) REFERENCES yuki_user(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_yuki_draft_paper
        FOREIGN KEY (paper_id) REFERENCES yuki_paper(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Exam draft progress';

CREATE TABLE IF NOT EXISTS yuki_user_wrong_book (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Wrong book ID',
    user_id BIGINT NOT NULL COMMENT 'User ID',
    question_item_id BIGINT NOT NULL COMMENT 'Question item ID',
    user_answer VARCHAR(1) NOT NULL COMMENT 'Wrong user answer',
    is_resolved TINYINT NOT NULL DEFAULT 0 COMMENT '1 resolved, 0 unresolved',
    wrong_count INT NOT NULL DEFAULT 1 COMMENT 'Wrong count',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_yuki_wrong_user_item (user_id, question_item_id),
    KEY idx_yuki_wrong_user_resolved (user_id, is_resolved),
    CONSTRAINT fk_yuki_wrong_user
        FOREIGN KEY (user_id) REFERENCES yuki_user(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_yuki_wrong_item
        FOREIGN KEY (question_item_id) REFERENCES yuki_question_item(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_yuki_wrong_answer CHECK (user_answer IN ('1', '2', '3', '4')),
    CONSTRAINT chk_yuki_wrong_resolved CHECK (is_resolved IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User wrong book';
