package com.example.demo.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Models.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, String> {

	Optional<Ticket> findFirstByStatus(String string);

	long countByStatus(String string);

}
