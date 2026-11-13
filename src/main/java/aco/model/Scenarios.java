package aco.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Готовые графы: примеры из задания, случайные города, свои города */
public final class Scenarios {

    /** Поле, в котором стоят города */
    public static final double WIDTH = 100;
    public static final double HEIGHT = 70;
    /** Отступ случайных городов от края поля */
    private static final double MARGIN = 6;

    private Scenarios() {
    }

    /** Шестиугольник из задания: 6 городов, путь между каждой парой; 1 сверху слева, дальше по часовой */
    public static Graph hexagon() {
        double cx = WIDTH / 2;
        double cy = HEIGHT / 2;
        double r = 28;
        List<Node> nodes = new ArrayList<>();
        // углы по часовой стрелке от 1 (120°); y на экране растёт вниз
        int[] degrees = {120, 60, 0, -60, -120, 180};
        for (int i = 0; i < 6; i++) {
            double t = Math.toRadians(degrees[i]);
            nodes.add(new Node(i, String.valueOf(i + 1), cx + r * Math.cos(t), cy - r * Math.sin(t)));
        }
        return Graph.complete(nodes);
    }

    /** «Два муравья» из задания: гнездо V0, еда V1, путь сверху длиной 20, снизу 10 */
    public static Graph twoAnts() {
        Node v0 = new Node(0, "V0", 20, HEIGHT / 2);
        Node v1 = new Node(1, "V1", 80, HEIGHT / 2);
        return new Graph(List.of(v0, v1), List.of(new Path(0, v0, v1, 20), new Path(1, v0, v1, 10)), v0);
    }

    /** n случайных городов; одинаковый seed — одинаковые города */
    public static Graph random(int n, long seed) {
        Random random = new Random(seed);
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            nodes.add(new Node(i, String.valueOf(i + 1), MARGIN + random.nextDouble() * (WIDTH - 2 * MARGIN),
                    MARGIN + random.nextDouble() * (HEIGHT - 2 * MARGIN)));
        }
        return Graph.complete(nodes);
    }
}
