CREATE TABLE emails2send (
    "uid" VARCHAR(36) UNIQUE NOT NULL,
    "to" TEXT NOT NULL,
    "subject" TEXT NOT NULL,
    "content" TEXT NOT NULL,
    "created_at" TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE library (
    "uid" VARCHAR(36) UNIQUE NOT NULL,
    "name" TEXT NOT NULL,
    "author" TEXT NOT NULL,
    "email" TEXT,
    "description" TEXT NOT NULL,
    "micheline" TEXT NOT NULL,
    "michelson" TEXT NOT NULL,
    "created_at" TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    "status" SMALLINT NOT NULL
);

CREATE TABLE users (
    "username" VARCHAR(30) NOT NULL UNIQUE,
    "password_hash" TEXT NOT NULL
);

COMMENT ON COLUMN users.password_hash  IS 'bcrypt hash of user password';

INSERT INTO users (username, password_hash) VALUES ('admin', '$2a$10$Idx1kaM2XQbX72tRh9hFteQ5D5ooOnfO9pR/xYIcHQ/.5BrAnEyrW'); -- plain password: "zxcv"