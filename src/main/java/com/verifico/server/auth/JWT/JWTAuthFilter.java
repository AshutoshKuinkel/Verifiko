package com.verifico.server.auth.JWT;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

//just to clarify we created a jwt filter because we know that user is authenticated with cookies,
//  but since spring security has no way to read/understand those jwt access tokens it doesn't know
//  that the user is authenticated and if we try Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//  it'll give us null value, hence we need a jwt filter so that we can validate our access tokens and convert them to a form 
// spring security understands

// Visual Flow:

//Request comes in with JWT cookie
//  ↓
// JwtAuthenticationFilter runs
//  ↓
// Extract JWT from cookie: "eyJhbGc..."
//  ↓
// Validate JWT: Valid, username = "johndoe"
//  ↓
// Create Spring Security object:
// UsernamePasswordAuthenticationToken("johndoe", null, null)
//  ↓
// Store in SecurityContext (Springs memory)
//  ↓
// Request reaches PostController
//  ↓
// PostService calls:
// SecurityContextHolder.getContext().getAuthentication()
//  ↓
// Returns the UsernamePasswordAuthenticationToken
//  ↓
// getName() → "johndoe" 

@Component
public class JWTAuthFilter extends OncePerRequestFilter {

  private final JWTService jwtService;

  public JWTAuthFilter(JWTService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    // skipping auth & webhook endpoints of all JWT and immediately pass the request
    // to the next filter/controller:
    String path = request.getRequestURI();
    if (path.startsWith("/api/auth") || path.equals("/api/v1/payments/webhook/stripe")) {
      filterChain.doFilter(request, response);
      return;
    }

    // getting jwt from cookie
    String jwt = null;
    if (request.getCookies() != null) {
      jwt = Arrays.stream(request.getCookies())
          .filter(cookie -> "access_token".equals(cookie.getName()))
          .map(Cookie::getValue)
          .findFirst()
          .orElse(null);
    }

    if (jwt == null) {
      filterChain.doFilter(request, response);
      return;
    }

    try {

      if (SecurityContextHolder.getContext().getAuthentication() != null) {
        filterChain.doFilter(request, response);
        return;
      }
      // validate JWT/token and get username as well as converting to Spring Security
      // format (`UsernamePasswordAuthenticationToken`) &
      // Store it in `SecurityContext` so Spring knows the user is authenticated
      if (jwtService.validateAccessToken(jwt)) {

        String username = jwtService.getUsernameFromToken(jwt);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username, null,
            List.of());

        authentication.setDetails(
            new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    } catch (Exception e) {
      SecurityContextHolder.clearContext();
      logger.error("JWT validation failed", e);
    }
    filterChain.doFilter(request, response);
  }

}
