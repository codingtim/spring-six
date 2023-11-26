package be.codingtim.springsix;

import brave.Tracing;
import brave.baggage.BaggageField;
import brave.baggage.BaggagePropagation;
import brave.baggage.BaggagePropagationConfig;
import brave.context.slf4j.MDCScopeDecorator;
import brave.handler.SpanHandler;
import brave.propagation.B3Propagation;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.sampler.Sampler;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BravePropagator;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.brave.ZipkinSpanHandler;
import zipkin2.reporter.urlconnection.URLConnectionSender;

@Configuration
public class MetricsToZipkinConfiguration {

    @Bean
    public MeterRegistry meterRegistry() {
        //simple registry for writing to logger or zipkin
        return new SimpleMeterRegistry();
    }

    @Bean
    public ThreadLocalCurrentTraceContext braveCurrentTraceContext() {
        // [Brave component] CurrentTraceContext is a Brave component that allows you to
// retrieve the current TraceContext.
        ThreadLocalCurrentTraceContext braveCurrentTraceContext = ThreadLocalCurrentTraceContext.newBuilder()
                .addScopeDecorator(MDCScopeDecorator.get()) // Example of Brave's
                // automatic MDC setup
                .build();
        return braveCurrentTraceContext;
    }

    @Bean
    public Tracing tracing(ThreadLocalCurrentTraceContext braveCurrentTraceContext) {
        //https://micrometer.io/docs/tracing#_micrometer_tracing_brave_setup
        // [Brave component] Example of using a SpanHandler. SpanHandler is a component
// that gets called when a span is finished. Here we have an example of setting it
// up with sending spans
// in a Zipkin format to the provided location via the UrlConnectionSender
// (through the <io.zipkin.reporter2:zipkin-sender-urlconnection> dependency)
// Another option could be to use a TestSpanHandler for testing purposes.
        SpanHandler spanHandler = ZipkinSpanHandler
                .create(AsyncReporter.create(URLConnectionSender.create("http://localhost:9411/api/v2/spans")));

        // [Brave component] Tracing is the root component that allows to configure the
// tracer, handlers, context propagation etc.
        Tracing tracing = Tracing.newBuilder()
                .currentTraceContext(braveCurrentTraceContext)
                .supportsJoin(false)
                .traceId128Bit(true)
                // For Baggage to work you need to provide a list of fields to propagate
                .propagationFactory(BaggagePropagation.newFactoryBuilder(B3Propagation.FACTORY)
                        .add(BaggagePropagationConfig.SingleBaggageField.remote(BaggageField.create("from_span_in_scope 1")))
                        .add(BaggagePropagationConfig.SingleBaggageField.remote(BaggageField.create("from_span_in_scope 2")))
                        .add(BaggagePropagationConfig.SingleBaggageField.remote(BaggageField.create("from_span")))
                        .build())
                .sampler(Sampler.ALWAYS_SAMPLE)
                .addSpanHandler(spanHandler)
                .build();
        return tracing;
    }

    @Bean
    public Tracer tracer(Tracing tracing, ThreadLocalCurrentTraceContext braveCurrentTraceContext) {
        // [Micrometer Tracing component] A Micrometer Tracing wrapper for Brave's
// CurrentTraceContext
        CurrentTraceContext bridgeContext = new BraveCurrentTraceContext(braveCurrentTraceContext);

// [Brave component] Tracer is a component that handles the life-cycle of a span
        brave.Tracer braveTracer = tracing.tracer();

// [Micrometer Tracing component] A Micrometer Tracing wrapper for Brave's Tracer
        Tracer tracer = new BraveTracer(braveTracer, bridgeContext, new BraveBaggageManager());
        return tracer;
    }

    @Bean
    public ObservationRegistry observationRegistry(MeterRegistry registry, Tracer tracer, Tracing tracing) {
        ObservationRegistry observationRegistry = ObservationRegistry.create();

        BravePropagator propagator = new BravePropagator(tracing);
        observationRegistry.observationConfig()
                // assuming that micrometer-core is on the classpath
                .observationHandler(new DefaultMeterObservationHandler(registry))
                // we set up a first matching handler that creates spans - it comes from
                // Micrometer
                // Tracing. We set up spans for sending and receiving data over the wire
                // and a default one
                .observationHandler(new ObservationHandler.FirstMatchingCompositeObservationHandler(
                        new PropagatingSenderTracingObservationHandler<>(tracer, propagator),
                        new PropagatingReceiverTracingObservationHandler<>(tracer, propagator),
                        new DefaultTracingObservationHandler(tracer)));
        return observationRegistry;
    }
}
