package aco.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SettingsTest {

    @Test
    void pdfValuesAreFromTask() {
        Settings s = Settings.pdf();
        assertEquals(2, s.ants());
        assertEquals(0.0, s.elite());
        assertEquals(3.0, s.alpha());
        assertEquals(1.0, s.beta());
        assertEquals(0.6, s.rho());
        assertEquals(10.0, s.q());
        assertEquals(0.1, s.tau0());
    }

    @Test
    void allProblemsReportedAtOnce() {
        InvalidSettingsException e = assertThrows(InvalidSettingsException.class,
                () -> new Settings(0, -1, 11, 1, 0, -0.1, -1, 0));
        assertEquals(List.of("ants", "alpha", "beta", "rho", "q", "tau0", "elite", "cycles"),
                e.problems().stream().map(InvalidSettingsException.Problem::field).toList());
    }

    @Test
    void notANumberIsRejected() {
        assertThrows(InvalidSettingsException.class, () -> new Settings(5, Double.NaN, 1, 0.5, 10, 0.1, 0, 10));
        assertThrows(InvalidSettingsException.class, () -> new Settings(5, 1, 1, Double.NaN, 10, 0.1, 0, 10));
        assertThrows(InvalidSettingsException.class, () -> new Settings(5, 1, 1, 0.5, Double.POSITIVE_INFINITY, 0.1, 0, 10));
    }

    @Test
    void edgesOfRanges() {
        assertDoesNotThrow(() -> new Settings(1, 0, 0, 0.01, 0.01, 0.0001, 0, 1));
        assertDoesNotThrow(() -> new Settings(Settings.MAX_ANTS, 10, 10, 0.99, 1000, 1000, Settings.MAX_ELITE, Settings.MAX_CYCLES));
        assertThrows(InvalidSettingsException.class, () -> new Settings(Settings.MAX_ANTS + 1, 1, 1, 0.5, 10, 0.1, 0, 10));
        assertThrows(InvalidSettingsException.class, () -> new Settings(5, 1, 1, 0, 10, 0.1, 0, 10));
        assertThrows(InvalidSettingsException.class, () -> new Settings(5, 1, 1, 0.5, 10, 0.1, 0, Settings.MAX_CYCLES + 1));
    }

    @Test
    void defaultsUseEliteAnts() {
        assertEquals(5.0, Settings.defaults().elite());
        assertEquals(0.1, Settings.defaults().rho());
    }

    @Test
    void withEliteKeepsOtherValues() {
        Settings s = Settings.pdf().withElite(3);
        assertEquals(3.0, s.elite());
        assertEquals(Settings.pdf().rho(), s.rho());
        assertEquals(Settings.pdf().ants(), s.ants());
    }

    @Test
    void withAntsKeepsOtherValues() {
        Settings s = Settings.defaults().withAnts(2);
        assertEquals(2, s.ants());
        assertEquals(Settings.defaults().alpha(), s.alpha());
        assertEquals(Settings.defaults().cycles(), s.cycles());
    }
}
