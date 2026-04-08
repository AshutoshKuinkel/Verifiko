package com.verifico.server.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import com.verifico.server.auth.jwt.JWTAuthFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Value("${SPRING_PROFILES_ACTIVE}")
  private String activeProfile;

  private final JWTAuthFilter jwtAuthFilter;

  public SecurityConfig(JWTAuthFilter jwtAuthFilter) {
    this.jwtAuthFilter = jwtAuthFilter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    if ("dev".equals(activeProfile)) {
      http.csrf(csrf -> csrf.disable());
    } else {
      http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
          .ignoringRequestMatchers("/api/auth/**"));
    }
    // we need to make sure we're getting our XSRF token in the frontend with like
    // axios and also setting is as header for our post, delete, put,patch etc.
    // methods
    http
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .anonymous(anonymous -> anonymous.disable()) // disabling any anonymous auth
        // spring seucirty sets before jwt,
        // may break OAUTH, watch this one
        // filter runs
        .authorizeHttpRequests(
            requests -> requests

                // swagger endpoints
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // oauth2 endpoints
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

                // auth endpoints
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/logout",
                    "/api/v1/payments/webhook/stripe")
                .permitAll()

                // feed endpoints (allowing access before authentication/authorisation)
                .requestMatchers(HttpMethod.GET, "/", "/api/posts", "/api/posts/{id}/comments", "/api/users/{id}")
                .permitAll()

                // app logic (after authentication/authorisation)
                .requestMatchers(HttpMethod.POST, "/api/post/create", "/api/posts/{id}/comments",
                    "/api/v1/payments/payment-intent")
                .authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/comments/{id}").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/users/me", "/api/credits/balance", "/api/credits/transactions")
                .authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/users/me").authenticated()
                .anyRequest().authenticated())

        .oauth2Login(oauth -> oauth.defaultSuccessUrl("/"));
    return http.build();
  }

  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
