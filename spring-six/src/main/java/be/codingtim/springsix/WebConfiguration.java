package be.codingtim.springsix;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.ServerHttpObservationFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableWebMvc
@ComponentScan(basePackageClasses = WebConfiguration.class,
        excludeFilters = {@ComponentScan.Filter(type = FilterType.ANNOTATION, value = Configuration.class)})
@Import(MetricsToZipkinConfiguration.class)
public class WebConfiguration {

    @Bean
    public ServerHttpObservationFilter serverHttpObservationFilter(ObservationRegistry observationRegistry) {
        return new ServerHttpObservationFilter(observationRegistry);
    }

    @Bean
    public RestTemplate restTemplate(ObservationRegistry observationRegistry) {
        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        restTemplate.setObservationRegistry(observationRegistry);
        return restTemplate;
    }

}
