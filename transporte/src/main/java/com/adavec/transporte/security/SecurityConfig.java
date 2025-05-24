package com.adavec.transporte.security;

import com.adavec.transporte.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, CustomUserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors()
                .and()
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Públicos
                        .requestMatchers(
                                "/api/auth/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/h2-console/**"
                        ).permitAll()

                        // Unidades
                        .requestMatchers(HttpMethod.GET, "/api/unidades/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/unidades/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/unidades").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/unidades/**").hasRole("ADMIN")

                        // Seguros
                        .requestMatchers(HttpMethod.GET, "/api/seguros/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/seguros").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/seguros/**").hasRole("ADMIN")

                        // Modelos
                        .requestMatchers(HttpMethod.GET, "/api/modelos").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/modelos").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/modelos/**").hasRole("ADMIN")

                        // Distribuidores
                        .requestMatchers(HttpMethod.GET, "/api/distribuidores").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/distribuidores").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/distribuidores/**").hasRole("ADMIN")

                        // Cobros
                        .requestMatchers(HttpMethod.GET, "/api/cobros/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/cobros").hasRole("ADMIN")

                        // Importación/Reportes
                        .requestMatchers(HttpMethod.POST, "/api/importar/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reportes/**").hasRole("ADMIN")

                        // Cualquier otra ruta
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Acceso no autorizado\"}");
                        })
                );
        // Para H2 Console (solo en desarrollo)
        http.headers().frameOptions().disable();

        return http.build();
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(List.of("*")); // Permite cualquier origen con pattern
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

}