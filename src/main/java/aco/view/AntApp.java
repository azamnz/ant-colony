package aco.view;

import aco.colony.Colony;
import aco.colony.CycleStats;
import aco.controller.AppController;
import aco.controller.FormResult;
import aco.controller.Scenario;
import aco.controller.SettingsForm;
import aco.controller.SpeedScale;
import aco.controller.Texts;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/** Главное окно: граф с муравьями, справа управление и параметры, внизу график и журнал циклов */
public class AntApp extends Application {

    /** Каждый запуск — свои случайные выборы муравьёв */
    private final AppController controller = new AppController(new Random());
    private final GraphCanvas canvas = new GraphCanvas((x, y) -> {
        controller.clickAt(x, y);
        afterGraphChange();
    });
    private final ComboBox<Scenario> scenarios = new ComboBox<>();
    private final TextField cityCount = new TextField(String.valueOf(AppController.DEFAULT_CITIES));
    private final Button run = new Button();
    private final Slider speed = new Slider(SpeedScale.SLIDER_MIN, SpeedScale.SLIDER_MAX, 0);
    private final Label speedLabel = new Label();
    private final Label cycleValue = new Label();
    private final Label bestValue = new Label();
    private final Label optimumValue = new Label();
    private final Label sameValue = new Label();
    private final Label pathsValue = new Label();
    private final Label status = new Label();
    private final Map<String, TextField> inputs = new LinkedHashMap<>();
    private final Label errors = new Label();
    private final Label message = new Label();
    private final XYChart.Series<Number, Number> bestSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> averageSeries = new XYChart.Series<>();
    private final ListView<String> journal = new ListView<>();
    private final NumberAxis cycleAxis = new NumberAxis();
    private AnimationTimer timer;

    /** Что уже показано: график и журнал дополняются, а не строятся заново */
    private Colony shownColony;
    private int shownCycles;

    static String stylesheet() {
        return AntApp.class.getResource("app.css").toExternalForm();
    }

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setTop(buildToolbar());
        BorderPane.setMargin(canvas, new Insets(8, 8, 4, 12));
        root.setCenter(canvas);
        root.setRight(buildPanel());
        root.setBottom(buildBottom());

        Scene scene = new Scene(root, 1240, 820);
        scene.getStylesheets().add(stylesheet());
        stage.setTitle("Алгоритм муравьёв");
        stage.setMinWidth(980);
        stage.setMinHeight(660);
        stage.setScene(scene);

        timer = new AnimationTimer() {
            private long last;

            @Override
            public void handle(long now) {
                // без бега кадр ничего не меняет: окно обновляют кнопки
                if (controller.running()) {
                    controller.tick(last == 0 ? 0 : now - last);
                    last = now;
                    refresh();
                } else {
                    last = 0;
                }
            }
        };
        canvas.widthProperty().addListener((o, a, b) -> drawGraph());
        canvas.heightProperty().addListener((o, a, b) -> drawGraph());
        fillSettings();
        refresh();
        stage.show();
        timer.start();
    }

    @Override
    public void stop() {
        if (timer != null) {
            timer.stop();
        }
    }

    private HBox buildToolbar() {
        Label title = new Label("Муравьиная колония");
        title.getStyleClass().add("title");
        scenarios.getItems().addAll(Scenario.values());
        scenarios.setValue(controller.scenario());
        scenarios.setId("scenario");
        scenarios.setOnAction(e -> {
            if (scenarios.getValue() != null && scenarios.getValue() != controller.scenario()) {
                controller.select(scenarios.getValue());
                afterGraphChange();
            }
        });
        cityCount.setPrefColumnCount(3);
        cityCount.setId("cities");
        Button newCities = new Button("Новые города");
        newCities.setId("newCities");
        newCities.setOnAction(e -> {
            int n = SettingsForm.cities(cityCount.getText());
            if (n < 0) {
                message.setText("Городов должно быть от " + AppController.MIN_CITIES + " до " + AppController.MAX_CITIES);
                return;
            }
            controller.newCities(n);
            afterGraphChange();
        });
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        Label hint = new Label("Клик по полю — поставить город, по городу — убрать");
        hint.getStyleClass().add("muted");
        HBox bar = new HBox(10, title, new Label("Граф:"), scenarios, new Label("Городов:"), cityCount, newCities, gap, hint);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("toolbar-row");
        return bar;
    }

    private ScrollPane buildPanel() {
        run.setId("run");
        run.setOnAction(e -> {
            controller.toggleRun();
            refresh();
        });
        Button step = button("Шаг", "step", controller::moveStep);
        Button cycle = button("Цикл", "cycle", controller::cycleStep);
        Button toEnd = button("До конца", "toEnd", controller::runToEnd);
        Button restart = button("Сначала", "restart", controller::restart);
        for (Button b : List.of(run, step, cycle, toEnd, restart)) {
            b.setMaxWidth(Double.MAX_VALUE);
        }
        run.setPrefWidth(100);
        GridPane buttons = new GridPane(6, 6);
        buttons.add(run, 0, 0);
        buttons.add(step, 1, 0);
        buttons.add(cycle, 2, 0);
        buttons.add(toEnd, 0, 1, 2, 1);
        buttons.add(restart, 2, 1);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100 / 3.0);
            buttons.getColumnConstraints().add(cc);
        }

        speed.setValue(SpeedScale.toSlider(controller.speed()));
        speed.setId("speed");
        speed.valueProperty().addListener((o, a, b) -> {
            controller.setSpeed(SpeedScale.toSpeed(b.doubleValue()));
            speedLabel.setText(speedText());
        });
        speedLabel.setText(speedText());
        speedLabel.getStyleClass().add("muted");

        GridPane stats = new GridPane(8, 8);
        stats.add(stat("Цикл", cycleValue), 0, 0);
        stats.add(stat("Лучший маршрут", bestValue), 1, 0);
        stats.add(stat("На одном маршруте", sameValue), 0, 1);
        stats.add(stat("Оптимум (перебор)", optimumValue), 1, 1);
        stats.add(stat("Дорог в ходу", pathsValue), 0, 2, 2, 1);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            stats.getColumnConstraints().add(cc);
        }
        status.getStyleClass().add("status");
        status.setWrapText(true);

        GridPane form = new GridPane(8, 6);
        int row = 0;
        for (Map.Entry<String, String> f : SettingsForm.FIELDS.entrySet()) {
            TextField field = new TextField();
            field.setId(f.getKey());
            field.setPrefColumnCount(6);
            field.setOnAction(e -> applySettings());
            inputs.put(f.getKey(), field);
            form.add(new Label(f.getValue()), 0, row);
            form.add(field, 1, row++);
        }
        Button apply = new Button("Применить");
        apply.setId("apply");
        apply.setOnAction(e -> applySettings());
        Button pdf = new Button("Как в задании");
        pdf.setId("pdfSettings");
        pdf.setOnAction(e -> {
            controller.applySettings(aco.model.Settings.pdf().withAnts(controller.settings().ants()));
            fillSettings();
            refresh();
        });
        errors.getStyleClass().add("warning-text");
        errors.setWrapText(true);
        message.getStyleClass().add("message");
        message.setWrapText(true);
        Label note = new Label("e = 0 — алгоритм как в задании. Элитные муравьи каждый цикл добавляют феромон "
                + "на лучший найденный маршрут: так к нему сходится вся колония.");
        note.getStyleClass().add("legend");
        note.setWrapText(true);
        Label legend = new Label("Толщина и цвет линии — феромон. Зелёный пунктир — лучший маршрут.");
        legend.getStyleClass().add("legend");
        legend.setWrapText(true);

        VBox panel = new VBox(10, section("Колония"), buttons, speedLabel, speed, stats, status, legend,
                section("Параметры"), form, new HBox(8, apply, pdf), errors, note, message);
        panel.setPadding(new Insets(12));
        panel.setPrefWidth(320);
        panel.setMaxWidth(320);
        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("panel");
        scroll.setPrefWidth(336);
        return scroll;
    }

    private HBox buildBottom() {
        cycleAxis.setLabel("цикл");
        cycleAxis.setAutoRanging(false);
        cycleAxis.setMinorTickVisible(false);
        cycleAxis.setLowerBound(0);
        NumberAxis x = cycleAxis;
        NumberAxis y = new NumberAxis();
        y.setLabel("длина маршрута");
        y.setForceZeroInRange(false);
        LineChart<Number, Number> chart = new LineChart<>(x, y);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setLegendVisible(true);
        bestSeries.setName("лучший в цикле");
        averageSeries.setName("средний");
        chart.getData().add(averageSeries);
        chart.getData().add(bestSeries);
        chart.setPrefHeight(210);
        chart.setMinHeight(170);
        journal.setId("journal");
        journal.setPrefWidth(560);
        journal.setFocusTraversable(false);
        HBox.setHgrow(chart, Priority.ALWAYS);
        HBox bottom = new HBox(10, chart, journal);
        bottom.setPadding(new Insets(4, 12, 10, 12));
        bottom.setPrefHeight(220);
        bottom.getStyleClass().add("bottom");
        return bottom;
    }

    private Button button(String text, String id, Runnable action) {
        Button b = new Button(text);
        b.setId(id);
        b.setOnAction(e -> {
            action.run();
            message.setText("");
            refresh();
        });
        return b;
    }

    private static VBox stat(String name, Label value) {
        Label n = new Label(name);
        n.getStyleClass().add("stat-name");
        value.getStyleClass().add("stat-value");
        VBox box = new VBox(2, n, value);
        box.getStyleClass().add("stat");
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private static Label section(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section");
        return l;
    }

    private String speedText() {
        return "Скорость: " + controller.speed() + " перемещ./с";
    }

    private void applySettings() {
        Map<String, String> texts = new LinkedHashMap<>();
        inputs.forEach((k, v) -> texts.put(k, v.getText()));
        inputs.values().forEach(f -> f.getStyleClass().remove("error"));
        switch (SettingsForm.parse(texts)) {
            case FormResult.Ok ok -> {
                controller.applySettings(ok.settings());
                errors.setText("");
                message.setText("Параметры применены, колония начата заново");
                fillSettings();
            }
            case FormResult.Errors e -> {
                e.errors().keySet().forEach(k -> inputs.get(k).getStyleClass().add("error"));
                errors.setText(e.errors().entrySet().stream()
                        .map(en -> SettingsForm.FIELDS.get(en.getKey()) + ": " + en.getValue())
                        .collect(Collectors.joining("\n")));
                message.setText("");
            }
        }
        refresh();
    }

    private void fillSettings() {
        SettingsForm.texts(controller.settings()).forEach((k, v) -> inputs.get(k).setText(v));
        inputs.values().forEach(f -> f.getStyleClass().remove("error"));
        errors.setText("");
    }

    private void afterGraphChange() {
        scenarios.setValue(controller.scenario());
        message.setText("");
        fillSettings();
        refresh();
    }

    private void refresh() {
        Colony colony = controller.colony();
        run.setText(controller.running() ? "Пауза" : "Пуск");
        run.setDisable(colony == null || colony.done());
        if (colony == null) {
            cycleValue.setText("—");
            bestValue.setText("—");
            sameValue.setText("—");
            pathsValue.setText("—");
            optimumValue.setText("—");
            status.setText("Поставьте хотя бы " + AppController.MIN_CITIES + " города");
        } else {
            List<CycleStats> history = colony.history();
            CycleStats last = history.isEmpty() ? null : history.getLast();
            cycleValue.setText(colony.cycle() + " из " + colony.settings().cycles());
            bestValue.setText(colony.best() == null ? "—" : Texts.length(colony.best().length()));
            sameValue.setText(last == null ? "—" : Texts.percent(last.sameRoute()));
            pathsValue.setText(last == null ? "—" : last.usedPaths() + " из " + colony.graph().paths().size());
            optimumValue.setText(controller.optimum() == null ? "больше 10 городов" : Texts.length(controller.optimum()));
            status.setText(statusText(colony, last));
        }
        updateChartAndJournal(colony);
        drawGraph();
    }

    private String statusText(Colony colony, CycleStats last) {
        if (last == null) {
            return "Феромон везде одинаковый: муравьи выбирают пути почти наугад";
        }
        boolean optimal = controller.optimum() != null && Math.abs(last.best() - controller.optimum()) < 1e-9;
        if (last.sameRoute() == 1 && optimal) {
            return "Все муравьи идут по кратчайшему маршруту";
        }
        if (last.sameRoute() == 1) {
            return "Все муравьи идут по одному маршруту";
        }
        if (colony.done()) {
            return "Все " + colony.settings().cycles() + " циклов выполнены";
        }
        return controller.running() ? "Муравьи ищут путь…" : "Пауза";
    }

    private void updateChartAndJournal(Colony colony) {
        if (colony != shownColony) {
            bestSeries.getData().clear();
            averageSeries.getData().clear();
            journal.getItems().clear();
            shownColony = colony;
            shownCycles = 0;
            scaleCycleAxis(0);
        }
        if (colony == null) {
            return;
        }
        List<CycleStats> history = colony.history();
        if (history.size() == shownCycles) {
            return;
        }
        for (CycleStats st : history.subList(shownCycles, history.size())) {
            bestSeries.getData().add(new XYChart.Data<>(st.cycle(), st.best()));
            averageSeries.getData().add(new XYChart.Data<>(st.cycle(), st.average()));
            journal.getItems().add(Texts.cycleLine(st, colony.graph().paths().size()));
        }
        shownCycles = history.size();
        journal.scrollTo(journal.getItems().size() - 1);
        scaleCycleAxis(shownCycles);
    }

    /** Ось циклов: целые деления, не меньше 10 циклов */
    private void scaleCycleAxis(int cycles) {
        int upper = Math.max(10, cycles);
        int unit = Math.max(1, (int) Math.ceil(upper / 10.0));
        cycleAxis.setUpperBound(Math.ceil((double) upper / unit) * unit);
        cycleAxis.setTickUnit(unit);
    }

    private void drawGraph() {
        canvas.draw(controller.cities(), controller.graph(), controller.colony(), controller.progress(),
                "Кликните по полю, чтобы поставить города (нужно хотя бы " + AppController.MIN_CITIES + ")");
    }
}
