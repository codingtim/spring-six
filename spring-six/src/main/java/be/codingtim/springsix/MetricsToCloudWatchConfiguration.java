package be.codingtim.springsix;

import brave.Tracing;
import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Configuration
public class MetricsToCloudWatchConfiguration {

    @Bean
    public MeterRegistry meterRegistry() {
        CloudWatchMeterRegistry registry = new CloudWatchMeterRegistry(
                new CloudWatchConfig() {
                    @Override
                    public String get(String key) {
                        return null;
                    }

                    @Override
                    public Duration step() {
                        return Duration.ofMillis(50);
                    }

                    @Override
                    public String namespace() {
                        return "MyCloud/test";
                    }
                }, Clock.SYSTEM, new CloudWatchAsyncClient() {
            @Override
            public String serviceName() {
                return null;
            }

            @Override
            public void close() {

            }

            @Override
            public CompletableFuture<PutMetricDataResponse> putMetricData(PutMetricDataRequest putMetricDataRequest) {
                System.out.println(putMetricDataRequest);
                return CompletableFuture.completedFuture(PutMetricDataResponse.builder().build());
            }
        });
        return registry;
    }

    @Bean
    public ObservationRegistry observationRegistry(MeterRegistry registry, Tracer tracer, Tracing tracing) {
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        //Write observations to the registry, could be a CloudWatchRegistry
        observationRegistry.observationConfig().observationHandler(new DefaultMeterObservationHandler(registry));
        return observationRegistry;
    }
}
