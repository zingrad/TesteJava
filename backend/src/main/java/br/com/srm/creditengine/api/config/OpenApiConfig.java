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
                                Plataforma de cessão de crédito multimoedas: precificação de recebíveis com \
                                deságio por risco, conversão cambial e liquidação auditável de lotes.

                                Erros seguem a RFC 7807 (application/problem+json), com as extensoes `code`, \
                                `timestamp` e, nas falhas de validação, `violations`."""))
                .tags(List.of(
                        new Tag().name("Câmbio").description("Cotações entre moedas"),
                        new Tag().name("Precificação").description("Simulação de deságio de recebíveis"),
                        new Tag().name("Liquidação").description("Registro e liquidação de lotes"),
                        new Tag().name("Relatórios").description("Extrato analítico de liquidações")));
    }
}
