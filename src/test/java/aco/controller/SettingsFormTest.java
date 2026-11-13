package aco.controller;

import aco.model.Settings;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsFormTest {

    private static Map<String, String> filled() {
        Map<String, String> m = new HashMap<>(SettingsForm.texts(Settings.pdf()));
        return m;
    }

    @Test
    void roundTripOfSettings() {
        FormResult r = SettingsForm.parse(SettingsForm.texts(Settings.defaults()));
        assertEquals(new FormResult.Ok(Settings.defaults()), r);
    }

    @Test
    void textsAreShortWithoutTrailingZeros() {
        Map<String, String> t = SettingsForm.texts(Settings.pdf());
        assertEquals("3", t.get("alpha"));
        assertEquals("0.6", t.get("rho"));
        assertEquals("2", t.get("ants"));
    }

    @Test
    void commaWorksAsDecimalPoint() {
        Map<String, String> m = filled();
        m.put("rho", " 0,25 ");
        FormResult.Ok ok = assertInstanceOf(FormResult.Ok.class, SettingsForm.parse(m));
        assertEquals(0.25, ok.settings().rho());
    }

    @Test
    void garbageIsReportedPerField() {
        Map<String, String> m = filled();
        m.put("ants", "2.5");
        m.put("alpha", "abc");
        m.put("cycles", "");
        FormResult.Errors e = assertInstanceOf(FormResult.Errors.class, SettingsForm.parse(m));
        assertEquals(3, e.errors().size());
        assertTrue(e.errors().get("ants").contains("целое"));
        assertTrue(e.errors().get("alpha").contains("число"));
        assertTrue(e.errors().get("cycles").contains("пусто"));
    }

    @Test
    void rangeErrorsComeFromSettings() {
        Map<String, String> m = filled();
        m.put("rho", "1");
        m.put("beta", "-2");
        FormResult.Errors e = assertInstanceOf(FormResult.Errors.class, SettingsForm.parse(m));
        assertEquals(2, e.errors().size());
        assertTrue(e.errors().get("rho").contains("ρ"));
        assertTrue(e.errors().get("beta").contains("β"));
    }

    @Test
    void notFiniteNumbersRejected() {
        Map<String, String> m = filled();
        m.put("q", "1e999");
        m.put("tau0", "NaN");
        FormResult.Errors e = assertInstanceOf(FormResult.Errors.class, SettingsForm.parse(m));
        assertEquals(2, e.errors().size());
    }

    @Test
    void hugeIntegerIsNotACrash() {
        Map<String, String> m = filled();
        m.put("ants", "99999999999999999999");
        FormResult.Errors e = assertInstanceOf(FormResult.Errors.class, SettingsForm.parse(m));
        assertTrue(e.errors().containsKey("ants"));
    }

    @Test
    void missingFieldIsEmpty() {
        Map<String, String> m = filled();
        m.remove("q");
        FormResult.Errors e = assertInstanceOf(FormResult.Errors.class, SettingsForm.parse(m));
        assertTrue(e.errors().get("q").contains("пусто"));
    }

    @Test
    void citiesCountParsedWithLimits() {
        assertEquals(8, SettingsForm.cities("8"));
        assertEquals(-1, SettingsForm.cities("2"));
        assertEquals(-1, SettingsForm.cities(String.valueOf(AppController.MAX_CITIES + 1)));
        assertEquals(-1, SettingsForm.cities("восемь"));
        assertEquals(12, SettingsForm.cities(" 12 "));
    }
}
