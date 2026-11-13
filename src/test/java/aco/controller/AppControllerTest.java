package aco.controller;

import aco.model.Node;
import aco.model.Settings;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppControllerTest {

    private static final long SECOND = 1_000_000_000L;

    private final AppController c = new AppController(new Random(1));

    @Test
    void startsWithHexagonFromPdf() {
        assertEquals(Scenario.HEXAGON, c.scenario());
        assertEquals(6, c.graph().nodes().size());
        assertEquals(12, c.settings().ants());
        assertEquals(0, c.colony().cycle());
        assertFalse(c.running());
    }

    @Test
    void twoAntsScenarioUsesPdfParameters() {
        c.select(Scenario.TWO_ANTS);
        assertEquals(Settings.pdf(), c.settings());
        assertEquals(2, c.graph().nodes().size());
        assertEquals(2, c.colony().ants().size());
    }

    @Test
    void randomScenarioHasTwoAntsPerCity() {
        c.select(Scenario.RANDOM);
        int n = c.graph().nodes().size();
        assertEquals(AppController.DEFAULT_CITIES, n);
        assertEquals(2 * n, c.settings().ants());
        c.newCities(9);
        assertEquals(9, c.graph().nodes().size());
        assertEquals(18, c.settings().ants());
    }

    @Test
    void moveStepAndCycleStep() {
        c.moveStep();
        assertEquals(1, c.colony().ants().getFirst().moves());
        c.cycleStep();
        assertEquals(1, c.colony().cycle());
        assertTrue(c.colony().ants().stream().allMatch(a -> a.moves() == 0));
        c.cycleStep();
        assertEquals(2, c.colony().cycle());
    }

    @Test
    void runToEndFinishesAllCycles() {
        c.runToEnd();
        assertTrue(c.colony().done());
        assertEquals(c.settings().cycles(), c.colony().cycle());
        assertFalse(c.running());
    }

    @Test
    void timerMovesBySpeedAndKeepsFraction() {
        c.setSpeed(10);
        c.toggleRun();
        assertTrue(c.running());
        c.tick(SECOND / 4);
        // 10 перемещений в секунду: за четверть секунды 2 и половина третьего
        assertEquals(2, c.colony().ants().getFirst().moves());
        assertEquals(0.5, c.progress(), 1e-9);
        c.toggleRun();
        c.tick(SECOND);
        assertEquals(2, c.colony().ants().getFirst().moves());
        assertEquals(1.0, c.progress());
    }

    @Test
    void timerStopsAtLastCycle() {
        c.applySettings(new Settings(12, 2, 1, 0.1, 10, 0.1, 5, 2));
        c.setSpeed(AppController.MAX_SPEED);
        c.toggleRun();
        for (int i = 0; i < 100 && c.running(); i++) {
            c.tick(SECOND);
        }
        assertFalse(c.running());
        assertEquals(2, c.colony().cycle());
    }

    @Test
    void oneTickIsCappedToKeepWindowAlive() {
        c.setSpeed(AppController.MAX_SPEED);
        c.toggleRun();
        c.tick(3600 * SECOND);
        int moves = c.colony().cycle() * 6 + c.colony().ants().getFirst().moves();
        assertTrue(moves <= AppController.MAX_MOVES_PER_TICK, "ходов " + moves);
    }

    @Test
    void speedIsClamped() {
        c.setSpeed(0);
        assertEquals(AppController.MIN_SPEED, c.speed());
        c.setSpeed(1_000_000);
        assertEquals(AppController.MAX_SPEED, c.speed());
    }

    @Test
    void applySettingsRestartsColony() {
        c.cycleStep();
        Settings s = new Settings(6, 1, 2, 0.3, 5, 0.2, 0, 50);
        c.applySettings(s);
        assertEquals(s, c.settings());
        assertEquals(0, c.colony().cycle());
        assertEquals(6, c.colony().ants().size());
        assertFalse(c.running());
    }

    @Test
    void restartKeepsGraphAndSettings() {
        var graph = c.graph();
        c.cycleStep();
        c.restart();
        assertEquals(0, c.colony().cycle());
        assertEquals(graph, c.graph());
    }

    @Test
    void clickOnEmptyPlaceAddsCityAndSwitchesToCustom() {
        c.clickAt(1, 1);
        assertEquals(Scenario.CUSTOM, c.scenario());
        assertEquals(7, c.graph().nodes().size());
        assertEquals(21, c.graph().paths().size());
        Node added = c.graph().nodes().getLast();
        assertEquals(1, added.x(), 1e-12);
        assertEquals("7", added.label());
        assertEquals(14, c.settings().ants());
    }

    @Test
    void clickOnCityRemovesIt() {
        Node first = c.graph().nodes().getFirst();
        c.clickAt(first.x() + 1, first.y());
        assertEquals(5, c.graph().nodes().size());
        // номера подряд с 1
        assertEquals("1", c.graph().nodes().getFirst().label());
        assertEquals("5", c.graph().nodes().getLast().label());
    }

    @Test
    void tooFewCitiesMeansNoColony() {
        c.select(Scenario.CUSTOM);
        assertEquals(0, c.cities().size());
        assertNull(c.colony());
        assertNull(c.graph());
        c.clickAt(10, 10);
        c.clickAt(20, 10);
        assertNull(c.colony());
        c.moveStep();
        c.toggleRun();
        assertFalse(c.running());
        c.clickAt(20, 30);
        assertNotNull(c.colony());
        assertEquals(3, c.graph().nodes().size());
    }

    @Test
    void cityLimit() {
        c.select(Scenario.CUSTOM);
        for (int i = 0; i < AppController.MAX_CITIES + 3; i++) {
            c.clickAt(4 + (i % 10) * 9, 5 + (i / 10) * 20);
        }
        assertEquals(AppController.MAX_CITIES, c.cities().size());
    }

    @Test
    void optimumByBruteForceOnlyForSmallGraphs() {
        assertEquals(6 * 28, c.optimum(), 1e-9);
        c.select(Scenario.RANDOM);
        c.newCities(15);
        assertNull(c.optimum());
    }

    @Test
    void editingStopsRun() {
        c.toggleRun();
        c.clickAt(1, 1);
        assertFalse(c.running());
        c.toggleRun();
        c.select(Scenario.TWO_ANTS);
        assertFalse(c.running());
    }
}
