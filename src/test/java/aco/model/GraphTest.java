package aco.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphTest {

    private final Node a = new Node(0, "1", 0, 0);
    private final Node b = new Node(1, "2", 3, 4);
    private final Node c = new Node(2, "3", 3, 0);

    @Test
    void completeGraphConnectsEveryPairByDistance() {
        Graph g = Graph.complete(List.of(a, b, c));
        assertEquals(3, g.paths().size());
        assertEquals(5.0, g.pathsBetween(a, b).getFirst().length(), 1e-12);
        assertEquals(3.0, g.pathsBetween(a, c).getFirst().length(), 1e-12);
        assertEquals(2, g.pathsFrom(a).size());
        assertNull(g.nest());
    }

    @Test
    void pathKnowsItsOtherEnd() {
        Path p = new Path(0, a, b, 5);
        assertSame(b, p.other(a));
        assertSame(a, p.other(b));
        assertThrows(IllegalArgumentException.class, () -> p.other(c));
        assertTrue(p.touches(a));
    }

    @Test
    void pathRejectsLoopsAndBadLength() {
        assertThrows(IllegalArgumentException.class, () -> new Path(0, a, a, 1));
        assertThrows(IllegalArgumentException.class, () -> new Path(0, a, b, 0));
        assertThrows(IllegalArgumentException.class, () -> new Path(0, a, b, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new Path(0, a, b, Double.POSITIVE_INFINITY));
    }

    @Test
    void pheromoneDepositsAndEvaporates() {
        Path p = new Path(0, a, b, 5);
        p.reset(0.1);
        p.deposit(0.3);
        assertEquals(0.4, p.pheromone(), 1e-12);
        p.evaporate(0.6);
        assertEquals(0.16, p.pheromone(), 1e-12);
    }

    @Test
    void graphNeedsEveryPairConnected() {
        // нет пути между b и c — муравей мог бы застрять
        List<Path> paths = List.of(new Path(0, a, b, 5), new Path(1, a, c, 3));
        assertThrows(IllegalArgumentException.class, () -> new Graph(List.of(a, b, c), paths, null));
        assertThrows(IllegalArgumentException.class, () -> Graph.complete(List.of(a)));
    }

    @Test
    void graphRejectsForeignNodesAndDuplicateIds() {
        Node stranger = new Node(7, "?", 1, 1);
        assertThrows(IllegalArgumentException.class,
                () -> new Graph(List.of(a, b), List.of(new Path(0, a, stranger, 1), new Path(1, a, b, 1)), null));
        assertThrows(IllegalArgumentException.class,
                () -> Graph.complete(List.of(a, new Node(0, "x", 9, 9))));
        assertThrows(IllegalArgumentException.class,
                () -> new Graph(List.of(a, b), List.of(new Path(0, a, b, 1)), stranger));
    }

    @Test
    void twoAntsHasTwoParallelPathsAndNest() {
        Graph g = Scenarios.twoAnts();
        assertEquals(2, g.nodes().size());
        List<Path> between = g.pathsBetween(g.nodes().get(0), g.nodes().get(1));
        assertEquals(List.of(20.0, 10.0), between.stream().map(Path::length).toList());
        assertSame(g.nodes().getFirst(), g.nest());
    }

    @Test
    void hexagonIsCompleteWithSixNodesNumberedLikePdf() {
        Graph g = Scenarios.hexagon();
        assertEquals(6, g.nodes().size());
        assertEquals(15, g.paths().size());
        assertEquals(List.of("1", "2", "3", "4", "5", "6"), g.nodes().stream().map(Node::label).toList());
        // соседние по кругу узлы — стороны одинаковой длины
        double side = g.pathsBetween(g.nodes().get(0), g.nodes().get(1)).getFirst().length();
        for (int i = 0; i < 6; i++) {
            Node from = g.nodes().get(i);
            Node to = g.nodes().get((i + 1) % 6);
            assertEquals(side, g.pathsBetween(from, to).getFirst().length(), 1e-9);
        }
        // 1 сверху слева, 3 справа, 6 слева, как на рисунке
        Node n1 = g.nodes().get(0);
        Node n3 = g.nodes().get(2);
        Node n6 = g.nodes().get(5);
        assertTrue(n3.x() > n1.x() && n6.x() < n1.x());
        assertTrue(n1.y() < n3.y());
    }

    @Test
    void randomCitiesDoNotStickTogether() {
        for (long seed = 1; seed <= 200; seed++) {
            List<Node> nodes = Scenarios.random(20, seed).nodes();
            for (int i = 0; i < nodes.size(); i++) {
                for (int j = i + 1; j < nodes.size(); j++) {
                    assertTrue(nodes.get(i).distanceTo(nodes.get(j)) >= Scenarios.MIN_GAP, "seed " + seed);
                }
            }
        }
    }

    @Test
    void randomCitiesAreRepeatableBySeedAndInsideField() {
        Graph g1 = Scenarios.random(8, 5);
        Graph g2 = Scenarios.random(8, 5);
        assertEquals(g1.nodes(), g2.nodes());
        assertEquals(8, g1.nodes().size());
        for (Node n : g1.nodes()) {
            assertTrue(n.x() >= 0 && n.x() <= Scenarios.WIDTH && n.y() >= 0 && n.y() <= Scenarios.HEIGHT, n.toString());
        }
        assertTrue(!g1.nodes().equals(Scenarios.random(8, 6).nodes()));
    }
}
