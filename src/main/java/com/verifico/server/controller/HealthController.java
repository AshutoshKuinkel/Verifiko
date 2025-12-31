package com.verifico.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController{

  @GetMapping("/")
  public String home(){
    return "🪽 Verifico Server Home";
  }

  @GetMapping("/health")
  public String health(){
    return "Health is valid";
  }
}