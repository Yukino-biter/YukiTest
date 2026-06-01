-- Mock data for YukiTest

-- User: testuser / 123456
REPLACE INTO yuki_user (id, username, password, nickname, status) VALUES
(1, 'testuser', '$2b$10$t9T2dCwr/U3Q8EYmthAuyu31H9HX2c8/lA64TK34poSjXZSxv3fgC', 'ゆき', 1);

-- Paper: N2 2024年7月
REPLACE INTO yuki_paper (id, level, paper_name, exam_minutes, is_published) VALUES
(1, 'N2', '2024年7月 N2 真題', 165, 1);

-- Question Main 1: 文字語彙 (vocabulary)
REPLACE INTO yuki_question_main (id, paper_id, level, section, title, material, sort_order) VALUES
(1, 1, 'N2', 'vocabulary', '問題1　漢字読み', NULL, 1);

-- Question Main 2: 文法 (grammar)
REPLACE INTO yuki_question_main (id, paper_id, level, section, title, material, sort_order) VALUES
(2, 1, 'N2', 'grammar', '問題3　文法', NULL, 2);

-- Question Main 3: 読解 (reading) — has material
REPLACE INTO yuki_question_main (id, paper_id, level, section, title, material, sort_order) VALUES
(3, 1, 'N2', 'reading', '問題5　読解A', '　先日、ある雑誌で「最近の若者は〇〇しない」という特集を読んだ。筆者はその記事を読みながら、本当にそうだろうかと疑問に思った。

　確かに、私の学生時代と比べて、若者の行動パターンは変わっているかもしれない。しかし、「変わった」ことと「しない」ことは全く別の話だ。私たちの世代が当たり前だと思っていたことが、今の若者にとって当たり前ではないだけのことだ。

　例えば、手紙を書かない若者が多いと言われているが、それは手紙というコミュニケーション手段が時代に合わなくなっただけで、コミュニケーション自体が減ったわけではない。実際、SNSを使ったコミュニケーションは以前よりも遥かに活発になっている。

　大切なのは、手段がどう変わろうと、人と人とのつながりを大切にすることだと思う。', 3);

-- Vocabulary items
REPLACE INTO yuki_question_item (id, main_id, content, options, correct_answer, sort_order) VALUES
(1, 1, '次の文の（　）に入れるのに最もよいものを、１・２・３・４から一つ選びなさい。\n\n新しい環境に（　）するまでには時間がかかる。', '{"1":"適合", "2":"適応", "3":"適当", "4":"適切"}', '2', 1),
(2, 1, '彼女の話を聞いて、心が（　）された。', '{"1":"深く", "2":"強く", "3":"暖かく", "4":"優しく"}', '2', 2),
(3, 1, 'この問題は非常に（　）で、簡単に解けない。', '{"1":"複雑", "2":"複合", "3":"多様", "4":"多角"}', '1', 3);

-- Grammar items
REPLACE INTO yuki_question_item (id, main_id, content, options, correct_answer, sort_order) VALUES
(4, 2, '次の文の（　）に入れるのに最もよいものを、１・２・３・４から一つ選びなさい。\n\n雨が降っている（　）、試合は中止になった。', '{"1":"ために", "2":"のに", "3":"ところに", "4":"うちに"}', '1', 1),
(5, 2, '彼は優秀だ。（　）、仕事が遅い。', '{"1":"ただし", "2":"それで", "3":"だから", "4":"すると"}', '1', 2),
(6, 2, '日本に来て（　）、日本語が上手になった。', '{"1":"からといって", "2":"以来", "3":"うちに", "4":"とたんに"}', '2', 3);

-- Reading items (share material from main 3)
REPLACE INTO yuki_question_item (id, main_id, content, options, correct_answer, sort_order) VALUES
(7, 3, '筆者は「最近の若者は〇〇しない」という特集についてどう思っているか。', '{"1":"その通りだと思う", "2":"疑問に思っている", "3":"関心がない", "4":"賛成している"}', '2', 1),
(8, 3, '筆者によれば、若者のコミュニケーションはどう変わったか。', '{"1":"減った", "2":"増えた", "3":"変わっていない", "4":"手紙が増えた"}', '2', 2),
(9, 3, '筆者が最も大切だと思っていることは何か。', '{"1":"手紙を書くこと", "2":"SNSを使うこと", "3":"人と人とのつながり", "4":"新しい技術を学ぶこと"}', '3', 3);
