package com.zamaz.mcp.controller.graphql;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

/**
 * Exception resolver for GraphQL agentic flow operations.
 */
@Component
@Slf4j
public class AgenticFlowGraphQLExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        log.error("GraphQL error in agentic flow operation", ex);

        if (ex instanceof IllegalArgumentException) {
            return GraphqlErrorBuilder.newError()
                    .message(ex.getMessage())
                    .errorType(ErrorType.BAD_REQUEST)
                    .path(env.getExecutionStepInfo().getPath())
                    .location(env.getField().getSourceLocation())
                    .build();
        }

        if (ex instanceof RuntimeException && ex.getMessage().contains("not found")) {
            return GraphqlErrorBuilder.newError()
                    .message(ex.getMessage())
                    .errorType(ErrorType.NOT_FOUND)
                    .path(env.getExecutionStepInfo().getPath())
                    .location(env.getField().getSourceLocation())
                    .build();
        }

        return GraphqlErrorBuilder.newError()
                .message("Internal server error")
                .errorType(ErrorType.INTERNAL_ERROR)
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
    }
}