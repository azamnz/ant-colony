package aco.controller;

import aco.model.InvalidSettingsException;
import aco.model.Settings;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Разбор полей параметров; никогда не бросает исключений */
public final class SettingsForm {

    /** Поля в порядке формы: имя параметра в Settings → подпись */
    public static final Map<String, String> FIELDS = fields();
    private static final Set<String> INTEGERS = Set.of("ants", "cycles");

    private SettingsForm() {
    }

    private static Map<String, String> fields() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("ants", "Муравьёв");
        m.put("alpha", "α — вес феромона");
        m.put("beta", "β — вес расстояния");
        m.put("rho", "ρ — обновление и испарение");
        m.put("q", "Q — феромон на маршрут");
        m.put("tau0", "τ₀ — начальный феромон");
        m.put("elite", "e — элитные муравьи");
        m.put("cycles", "Циклов M");
        return java.util.Collections.unmodifiableMap(m);
    }

    /** Тексты полей для параметров */
    public static Map<String, String> texts(Settings s) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("ants", String.valueOf(s.ants()));
        m.put("alpha", Texts.plain(s.alpha()));
        m.put("beta", Texts.plain(s.beta()));
        m.put("rho", Texts.plain(s.rho()));
        m.put("q", Texts.plain(s.q()));
        m.put("tau0", Texts.plain(s.tau0()));
        m.put("elite", Texts.plain(s.elite()));
        m.put("cycles", String.valueOf(s.cycles()));
        return m;
    }

    public static FormResult parse(Map<String, String> texts) {
        Map<String, String> errors = new LinkedHashMap<>();
        Map<String, Double> values = new LinkedHashMap<>();
        for (String field : FIELDS.keySet()) {
            String raw = texts.get(field);
            String t = raw == null ? "" : raw.strip().replace(',', '.');
            if (t.isEmpty()) {
                errors.put(field, "пусто: введите число");
            } else if (INTEGERS.contains(field)) {
                if (!t.matches("[+-]?\\d+")) {
                    errors.put(field, "нужно целое число");
                } else if (t.replaceFirst("^[+-]?0*", "").length() > 9) {
                    errors.put(field, "слишком большое число");
                } else {
                    values.put(field, (double) Integer.parseInt(t));
                }
            } else {
                try {
                    values.put(field, Double.parseDouble(t));
                } catch (NumberFormatException e) {
                    errors.put(field, "нужно число, например 0.5");
                }
            }
        }
        // нечитаемые поля временно берутся из умолчаний: так проверяются диапазоны остальных
        Map<String, String> defaults = texts(Settings.defaults());
        for (String field : errors.keySet()) {
            values.put(field, Double.parseDouble(defaults.get(field)));
        }
        try {
            Settings s = new Settings(values.get("ants").intValue(), values.get("alpha"),
                    values.get("beta"), values.get("rho"), values.get("q"), values.get("tau0"), values.get("elite"),
                    values.get("cycles").intValue());
            return errors.isEmpty() ? new FormResult.Ok(s) : new FormResult.Errors(errors);
        } catch (InvalidSettingsException e) {
            List<InvalidSettingsException.Problem> problems = e.problems();
            problems.forEach(p -> errors.putIfAbsent(p.field(), p.message()));
            return new FormResult.Errors(errors);
        }
    }

    /** Число случайных городов или −1, если текст не подходит */
    public static int cities(String text) {
        String t = text == null ? "" : text.strip();
        if (!t.matches("\\d{1,3}")) {
            return -1;
        }
        int n = Integer.parseInt(t);
        return n >= AppController.MIN_CITIES && n <= AppController.MAX_CITIES ? n : -1;
    }
}
