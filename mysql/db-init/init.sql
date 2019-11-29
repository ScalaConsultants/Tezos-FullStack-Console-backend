CREATE TABLE emails2send (
    `uid` VARCHAR(36) NOT NULL UNIQUE,
    `name` TINYTEXT NOT NULL,
    `phone` TINYTEXT NOT NULL,
    `email` TINYTEXT NOT NULL,
    `content` TEXT NOT NULL,
    `created_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE library (
    `uid` VARCHAR(36) NOT NULL UNIQUE,
    `name` TINYTEXT NOT NULL,
    `author` TINYTEXT NOT NULL,
    `email` TINYTEXT,
    `description` TEXT NOT NULL,
    `micheline` TEXT NOT NULL,
    `michelson` TEXT NOT NULL,
    `created_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `status` INT(2) NOT NULL
);

CREATE TABLE users (
    `username` VARCHAR(30) NOT NULL UNIQUE,
    `password_hash` TEXT NOT NULL COMMENT 'bcrypt hash of user password'
);

INSERT INTO users values('asdf', '$2a$10$Idx1kaM2XQbX72tRh9hFteQ5D5ooOnfO9pR/xYIcHQ/.5BrAnEyrW') # plain password: "zxcv"