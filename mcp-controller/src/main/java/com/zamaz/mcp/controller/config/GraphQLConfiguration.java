package com.zamaz.mcp.controller.config;

import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

/**
 * GraphQL configuration for scalar types and runtime wiring.
 */
@Configuration
public class GraphQLConfiguration {

    /**
     * Configures custom scalar types for GraphQL.
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.Json)
                .scalar(dateTimeScalar())
                .scalar(durationScalar());
    }

    /**
     * Custom DateTime scalar type.
     */
    private GraphQLScalarType dateTimeScalar() {
        return GraphQLScalarType.newScalar()
                .name("DateTime")
                .description("A date-time string in ISO 8601 format")
                .coercing(new Coercing<OffsetDateTime, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof OffsetDateTime) {
                            return ((OffsetDateTime) dataFetcherResult).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        } else if (dataFetcherResult instanceof Instant) {
                            return ((Instant) dataFetcherResult).atOffset(java.time.ZoneOffset.UTC)
                                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        } else {
                            throw new CoercingSerializeException("Expected OffsetDateTime or Instant but got " + 
                                    dataFetcherResult.getClass().getSimpleName());
                        }
                    }

                    @Override
                    public OffsetDateTime parseValue(Object input) throws CoercingParseValueException {
                        try {
                            if (input instanceof String) {
                                return OffsetDateTime.parse((String) input, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                            } else {
                                throw new CoercingParseValueException("Expected String but got " + 
                                        input.getClass().getSimpleName());
                            }
                        } catch (Exception e) {
                            throw new CoercingParseValueException("Invalid DateTime format: " + input, e);
                        }
                    }

                    @Override
                    public OffsetDateTime parseLiteral(Object input) throws CoercingParseLiteralException {
                        if (input instanceof StringValue) {
                            try {
                                return OffsetDateTime.parse(((StringValue) input).getValue(), 
                                        DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                            } catch (Exception e) {
                                throw new CoercingParseLiteralException("Invalid DateTime format", e);
                            }
                        } else {
                            throw new CoercingParseLiteralException("Expected StringValue but got " + 
                                    input.getClass().getSimpleName());
                        }
                    }
                })
                .build();
    }

    /**
     * Custom Duration scalar type.
     */
    private GraphQLScalarType durationScalar() {
        return GraphQLScalarType.newScalar()
                .name("Duration")
                .description("A duration string in ISO 8601 format (e.g., PT1H30M)")
                .coercing(new Coercing<Duration, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof Duration) {
                            return dataFetcherResult.toString();
                        } else if (dataFetcherResult instanceof Long) {
                            // Assume milliseconds
                            return Duration.ofMillis((Long) dataFetcherResult).toString();
                        } else {
                            throw new CoercingSerializeException("Expected Duration or Long but got " + 
                                    dataFetcherResult.getClass().getSimpleName());
                        }
                    }

                    @Override
                    public Duration parseValue(Object input) throws CoercingParseValueException {
                        try {
                            if (input instanceof String) {
                                return Duration.parse((String) input);
                            } else if (input instanceof Long) {
                                return Duration.ofMillis((Long) input);
                            } else {
                                throw new CoercingParseValueException("Expected String or Long but got " + 
                                        input.getClass().getSimpleName());
                            }
                        } catch (Exception e) {
                            throw new CoercingParseValueException("Invalid Duration format: " + input, e);
                        }
                    }

                    @Override
                    public Duration parseLiteral(Object input) throws CoercingParseLiteralException {
                        if (input instanceof StringValue) {
                            try {
                                return Duration.parse(((StringValue) input).getValue());
                            } catch (Exception e) {
                                throw new CoercingParseLiteralException("Invalid Duration format", e);
                            }
                        } else {
                            throw new CoercingParseLiteralException("Expected StringValue but got " + 
                                    input.getClass().getSimpleName());
                        }
                    }
                })
                .build();
    }
}