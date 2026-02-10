package com.verifico.server.credit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.verifico.server.user.User;
import com.verifico.server.user.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreditService {
  private final CreditTransactionRepository transactionRepository;
  private final UserRepository userRepository;

  @Transactional
  public CreditTransaction addCredits(Long userId, TransactionType type, Long relatedCommentId, Long relatedPostId) {
    User user = checkForUserById(userId);

    int amount = getAmountforType(type);

    if (amount <= 0) {
      throw new IllegalArgumentException("addCredits cannot be used for negative transactions");
    }

    user.setCredits(user.getCredits() + amount);

    userRepository.save(user);

    return logTransaction(user, amount, type, relatedPostId, relatedCommentId);
  }

  @Transactional
  public CreditTransaction spendCredits(Long userId, TransactionType type, Long relatedCommentId, Long relatedPostId) {
    User user = checkForUserById(userId);

    int amount = getAmountforType(type);

    if (amount >= 0) {
      throw new IllegalArgumentException("spendCredits cannot be used for positive transactions");
    }

    // checking if user has enough to spend
    if (user.getCredits() < Math.abs(amount)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Insufficient credits. You have " + user.getCredits() + " but need " + Math.abs(amount)
              + "Either buy more or contribute to the community");
    }

    // amounts are stored as -20 for making post
    // so current credits + - 20 = expected output.
    user.setCredits(user.getCredits() + amount);

    userRepository.save(user);

    return logTransaction(user, amount, type, relatedPostId, null);
  }

  @Transactional
  public CreditTransaction addPurchasedCredits(Long userId, int amount) {
    User user = checkForUserById(userId);

    // This method should ONLY be called by PaymentService after webhook
    // verification.

    user.setCredits(user.getCredits() + amount);
    userRepository.save(user);

    return logTransaction(user, amount, TransactionType.PURCHASE_CREDITS, null, null);
  }

  public int checkBalance() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found!");
    }
    String username = auth.getName();

    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    return user.getCredits();
  }

  public Page<CreditTransaction> getTransactions(int page, int size) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found!");
    }
    String username = auth.getName();

    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    Pageable pageable = PageRequest.of(page, size);

    return transactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(),
        pageable);
  }

  // helpers:
  private int getAmountforType(TransactionType type) {
    return switch (type) {
      case COMMENT_MARKED_HELPFUL -> 5;
      case CREATE_POST -> -20;
      case BOOST_POST -> -50;
      case PURCHASE_CREDITS -> throw new IllegalArgumentException(
          "Use addPurchasedCredits() for purchases");
    };
  }

  private CreditTransaction logTransaction(User user, int amount, TransactionType type,
      Long relatedPostId, Long relatedCommentId) {
    CreditTransaction transaction = new CreditTransaction();
    transaction.setUser(user);
    transaction.setAmount(amount);
    transaction.setTransactionType(type);
    transaction.setRelatedPostId(relatedPostId);
    transaction.setRelatedCommentId(relatedCommentId);
    transaction.setBalanceAfter(user.getCredits());

    transactionRepository.save(transaction);

    return transaction;
  }

  private User checkForUserById(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
  }

}
