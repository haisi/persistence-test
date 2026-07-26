package li.selman.persistencetest.plan;

/** Thrown when an {@link ExecutionPlanAnalyzer} can't obtain or parse a plan. */
public class ExecutionPlanException extends RuntimeException {

    public ExecutionPlanException(String message, Throwable cause) {
        super(message, cause);
    }
}
