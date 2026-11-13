package aco.colony;

import aco.model.Path;

import java.util.List;

/** Замкнутый маршрут муравья: пройденные пути по порядку и общая длина L */
public record Tour(List<Path> paths, double length) {

    public Tour {
        paths = List.copyOf(paths);
    }

    /** Номера путей по возрастанию: маршрут не зависит от того, откуда и в какую сторону его прошли */
    public List<Integer> route() {
        return paths.stream().map(Path::id).sorted().toList();
    }
}
