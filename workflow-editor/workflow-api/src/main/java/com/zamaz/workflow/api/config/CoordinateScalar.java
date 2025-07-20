package com.zamaz.workflow.api.config;

import graphql.language.ArrayValue;
import graphql.language.FloatValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

import java.util.List;

public class CoordinateScalar implements Coercing<List<Double>, List<Double>> {
    
    @Override
    public List<Double> serialize(Object dataFetcherResult) throws CoercingSerializeException {
        if (dataFetcherResult instanceof List) {
            List<?> list = (List<?>) dataFetcherResult;
            if (list.size() == 2 && list.get(0) instanceof Number && list.get(1) instanceof Number) {
                return List.of(
                        ((Number) list.get(0)).doubleValue(),
                        ((Number) list.get(1)).doubleValue()
                );
            }
        }
        throw new CoercingSerializeException("Expected a list of two numbers for Coordinate");
    }
    
    @Override
    public List<Double> parseValue(Object input) throws CoercingParseValueException {
        if (input instanceof List) {
            List<?> list = (List<?>) input;
            if (list.size() == 2 && list.get(0) instanceof Number && list.get(1) instanceof Number) {
                return List.of(
                        ((Number) list.get(0)).doubleValue(),
                        ((Number) list.get(1)).doubleValue()
                );
            }
        }
        throw new CoercingParseValueException("Expected a list of two numbers for Coordinate");
    }
    
    @Override
    public List<Double> parseLiteral(Object input) throws CoercingParseLiteralException {
        if (input instanceof ArrayValue) {
            ArrayValue arrayValue = (ArrayValue) input;
            List<Value> values = arrayValue.getValues();
            if (values.size() == 2 && 
                values.get(0) instanceof FloatValue && 
                values.get(1) instanceof FloatValue) {
                return List.of(
                        ((FloatValue) values.get(0)).getValue().doubleValue(),
                        ((FloatValue) values.get(1)).getValue().doubleValue()
                );
            }
        }
        throw new CoercingParseLiteralException("Expected an array of two floats for Coordinate");
    }
}