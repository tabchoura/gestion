package com.chequier.chequier_app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
  private String secret;
  private long expiration = 86400000L;

  public String getSecret() { return secret; }
  public void setSecret(String secret) { this.secret = secret; }
  public long getExpiration() { return expiration; }
  public void setExpiration(long expiration) { this.expiration = expiration; }
}
