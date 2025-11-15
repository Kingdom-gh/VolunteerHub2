package com.example.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

  @Value("${app.jwt.secret}")
  private String secret;

  @Value("${app.jwt.expiration}")
  private long expiration; // 7 ngày tính bằng milliseconds

  // Cần một key đủ dài (Base64 encoded)
  private SecretKey getSigningKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secret);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public String generateToken(String email) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("email", email);

    return Jwts.builder()
            .setClaims(claims)
            .setSubject(email)
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
  }

  // Tạo Header Set-Cookie
  public String createCookieHeader(String name, String value, boolean isJwt, long maxAge) {

    StringBuilder sb = new StringBuilder();
    sb.append(name).append("=").append(value);

    long finalMaxAge = maxAge;
    if (isJwt) {
      finalMaxAge = expiration / 1000; // Đổi từ ms sang giây
    } else if (maxAge == 0) {
      finalMaxAge = 0; // Logout
    }

    if (finalMaxAge >= 0) {
      sb.append("; Max-Age=").append(finalMaxAge);
    }

    sb.append("; Path=/");
    sb.append("; HttpOnly");

    sb.append("; SameSite=Lax");

    return sb.toString();
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
  }

  public String extractUsername(String token) {
    return extractAllClaims(token).getSubject();
  }

  // Phiên bản cho JWT
  public String createCookieHeader(String name, String value, boolean isJwt) {
    return createCookieHeader(name, value, isJwt, -1);
  }
}