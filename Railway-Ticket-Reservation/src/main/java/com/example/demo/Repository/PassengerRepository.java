package com.example.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Models.Passenger;

public interface PassengerRepository extends JpaRepository<Passenger, String> {

}
