package com.verifico.server.post;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.verifico.server.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

enum Stage {

}

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
public class Post {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User author;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(nullable = false, length = 150)
  private String tagline;

  // add category enum enum: AI, SAAS, FINTECH, HEALTHCARE etc...
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Category category;

  // add stage enum e.g idea, development, beta, launched, scaling/promo
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Stage stage;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String problemDescription;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String solutionDescription;

  // save all of these image & vid stuff on cloudinary or somewhere else later...,
  // maybe having videos isn't the best idea, maybe just restrict it to
  // screenshots at max... Alright, let's just make it screenshots only
  // private String demo_video_url;
  @ElementCollection
  private List<String> screenshotUrls = new ArrayList<>();

  private String liveDemoUrl;

  // set default to false,
  @Column(nullable = false)
  private boolean isBoosted = false;

  // maybe make it so that the longer you want to boost your post, the more
  // credits it costs
  private LocalDate boostedUntil;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private Instant updatedAt;

}
