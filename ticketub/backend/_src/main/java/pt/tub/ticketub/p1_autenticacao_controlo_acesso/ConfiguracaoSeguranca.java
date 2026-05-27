package pt.tub.ticketub.p1_autenticacao_controlo_acesso;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class ConfiguracaoSeguranca {

    @Value("${app.security.auth-enabled:false}")
    private boolean authEnabled;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable());

        if (authEnabled) {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/public/**").permitAll()
                    .requestMatchers("/api/routes/**").permitAll()
                    .requestMatchers("/api/stops/**").permitAll()
                    .requestMatchers("/api/validations/**").permitAll()
                    .requestMatchers("/api/metrics").permitAll()
                    
                    // RBAC endpoint protection (UC01.3 & UC04.1)
                    .requestMatchers("/api/dashboard/**").hasAnyRole("GESTOR", "ANALISTA", "ADMIN")
                    .requestMatchers("/api/alertas/**").hasAnyRole("GESTOR", "ADMIN")
                    .requestMatchers("/api/exportacao/**").hasAnyRole("ANALISTA", "ADMIN")
                    .requestMatchers("/api/od/**").hasAnyRole("ANALISTA", "ADMIN")
                    .requestMatchers("/api/planeamento/**").hasAnyRole("ANALISTA", "GESTOR", "ADMIN")
                    .requestMatchers("/api/simulacao/**").hasAnyRole("ANALISTA", "GESTOR", "ADMIN")
                    .requestMatchers("/api/rgpd/**").hasAnyRole("DPO", "ADMIN")
                    
                    .anyRequest().authenticated())
                    .oauth2ResourceServer(oauth2 -> oauth2
                            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        } else {
            http.authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll());
        }

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
            Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
            
            // Map standard scope claims
            try {
                var defaultAuths = defaultConverter.convert(jwt);
                if (defaultAuths != null) {
                    for (var auth : defaultAuths) {
                        authorities.add((SimpleGrantedAuthority) auth);
                    }
                }
            } catch (Exception ignored) {}

            // Extract custom roles/permissions
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null) {
                roles = jwt.getClaimAsStringList("https://ticketub.pt/roles");
            }
            if (roles == null) {
                roles = jwt.getClaimAsStringList("permissions");
            }

            if (roles != null) {
                for (String role : roles) {
                    String formattedRole = role.toUpperCase();
                    if (!formattedRole.startsWith("ROLE_")) {
                        formattedRole = "ROLE_" + formattedRole;
                    }
                    authorities.add(new SimpleGrantedAuthority(formattedRole));
                }
            }
            return (Collection) authorities;
        });
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
