package aco.colony;

import aco.model.Graph;
import aco.model.Node;
import aco.model.Path;
import aco.model.Settings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Муравей: обходит все узлы по одному разу и возвращается домой, путь выбирает по феромону и расстоянию */
public final class Ant {

    private final int id;
    private final Graph graph;
    private final Node home;
    private final Set<Node> visited = new HashSet<>();
    private final List<Path> paths = new ArrayList<>();
    private final List<Node> trail = new ArrayList<>();
    private Node current;

    public Ant(int id, Graph graph, Node home) {
        this.id = id;
        this.graph = graph;
        this.home = home;
        reset();
    }

    /** Новый цикл: снова дома, ничего не посещено */
    public void reset() {
        current = home;
        visited.clear();
        visited.add(home);
        paths.clear();
        trail.clear();
        trail.add(home);
    }

    /** Куда можно пойти: в непосещённые узлы, а когда все посещены — только домой */
    public List<Path> candidates() {
        if (finished()) {
            return List.of();
        }
        if (visited.size() < graph.nodes().size()) {
            return graph.pathsFrom(current).stream().filter(p -> !visited.contains(p.other(current))).toList();
        }
        return graph.pathsBetween(current, home);
    }

    /** P = τ^α·η^β / Σ τ^α·η^β по доступным путям, η = 1/d */
    public Map<Path, Double> probabilities(Settings s) {
        List<Path> options = candidates();
        Map<Path, Double> weights = new LinkedHashMap<>();
        double sum = 0;
        for (Path p : options) {
            double w = Math.pow(p.pheromone(), s.alpha()) * Math.pow(1 / p.length(), s.beta());
            weights.put(p, w);
            sum += w;
        }
        Map<Path, Double> result = new LinkedHashMap<>();
        // феромон мог уйти в ноль double: тогда все пути равновероятны
        boolean usable = sum > 0 && Double.isFinite(sum);
        for (Path p : options) {
            result.put(p, usable ? weights.get(p) / sum : 1.0 / options.size());
        }
        return result;
    }

    /** Выбор следующего пути: рулетка по вероятностям */
    public Path chooseNext(Settings s, Random random) {
        Map<Path, Double> p = probabilities(s);
        if (p.isEmpty()) {
            throw new IllegalStateException("муравей " + id + " уже вернулся домой");
        }
        double r = random.nextDouble();
        double sum = 0;
        Path last = null;
        for (Map.Entry<Path, Double> e : p.entrySet()) {
            sum += e.getValue();
            last = e.getKey();
            if (r < sum) {
                return last;
            }
        }
        // сумма вероятностей могла выйти чуть меньше 1 из-за округления
        return last;
    }

    /** Перемещение по пути в соседний узел */
    public void move(Path path) {
        if (!candidates().contains(path)) {
            throw new IllegalArgumentException("муравей " + id + " из " + current + " не может пойти по " + path);
        }
        current = path.other(current);
        visited.add(current);
        paths.add(path);
        trail.add(current);
    }

    /** Один шаг: выбрать путь и пройти его; вернувшийся домой стоит */
    public void step(Settings s, Random random) {
        if (!finished()) {
            move(chooseNext(s, random));
        }
    }

    public boolean finished() {
        return !paths.isEmpty() && current.equals(home) && visited.size() == graph.nodes().size();
    }

    public Tour tour() {
        if (!finished()) {
            throw new IllegalStateException("муравей " + id + " ещё не вернулся домой");
        }
        return new Tour(paths, paths.stream().mapToDouble(Path::length).sum());
    }

    public int id() {
        return id;
    }

    public Node home() {
        return home;
    }

    public Node current() {
        return current;
    }

    public int moves() {
        return paths.size();
    }

    /** Узлы по порядку, начиная с дома */
    public List<Node> trail() {
        return List.copyOf(trail);
    }
}
