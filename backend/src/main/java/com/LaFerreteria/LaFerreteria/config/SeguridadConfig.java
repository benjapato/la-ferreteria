package com.LaFerreteria.LaFerreteria.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SeguridadConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {

        http
                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource
                ))
                .authorizeHttpRequests(autorizacion ->
                        autorizacion
                                .requestMatchers(
                                        "/login",
                                        "/acceso-denegado",
                                        "/error",
                                        "/favicon.ico",
                                        "/app",
                                        "/app/**",
                                        "/api/auth/login",
                                        "/api/auth/me",
                                        "/api/auth/csrf"
                                )
                                .permitAll()

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/cash/open",
                                        "/api/cash/close"
                                )
                                .hasAnyRole(
                                        "ADMINISTRADOR",
                                        "VENDEDOR"
                                )

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/cash/status"
                                )
                                .hasAnyRole(
                                        "ADMINISTRADOR",
                                        "VENDEDOR"
                                )

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/sales/products",
                                        "/api/sales/categories",
                                        "/api/sales/cash/open",
                                        "/api/sales/*/cancel",
                                        "/api/catalog/**",
                                        "/api/customers/*/payments/*/cancel"
                                )
                                .hasRole("ADMINISTRADOR")

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/catalog/**"
                                )
                                .hasRole("ADMINISTRADOR")

                                .requestMatchers(
                                        "/api/purchases/**"
                                )
                                .hasRole("ADMINISTRADOR")

                                .requestMatchers(
                                        "/api/reports/**",
                                        "/api/users/**",
                                        "/api/backups/**",
                                        "/api/cash/**",
                                        "/api/stock-movements/**"
                                )
                                .hasRole("ADMINISTRADOR")

                                .requestMatchers(
                                        "/api/catalog/providers/**"
                                )
                                .hasRole("ADMINISTRADOR")

                                .requestMatchers(HttpMethod.PUT, "/api/printing/settings")
                                .hasRole("ADMINISTRADOR")
                                .requestMatchers(HttpMethod.POST, "/api/printing/test")
                                .hasRole("ADMINISTRADOR")
                                .requestMatchers("/api/printing/**")
                                .hasAnyRole("ADMINISTRADOR", "VENDEDOR")

                                .requestMatchers("/api/**")
                                .authenticated()

                                .requestMatchers(
                                        "/usuarios/**",
                                        "/compras/**",
                                        "/proveedores/**",
                                        "/categorias/**",
                                        "/movimientos-stock/**",
                                        "/reportes/**",
                                        "/caja/**",
                                        "/copias-seguridad/**"
                                )
                                .hasRole("ADMINISTRADOR")

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/ventas/cancelar/**"
                                )
                                .hasRole("ADMINISTRADOR")

                                .requestMatchers(
                                        "/productos/nuevo",
                                        "/productos/editar/**",
                                        "/productos/importar/**"
                                )
                                .hasRole("ADMINISTRADOR")

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/productos/**"
                                )
                                .hasRole("ADMINISTRADOR")

                                .anyRequest()
                                .authenticated()
                )
                .formLogin(formulario ->
                        formulario
                                .loginPage("/login")
                                .loginProcessingUrl("/login")
                                .usernameParameter("nombreUsuario")
                                .passwordParameter("contrasena")
                                .defaultSuccessUrl("/", true)
                                .failureUrl("/login?error")
                                .permitAll()
                )
                .logout(salida ->
                        salida
                                .logoutUrl("/logout")
                                .logoutSuccessUrl("/login?logout")
                                .invalidateHttpSession(true)
                                .deleteCookies("JSESSIONID")
                                .permitAll()
                )
                .exceptionHandling(excepciones ->
                        excepciones
                                .authenticationEntryPoint(
                                        (request, response, exception) -> {
                                            if (request.getRequestURI()
                                                    .startsWith("/api/")) {
                                                response.sendError(401);
                                                return;
                                            }

                                            response.sendRedirect(
                                                    request.getContextPath()
                                                            + "/login"
                                            );
                                        }
                                )
                                .accessDeniedPage(
                                        "/acceso-denegado"
                                )
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.frontend.url:http://localhost:5173}")
            String frontendUrl
    ) {
        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of(frontendUrl)
        );
        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );
        configuration.setAllowedHeaders(
                List.of("Content-Type", "X-CSRF-TOKEN")
        );
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(
                "/api/**",
                configuration
        );

        return source;
    }
}
