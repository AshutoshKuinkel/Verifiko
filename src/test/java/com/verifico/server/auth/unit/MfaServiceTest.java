package com.verifico.server.auth.unit;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.verifico.server.auth.mfa.MfaRepository;
import com.verifico.server.auth.mfa.MfaService;

@ExtendWith(MockitoExtension.class)
public class MfaServiceTest {

  @Mock
  MfaRepository mfaRepository;

  @InjectMocks
  MfaService mfaService;

  
}
