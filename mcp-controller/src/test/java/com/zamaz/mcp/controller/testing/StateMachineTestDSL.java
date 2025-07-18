package com.zamaz.mcp.controller.testing;

import com.zamaz.mcp.controller.domain.model.DebateStatus;
import com.zamaz.mcp.controller.domain.model.Debate;
import org.assertj.core.api.Assertions;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * DSL for testing state machine transitions in debates.
 * Provides fluent API for defining and validating state transitions.
 */
public class StateMachineTestDSL {

    private final StateTransitionMap transitionMap = new StateTransitionMap();
    private final List<StateValidation> validations = new ArrayList<>();
    private final Map<String, Object> context = new HashMap<>();
    
    public static StateMachineTestDSL debate() {
        return new StateMachineTestDSL();
    }

    /**
     * Defines a state transition scenario.
     */
    public StateTransitionBuilder from(DebateStatus fromStatus) {
        return new StateTransitionBuilder(fromStatus);
    }

    /**
     * Validates state transitions.
     */
    public ValidationBuilder should() {
        return new ValidationBuilder();
    }

    /**
     * Executes the test scenario.
     */
    public TestResult execute(Debate initialDebate) {
        TestResult result = new TestResult();
        result.setInitialState(initialDebate.getStatus());
        
        try {
            // Execute all validations
            for (StateValidation validation : validations) {
                validation.execute(initialDebate, result);
            }
            
            result.setSuccess(true);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError(e);
        }
        
        return result;
    }

    public class StateTransitionBuilder {
        private final DebateStatus fromStatus;
        private final List<TransitionRule> rules = new ArrayList<>();

        public StateTransitionBuilder(DebateStatus fromStatus) {
            this.fromStatus = fromStatus;
        }

        public StateTransitionBuilder to(DebateStatus toStatus) {
            rules.add(new TransitionRule(fromStatus, toStatus, null, null));
            return this;
        }

        public StateTransitionBuilder when(String action) {
            if (!rules.isEmpty()) {
                rules.get(rules.size() - 1).setAction(action);
            }
            return this;
        }

        public StateTransitionBuilder requires(Predicate<Debate> condition) {
            if (!rules.isEmpty()) {
                rules.get(rules.size() - 1).setCondition(condition);
            }
            return this;
        }

        public StateTransitionBuilder withTimeout(Duration timeout) {
            context.put("timeout", timeout);
            return this;
        }

        public StateMachineTestDSL and() {
            transitionMap.addRules(rules);
            return StateMachineTestDSL.this;
        }
    }

    public class ValidationBuilder {
        public ValidationBuilder allowTransition(DebateStatus from, DebateStatus to) {
            validations.add(new AllowTransitionValidation(from, to));
            return this;
        }

        public ValidationBuilder rejectTransition(DebateStatus from, DebateStatus to) {
            validations.add(new RejectTransitionValidation(from, to));
            return this;
        }

        public ValidationBuilder requireCondition(Predicate<Debate> condition, String description) {
            validations.add(new ConditionValidation(condition, description));
            return this;
        }

        public ValidationBuilder maintainInvariant(Predicate<Debate> invariant, String description) {
            validations.add(new InvariantValidation(invariant, description));
            return this;
        }

        public ValidationBuilder reachState(DebateStatus targetState, int maxSteps) {
            validations.add(new ReachabilityValidation(targetState, maxSteps));
            return this;
        }

        public ValidationBuilder completeCycle(DebateStatus... states) {
            validations.add(new CycleValidation(Arrays.asList(states)));
            return this;
        }

        public StateMachineTestDSL within(Duration timeout) {
            context.put("validation_timeout", timeout);
            return StateMachineTestDSL.this;
        }
    }

    // Core classes for state machine testing

    public static class StateTransitionMap {
        private final Map<DebateStatus, List<TransitionRule>> transitions = new HashMap<>();

        public void addRules(List<TransitionRule> rules) {
            for (TransitionRule rule : rules) {
                transitions.computeIfAbsent(rule.fromStatus, k -> new ArrayList<>()).add(rule);
            }
        }

        public boolean isTransitionAllowed(DebateStatus from, DebateStatus to, Debate debate) {
            List<TransitionRule> rules = transitions.getOrDefault(from, Collections.emptyList());
            return rules.stream().anyMatch(rule -> 
                rule.toStatus == to && (rule.condition == null || rule.condition.test(debate))
            );
        }

        public List<DebateStatus> getAllowedTransitions(DebateStatus from, Debate debate) {
            return transitions.getOrDefault(from, Collections.emptyList()).stream()
                .filter(rule -> rule.condition == null || rule.condition.test(debate))
                .map(rule -> rule.toStatus)
                .toList();
        }
    }

    public static class TransitionRule {
        private final DebateStatus fromStatus;
        private final DebateStatus toStatus;
        private String action;
        private Predicate<Debate> condition;

        public TransitionRule(DebateStatus fromStatus, DebateStatus toStatus, String action, Predicate<Debate> condition) {
            this.fromStatus = fromStatus;
            this.toStatus = toStatus;
            this.action = action;
            this.condition = condition;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public void setCondition(Predicate<Debate> condition) {
            this.condition = condition;
        }

        // Getters
        public DebateStatus getFromStatus() { return fromStatus; }
        public DebateStatus getToStatus() { return toStatus; }
        public String getAction() { return action; }
        public Predicate<Debate> getCondition() { return condition; }
    }

    // Validation implementations

    public abstract static class StateValidation {
        protected final String description;

        public StateValidation(String description) {
            this.description = description;
        }

        public abstract void execute(Debate debate, TestResult result);
    }

    public static class AllowTransitionValidation extends StateValidation {
        private final DebateStatus from;
        private final DebateStatus to;

        public AllowTransitionValidation(DebateStatus from, DebateStatus to) {
            super(String.format("Allow transition from %s to %s", from, to));
            this.from = from;
            this.to = to;
        }

        @Override
        public void execute(Debate debate, TestResult result) {
            if (debate.getStatus() == from) {
                // Simulate transition attempt
                boolean allowed = canTransition(debate, to);
                Assertions.assertThat(allowed)
                    .as(description)
                    .isTrue();
                result.addValidation(description, true);
            }
        }

        private boolean canTransition(Debate debate, DebateStatus toStatus) {
            // Implement actual transition logic or use state machine
            return switch (debate.getStatus()) {
                case CREATED -> toStatus == DebateStatus.IN_PROGRESS || toStatus == DebateStatus.CANCELLED;
                case IN_PROGRESS -> toStatus == DebateStatus.PAUSED || toStatus == DebateStatus.COMPLETED || toStatus == DebateStatus.CANCELLED;
                case PAUSED -> toStatus == DebateStatus.IN_PROGRESS || toStatus == DebateStatus.CANCELLED;
                case COMPLETED, CANCELLED -> false; // Terminal states
            };
        }
    }

    public static class RejectTransitionValidation extends StateValidation {
        private final DebateStatus from;
        private final DebateStatus to;

        public RejectTransitionValidation(DebateStatus from, DebateStatus to) {
            super(String.format("Reject transition from %s to %s", from, to));
            this.from = from;
            this.to = to;
        }

        @Override
        public void execute(Debate debate, TestResult result) {
            if (debate.getStatus() == from) {
                boolean allowed = canTransition(debate, to);
                Assertions.assertThat(allowed)
                    .as(description)
                    .isFalse();
                result.addValidation(description, true);
            }
        }

        private boolean canTransition(Debate debate, DebateStatus toStatus) {
            // Same logic as AllowTransitionValidation but expecting false
            return switch (debate.getStatus()) {
                case CREATED -> toStatus == DebateStatus.IN_PROGRESS || toStatus == DebateStatus.CANCELLED;
                case IN_PROGRESS -> toStatus == DebateStatus.PAUSED || toStatus == DebateStatus.COMPLETED || toStatus == DebateStatus.CANCELLED;
                case PAUSED -> toStatus == DebateStatus.IN_PROGRESS || toStatus == DebateStatus.CANCELLED;
                case COMPLETED, CANCELLED -> false;
            };
        }
    }

    public static class ConditionValidation extends StateValidation {
        private final Predicate<Debate> condition;

        public ConditionValidation(Predicate<Debate> condition, String description) {
            super(description);
            this.condition = condition;
        }

        @Override
        public void execute(Debate debate, TestResult result) {
            boolean conditionMet = condition.test(debate);
            Assertions.assertThat(conditionMet)
                .as(description)
                .isTrue();
            result.addValidation(description, conditionMet);
        }
    }

    public static class InvariantValidation extends StateValidation {
        private final Predicate<Debate> invariant;

        public InvariantValidation(Predicate<Debate> invariant, String description) {
            super("Invariant: " + description);
            this.invariant = invariant;
        }

        @Override
        public void execute(Debate debate, TestResult result) {
            boolean invariantHolds = invariant.test(debate);
            Assertions.assertThat(invariantHolds)
                .as(description)
                .isTrue();
            result.addValidation(description, invariantHolds);
        }
    }

    public static class ReachabilityValidation extends StateValidation {
        private final DebateStatus targetState;
        private final int maxSteps;

        public ReachabilityValidation(DebateStatus targetState, int maxSteps) {
            super(String.format("Can reach %s within %d steps", targetState, maxSteps));
            this.targetState = targetState;
            this.maxSteps = maxSteps;
        }

        @Override
        public void execute(Debate debate, TestResult result) {
            boolean reachable = canReachState(debate, targetState, maxSteps);
            Assertions.assertThat(reachable)
                .as(description)
                .isTrue();
            result.addValidation(description, reachable);
        }

        private boolean canReachState(Debate debate, DebateStatus target, int steps) {
            if (steps <= 0) return false;
            if (debate.getStatus() == target) return true;

            // BFS to find path to target state
            Set<DebateStatus> visited = new HashSet<>();
            Queue<StateStep> queue = new LinkedList<>();
            queue.add(new StateStep(debate.getStatus(), 0));

            while (!queue.isEmpty()) {
                StateStep current = queue.poll();
                if (current.steps >= steps) continue;
                if (visited.contains(current.state)) continue;
                visited.add(current.state);

                List<DebateStatus> nextStates = getPossibleTransitions(current.state);
                for (DebateStatus nextState : nextStates) {
                    if (nextState == target) return true;
                    queue.add(new StateStep(nextState, current.steps + 1));
                }
            }

            return false;
        }

        private List<DebateStatus> getPossibleTransitions(DebateStatus from) {
            return switch (from) {
                case CREATED -> List.of(DebateStatus.IN_PROGRESS, DebateStatus.CANCELLED);
                case IN_PROGRESS -> List.of(DebateStatus.PAUSED, DebateStatus.COMPLETED, DebateStatus.CANCELLED);
                case PAUSED -> List.of(DebateStatus.IN_PROGRESS, DebateStatus.CANCELLED);
                case COMPLETED, CANCELLED -> Collections.emptyList();
            };
        }

        private static class StateStep {
            final DebateStatus state;
            final int steps;

            StateStep(DebateStatus state, int steps) {
                this.state = state;
                this.steps = steps;
            }
        }
    }

    public static class CycleValidation extends StateValidation {
        private final List<DebateStatus> cycle;

        public CycleValidation(List<DebateStatus> cycle) {
            super("Complete cycle: " + cycle);
            this.cycle = cycle;
        }

        @Override
        public void execute(Debate debate, TestResult result) {
            boolean canCompleteCycle = validateCycle(debate);
            Assertions.assertThat(canCompleteCycle)
                .as(description)
                .isTrue();
            result.addValidation(description, canCompleteCycle);
        }

        private boolean validateCycle(Debate debate) {
            DebateStatus currentState = debate.getStatus();
            
            for (int i = 0; i < cycle.size(); i++) {
                DebateStatus expectedState = cycle.get(i);
                if (currentState == expectedState) {
                    // Check if we can complete the rest of the cycle
                    return canCompleteRemainingCycle(currentState, i);
                }
            }
            
            return false;
        }

        private boolean canCompleteRemainingCycle(DebateStatus startState, int startIndex) {
            DebateStatus currentState = startState;
            
            for (int i = startIndex; i < cycle.size() + startIndex; i++) {
                DebateStatus targetState = cycle.get(i % cycle.size());
                if (currentState == targetState) {
                    if (i < cycle.size() + startIndex - 1) {
                        DebateStatus nextState = cycle.get((i + 1) % cycle.size());
                        if (!canDirectlyTransition(currentState, nextState)) {
                            return false;
                        }
                        currentState = nextState;
                    }
                } else {
                    return false;
                }
            }
            
            return true;
        }

        private boolean canDirectlyTransition(DebateStatus from, DebateStatus to) {
            return switch (from) {
                case CREATED -> to == DebateStatus.IN_PROGRESS || to == DebateStatus.CANCELLED;
                case IN_PROGRESS -> to == DebateStatus.PAUSED || to == DebateStatus.COMPLETED || to == DebateStatus.CANCELLED;
                case PAUSED -> to == DebateStatus.IN_PROGRESS || to == DebateStatus.CANCELLED;
                case COMPLETED, CANCELLED -> false;
            };
        }
    }

    public static class TestResult {
        private DebateStatus initialState;
        private boolean success;
        private Exception error;
        private final Map<String, Boolean> validations = new HashMap<>();
        private final List<String> trace = new ArrayList<>();

        public void setInitialState(DebateStatus state) {
            this.initialState = state;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public void setError(Exception error) {
            this.error = error;
        }

        public void addValidation(String validation, boolean passed) {
            validations.put(validation, passed);
            trace.add(String.format("%s: %s", validation, passed ? "PASS" : "FAIL"));
        }

        // Getters
        public DebateStatus getInitialState() { return initialState; }
        public boolean isSuccess() { return success; }
        public Exception getError() { return error; }
        public Map<String, Boolean> getValidations() { return validations; }
        public List<String> getTrace() { return trace; }

        public int getPassedValidations() {
            return (int) validations.values().stream().mapToInt(b -> b ? 1 : 0).sum();
        }

        public int getTotalValidations() {
            return validations.size();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("State Machine Test Result:\n");
            sb.append(String.format("Initial State: %s\n", initialState));
            sb.append(String.format("Success: %s\n", success));
            sb.append(String.format("Validations: %d/%d passed\n", getPassedValidations(), getTotalValidations()));
            
            if (error != null) {
                sb.append(String.format("Error: %s\n", error.getMessage()));
            }
            
            sb.append("Trace:\n");
            for (String entry : trace) {
                sb.append("  ").append(entry).append("\n");
            }
            
            return sb.toString();
        }
    }
}