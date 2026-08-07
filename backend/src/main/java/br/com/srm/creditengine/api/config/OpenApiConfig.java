package br.com.srm.creditengine.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI creditEngineOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SRM Credit Engine API")
                        .version("1.0.0")
                        .description("""
                                Plataforma de cessao de credito multimoedas: precificacao de recebiveis com \
                                desagio por risco, conversao cambial e liquidacao auditavel de lotes.

                                Erros seguem a RFC 7807 (application/problem+json), com as extensoes `code`, \
                                `timestamp` e, nas falhas de validacao, `violations`."""))
                .tags(List.of(
                        new Tag().name("Cambio").description("Cotacoes entre moedas"),
                        new Tag().name("Precificacao").description("Simulacao de desagio de recebiveis"),
                        new Tag().name("Liquidacao").description("Registro e liquidacao de lotes"),
                        new Tag().name("Relatorios").description("Extrato analitico de liquidacoes")));
    }
}
