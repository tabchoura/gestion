package com.chequier.chequier_app.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.chequier.chequier_app.model.User;
import com.chequier.chequier_app.repository.UserRepository;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public UserDetailsService userDetailsService(UserRepository repo){
    return username -> {
      System.out.println("🔍 Loading user details for: " + username);
      User u = repo.findByEmail(username).orElseThrow(() -> {
        System.out.println("❌ User not found: " + username);
        return new UsernameNotFoundException("User not found: " + username);
      });
      System.out.println("✅ User found: " + u.getEmail() + " - Role: " + u.getRole());
      return new org.springframework.security.core.userdetails.User(
        u.getEmail(), u.getPassword(),
        List.of(new SimpleGrantedAuthority("ROLE_"+u.getRole().name()))
      );
    };
  }

  @Bean public PasswordEncoder passwordEncoder(){ return new BCryptPasswordEncoder(); }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception {
    return c.getAuthenticationManager();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(){
    System.out.println("🌐 Configuring CORS...");
    CorsConfiguration cfg = new CorsConfiguration();
    // ⚠️ Pas de wildcard si credentials = true
    cfg.setAllowedOrigins(List.of("http://localhost:4200"));
    cfg.setAllowCredentials(true);
    cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
    cfg.setAllowedHeaders(List.of(
      "Authorization","Content-Type","X-Requested-With","Accept","Origin"
    ));
    // (optionnel) entêtes exposés
    // cfg.setExposedHeaders(List.of("Authorization"));

    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", cfg);
    return src;
  }

  @Bean
  public SecurityFilterChain chain(HttpSecurity http, JwtAuthenticationFilter jwt) throws Exception {
    System.out.println("🔧 Configuring Security Filter Chain...");

    http
      .csrf(AbstractHttpConfigurer::disable)
      .cors(c -> c.configurationSource(corsConfigurationSource()))
      .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(a -> a
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()    // préflight CORS
        .requestMatchers("/auth/**").permitAll()                   // login/register
        .requestMatchers("/error").permitAll()
        // .requestMatchers("/demandes/**").permitAll()             // (option debug)
        .anyRequest().authenticated()
      )
      .httpBasic(AbstractHttpConfigurer::disable)
      .formLogin(AbstractHttpConfigurer::disable)
      .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
      .exceptionHandling(ex -> ex.authenticationEntryPoint((req,res,e)->{
        System.out.println("🚫 Authentication entry point triggered:");
        System.out.println("🚫 URI: " + req.getRequestURI());
        System.out.println("🚫 Method: " + req.getMethod());
        System.out.println("🚫 Auth header: " + req.getHeader("Authorization"));
        System.out.println("🚫 Exception: " + e.getMessage());

        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"JWT manquant ou invalide\"}");
      }));

    System.out.println("✅ Security Filter Chain configured successfully");
    return http.build();
  }
}
