package aco.colony;

import aco.model.Graph;
import aco.model.Path;

/** Точный кратчайший маршрут полным перебором — эталон для проверки муравьёв */
public final class BruteForce {

    /** 10 узлов — 9! = 362 880 перестановок, доли секунды */
    public static final int MAX_NODES = 10;

    private BruteForce() {
    }

    public static double shortest(Graph g) {
        int n = g.nodes().size();
        if (n > MAX_NODES) {
            throw new IllegalArgumentException("перебор только до " + MAX_NODES + " узлов, а их " + n);
        }
        double[][] d = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    d[i][j] = g.pathsBetween(g.nodes().get(i), g.nodes().get(j)).stream()
                            .mapToDouble(Path::length).min().orElseThrow();
                }
            }
        }
        boolean[] used = new boolean[n];
        used[0] = true;
        return search(d, used, 0, 1, 0, Double.POSITIVE_INFINITY);
    }

    private static double search(double[][] d, boolean[] used, int at, int count, double length, double best) {
        if (length >= best) {
            return best;
        }
        if (count == d.length) {
            return Math.min(best, length + d[at][0]);
        }
        for (int next = 1; next < d.length; next++) {
            if (!used[next]) {
                used[next] = true;
                best = search(d, used, next, count + 1, length + d[at][next], best);
                used[next] = false;
            }
        }
        return best;
    }
}
