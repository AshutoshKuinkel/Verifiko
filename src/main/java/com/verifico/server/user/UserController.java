package com.verifico.server.user;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

  private final UserRepository userRepository;

  public UserController(UserRepository userRepository){
    this.userRepository = userRepository;
  }
  
  @GetMapping("/all")
  public List<User> getAllUser(){
    
    return userRepository.findAll();
  }
}
