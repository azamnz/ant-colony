package aco.controller;

import aco.model.Settings;

import java.util.Map;

/** Итог разбора формы: параметры или ошибки по полям */
public sealed interface FormResult {

    record Ok(Settings settings) implements FormResult {
    }

    /** Имя поля → что не так */
    record Errors(Map<String, String> errors) implements FormResult {
        public Errors {
            errors = Map.copyOf(errors);
        }
    }
}
