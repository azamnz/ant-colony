package aco.model;

/** Путь (ребро) между двумя узлами: длина и феромон на нём; путь двунаправленный */
public final class Path {

    private final int id;
    private final Node a;
    private final Node b;
    private final double length;
    private double pheromone;

    public Path(int id, Node a, Node b, double length) {
        if (a.equals(b)) {
            throw new IllegalArgumentException("путь из узла " + a + " в него же");
        }
        if (!(length > 0) || Double.isInfinite(length)) {
            throw new IllegalArgumentException("длина пути " + a + "–" + b + " должна быть положительной: " + length);
        }
        this.id = id;
        this.a = a;
        this.b = b;
        this.length = length;
    }

    public int id() {
        return id;
    }

    public Node a() {
        return a;
    }

    public Node b() {
        return b;
    }

    public double length() {
        return length;
    }

    public double pheromone() {
        return pheromone;
    }

    public boolean touches(Node n) {
        return a.equals(n) || b.equals(n);
    }

    /** Другой конец пути */
    public Node other(Node from) {
        if (a.equals(from)) {
            return b;
        }
        if (b.equals(from)) {
            return a;
        }
        throw new IllegalArgumentException("путь " + this + " не выходит из " + from);
    }

    /** Начальный феромон τ₀ */
    public void reset(double tau0) {
        pheromone = tau0;
    }

    public void deposit(double amount) {
        pheromone += amount;
    }

    /** Испарение: τ = τ·(1 − ρ) */
    public void evaporate(double rho) {
        pheromone *= 1 - rho;
    }

    @Override
    public String toString() {
        return a + "–" + b;
    }
}
