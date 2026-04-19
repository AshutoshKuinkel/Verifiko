package com.verifico.server.user;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_username", columnList = "username")
})
@Getter
@Setter
@NoArgsConstructor

public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(name = "first_name", nullable = false)
  private String firstName;

  @Column(name = "last_name", nullable = false)
  private String lastName;

  @Column(nullable = false, unique = true)
  private String email;

  // This is made true as OAUTH users will not have passwords...
  // Enforce password validity for users manually signup.
  @Column(nullable = true)
  @JsonIgnore
  private String password;

  @Column(length = 500)
  private String bio;

  @Column(name = "avatar_url")
  private String avatarUrl;

  @Column(nullable = false)
  private int credits = 0;

  @Version
  private Long version;

  @Column(name = "joined_date", nullable = false, updatable = false)
  private LocalDate joinedDate = LocalDate.now();

  // OAUTH fields:
  @Column(name = "auth_provider")
  private String authProvider; // i.e. google/github & null for non-oauth users

  @Column(name = "provided_user_id")
  private String providerUserId;

  public static User createOAuthUser(String email, String name, String profilePicture, String provider,
      String providerId) {
    User user = new User();

    user.setEmail(email);
    user.setUsername(createUserNameFromNameForOauthUsers(name));
    String[] parts = name != null ? name.split(" ", 2) : new String[] { "User", "" };
    user.setFirstName(parts[0]);
    user.setLastName(parts.length > 1 ? parts[1] : "");
    user.setPassword(null);
    user.setAuthProvider(provider);
    user.setProviderUserId(providerId);
    user.setBio("");
    user.setAvatarUrl(profilePicture);
    return user;
  }

  // link new OAuth provider to existing email/password account:
  public void linkOAuthProvider(String provider, String providerUserId){
    this.authProvider = provider;
    this.providerUserId = providerUserId;
  }

  // unqiue username from oauth helper function:
  // based on the single string for full name
  // we make a unique username.
  public static String createUserNameFromNameForOauthUsers(String name) {
    return "";
  }

}
