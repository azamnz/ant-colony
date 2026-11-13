package aco.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Параметры алгоритма; проверяются все сразу.
 *
 * @param ants   число муравьёв
 * @param alpha  α — вес феромона
 * @param beta   β — вес расстояния
 * @param rho    ρ — доля нового феромона и доля испарения, 0 &lt; ρ &lt; 1
 * @param q      Q — феромон муравья на весь маршрут, Δτ = Q / L
 * @param tau0   τ₀ — начальный феромон на путях
 * @param elite  e — сколько элитных муравьёв усиливают лучший маршрут; 0 — как в задании
 * @param cycles M — сколько циклов выполнить
 */
public record Settings(int ants, double alpha, double beta, double rho, double q, double tau0, double elite, int cycles) {

    public static final int MAX_ANTS = 500;
    public static final double MAX_POWER = 10;
    public static final double MAX_AMOUNT = 1000;
    public static final int MAX_CYCLES = 100_000;
    public static final double MAX_ELITE = 100;

    public Settings {
        List<InvalidSettingsException.Problem> problems = new ArrayList<>();
        if (ants < 1 || ants > MAX_ANTS) {
            problems.add(new InvalidSettingsException.Problem("ants", "муравьёв должно быть от 1 до " + MAX_ANTS));
        }
        if (!(alpha >= 0 && alpha <= MAX_POWER)) {
            problems.add(new InvalidSettingsException.Problem("alpha", "α должно быть от 0 до 10"));
        }
        if (!(beta >= 0 && beta <= MAX_POWER)) {
            problems.add(new InvalidSettingsException.Problem("beta", "β должно быть от 0 до 10"));
        }
        if (!(rho > 0 && rho < 1)) {
            problems.add(new InvalidSettingsException.Problem("rho", "ρ должно быть больше 0 и меньше 1"));
        }
        if (!(q > 0 && q <= MAX_AMOUNT)) {
            problems.add(new InvalidSettingsException.Problem("q", "Q должно быть больше 0 и не больше 1000"));
        }
        if (!(tau0 > 0 && tau0 <= MAX_AMOUNT)) {
            problems.add(new InvalidSettingsException.Problem("tau0", "τ₀ должно быть больше 0 и не больше 1000"));
        }
        if (!(elite >= 0 && elite <= MAX_ELITE)) {
            problems.add(new InvalidSettingsException.Problem("elite", "e должно быть от 0 до 100"));
        }
        if (cycles < 1 || cycles > MAX_CYCLES) {
            problems.add(new InvalidSettingsException.Problem("cycles", "циклов должно быть от 1 до " + MAX_CYCLES));
        }
        if (!problems.isEmpty()) {
            throw new InvalidSettingsException(problems);
        }
    }

    /** Подобраны замером: вся колония сходится к кратчайшему маршруту на 8–10 случайных городах */
    public static Settings defaults() {
        return new Settings(16, 2, 1, 0.1, 10, 0.1, 5, 200);
    }

    /** Пример из задания: α 3, β 1, ρ 0.6, Q 10, τ₀ 0.1, два муравья, без элиты */
    public static Settings pdf() {
        return new Settings(2, 3, 1, 0.6, 10, 0.1, 0, 100);
    }

    public Settings withAnts(int n) {
        return new Settings(n, alpha, beta, rho, q, tau0, elite, cycles);
    }

    public Settings withElite(double e) {
        return new Settings(ants, alpha, beta, rho, q, tau0, e, cycles);
    }
}
