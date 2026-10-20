package com.LaFerreteria.LaFerreteria;

import com.LaFerreteria.LaFerreteria.enums.RolUsuario;
import com.LaFerreteria.LaFerreteria.repository.UsuarioRepository;
import com.LaFerreteria.LaFerreteria.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConfiguracionPublicacionTests {
    @Test
    void configuracionPublicaNoIncluyeConexionNiContrasenaPorDefecto() throws Exception {
        var config = new Properties();
        // Leer la configuracion publicable, no la configuracion H2 de las pruebas.
        try (var input = Files.newInputStream(Path.of("src/main/resources/application.properties"))) {
            config.load(input);
        }
        assertEquals("${SPRING_DATASOURCE_URL}", config.getProperty("spring.datasource.url"));
        assertEquals("${SPRING_DATASOURCE_USERNAME}", config.getProperty("spring.datasource.username"));
        assertEquals("${SPRING_DATASOURCE_PASSWORD}", config.getProperty("spring.datasource.password"));
        assertEquals("${APP_USUARIO_INICIAL_CONTRASENA:}", config.getProperty("app.usuario-inicial.contrasena"));
    }

    @Test
    void baseNuevaNoCreaAdministradorSinContrasena() {
        var repository = mock(UsuarioRepository.class);
        var encoder = mock(PasswordEncoder.class);
        var service = new UsuarioService(repository, encoder);

        assertThrows(IllegalArgumentException.class,
                () -> service.crearAdministradorInicial("Administrador", "admin", ""));
        verify(repository, never()).save(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void administradorExistenteNoNecesitaNiReemplazaLaContrasenaInicial() {
        var repository = mock(UsuarioRepository.class);
        var encoder = mock(PasswordEncoder.class);
        when(repository.countByRolAndActivoTrue(RolUsuario.ADMINISTRADOR)).thenReturn(1L);
        var service = new UsuarioService(repository, encoder);

        assertNull(service.crearAdministradorInicial("Administrador", "admin", ""));
        verify(repository, never()).save(any());
        verifyNoInteractions(encoder);
    }
}
