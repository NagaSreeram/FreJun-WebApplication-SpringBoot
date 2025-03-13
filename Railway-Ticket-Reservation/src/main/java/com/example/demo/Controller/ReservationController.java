package com.example.demo.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Models.Passenger;
import com.example.demo.Models.Ticket;
import com.example.demo.Service.ReservationService;

@RestController
@RequestMapping("/api/v1/tickets")
class ReservationController {
    private final ReservationService reservationService;

    @Autowired
    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/book")
    public ResponseEntity<Ticket> bookTicket(@RequestBody Passenger passenger, @RequestParam String berthPreference) {
        try {
            Ticket ticket = reservationService.bookTicket(passenger, berthPreference);
            return ResponseEntity.ok(ticket);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/cancel/{ticketId}")
    public ResponseEntity<String> cancelTicket(@PathVariable String ticketId) {
        try {
            reservationService.cancelTicket(ticketId);
            return ResponseEntity.ok("Ticket canceled successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error canceling ticket: " + e.getMessage());
        }
    }

    @GetMapping("/booked")
    public ResponseEntity<List<Ticket>> getBookedTickets() {
        List<Ticket> tickets = reservationService.getAllBookedTickets();
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/available")
    public ResponseEntity<Map<String, Long>> getAvailableTickets() {
        Map<String, Long> availableTickets = reservationService.getAvailableTickets();
        return ResponseEntity.ok(availableTickets);
    }
}
