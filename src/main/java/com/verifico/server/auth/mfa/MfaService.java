package com.verifico.server.auth.mfa;

import java.time.LocalDateTime;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.verifico.server.email.EmailService;
import com.verifico.server.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MfaService {

  private static final int maxAttempts = 5;

  private final MfaRepository mfaRepository;
  private final EmailService emailService;
  private final PasswordEncoder passwordEncoder;

  public void mfaTokenGeneration(User user) {
    // invalidate any code before if present
    mfaRepository.invalidateAllActiveForUser(user.getId());

    String notHashedCode = generateMfaCodeHelper();
    // generate hashed code (only user should get to see code, make sure it's hashed
    // in db)
    String hashedCode = passwordEncoder.encode(notHashedCode);

    Mfa req = new Mfa();
    req.setMfaCode(hashedCode);
    req.setUser(user);
    // set the expiry time to 10 mins
    req.setExpiresAt(LocalDateTime.now().plusSeconds(600));
    req.setUsed(false);
    req.setAttempts(0);

    // saving in db
    mfaRepository.save(req);

    // send user email
    emailService.sendMfaGmailCodeEmailForv1(notHashedCode, user.getEmail());
  }

  public void validateMfaToken(User user, String submittedCode) {
    // check that token isn't expired
    Mfa token = mfaRepository
        .findTopByUserIdAndUsedFalseAndExpiresAtAfterOrderByIdDesc(user.getId(), LocalDateTime.now())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired code"));

    // check that token hasn't previously been used for this user & not expired,
    // etc..:
    if (token.isMfaTokenExpired()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired code");
    }

    if (token.isUsed()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired code");
    }

    if (token.getAttempts() >= maxAttempts) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
          "Too many failed attempts, please request another code.");
    }

    if (!passwordEncoder.matches(submittedCode, token.getMfaCode())) {
      token.setAttempts(token.getAttempts() + 1);
      mfaRepository.save(token);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code");
    }

    token.setUsed(true);
    mfaRepository.save(token);
  }

  // helper
  private String generateMfaCodeHelper() {
    return RandomStringUtils.secure().nextAlphanumeric(6);
  }

}
