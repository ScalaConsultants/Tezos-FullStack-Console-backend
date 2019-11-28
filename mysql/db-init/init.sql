CREATE TABLE emails2send (
    `uid` VARCHAR(8) NOT NULL UNIQUE,
    `name` TINYTEXT NOT NULL,
    `phone` TINYTEXT NOT NULL,
    `email` TINYTEXT NOT NULL,
    `content` TEXT NOT NULL,
    `created_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE library (
    `uid` VARCHAR(8) NOT NULL UNIQUE,
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
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `username` TEXT NOT NULL,
    `password_hash` TEXT NOT NULL
);
