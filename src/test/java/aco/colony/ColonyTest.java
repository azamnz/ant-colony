package aco.colony;

import aco.model.Graph;
import aco.model.Node;
import aco.model.Path;
import aco.model.Scenarios;
import aco.model.Settings;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColonyTest {

    private static double side(Graph hex) {
        return hex.pathsBetween(hex.nodes().get(0), hex.nodes().get(1)).getFirst().length();
    }

    @Test
    void pdfTwoAntsExample() {
        // ρ 0.6, α 3, β 1, Q 10; A0 сходил по пути 20 туда и обратно, A1 — по пути 10
        Graph g = Scenarios.twoAnts();
        Settings s = Settings.pdf();
        Colony colony = new Colony(g, s, 1);
        Path upper = g.paths().get(0);
        Path lower = g.paths().get(1);
        Ant a0 = colony.ants().get(0);
        Ant a1 = colony.ants().get(1);
        a0.move(upper);
        a0.move(upper);
        a1.move(lower);
        a1.move(lower);
        List<Tour> tours = List.of(a0.tour(), a1.tour());
        // за два прохода по 10/40 — то же, что Q/L = 10/20 = 0.5 из задания
        Colony.deposit(g.paths(), tours, null, s);
        assertEquals(0.4, upper.pheromone(), 1e-12);
        assertEquals(0.7, lower.pheromone(), 1e-12);
        Colony.evaporate(g.paths(), s);
        assertEquals(0.16, upper.pheromone(), 1e-12);
        assertEquals(0.28, lower.pheromone(), 1e-12);
    }

    @Test
    void antsStartSpreadEvenlyOrFromNest() {
        Graph hex = Scenarios.hexagon();
        Colony c = new Colony(hex, Settings.defaults().withAnts(12), 1);
        Map<Node, Long> perNode = c.ants().stream().collect(Collectors.groupingBy(Ant::home, Collectors.counting()));
        assertEquals(6, perNode.size());
        assertTrue(perNode.values().stream().allMatch(n -> n == 2));
        Graph two = Scenarios.twoAnts();
        Colony fromNest = new Colony(two, Settings.defaults().withAnts(5), 1);
        assertTrue(fromNest.ants().stream().allMatch(a -> a.home() == two.nest()));
    }

    @Test
    void newColonyResetsPheromoneToTau0() {
        Graph hex = Scenarios.hexagon();
        hex.paths().forEach(p -> p.reset(7));
        new Colony(hex, Settings.defaults(), 1);
        assertTrue(hex.paths().stream().allMatch(p -> p.pheromone() == 0.1));
    }

    @Test
    void cycleEndsWhenEveryAntIsBackHome() {
        Graph hex = Scenarios.hexagon();
        Colony c = new Colony(hex, Settings.defaults().withAnts(12), 1);
        for (int i = 0; i < 5; i++) {
            assertFalse(c.moveAll());
        }
        assertEquals(0, c.cycle());
        assertNull(c.best());
        assertTrue(c.moveAll());
        assertEquals(1, c.cycle());
        assertEquals(1, c.history().size());
        assertTrue(c.ants().stream().allMatch(a -> a.current() == a.home() && a.moves() == 0));
        // феромон обновлён: где-то больше начального
        assertTrue(hex.paths().stream().anyMatch(p -> p.pheromone() != 0.1));
    }

    @Test
    void statsOfCycleDescribeTours() {
        Colony c = new Colony(Scenarios.hexagon(), Settings.defaults().withAnts(12), 4);
        c.runCycle();
        CycleStats st = c.history().getFirst();
        assertEquals(1, st.cycle());
        assertTrue(st.best() <= st.average());
        assertEquals(st.best(), st.bestSoFar());
        assertEquals(st.best(), c.best().length(), 1e-12);
        assertTrue(st.sameRoute() > 0 && st.sameRoute() <= 1);
        assertTrue(st.usedPaths() >= 6 && st.usedPaths() <= 15);
    }

    @Test
    void firstCycleWandersOverManyRoads() {
        // пункт 4 задания: сначала муравьи ходят по всем дорогам
        for (long seed = 1; seed <= 20; seed++) {
            Colony c = new Colony(Scenarios.hexagon(), Settings.defaults().withAnts(12), seed);
            c.runCycle();
            assertTrue(c.history().getFirst().usedPaths() >= 12, "seed " + seed);
            assertTrue(c.history().getFirst().sameRoute() < 0.5, "seed " + seed);
        }
    }

    @Test
    void hexagonConvergesToPerimeterForEveryAnt() {
        // пункт 5 задания: в конце все муравьи идут по кратчайшему маршруту
        for (long seed = 1; seed <= 20; seed++) {
            Graph hex = Scenarios.hexagon();
            Colony c = new Colony(hex, Settings.defaults().withAnts(12), seed);
            c.run();
            CycleStats last = c.history().getLast();
            assertEquals(6 * side(hex), last.best(), 1e-9, "seed " + seed);
            assertEquals(1.0, last.sameRoute(), "seed " + seed);
            assertEquals(6, last.usedPaths(), "seed " + seed);
            assertEquals(last.best(), last.average(), 1e-9);
        }
    }

    @Test
    void pheromoneStaysOnlyOnBestRoute() {
        Graph hex = Scenarios.hexagon();
        Colony c = new Colony(hex, Settings.defaults().withAnts(12), 2);
        c.run();
        double sideLength = side(hex);
        double minOnRim = hex.paths().stream().filter(p -> Math.abs(p.length() - sideLength) < 1e-9)
                .mapToDouble(Path::pheromone).min().orElseThrow();
        double maxInside = hex.paths().stream().filter(p -> p.length() > sideLength + 1e-9)
                .mapToDouble(Path::pheromone).max().orElseThrow();
        assertTrue(minOnRim > 1000 * maxInside, minOnRim + " vs " + maxInside);
    }

    @Test
    void twoAntsFindShortPath() {
        int found = 0;
        for (long seed = 1; seed <= 50; seed++) {
            Graph g = Scenarios.twoAnts();
            Colony c = new Colony(g, Settings.pdf(), seed);
            c.run();
            if (c.history().getLast().average() == 20) {
                found++;
            }
        }
        assertTrue(found >= 45, "нашли короткий путь в " + found + " из 50");
    }

    @Test
    void randomCitiesAllAntsOnExactOptimum() {
        // замер: 29 из 30 на 8 городах, 27 из 30 на 10
        int exact = 0;
        for (long seed = 1; seed <= 20; seed++) {
            Graph g = Scenarios.random(8, seed);
            Colony c = new Colony(g, Settings.defaults(), seed);
            c.run();
            CycleStats last = c.history().getLast();
            if (last.sameRoute() == 1 && Math.abs(last.best() - BruteForce.shortest(g)) < 1e-9) {
                exact++;
            }
        }
        assertTrue(exact >= 18, "вся колония на оптимуме в " + exact + " из 20");
    }

    @Test
    void eliteAddsPheromoneOnlyOnBestRoute() {
        Graph g = Scenarios.twoAnts();
        Settings s = Settings.pdf().withElite(2);
        Colony colony = new Colony(g, s, 1);
        Path upper = g.paths().get(0);
        Path lower = g.paths().get(1);
        Ant a0 = colony.ants().get(0);
        a0.move(lower);
        a0.move(lower);
        Tour best = a0.tour();
        // муравьи ничего не прошли, элита: 2 прохода по 2·10/20 = 2, ρ·2 = 1.2
        Colony.deposit(g.paths(), List.of(), best, s);
        assertEquals(0.1 + 0.6 * 2, lower.pheromone(), 1e-12);
        assertEquals(0.1, upper.pheromone(), 1e-12);
        // e = 0 — элиты нет
        Colony.deposit(g.paths(), List.of(), best, Settings.pdf());
        assertEquals(0.1 + 0.6 * 2, lower.pheromone(), 1e-12);
    }

    @Test
    void runStopsAfterMCycles() {
        Colony c = new Colony(Scenarios.hexagon(), new Settings(6, 3, 1, 0.6, 10, 0.1, 0, 5), 1);
        c.run();
        assertEquals(5, c.cycle());
        assertTrue(c.done());
        assertFalse(c.moveAll());
        assertEquals(5, c.cycle());
        assertTrue(c.ants().stream().allMatch(a -> a.moves() == 0));
    }

    @Test
    void sameSeedSameHistory() {
        Function<Long, List<CycleStats>> run = seed -> {
            Colony c = new Colony(Scenarios.random(7, 3), Settings.defaults().withAnts(10), seed);
            c.run();
            return c.history();
        };
        assertEquals(run.apply(9L), run.apply(9L));
        assertFalse(run.apply(9L).equals(run.apply(10L)));
    }

    @Test
    void bestSoFarNeverGrows() {
        Colony c = new Colony(Scenarios.random(9, 4), Settings.defaults().withAnts(9), 4);
        c.run();
        double prev = Double.POSITIVE_INFINITY;
        for (CycleStats st : c.history()) {
            assertTrue(st.bestSoFar() <= prev);
            assertTrue(st.bestSoFar() <= st.best());
            prev = st.bestSoFar();
        }
        assertEquals(prev, c.best().length(), 1e-12);
    }
}
