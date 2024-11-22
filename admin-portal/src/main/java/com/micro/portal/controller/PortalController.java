package com.micro.portal.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class PortalController {

    @GetMapping
    public ResponseEntity<?> getDashBoard(){
        return ResponseEntity.status(HttpStatus.OK).body("Dashboard OK from portal server - GET");
    }

    @PostMapping
    public ResponseEntity<?> getDashBoardPost(){
        return ResponseEntity.status(HttpStatus.OK).body("Dashboard OK from portal server - POST");
    }
}
