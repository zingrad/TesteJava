package br.com.srm.creditengine.api.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * O relogio e injetado em vez de consultado estaticamente para que as regras que dependem do
 * momento atual (cotacao vigente, prazo do titulo) possam ser testadas de forma deterministica.
 */
@Configuration
class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
