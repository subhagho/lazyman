package com.codekutter.lazyman.ui;

import com.google.common.base.Preconditions;
import javafx.application.Application;
import javafx.stage.Stage;

public class Viewer extends Application {
    private volatile boolean running;

    @Override
    public void start(Stage stage) throws Exception {
        Preconditions.checkState(Helper.journey != null);
        Helper.show(Helper.journey);
    }

    public static void show() {
        launch();
    }
}
