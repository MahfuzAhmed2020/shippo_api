CREATE DATABASE IF NOT EXISTS shippo_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE shippo_db;

-- Hibernate creates/updates the shipments table from Shipment.java.
-- spring.jpa.hibernate.ddl-auto=update is used by this demo.
