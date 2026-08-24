CREATE TABLE sys_sequence (
    seq_key VARCHAR(50) PRIMARY KEY,
    current_value BIGINT NOT NULL
);

INSERT INTO sys_sequence (seq_key, current_value) VALUES ('KH', 0);
INSERT INTO sys_sequence (seq_key, current_value) VALUES ('SP', 0);
INSERT INTO sys_sequence (seq_key, current_value) VALUES ('NPP', 0);
