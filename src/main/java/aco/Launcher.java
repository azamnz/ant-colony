package aco;

import aco.view.AntApp;
import javafx.application.Application;

/** Точка входа; не сам AntApp, иначе запуск без модулей падает с «JavaFX runtime components are missing» */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(AntApp.class, args);
    }
}
