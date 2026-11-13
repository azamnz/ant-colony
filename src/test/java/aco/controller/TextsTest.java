package aco.controller;

import aco.colony.CycleStats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextsTest {

    @Test
    void cycleLine() {
        CycleStats st = new CycleStats(12, 241.34, 260.81, 238.0, 0.4375, 18);
        assertEquals("Цикл 12: лучший 241.3, средний 260.8, на одном маршруте 44 %, дорог 18 из 45",
                Texts.cycleLine(st, 45));
    }

    @Test
    void numberWithoutNoise() {
        assertEquals("168", Texts.length(167.99999999999994));
        assertEquals("20", Texts.length(20));
        assertEquals("241.3", Texts.length(241.34));
    }

    @Test
    void percent() {
        assertEquals("100 %", Texts.percent(1));
        assertEquals("8 %", Texts.percent(1 / 12.0));
    }

    @Test
    void plain() {
        assertEquals("0.6", Texts.plain(0.6));
        assertEquals("3", Texts.plain(3.0));
        assertEquals("0.0001", Texts.plain(0.0001));
        assertEquals("100000", Texts.plain(100000));
    }
}
