package com.zamaz.workflow.api.config;

import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration
public class GraphQLConfig {
    
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.DateTime)
                .scalar(ExtendedScalars.Json)
                .scalar(coordinateScalar())
                .scalar(instantScalar());
    }
    
    @Bean
    public GraphQLScalarType coordinateScalar() {
        return GraphQLScalarType.newScalar()
                .name("Coordinate")
                .description("Geographic coordinate as [longitude, latitude]")
                .coercing(new CoordinateScalar())
                .build();
    }
    
    @Bean
    public GraphQLScalarType instantScalar() {
        return GraphQLScalarType.newScalar()
                .name("Instant")
                .description("An instantaneous point on the time-line represented by ISO-8601 format")
                .coercing(new InstantScalar())
                .build();
    }
}