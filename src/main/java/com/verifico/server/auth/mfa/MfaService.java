package com.verifico.server.auth.mfa;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MfaService {

  private final MfaRepository mfaRepository;

  public void mfaTokenGeneration() {
    // generate hashed code (only user should get to see code, make sure it's hashed
    // in db)
    // set the expiry time to 10 mins
    // send user email
  }

  public void validateMfaToken() {
    // check that token isn't expired
    // check that token hasn't previously been used for this user:
    // i.e check used column isn't true
  }

  // helper
  private String generateMfaCodeHelper() {
    return RandomStringUtils.secure().nextAlphanumeric(6);
  }

}
