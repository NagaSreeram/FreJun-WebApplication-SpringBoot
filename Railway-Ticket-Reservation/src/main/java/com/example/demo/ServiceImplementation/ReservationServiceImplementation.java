package com.example.demo.ServiceImplementation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Constants.ReservationConstants;
import com.example.demo.Models.Passenger;
import com.example.demo.Models.Ticket;
import com.example.demo.Repository.PassengerRepository;
import com.example.demo.Repository.TicketRepository;
import com.example.demo.Service.ReservationService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ReservationServiceImplementation implements ReservationService {

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private TicketRepository ticketRepository;

    private final Lock lock = new ReentrantLock();

    /**
     * This method handles the promotion of tickets in a railway reservation system.
     * It ensures that when a "RAC" (Reservation Against Cancellation) ticket is confirmed,
     * the first ticket from the "WAITING_LIST" is moved to "RAC" status with a "SIDE_LOWER" berth allocation.
     *
     * The method uses a lock to maintain thread safety, preventing race conditions when multiple
     * threads attempt to promote tickets simultaneously.
     *
     * Steps:
     * 1. Find the first "RAC" ticket and promote it to "CONFIRMED".
     * 2. Find the first "WAITING_LIST" ticket, if available, and move it to "RAC" with a "SIDE_LOWER" berth.
     * 3. Ensure proper error handling and null checks to prevent runtime exceptions.
     */
    private void promoteTickets() {
        lock.lock();
        try {
            Optional<Ticket> racTicket = ticketRepository.findFirstByStatus("RAC");
            if (racTicket.isPresent()) {
                Ticket ticket = racTicket.get();
                if (ticket != null) {
                    ticket.setStatus("CONFIRMED");
                    ticketRepository.save(ticket);
                } else {
                    System.err.println("Error: RAC ticket is null.");
                    return;
                }

                Optional<Ticket> waitingListTicket = ticketRepository.findFirstByStatus("WAITING_LIST");
                if (waitingListTicket.isPresent()) {
                    Ticket wt = waitingListTicket.get();
                    if (wt != null) {
                        wt.setStatus("RAC");
                        wt.setBerthType("SIDE_LOWER");
                        ticketRepository.save(wt);
                    } else {
                        System.err.println("Error: Waiting list ticket is null.");
                    }
                }
            } else {
                System.out.println("No RAC tickets available for promotion.");
            }
        } catch (Exception e) {
            System.err.println("An error occurred during ticket promotion: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * This method allocates a berth for a passenger based on their age, gender, and preferences.
     *
     * Rules for berth allocation:
     * 1. If the passenger is a senior citizen (age >= 60) or a female passenger traveling with a child,
     *    they are automatically assigned a "LOWER" berth.
     * 2. Otherwise, the passenger is assigned their preferred berth.
     *
     * The method also includes input validation to ensure passenger details are correct:
     * - Age must be non-negative.
     * - Name must be non-null and not blank.
     *
     * @param passenger the Passenger object containing age, gender, and other details
     * @param preferredBerth the berth preference of the passenger
     * @return the allocated berth as a String
     * @throws IllegalArgumentException if passenger details are invalid
     */
    private String allocateBerth(Passenger passenger, String preferredBerth) {
        if (passenger == null) {
            throw new IllegalArgumentException("Passenger cannot be null");
        }
        if (passenger.getAge() < 0 || passenger.getName() == null || passenger.getName().isBlank()) {
            throw new IllegalArgumentException("Invalid passenger details");
        }

        boolean isSeniorCitizen = passenger.getAge() >= 60;
        boolean isFemaleWithChild = "FEMALE".equalsIgnoreCase(passenger.getGender()) && passenger.isChild();

        if (isSeniorCitizen || isFemaleWithChild) {
            return "LOWER";
        }

        return preferredBerth != null && !preferredBerth.isBlank() ? preferredBerth : "NO_PREFERENCE";
    }

    /**
     * This method books a ticket for a given passenger based on berth availability and preferences.
     * It follows a priority system for assigning tickets:
     *
     * 1. If confirmed berths are available, the ticket is booked as "CONFIRMED" with the preferred berth.
     * 2. If confirmed berths are full but RAC (Reservation Against Cancellation) slots are available,
     *    the ticket is booked as "RAC" with a "SIDE_LOWER" berth.
     * 3. If both confirmed and RAC slots are full but waiting list slots are open,
     *    the ticket is booked as "WAITING_LIST" without a berth allocation.
     * 4. If all slots are full, an exception is thrown indicating no tickets are available.
     *
     * This method is transactional to ensure atomicity and uses a lock to prevent race conditions
     * when multiple threads attempt to book tickets simultaneously.
     *
     * Validations and error handling:
     * - Ensures passenger details are saved before booking the ticket.
     * - Throws a RuntimeException if no tickets are available.
     *
     * @param passenger the Passenger object containing passenger details
     * @param berthPreference the passenger's preferred berth type
     * @return the booked Ticket object
     * @throws RuntimeException if no tickets are available
     */
    @Transactional
    @Override
    public Ticket bookTicket(Passenger passenger, String berthPreference) {
        lock.lock();
        try {
            if (passenger == null || passenger.getName() == null || passenger.getName().isBlank() || passenger.getAge() < 0) {
                throw new IllegalArgumentException("Invalid passenger details");
            }

            passengerRepository.save(passenger);
            long confirmedCount = ticketRepository.countByStatus("CONFIRMED");
            long racCount = ticketRepository.countByStatus("RAC");
            long waitingListCount = ticketRepository.countByStatus("WAITING_LIST");

            Ticket ticket = new Ticket();
            ticket.setPassenger(passenger);

            if (confirmedCount < ReservationConstants.TOTAL_CONFIRMED_BERTHS) {
                ticket.setStatus("CONFIRMED");
                ticket.setBerthType(allocateBerth(passenger, berthPreference));
            } else if (racCount < ReservationConstants.TOTAL_RAC_TICKETS) {
                ticket.setStatus("RAC");
                ticket.setBerthType("SIDE_LOWER");
            } else if (waitingListCount < ReservationConstants.TOTAL_WAITING_LIST) {
                ticket.setStatus("WAITING_LIST");
                ticket.setBerthType(null);
            } else {
                throw new RuntimeException("No tickets available");
            }

            return ticketRepository.save(ticket);
        } catch (Exception e) {
            throw new RuntimeException("Error booking ticket: " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }


    /**
     * This method cancels a ticket by its ID and triggers ticket promotion.
     *
     * Steps:
     * 1. Finds the ticket by its ID.
     * 2. If the ticket is found, it is deleted from the repository.
     * 3. Calls the promoteTickets method to adjust RAC and WAITING_LIST queues.
     *
     * This method is transactional to ensure atomicity and uses a lock to prevent race conditions
     * when multiple threads attempt to cancel tickets or update the ticket queues concurrently.
     *
     * Validations and error handling:
     * - Throws a RuntimeException if the ticket is not found.
     *
     * @param ticketId the unique identifier of the ticket to be canceled
     * @throws RuntimeException if the ticket is not found
     */
    @Transactional
    @Override
    public void cancelTicket(String ticketId) {
        lock.lock();
        try {
            if (ticketId == null || ticketId.isBlank()) {
                throw new IllegalArgumentException("Invalid ticket ID");
            }

            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new RuntimeException("Ticket not found"));

            ticketRepository.delete(ticket);
            promoteTickets();
        } catch (Exception e) {
            throw new RuntimeException("Error canceling ticket: " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }


    /**
     * This method retrieves all booked tickets from the ticket repository.
     *
     * It fetches and returns a list of all Ticket objects, regardless of their status
     * (CONFIRMED, RAC, WAITING_LIST).
     *
     * Error handling:
     * - Returns an empty list if no tickets are found.
     *
     * @return a List of all booked tickets
     */
    @Override
    public List<Ticket> getAllBookedTickets() {
        try {
            List<Ticket> tickets = ticketRepository.findAll();
            return tickets != null ? tickets : Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Error retrieving booked tickets: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * This method calculates and returns the number of available tickets for each category: 
     * CONFIRMED, RAC (Reservation Against Cancellation), and WAITING_LIST.
     *
     * Steps:
     * 1. Counts the number of tickets currently booked for each status.
     * 2. Calculates the available slots by subtracting booked tickets from the total capacity.
     * 3. Returns a map containing the available counts for each ticket status.
     *
     * Validations and error handling:
     * - Ensures counts cannot be negative.
     *
     * @return a Map with keys as ticket statuses ("CONFIRMED", "RAC", "WAITING_LIST") 
     *         and values as the count of available tickets.
     */
    @Override
    public Map<String, Long> getAvailableTickets() {
        try {
            long confirmedCount = ticketRepository.countByStatus("CONFIRMED");
            long racCount = ticketRepository.countByStatus("RAC");
            long waitingListCount = ticketRepository.countByStatus("WAITING_LIST");

            long confirmedAvailable = Math.max(0, ReservationConstants.TOTAL_CONFIRMED_BERTHS - confirmedCount);
            long racAvailable = Math.max(0, ReservationConstants.TOTAL_RAC_TICKETS - racCount);
            long waitingListAvailable = Math.max(0, ReservationConstants.TOTAL_WAITING_LIST - waitingListCount);

            return Map.of(
                "CONFIRMED", confirmedAvailable,
                "RAC", racAvailable,
                "WAITING_LIST", waitingListAvailable
            );
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving available tickets: " + e.getMessage(), e);
        }
    }
}
