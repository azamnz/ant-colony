package aco.model;

/** Узел сети — город; координаты нужны для расстояний и рисования */
public record Node(int id, String label, double x, double y) {

    public double distanceTo(Node other) {
        return Math.hypot(x - other.x, y - other.y);
    }

    @Override
    public String toString() {
        return label;
    }
}
