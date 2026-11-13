package aco.controller;

/** Какой граф на экране */
public enum Scenario {
    HEXAGON("Шестиугольник из задания"),
    TWO_ANTS("Два муравья из задания"),
    RANDOM("Случайные города"),
    CUSTOM("Свои города");

    private final String title;

    Scenario(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }

    @Override
    public String toString() {
        return title;
    }
}
