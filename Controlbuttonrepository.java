package com.example.controlpanel.repository;

import com.example.controlpanel.entity.ControlButton;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ControlButtonRepository extends JpaRepository<ControlButton, Long> {
    List<ControlButton> findByCreatedBy(String createdBy);
    List<ControlButton> findByActiveTrue();
    List<ControlButton> findByCreatedByAndActiveTrue(String createdBy);
    List<ControlButton> findByCategory(String category);
}
