package com.example.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Models.BerthAllocation;

public interface BerthAllocationRepository extends JpaRepository<BerthAllocation, String> {

}
