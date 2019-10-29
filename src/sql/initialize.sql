CREATE TABLE translations (
    id int NOT NULL AUTO_INCREMENT,
    `from` VARCHAR(20),
    `source` TEXT,
    translation TEXT,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id)
);