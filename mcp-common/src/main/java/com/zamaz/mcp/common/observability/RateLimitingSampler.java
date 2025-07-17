package com.zamaz.mcp.common.observability;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Rate limiting sampler that limits the number of traces per second.
 * This sampler helps prevent overwhelming the tracing backend with too many traces.
 */
public class RateLimitingSampler implements Sampler {
    
    private static final String DESCRIPTION_PREFIX = "RateLimitingSampler{";
    private static final AttributeKey<String> SAMPLING_PROBABILITY = AttributeKey.stringKey("sampling.probability");
    
    private final int maxTracesPerSecond;
    private final AtomicLong traceCount;
    private final AtomicReference<Long> windowStart;
    private final long windowSizeNanos;
    
    /**
     * Creates a new rate limiting sampler.
     *
     * @param maxTracesPerSecond the maximum number of traces to sample per second
     */
    public RateLimitingSampler(int maxTracesPerSecond) {
        if (maxTracesPerSecond <= 0) {
            throw new IllegalArgumentException("maxTracesPerSecond must be positive");
        }
        this.maxTracesPerSecond = maxTracesPerSecond;
        this.traceCount = new AtomicLong(0);
        this.windowStart = new AtomicReference<>(System.nanoTime());
        this.windowSizeNanos = TimeUnit.SECONDS.toNanos(1);
    }
    
    @Override
    public SamplingResult shouldSample(
            Context parentContext,
            String traceId,
            String name,
            SpanKind spanKind,
            Attributes attributes,
            List<LinkData> parentLinks) {
        
        long currentTime = System.nanoTime();
        long currentWindowStart = windowStart.get();
        
        // Check if we need to reset the window
        if (currentTime - currentWindowStart >= windowSizeNanos) {
            // Try to reset the window
            if (windowStart.compareAndSet(currentWindowStart, currentTime)) {
                traceCount.set(0);
            }
        }
        
        // Check if we're within the rate limit
        long currentCount = traceCount.incrementAndGet();
        if (currentCount <= maxTracesPerSecond) {
            return SamplingResult.create(
                    SamplingDecision.RECORD_AND_SAMPLE,
                    Attributes.of(SAMPLING_PROBABILITY, String.format("%.2f", getProbability()))
            );
        } else {
            return SamplingResult.create(SamplingDecision.DROP);
        }
    }
    
    @Override
    public String getDescription() {
        return DESCRIPTION_PREFIX + "maxTracesPerSecond=" + maxTracesPerSecond + "}";
    }
    
    /**
     * Get the current sampling probability based on the rate.
     *
     * @return the sampling probability
     */
    private double getProbability() {
        long count = traceCount.get();
        if (count <= maxTracesPerSecond) {
            return 1.0;
        } else {
            return (double) maxTracesPerSecond / count;
        }
    }
    
    /**
     * Creates a rate limiting sampler with the specified maximum traces per second.
     *
     * @param maxTracesPerSecond the maximum number of traces to sample per second
     * @return the rate limiting sampler
     */
    public static RateLimitingSampler create(int maxTracesPerSecond) {
        return new RateLimitingSampler(maxTracesPerSecond);
    }
}