package aco.colony;

import aco.model.Node;
import aco.model.Path;

/** Последнее перемещение муравья: откуда, по какому пути, куда; для анимации */
public record Hop(Ant ant, Node from, Path path, Node to) {
}
