package aco.controller;

/** Слайдер скорости ↔ перемещений в секунду; шкала логарифмическая, медленным скоростям — половина слайдера */
public final class SpeedScale {

    public static final double SLIDER_MIN = 0;
    public static final double SLIDER_MAX = 100;

    private static final double LOG_MIN = Math.log(AppController.MIN_SPEED);
    private static final double LOG_MAX = Math.log(AppController.MAX_SPEED);

    private SpeedScale() {
    }

    public static int toSpeed(double slider) {
        double t = (slider - SLIDER_MIN) / (SLIDER_MAX - SLIDER_MIN);
        long speed = Math.round(Math.exp(LOG_MIN + t * (LOG_MAX - LOG_MIN)));
        // за краями exp даёт 0 или бесконечность, NaN округляется в 0 — всё прижимается к диапазону
        return (int) Math.clamp(speed, AppController.MIN_SPEED, AppController.MAX_SPEED);
    }

    public static double toSlider(int speed) {
        int clamped = Math.clamp(speed, AppController.MIN_SPEED, AppController.MAX_SPEED);
        return SLIDER_MIN + (Math.log(clamped) - LOG_MIN) / (LOG_MAX - LOG_MIN) * (SLIDER_MAX - SLIDER_MIN);
    }
}
