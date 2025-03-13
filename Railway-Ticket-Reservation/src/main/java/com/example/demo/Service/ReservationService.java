package com.example.demo.Service;

import java.util.List;
import java.util.Map;

import com.example.demo.Models.Passenger;
import com.example.demo.Models.Ticket;

public interface ReservationService {

	public Ticket bookTicket(Passenger passenger, String berthPreference);
	
	public void cancelTicket(String ticketId);
	
	public List<Ticket> getAllBookedTickets();
	
	public Map<String, Long> getAvailableTickets();
	
}
