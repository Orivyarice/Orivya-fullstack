-- ══════════════════════════════════════════════════════
-- ORIVYA RICE — Complete Database Setup
-- HOW TO RUN:
-- 1. Open MySQL Workbench
-- 2. Connect to your MySQL server
-- 3. Press Ctrl+A to select ALL this text
-- 4. Click the lightning bolt ⚡ button (Execute All)
-- ══════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS orivya_db;
USE orivya_db;

-- ── TABLE 1: users ──────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id         BIGINT        AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255)  NOT NULL,
    email      VARCHAR(255)  NOT NULL UNIQUE,
    password   VARCHAR(255)  NOT NULL,
    role       ENUM('ADMIN','CUSTOMER') NOT NULL DEFAULT 'CUSTOMER',
    phone      VARCHAR(20),
    address    TEXT
);

-- ── TABLE 2: products ───────────────────────────────
CREATE TABLE IF NOT EXISTS products (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(255)  NOT NULL,
    description     TEXT,
    price           DOUBLE        NOT NULL,
    weight          VARCHAR(100)  NOT NULL,
    stock_quantity  INT           DEFAULT 0,
    badge           VARCHAR(100),
    category        VARCHAR(100),
    image_url       VARCHAR(500),
    is_active       TINYINT(1)    DEFAULT 1
);

-- ── TABLE 3: orders ─────────────────────────────────
CREATE TABLE IF NOT EXISTS orders (
    id               BIGINT  AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT  NOT NULL,
    total_price      DOUBLE  NOT NULL,
    status           ENUM('PENDING','CONFIRMED','PROCESSING','SHIPPED','DELIVERED','CANCELLED') DEFAULT 'PENDING',
    delivery_address TEXT,
    payment_method   VARCHAR(50),
    payment_status   VARCHAR(50)  DEFAULT 'PENDING',
    transaction_id   VARCHAR(200),
    created_at       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ── TABLE 4: order_items ────────────────────────────
CREATE TABLE IF NOT EXISTS order_items (
    id          BIGINT  AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT  NOT NULL,
    product_id  BIGINT  NOT NULL,
    quantity    INT     NOT NULL,
    unit_price  DOUBLE  NOT NULL,
    subtotal    DOUBLE  NOT NULL,
    FOREIGN KEY (order_id)   REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- ── TABLE 5: cart_items ─────────────────────────────
CREATE TABLE IF NOT EXISTS cart_items (
    id          BIGINT   AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   NOT NULL,
    product_id  BIGINT   NOT NULL,
    quantity    INT      NOT NULL DEFAULT 1,
    added_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_user_product (user_id, product_id),
    FOREIGN KEY (user_id)    REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- ── VERIFY TABLES CREATED ───────────────────────────
SELECT 'All tables created!' AS status;
SHOW TABLES;

-- ── INSERT ADMIN USER ───────────────────────────────
-- Login: orivyaorganicrice@gmail.com  |  Password: admin123
INSERT IGNORE INTO users (name, email, password, role, phone) VALUES
('Orivya Admin','orivyaorganicrice@gmail.com',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LPVyc722.2W',
 'ADMIN','9640525341');

-- ── INSERT SAMPLE CUSTOMER ──────────────────────────
-- Login: ravi@gmail.com  |  Password: customer123
INSERT IGNORE INTO users (name, email, password, role, phone) VALUES
('Ravi Kumar','ravi@gmail.com',
 '$2a$10$Nq0kC7J4zGhxHCExe4yJWeomfMX6.PKuczIXg9Xu.Sl5UajDk8GwG',
 'CUSTOMER','9876543210');

-- ── INSERT PRODUCTS ─────────────────────────────────
INSERT IGNORE INTO products
  (name, description, price, weight, stock_quantity, badge, category, image_url, is_active)
VALUES
('Sona Masoori Rice',
 'Premium quality Sona Masoori rice. Soft and fluffy after cooking. Ideal for daily meals.',
 1299.00,'26 kg',100,'Best Seller','Sona Masoori',NULL,1),

('Sona Masoori Rice',
 'Same premium Sona Masoori in a smaller pack. Perfect for small families.',
 279.00,'5 kg',200,NULL,'Sona Masoori',NULL,1),

('Raw Rice',
 'Fine quality raw rice hygienically milled and packed directly from our mill.',
 1099.00,'26 kg',80,'Wholesale','Raw Rice',NULL,1),

('Broken Rice',
 'Quality broken rice, great for porridges, idli batter, and other uses.',
 649.00,'25 kg',150,NULL,'Broken Rice',NULL,1),

('Sona Masoori Rice',
 'Bulk economy pack for large families and businesses. Best value.',
 2399.00,'50 kg',50,'Bulk Pack','Sona Masoori',NULL,1);

-- ── FINAL VERIFICATION ──────────────────────────────
SELECT 'USERS INSERTED:' AS check_name, COUNT(*) AS count FROM users
UNION ALL
SELECT 'PRODUCTS INSERTED:', COUNT(*) FROM products;

SELECT id, name, email, role FROM users;
SELECT id, name, weight, price, badge FROM products;

SELECT 'Setup complete! Now start Spring Boot.' AS done;
