package com.example.ddd_start.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtTokenProvider jwtTokenProvider;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity security) throws Exception {
    return security
        .httpBasic(AbstractHttpConfigurer::disable)
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> auth
                .requestMatchers(
                    new AntPathRequestMatcher("/"),
                    new AntPathRequestMatcher("/login"),
                    new AntPathRequestMatcher("/signup"),
                    new AntPathRequestMatcher("/shop"),
                    new AntPathRequestMatcher("/shop/**"),
                    new AntPathRequestMatcher("/cart"),
                    new AntPathRequestMatcher("/my-orders"),
                    new AntPathRequestMatcher("/checkout"),
                    new AntPathRequestMatcher("/members/join"),
                    new AntPathRequestMatcher("/members/sign-in"),
                    new AntPathRequestMatcher("/swagger-ui.html"),
                    new AntPathRequestMatcher("/v3/api-docs/**"),
                    new AntPathRequestMatcher("/swagger-ui/**"),
                    new AntPathRequestMatcher("/categories")
                ).permitAll()
                // 상품 조회는 누구나 가능
                .requestMatchers(new AntPathRequestMatcher("/products", HttpMethod.GET.name())).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/products/**", HttpMethod.GET.name())).permitAll()
                // 상품 등록, 수정, 삭제는 인증 필요
                .requestMatchers(new AntPathRequestMatcher("/products", HttpMethod.POST.name())).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/products/**", HttpMethod.POST.name())).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/products", HttpMethod.PUT.name())).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/products/**", HttpMethod.PUT.name())).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/products", HttpMethod.DELETE.name())).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/products/**", HttpMethod.DELETE.name())).authenticated()
                .anyRequest().authenticated()
        )
        .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
            UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    // BCrypt Encoder 사용
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }
}
