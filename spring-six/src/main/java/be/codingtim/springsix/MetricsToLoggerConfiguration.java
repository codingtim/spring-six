package be.codingtim.springsix;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.ObservationTextPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsToLoggerConfiguration {

    private static final Logger OBSERVARTIONS_LOGGER = LoggerFactory.getLogger("Observations");


    @Bean
    public MeterRegistry meterRegistry() {
        //simple registry for writing to logger or zipkin
        return new SimpleMeterRegistry();
    }

    @Bean
    public ObservationRegistry observationRegistry(MeterRegistry registry) {
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        //Write observations to logger
        observationRegistry.observationConfig().observationHandler(new ObservationTextPublisher(OBSERVARTIONS_LOGGER::info));
        return observationRegistry;
    }
}
