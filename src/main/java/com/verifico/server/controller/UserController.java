package com.verifico.server.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.verifico.server.exception.ResourceNotFoundException;
import com.verifico.server.model.User;
import com.verifico.server.repository.UserRepository;

@RestController
@RequestMapping("api/users")
public class UserController {

  @Autowired
  private UserRepository userRepository;

  @GetMapping
  public List<User> getAllProfiles() {
    return userRepository.findAll();
  }

  @PostMapping
  public User createProfile(@RequestBody User user) {
    return userRepository.save(user);
  }

  @GetMapping("/{id}")
  public User getUserbyId(@PathVariable Long id) {
    return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
  }

  @PutMapping("/{id}")
  public User updatUser(@PathVariable Long id, @RequestBody User user){
    User userData =  userRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("User not found with id "+id));
    userData.setEmail(user.getEmail());
    userData.setUsername(user.getUsername());
    return userRepository.save(userData);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteUser(@PathVariable Long id){
    User userData = userRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("User not found with id "+id));
    userRepository.delete(userData);
    return ResponseEntity.ok().build();
  }
}
