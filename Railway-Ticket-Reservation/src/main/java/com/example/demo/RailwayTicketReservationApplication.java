package com.example.demo;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class RailwayTicketReservationApplication {

	public static void main(String[] args) {
		SpringApplication.run(RailwayTicketReservationApplication.class, args);
	}

	@Bean
    public CommandLineRunner demo(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        return args -> {
            System.out.println("Connected to Database: " + dataSource.getConnection().getMetaData().getURL());

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS Passenger (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL, " +
                "age INT NOT NULL, " +
                "gender VARCHAR(10) NOT NULL, " +
                "is_child BOOLEAN NOT NULL");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS Ticket (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "passenger_id BIGINT, " +
                "status VARCHAR(20) NOT NULL, " +
                "berth_type VARCHAR(20), " +
                "FOREIGN KEY (passenger_id) REFERENCES Passenger(id) ON DELETE CASCADE");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS BerthAllocation (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "ticket_id BIGINT, " +
                "berth_position VARCHAR(20) NOT NULL, " +
                "FOREIGN KEY (ticket_id) REFERENCES Ticket(id) ON DELETE CASCADE");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS BookingQueue (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "ticket_id BIGINT, " +
                "queue_type VARCHAR(20) NOT NULL, " +
                "queue_position INT NOT NULL, " +
                "FOREIGN KEY (ticket_id) REFERENCES Ticket(id) ON DELETE CASCADE");
        };
    }
	
}
