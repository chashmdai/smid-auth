package cl.smid.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String DESCRIPCION = """
            Servicio de Identidad y Acceso de SMID. Emite, renueva y revoca credenciales \
            mediante endpoints publicos detras del Gateway, usando solo DTOs publicos con \
            identificadores opacos altKey.
            """;

    @Bean
    public OpenAPI smidOpenApi(ObjectProvider<BuildProperties> buildProperties) {
        BuildProperties build = buildProperties.getIfAvailable();
        String version = build != null ? build.getVersion() : "1.0.0";

        return new OpenAPI()
                .info(new Info()
                        .title("SMID - Identidad y Acceso API")
                        .version(version)
                        .description(DESCRIPCION));
    }
}
