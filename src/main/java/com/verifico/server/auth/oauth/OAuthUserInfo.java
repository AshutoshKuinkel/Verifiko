package com.verifico.server.auth.oauth;

import java.time.LocalDateTime;

public record OAuthUserInfo(
   String email,
   String name,
   String picture,
   String provider,
   String providerId,
   LocalDateTime iat,
   LocalDateTime exp
) {}
