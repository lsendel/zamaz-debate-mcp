package com.zamaz.workflow.api.config;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

import java.time.Instant;
import java.time.format.DateTimeParseException;

public class InstantScalar implements Coercing<Instant, String> {
    
    @Override
    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
        if (dataFetcherResult instanceof Instant) {
            return ((Instant) dataFetcherResult).toString();
        }
        throw new CoercingSerializeException("Expected an Instant object");
    }
    
    @Override
    public Instant parseValue(Object input) throws CoercingParseValueException {
        if (input instanceof String) {
            try {
                return Instant.parse((String) input);
            } catch (DateTimeParseException e) {
                throw new CoercingParseValueException("Invalid ISO-8601 instant format: " + input);
            }
        }
        throw new CoercingParseValueException("Expected a String");
    }
    
    @Override
    public Instant parseLiteral(Object input) throws CoercingParseLiteralException {
        if (input instanceof StringValue) {
            try {
                return Instant.parse(((StringValue) input).getValue());
            } catch (DateTimeParseException e) {
                throw new CoercingParseLiteralException("Invalid ISO-8601 instant format");
            }
        }
        throw new CoercingParseLiteralException("Expected a StringValue");
    }
}