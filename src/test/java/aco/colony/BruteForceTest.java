package aco.colony;

import aco.model.Graph;
import aco.model.Node;
import aco.model.Scenarios;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BruteForceTest {

    @Test
    void squareShortestIsPerimeterNotCross() {
        Graph square = Graph.complete(List.of(new Node(0, "1", 0, 0), new Node(1, "2", 0, 1),
                new Node(2, "3", 1, 0), new Node(3, "4", 1, 1)));
        assertEquals(4.0, BruteForce.shortest(square), 1e-12);
    }

    @Test
    void parallelPathsTakeShortestBothWays() {
        assertEquals(20.0, BruteForce.shortest(Scenarios.twoAnts()), 1e-12);
    }

    @Test
    void hexagonIsPerimeter() {
        Graph hex = Scenarios.hexagon();
        double side = hex.pathsBetween(hex.nodes().get(0), hex.nodes().get(1)).getFirst().length();
        assertEquals(6 * side, BruteForce.shortest(hex), 1e-9);
    }

    @Test
    void tooManyNodesRefused() {
        assertThrows(IllegalArgumentException.class, () -> BruteForce.shortest(Scenarios.random(BruteForce.MAX_NODES + 1, 1)));
    }
}
