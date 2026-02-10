package com.verifico.server.payment;

import java.time.Instant;

import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.verifico.server.credit.TransactionType;
import com.verifico.server.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "payments", indexes = {
    @Index(name = "idx_usr_paymentId", columnList = "user_id,id"),
    @Index(name = "idx_payment_intent", columnList = "paymentIntentId"),
    @Index(name = "idx_idempotency_key", columnList = "idempotencyKey")
})
@Getter
@Setter
@NoArgsConstructor
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, updatable = false)
  private String paymentIntentId;

  @Column(unique = true, nullable = false)
  private String idempotencyKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @ColumnDefault("'PURCHASE_CREDITS'")
  private TransactionType type = TransactionType.PURCHASE_CREDITS;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, name = "purchased_package")
  private CreditsPurchasedAmount purchasedPackage;

  @Column(nullable = false)
  private Long amountInCents;

  @Column
  private Boolean creditsAwarded = false;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User transactionInitiator;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @ColumnDefault("'PENDING'")
  private PaymentStatus status = PaymentStatus.PENDING;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private Instant updatedAt;
}
