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

@Configuration @EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public UserDetailsService userDetailsService(UserRepository repo){
    return username -> {
      User u = repo.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("not found"));
      return new org.springframework.security.core.userdetails.User(
        u.getEmail(), u.getPassword(),
        List.of(new SimpleGrantedAuthority("ROLE_"+u.getRole().name()))
      );
    };
  }

  @Bean public PasswordEncoder passwordEncoder(){ return new BCryptPasswordEncoder(); }
  @Bean public AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception { return c.getAuthenticationManager(); }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(){
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(List.of("http://localhost:4200"));
    cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
    cfg.setAllowedHeaders(List.of("Authorization","Content-Type","X-Requested-With","Accept","Origin"));
    cfg.setAllowCredentials(true);
    var src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", cfg);
    return src;
  }@Bean
public SecurityFilterChain chain(HttpSecurity http, JwtAuthenticationFilter jwt) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
       .cors(c -> c.configurationSource(corsConfigurationSource()))
       .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
       .authorizeHttpRequests(a -> a
         .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
.requestMatchers("/auth/**").permitAll()         .anyRequest().authenticated()
       )
       .httpBasic(AbstractHttpConfigurer::disable)
       .formLogin(AbstractHttpConfigurer::disable)
       .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
       .exceptionHandling(ex -> ex.authenticationEntryPoint((req,res,e)->{
         res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
         res.setContentType("application/json;charset=UTF-8");
         res.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"JWT manquant ou invalide\"}");
       }));
    return http.build();
}
}