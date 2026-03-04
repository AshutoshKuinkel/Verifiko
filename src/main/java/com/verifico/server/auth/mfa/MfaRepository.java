package com.verifico.server.auth.mfa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MfaRepository extends JpaRepository<Mfa, Long> {

}
