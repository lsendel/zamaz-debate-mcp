package com.zamaz.mcp.controller.config;

import com.zamaz.mcp.controller.statemachine.DebateEvents;
import com.zamaz.mcp.controller.statemachine.DebateStates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory
@Slf4j
public class StateMachineConfig extends StateMachineConfigurerAdapter<DebateStates, DebateEvents> {
    
    @Override
    public void configure(StateMachineStateConfigurer<DebateStates, DebateEvents> states) throws Exception {
        states
            .withStates()
            .initial(DebateStates.CREATED)
            .states(EnumSet.allOf(DebateStates.class))
            .end(DebateStates.ARCHIVED)
            .end(DebateStates.ERROR);
    }
    
    @Override
    public void configure(StateMachineTransitionConfigurer<DebateStates, DebateEvents> transitions) throws Exception {
        transitions
            .withExternal()
                .source(DebateStates.CREATED).target(DebateStates.INITIALIZED)
                .event(DebateEvents.INITIALIZE)
            .and()
            .withExternal()
                .source(DebateStates.INITIALIZED).target(DebateStates.IN_PROGRESS)
                .event(DebateEvents.START)
            .and()
            .withExternal()
                .source(DebateStates.IN_PROGRESS).target(DebateStates.IN_PROGRESS)
                .event(DebateEvents.SUBMIT_RESPONSE)
            .and()
            .withExternal()
                .source(DebateStates.IN_PROGRESS).target(DebateStates.ROUND_COMPLETE)
                .event(DebateEvents.COMPLETE_ROUND)
            .and()
            .withExternal()
                .source(DebateStates.ROUND_COMPLETE).target(DebateStates.IN_PROGRESS)
                .event(DebateEvents.START)
            .and()
            .withExternal()
                .source(DebateStates.ROUND_COMPLETE).target(DebateStates.DEBATE_COMPLETE)
                .event(DebateEvents.END_DEBATE)
            .and()
            .withExternal()
                .source(DebateStates.DEBATE_COMPLETE).target(DebateStates.ARCHIVED)
                .event(DebateEvents.ARCHIVE)
            .and()
            .withExternal()
                .source(DebateStates.IN_PROGRESS).target(DebateStates.ERROR)
                .event(DebateEvents.ERROR)
            .and()
            .withExternal()
                .source(DebateStates.ROUND_COMPLETE).target(DebateStates.ERROR)
                .event(DebateEvents.ERROR);
    }
}