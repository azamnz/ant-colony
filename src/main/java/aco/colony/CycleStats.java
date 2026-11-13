package aco.colony;

/**
 * Итог одного цикла.
 *
 * @param best      самый короткий маршрут цикла
 * @param average   средняя длина маршрута
 * @param bestSoFar лучший маршрут за все циклы
 * @param sameRoute доля муравьёв, прошедших самый частый маршрут
 * @param usedPaths сколько разных путей прошли муравьи
 */
public record CycleStats(int cycle, double best, double average, double bestSoFar, double sameRoute, int usedPaths) {
}
