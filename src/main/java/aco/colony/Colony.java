package aco.colony;

import aco.model.Graph;
import aco.model.Path;
import aco.model.Settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Муравьиная колония. Цикл: все муравьи обходят граф, затем феромон обновляется и испаряется.
 * Обновление как в примере задания: τ = τ + ρ·ΣΔτ, Δτ = Q/L; испарение τ = τ·(1 − ρ).
 * При e &gt; 0 лучший найденный маршрут получает ещё e·Q/L (элитные муравьи); e = 0 — алгоритм задания
 */
public final class Colony {

    private final Graph graph;
    private final Settings settings;
    private final Random random;
    private final List<Ant> ants = new ArrayList<>();
    private final List<CycleStats> history = new ArrayList<>();
    private List<Hop> lastHops = List.of();
    private Tour best;

    public Colony(Graph graph, Settings settings, long seed) {
        this.graph = graph;
        this.settings = settings;
        this.random = new Random(seed);
        graph.paths().forEach(p -> p.reset(settings.tau0()));
        for (int i = 0; i < settings.ants(); i++) {
            // поровну по узлам, если нет гнезда
            ants.add(new Ant(i, graph, graph.nest() != null ? graph.nest() : graph.nodes().get(i % graph.nodes().size())));
        }
    }

    /** Каждый муравей делает одно перемещение; true — цикл на этом закончился */
    public boolean moveAll() {
        if (done()) {
            lastHops = List.of();
            return false;
        }
        List<Hop> hops = new ArrayList<>();
        for (Ant ant : ants) {
            if (!ant.finished()) {
                var from = ant.current();
                Path path = ant.chooseNext(settings, random);
                ant.move(path);
                hops.add(new Hop(ant, from, path, ant.current()));
            }
        }
        lastHops = List.copyOf(hops);
        if (ants.stream().allMatch(Ant::finished)) {
            endCycle();
            return true;
        }
        return false;
    }

    /** Довести текущий цикл до конца */
    public void runCycle() {
        if (done()) {
            return;
        }
        while (!moveAll()) {
            // ходим, пока все не вернутся
        }
    }

    /** Все M циклов */
    public void run() {
        while (!done()) {
            runCycle();
        }
    }

    private void endCycle() {
        List<Tour> tours = ants.stream().map(Ant::tour).toList();
        Tour cycleBest = tours.stream().min((x, y) -> Double.compare(x.length(), y.length())).orElseThrow();
        if (best == null || cycleBest.length() < best.length()) {
            best = cycleBest;
        }
        deposit(graph.paths(), tours, best, settings);
        evaporate(graph.paths(), settings);
        history.add(new CycleStats(history.size() + 1, cycleBest.length(),
                tours.stream().mapToDouble(Tour::length).average().orElseThrow(), best.length(),
                sameRouteShare(tours), usedPaths(tours)));
        ants.forEach(Ant::reset);
    }

    /** Феромон муравьёв: τ = τ + ρ·ΣΔτ, Δτ = Q/L за каждый проход пути; элита добавляет e·Q/L лучшего маршрута */
    static void deposit(Collection<Path> paths, List<Tour> tours, Tour best, Settings s) {
        Map<Path, Double> delta = new HashMap<>();
        for (Tour t : tours) {
            for (Path p : t.paths()) {
                delta.merge(p, s.q() / t.length(), Double::sum);
            }
        }
        if (best != null) {
            for (Path p : best.paths()) {
                delta.merge(p, s.elite() * s.q() / best.length(), Double::sum);
            }
        }
        for (Path p : paths) {
            p.deposit(s.rho() * delta.getOrDefault(p, 0.0));
        }
    }

    /** Испарение: τ = τ·(1 − ρ) */
    static void evaporate(Collection<Path> paths, Settings s) {
        paths.forEach(p -> p.evaporate(s.rho()));
    }

    private static double sameRouteShare(List<Tour> tours) {
        Map<List<Integer>, Integer> counts = new HashMap<>();
        tours.forEach(t -> counts.merge(t.route(), 1, Integer::sum));
        return (double) counts.values().stream().mapToInt(Integer::intValue).max().orElseThrow() / tours.size();
    }

    private static int usedPaths(List<Tour> tours) {
        Set<Path> used = new HashSet<>();
        tours.forEach(t -> used.addAll(t.paths()));
        return used.size();
    }

    public boolean done() {
        return history.size() >= settings.cycles();
    }

    /** Сколько циклов завершено */
    public int cycle() {
        return history.size();
    }

    public List<Ant> ants() {
        return List.copyOf(ants);
    }

    public List<CycleStats> history() {
        return List.copyOf(history);
    }

    /** Лучший маршрут за все циклы; null до конца первого цикла */
    public Tour best() {
        return best;
    }

    public List<Hop> lastHops() {
        return lastHops;
    }

    public Graph graph() {
        return graph;
    }

    public Settings settings() {
        return settings;
    }
}
