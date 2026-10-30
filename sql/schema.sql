CREATE DATABASE IF NOT EXISTS subscription_system;
USE subscription_system;

CREATE TABLE IF NOT EXISTS customers (
    customer_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL CHECK (CHAR_LENGTH(TRIM(name)) >= 2),
    email VARCHAR(150) NOT NULL UNIQUE CHECK (email REGEXP '^[A-Za-z0-9](?:[A-Za-z0-9._%+-]*[A-Za-z0-9])?@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)*\\.[A-Za-z]{2,}$'),
    phone VARCHAR(10) NOT NULL CHECK (phone REGEXP '^[0-9]{10}$')
);

CREATE TABLE IF NOT EXISTS plans (
    plan_id INT PRIMARY KEY AUTO_INCREMENT,
    plan_name VARCHAR(100) NOT NULL UNIQUE CHECK (CHAR_LENGTH(TRIM(plan_name)) >= 2),
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    duration_days INT NOT NULL CHECK (duration_days > 0 AND duration_days <= 36500)
);

CREATE TABLE IF NOT EXISTS subscriptions (
    subscription_id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    plan_id INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status ENUM('ACTIVE','CANCELLED','EXPIRED')
        NOT NULL DEFAULT 'ACTIVE',
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    FOREIGN KEY (plan_id) REFERENCES plans(plan_id),
    CHECK (end_date >= start_date)
);

CREATE TABLE IF NOT EXISTS payments (
    payment_id INT PRIMARY KEY AUTO_INCREMENT,
    subscription_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL CHECK (amount >= 0),
    payment_date DATE NOT NULL,
    status ENUM('PENDING','COMPLETED','FAILED')
        NOT NULL DEFAULT 'PENDING',
    FOREIGN KEY (subscription_id)
        REFERENCES subscriptions(subscription_id)
);
