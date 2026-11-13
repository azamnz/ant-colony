package aco.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpeedScaleTest {

    @Test
    void edgesAndRoundTrip() {
        assertEquals(AppController.MIN_SPEED, SpeedScale.toSpeed(SpeedScale.SLIDER_MIN));
        assertEquals(AppController.MAX_SPEED, SpeedScale.toSpeed(SpeedScale.SLIDER_MAX));
        for (int speed : new int[]{1, 5, 20, 100, AppController.MAX_SPEED}) {
            assertEquals(speed, SpeedScale.toSpeed(SpeedScale.toSlider(speed)));
        }
    }

    @Test
    void outOfRangeIsClamped() {
        assertEquals(AppController.MIN_SPEED, SpeedScale.toSpeed(-1e9));
        assertEquals(AppController.MAX_SPEED, SpeedScale.toSpeed(1e9));
        assertEquals(AppController.MIN_SPEED, SpeedScale.toSpeed(Double.NaN));
        assertEquals(SpeedScale.SLIDER_MAX, SpeedScale.toSlider(1_000_000));
    }
}
