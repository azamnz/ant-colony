package aco.controller;

import aco.colony.CycleStats;

import java.math.BigDecimal;

/** Тексты окна: строки журнала и числа без лишних знаков */
public final class Texts {

    private Texts() {
    }

    /** Длина с одним знаком, без «.0» и шума округления */
    public static String length(double v) {
        double r = Math.round(v * 10) / 10.0;
        return r == Math.rint(r) ? String.valueOf((long) r) : String.valueOf(r);
    }

    public static String percent(double share) {
        return Math.round(share * 100) + " %";
    }

    /** Число как ввели: 0.6, 3, 0.0001 */
    public static String plain(double v) {
        return BigDecimal.valueOf(v).stripTrailingZeros().toPlainString();
    }

    public static String cycleLine(CycleStats st, int totalPaths) {
        return "Цикл " + st.cycle() + ": лучший " + length(st.best()) + ", средний " + length(st.average())
                + ", на одном маршруте " + percent(st.sameRoute()) + ", дорог " + st.usedPaths() + " из " + totalPaths;
    }
}
