package aco.colony;

import aco.model.Graph;
import aco.model.Node;
import aco.model.Path;
import aco.model.Scenarios;
import aco.model.Settings;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntTest {

    private final Node a = new Node(0, "1", 0, 0);
    private final Node b = new Node(1, "2", 3, 4);
    private final Node c = new Node(2, "3", 3, 0);
    private final Graph g = Graph.complete(List.of(a, b, c));
    private final Path ab = g.pathsBetween(a, b).getFirst();
    private final Path ac = g.pathsBetween(a, c).getFirst();
    private final Path bc = g.pathsBetween(b, c).getFirst();
    private final Settings s = Settings.pdf();

    /** Генератор, который всегда выдаёт одно число */
    private static Random fixed(double r) {
        return new Random() {
            @Override
            public double nextDouble() {
                return r;
            }
        };
    }

    private Ant ant() {
        g.paths().forEach(p -> p.reset(0.1));
        return new Ant(0, g, a);
    }

    @Test
    void equalPheromoneMeansShorterIsLikelierByEta() {
        // η = 1/d: 1/5 против 1/3 → 3/8 и 5/8
        Map<Path, Double> p = ant().probabilities(s);
        assertEquals(0.375, p.get(ab), 1e-12);
        assertEquals(0.625, p.get(ac), 1e-12);
        assertEquals(2, p.size());
    }

    @Test
    void pheromoneCountsWithPowerAlpha() {
        Ant ant = ant();
        ab.reset(0.2);
        // 0.2³/5 = 0.0016, 0.1³/3 = 0.000333…
        double wab = Math.pow(0.2, 3) / 5;
        double wac = Math.pow(0.1, 3) / 3;
        assertEquals(wab / (wab + wac), ant.probabilities(s).get(ab), 1e-12);
        // β = 2: расстояние в квадрате
        Settings beta2 = new Settings(3, 3, 2, 0.6, 10, 0.1, 0, 10);
        double w2ab = Math.pow(0.2, 3) / 25;
        double w2ac = Math.pow(0.1, 3) / 9;
        assertEquals(w2ab / (w2ab + w2ac), ant.probabilities(beta2).get(ab), 1e-12);
    }

    @Test
    void choiceIsRouletteByProbability() {
        assertSame(ab, ant().chooseNext(s, fixed(0.0)));
        assertSame(ab, ant().chooseNext(s, fixed(0.374)));
        assertSame(ac, ant().chooseNext(s, fixed(0.376)));
        assertSame(ac, ant().chooseNext(s, fixed(0.9999999)));
    }

    @Test
    void visitedNodesAreNotOfferedAndLastStepGoesHome() {
        Ant ant = ant();
        ant.move(ab);
        assertSame(b, ant.current());
        assertEquals(List.of(bc), List.copyOf(ant.probabilities(s).keySet()));
        ant.move(bc);
        assertEquals(List.of(ac), List.copyOf(ant.probabilities(s).keySet()));
        assertFalse(ant.finished());
        ant.move(ac);
        assertTrue(ant.finished());
        assertSame(a, ant.current());
        assertEquals(5 + 4 + 3, ant.tour().length(), 1e-12);
        assertEquals(List.of(ab, bc, ac), ant.tour().paths());
    }

    @Test
    void moveRejectsForeignOrVisitedPaths() {
        Ant ant = ant();
        assertThrows(IllegalArgumentException.class, () -> ant.move(bc));
        ant.move(ab);
        // назад в a нельзя, пока не обошёл c
        assertThrows(IllegalArgumentException.class, () -> ant.move(ab));
    }

    @Test
    void tourBeforeFinishIsAnError() {
        Ant ant = ant();
        ant.move(ab);
        assertThrows(IllegalStateException.class, ant::tour);
    }

    @Test
    void finishedAntStandsStill() {
        Ant ant = ant();
        while (!ant.finished()) {
            ant.step(s, new Random(1));
        }
        int moves = ant.moves();
        ant.step(s, new Random(1));
        assertEquals(moves, ant.moves());
        assertEquals(3, moves);
    }

    @Test
    void walkVisitsEveryNodeOnceAndReturnsHome() {
        Graph hex = Scenarios.hexagon();
        hex.paths().forEach(p -> p.reset(0.1));
        Random random = new Random(3);
        for (Node home : hex.nodes()) {
            Ant ant = new Ant(1, hex, home);
            while (!ant.finished()) {
                ant.step(s, random);
            }
            List<Node> trail = ant.trail();
            assertEquals(7, trail.size());
            assertSame(home, trail.getFirst());
            assertSame(home, trail.getLast());
            assertEquals(6, new HashSet<>(trail.subList(0, 6)).size());
        }
    }

    @Test
    void vanishedPheromoneFallsBackToEqualChance() {
        // 1e-200 в кубе — ноль в double: без запасного хода вероятности были бы NaN
        Ant ant = ant();
        ab.reset(1e-200);
        ac.reset(1e-200);
        Map<Path, Double> p = ant.probabilities(s);
        assertEquals(0.5, p.get(ab), 1e-12);
        assertEquals(0.5, p.get(ac), 1e-12);
        assertSame(ac, ant.chooseNext(s, fixed(0.75)));
    }

    @Test
    void resetReturnsAntHome() {
        Ant ant = ant();
        ant.move(ab);
        ant.reset();
        assertSame(a, ant.current());
        assertEquals(0, ant.moves());
        assertEquals(List.of(a), ant.trail());
    }

    @Test
    void parallelPathsAreBothOfferedBackHome() {
        Graph two = Scenarios.twoAnts();
        two.paths().forEach(p -> p.reset(0.1));
        Ant ant = new Ant(0, two, two.nest());
        assertEquals(2, ant.probabilities(s).size());
        ant.move(two.paths().getFirst());
        assertEquals(2, ant.probabilities(s).size());
        ant.move(two.paths().getFirst());
        assertTrue(ant.finished());
        assertEquals(40, ant.tour().length(), 1e-12);
    }
}
