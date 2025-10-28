package com.example.controlpanel.controller;

import com.example.controlpanel.dto.ButtonDTO;
import com.example.controlpanel.service.ControlButtonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/control-panel")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class ControlPanelController {

    private final ControlButtonService buttonService;

    @GetMapping("/buttons")
    public ResponseEntity<List<ButtonDTO.ButtonResponse>> getAllButtons() {
        log.info("Fetching all control buttons");
        List<ButtonDTO.ButtonResponse> buttons = buttonService.getAllButtons();
        return ResponseEntity.ok(buttons);
    }

    @GetMapping("/button/{id}")
    public ResponseEntity<ButtonDTO.ButtonResponse> getButtonById(@PathVariable Long id) {
        log.info("Fetching button with id: {}", id);
        ButtonDTO.ButtonResponse button = buttonService.getButtonById(id);
        return ResponseEntity.ok(button);
    }

    @PostMapping("/button")
    public ResponseEntity<ButtonDTO.ButtonResponse> createButton(
            @Valid @RequestBody ButtonDTO.CreateButtonRequest request) {
        log.info("Creating new button: {}", request.getLabel());
        ButtonDTO.ButtonResponse response = buttonService.createButton(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/button/{id}")
    public ResponseEntity<ButtonDTO.ButtonResponse> updateButton(
            @PathVariable Long id,
            @Valid @RequestBody ButtonDTO.UpdateButtonRequest request) {
        log.info("Updating button with id: {}", id);
        ButtonDTO.ButtonResponse response = buttonService.updateButton(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/button/{id}")
    public ResponseEntity<Void> deleteButton(@PathVariable Long id) {
        log.info("Deleting button with id: {}", id);
        buttonService.deleteButton(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/execute/{id}")
    public ResponseEntity<ButtonDTO.ExecuteButtonResponse> executeButton(
            @PathVariable Long id,
            @RequestBody ButtonDTO.ExecuteButtonRequest request) {
        log.info("Executing button with id: {}", id);
        ButtonDTO.ExecuteButtonResponse response = buttonService.executeButton(id, request);
        return ResponseEntity.ok(response);
    }
}
