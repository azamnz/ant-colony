package aco.view;

import aco.colony.Ant;
import aco.colony.Colony;
import aco.colony.Hop;
import aco.colony.Tour;
import aco.controller.Texts;
import aco.model.Graph;
import aco.model.Node;
import aco.model.Path;
import aco.model.Scenarios;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** Граф: толщина и цвет пути — феромон, пунктир — лучший маршрут, точки — муравьи */
public final class GraphCanvas extends Pane {

    private static final Color BACKGROUND = Color.web("#fbfaf7");
    private static final Color FAINT = Color.web("#e4e2dc");
    private static final Color STRONG = Color.web("#ba7517");
    private static final Color BEST = Color.web("#0f6e56");
    private static final Color INK = Color.web("#2c2c2a");
    private static final Color MUTED = Color.web("#6b6a65");
    private static final double NODE_R = 13;
    private static final double MARGIN = 30;
    /** Выгиб параллельных путей, доля длины */
    private static final double BULGE = 0.45;

    private final Canvas canvas = new Canvas();
    private double scale = 1;
    private double offsetX;
    private double offsetY;

    /** onClick получает координаты поля, а не экрана */
    public GraphCanvas(BiConsumer<Double, Double> onClick) {
        getChildren().add(canvas);
        setMinSize(300, 220);
        canvas.setOnMouseClicked(e -> onClick.accept((e.getX() - offsetX) / scale, (e.getY() - offsetY) / scale));
    }

    @Override
    protected void layoutChildren() {
        // холст подгоняется в draw: окно перерисовывает граф при каждом изменении размера
    }

    public void draw(List<Node> cities, Graph graph, Colony colony, double progress, String emptyHint) {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.setFill(BACKGROUND);
        g.fillRect(0, 0, w, h);
        scale = Math.min((w - 2 * MARGIN) / Scenarios.WIDTH, (h - 2 * MARGIN) / Scenarios.HEIGHT);
        offsetX = (w - scale * Scenarios.WIDTH) / 2;
        offsetY = (h - scale * Scenarios.HEIGHT) / 2;
        g.setStroke(FAINT);
        g.setLineWidth(1);
        g.strokeRect(offsetX, offsetY, scale * Scenarios.WIDTH, scale * Scenarios.HEIGHT);

        if (graph != null) {
            drawPaths(g, graph, colony);
        }
        for (Node n : cities) {
            drawNode(g, n, graph);
        }
        if (colony != null) {
            drawAnts(g, colony, progress);
        }
        if (graph == null) {
            g.setFill(MUTED);
            g.setFont(Font.font("PT Sans", 15));
            g.setTextAlign(TextAlignment.CENTER);
            g.setTextBaseline(VPos.CENTER);
            g.fillText(emptyHint, w / 2, h / 2);
        }
    }

    private void drawPaths(GraphicsContext g, Graph graph, Colony colony) {
        double max = graph.maxPheromone();
        double min = graph.paths().stream().mapToDouble(Path::pheromone).min().orElse(0);
        // относительно самого слабого пути: в начале, когда феромон всюду одинаков, все линии тонкие
        double span = max - min > 1e-12 * max ? max - min : 0;
        g.setLineCap(StrokeLineCap.ROUND);
        // слабые снизу, сильные сверху
        List<Path> order = graph.paths().stream()
                .sorted((x, y) -> Double.compare(x.pheromone(), y.pheromone())).toList();
        for (Path p : order) {
            double ratio = span > 0 ? (p.pheromone() - min) / span : 0;
            g.setStroke(FAINT.interpolate(STRONG, Math.sqrt(ratio)));
            g.setLineWidth(1.2 + 9 * ratio);
            strokePath(g, graph, p);
        }
        Tour best = colony == null ? null : colony.best();
        if (best != null) {
            g.setStroke(BEST);
            g.setLineWidth(2);
            g.setLineDashes(7, 6);
            for (Path p : best.paths()) {
                strokePath(g, graph, p);
            }
            g.setLineDashes((double[]) null);
        }
        // подписи длины и феромона — только на маленьком графе с параллельными путями
        if (graph.nodes().size() == 2) {
            g.setFont(Font.font("PT Sans", FontWeight.BOLD, 14));
            g.setTextAlign(TextAlignment.CENTER);
            g.setTextBaseline(VPos.CENTER);
            for (Path p : graph.paths()) {
                Point2D mid = pointOn(graph, p, p.a(), 0.5);
                double up = mid.getY() < screen(p.a()).getY() ? -18 : 18;
                g.setFill(INK);
                g.fillText("L = " + Texts.length(p.length()) + "   τ = " + Texts.plain(round3(p.pheromone())),
                        mid.getX(), mid.getY() + up);
            }
        }
    }

    private static double round3(double v) {
        return Math.round(v * 1000) / 1000.0;
    }

    private void strokePath(GraphicsContext g, Graph graph, Path p) {
        Point2D a = screen(p.a());
        Point2D b = screen(p.b());
        Point2D c = control(graph, p);
        g.beginPath();
        g.moveTo(a.getX(), a.getY());
        g.quadraticCurveTo(c.getX(), c.getY(), b.getX(), b.getY());
        g.stroke();
    }

    /** Контрольная точка кривой: у одиночного пути — середина; параллельные: первый вверх, второй вниз, длиннее — выгнут сильнее */
    private Point2D control(Graph graph, Path p) {
        Point2D a = screen(p.a());
        Point2D b = screen(p.b());
        Point2D mid = a.midpoint(b);
        List<Path> twins = graph.pathsBetween(p.a(), p.b());
        if (twins.size() < 2) {
            return mid;
        }
        double k = (twins.size() - 1) / 2.0 - twins.indexOf(p);
        double longest = twins.stream().mapToDouble(Path::length).max().orElseThrow();
        Point2D d = b.subtract(a);
        Point2D normal = new Point2D(d.getY(), -d.getX()).normalize();
        return mid.add(normal.multiply(k * 2 * BULGE * d.magnitude() * p.length() / longest));
    }

    /** Точка на пути: t от 0 (узел from) до 1 */
    private Point2D pointOn(Graph graph, Path p, Node from, double t) {
        Point2D a = screen(p.a());
        Point2D b = screen(p.b());
        Point2D c = control(graph, p);
        double s = p.a().equals(from) ? t : 1 - t;
        double u = 1 - s;
        return a.multiply(u * u).add(c.multiply(2 * u * s)).add(b.multiply(s * s));
    }

    private Point2D screen(Node n) {
        return new Point2D(offsetX + n.x() * scale, offsetY + n.y() * scale);
    }

    private void drawNode(GraphicsContext g, Node n, Graph graph) {
        Point2D p = screen(n);
        boolean nest = graph != null && n.equals(graph.nest());
        g.setFill(nest ? Color.web("#f1efe8") : Color.WHITE);
        g.fillOval(p.getX() - NODE_R, p.getY() - NODE_R, 2 * NODE_R, 2 * NODE_R);
        g.setStroke(INK);
        g.setLineWidth(1.6);
        g.strokeOval(p.getX() - NODE_R, p.getY() - NODE_R, 2 * NODE_R, 2 * NODE_R);
        g.setFill(INK);
        g.setFont(Font.font("PT Sans", FontWeight.BOLD, 12));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.fillText(n.label(), p.getX(), p.getY());
        if (graph != null && graph.nest() != null) {
            g.setFill(MUTED);
            g.setFont(Font.font("PT Sans", 12));
            g.fillText(nest ? "гнездо" : "еда", p.getX(), p.getY() + NODE_R + 12);
        }
    }

    private void drawAnts(GraphicsContext g, Colony colony, double progress) {
        Map<Ant, Hop> hops = new HashMap<>();
        colony.lastHops().forEach(h -> hops.put(h.ant(), h));
        Graph graph = colony.graph();
        List<Ant> ants = colony.ants();
        for (Ant ant : ants) {
            Hop hop = hops.get(ant);
            Point2D at = hop != null && progress < 1
                    ? pointOn(graph, hop.path(), hop.from(), progress)
                    : screen(hop != null ? hop.to() : ant.current());
            // в пути муравьи чуть разнесены, иначе видно одного; в городе — по кругу снаружи, не закрывая номер
            boolean moving = hop != null && progress < 1;
            double angle = 2 * Math.PI * ant.id() / Math.max(1, ants.size()) - Math.PI / 2;
            double spread = moving ? (ants.size() > 1 ? 5 : 0) : NODE_R + 5;
            double x = at.getX() + spread * Math.cos(angle);
            double y = at.getY() + spread * Math.sin(angle);
            g.setFill(INK);
            g.fillOval(x - 3.5, y - 3.5, 7, 7);
            g.setFill(Color.web("#d85a30"));
            g.fillOval(x - 1.5, y - 1.5, 3, 3);
        }
    }
}
