package aco.controller;

import aco.colony.BruteForce;
import aco.colony.Colony;
import aco.model.Graph;
import aco.model.Node;
import aco.model.Scenarios;
import aco.model.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Логика окна без JavaFX: выбор графа, правка городов, параметры, шаги и таймер */
public final class AppController {

    public static final int MIN_SPEED = 1;
    public static final int MAX_SPEED = 300;
    /** Больше перемещений за кадр не делаем: окно не должно зависать после долгой паузы */
    public static final int MAX_MOVES_PER_TICK = 600;
    public static final int MIN_CITIES = 3;
    public static final int MAX_CITIES = 20;
    public static final int DEFAULT_CITIES = 8;
    /** Клик ближе этого к городу — убрать город, в единицах поля */
    static final double PICK_RADIUS = 3;

    private final Random seeds;
    private Scenario scenario;
    private List<Node> cities = List.of();
    private Graph graph;
    private Settings settings;
    private Colony colony;
    private Double optimum;
    private boolean running;
    private int speed = 5;
    private double backlog;
    private double progress = 1;

    /** seeds — источник зёрен для колоний и случайных городов */
    public AppController(Random seeds) {
        this.seeds = seeds;
        select(Scenario.HEXAGON);
    }

    public void select(Scenario s) {
        scenario = s;
        switch (s) {
            case HEXAGON -> setGraph(Scenarios.hexagon(), null);
            case TWO_ANTS -> setGraph(Scenarios.twoAnts(), Settings.pdf());
            case RANDOM -> setGraph(Scenarios.random(DEFAULT_CITIES, seeds.nextLong()), null);
            case CUSTOM -> {
                cities = List.of();
                graph = null;
                settings = Settings.defaults();
                rebuild();
            }
        }
    }

    /** Новые случайные города, параметры кроме числа муравьёв сохраняются */
    public void newCities(int count) {
        scenario = Scenario.RANDOM;
        Graph g = Scenarios.random(count, seeds.nextLong());
        setGraph(g, settings.withAnts(2 * count));
    }

    /** Клик по полю: рядом с городом — убрать его, иначе поставить новый; граф становится «своим» */
    public void clickAt(double x, double y) {
        List<Node> next = new ArrayList<>(cities);
        Node near = null;
        for (Node n : cities) {
            if (Math.hypot(n.x() - x, n.y() - y) <= PICK_RADIUS) {
                near = n;
            }
        }
        if (near != null) {
            next.remove(near);
        } else if (next.size() < MAX_CITIES) {
            next.add(new Node(0, "", x, y));
        } else {
            return;
        }
        List<Node> renumbered = new ArrayList<>();
        for (Node n : next) {
            renumbered.add(new Node(renumbered.size(), String.valueOf(renumbered.size() + 1), n.x(), n.y()));
        }
        scenario = Scenario.CUSTOM;
        cities = List.copyOf(renumbered);
        graph = cities.size() >= MIN_CITIES ? Graph.complete(cities) : null;
        settings = settings.withAnts(Math.max(1, Math.min(Settings.MAX_ANTS, 2 * cities.size())));
        rebuild();
    }

    private void setGraph(Graph g, Settings s) {
        graph = g;
        cities = g.nodes();
        settings = s != null ? s : Settings.defaults().withAnts(2 * g.nodes().size());
        rebuild();
    }

    public void applySettings(Settings s) {
        settings = s;
        rebuild();
    }

    /** Новая колония на том же графе: феромон с нуля, новое зерно */
    public void restart() {
        rebuild();
    }

    private void rebuild() {
        stop();
        colony = graph == null ? null : new Colony(graph, settings, seeds.nextLong());
        optimum = graph != null && graph.nodes().size() <= BruteForce.MAX_NODES ? BruteForce.shortest(graph) : null;
    }

    private void stop() {
        running = false;
        backlog = 0;
        progress = 1;
    }

    /** Каждый муравей проходит один путь */
    public void moveStep() {
        if (colony != null) {
            stop();
            colony.moveAll();
        }
    }

    /** Довести цикл до конца */
    public void cycleStep() {
        if (colony != null) {
            stop();
            colony.runCycle();
        }
    }

    /** Все оставшиеся циклы сразу */
    public void runToEnd() {
        if (colony != null) {
            stop();
            colony.run();
        }
    }

    public void toggleRun() {
        if (running || colony == null || colony.done()) {
            stop();
        } else {
            running = true;
            backlog = 0;
        }
    }

    /** Таймер окна: elapsedNanos с прошлого кадра; перемещений — по скорости */
    public void tick(long elapsedNanos) {
        if (!running) {
            return;
        }
        backlog += elapsedNanos / 1e9 * speed;
        int moves = 0;
        while (backlog >= 1 && moves < MAX_MOVES_PER_TICK) {
            colony.moveAll();
            backlog -= 1;
            moves++;
            if (colony.done()) {
                stop();
                return;
            }
        }
        // долгая пауза: лишнее отбрасываем, чтобы не догонять
        backlog -= Math.floor(backlog);
        progress = backlog;
    }

    public void setSpeed(int movesPerSecond) {
        speed = Math.clamp(movesPerSecond, MIN_SPEED, MAX_SPEED);
    }

    public int speed() {
        return speed;
    }

    /** Доля пройденного последнего перемещения, 0..1: для плавного движения муравьёв */
    public double progress() {
        return progress;
    }

    public boolean running() {
        return running;
    }

    public Scenario scenario() {
        return scenario;
    }

    public List<Node> cities() {
        return cities;
    }

    /** null, пока своих городов меньше трёх */
    public Graph graph() {
        return graph;
    }

    public Settings settings() {
        return settings;
    }

    /** null, пока нет графа */
    public Colony colony() {
        return colony;
    }

    /** Точный кратчайший маршрут перебором; null, если городов больше 10 */
    public Double optimum() {
        return optimum;
    }
}
