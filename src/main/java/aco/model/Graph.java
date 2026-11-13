package aco.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Среда муравьёв: двунаправленный граф; любая пара узлов соединена хотя бы одним путём */
public final class Graph {

    private final List<Node> nodes;
    private final List<Path> paths;
    /** Гнездо: все муравьи выходят отсюда; null — муравьи распределены по узлам */
    private final Node nest;

    public Graph(List<Node> nodes, List<Path> paths, Node nest) {
        if (nodes.size() < 2) {
            throw new IllegalArgumentException("нужно хотя бы 2 узла, а их " + nodes.size());
        }
        Set<Integer> ids = new HashSet<>();
        for (Node n : nodes) {
            if (!ids.add(n.id())) {
                throw new IllegalArgumentException("номер узла повторяется: " + n.id());
            }
        }
        for (Path p : paths) {
            if (!nodes.contains(p.a()) || !nodes.contains(p.b())) {
                throw new IllegalArgumentException("путь " + p + " ведёт в чужой узел");
            }
        }
        if (nest != null && !nodes.contains(nest)) {
            throw new IllegalArgumentException("гнездо " + nest + " не в графе");
        }
        this.nodes = List.copyOf(nodes);
        this.paths = List.copyOf(paths);
        this.nest = nest;
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                if (pathsBetween(nodes.get(i), nodes.get(j)).isEmpty()) {
                    throw new IllegalArgumentException("нет пути между " + nodes.get(i) + " и " + nodes.get(j));
                }
            }
        }
    }

    /** Полный граф: путь между каждой парой, длина — расстояние по прямой */
    public static Graph complete(List<Node> nodes) {
        List<Path> paths = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                paths.add(new Path(paths.size(), nodes.get(i), nodes.get(j), nodes.get(i).distanceTo(nodes.get(j))));
            }
        }
        return new Graph(nodes, paths, null);
    }

    public List<Node> nodes() {
        return nodes;
    }

    public List<Path> paths() {
        return paths;
    }

    public Node nest() {
        return nest;
    }

    public List<Path> pathsFrom(Node n) {
        return paths.stream().filter(p -> p.touches(n)).toList();
    }

    public List<Path> pathsBetween(Node from, Node to) {
        return paths.stream().filter(p -> p.touches(from) && p.touches(to)).toList();
    }

    /** Наибольший феромон на путях: для яркости линий */
    public double maxPheromone() {
        return paths.stream().mapToDouble(Path::pheromone).max().orElse(0);
    }
}
