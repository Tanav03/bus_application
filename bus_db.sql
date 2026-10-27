CREATE DATABASE IF NOT EXISTS bus_db;
USE bus_db;

CREATE TABLE IF NOT EXISTS buses (
    bus_number VARCHAR(20) PRIMARY KEY,
    route VARCHAR(100) NOT NULL,
    travel_date DATE NOT NULL,
    total_seats INT NOT NULL,
    available_seats INT NOT NULL
);

CREATE TABLE IF NOT EXISTS bookings (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    bus_number VARCHAR(20) NOT NULL,
    passenger_name VARCHAR(100) NOT NULL,
    seats_booked INT NOT NULL,
    booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bus_number) REFERENCES buses(bus_number) ON DELETE CASCADE
);

-- Sample Data
INSERT INTO buses (bus_number, route, travel_date, total_seats, available_seats) 
VALUES 
('BUS-101', 'New York to Boston', '2026-11-15', 40, 40),
('BUS-202', 'Chicago to Detroit', '2026-11-20', 30, 30)
ON DUPLICATE KEY UPDATE bus_number=bus_number;