package com.example.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Models.BookingQueue;

public interface BookingQueueRepository extends JpaRepository<BookingQueue, String> {

}
