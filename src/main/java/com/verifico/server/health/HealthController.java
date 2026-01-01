package com.verifico.server.health;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
  
  @GetMapping("/")
  public Map<String,String> homeString(){
    return Map.of("message","Verifico");
  }

  @GetMapping("/health")
  public Map<String,String> healthString(){
    return Map.of("status","ok");
  }
}
