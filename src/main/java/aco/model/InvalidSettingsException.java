package aco.model;

import java.util.List;
import java.util.stream.Collectors;

/** Неверные параметры; перечислены все проблемы сразу */
public final class InvalidSettingsException extends IllegalArgumentException {

    /** field — имя параметра в Settings, message — что не так */
    public record Problem(String field, String message) {
    }

    private final List<Problem> problems;

    public InvalidSettingsException(List<Problem> problems) {
        super(problems.stream().map(Problem::message).collect(Collectors.joining("; ")));
        this.problems = List.copyOf(problems);
    }

    public List<Problem> problems() {
        return problems;
    }
}
