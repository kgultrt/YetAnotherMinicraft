package com.mojang.ld22.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.mojang.ld22.Game;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> showCrashDialog(e));

        if (StartupHelper.startNewJvmIfRequired()) return;
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        Game.version = "1.0.0";
        return new Lwjgl3Application(new Game(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Minicraft");
        configuration.useVsync(true);
        configuration.setForegroundFPS(60);
        configuration.setWindowedMode(960, 720);
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        return configuration;
    }

    private static void showCrashDialog(Throwable e) {
        EventQueue.invokeLater(() -> {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String stackTrace = sw.toString();

            JTextArea textArea = new JTextArea(stackTrace, 20, 60);
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);

            Object[] options = {"复制错误信息", "退出"};

            int choice = JOptionPane.showOptionDialog(
                    null,
                    scrollPane,
                    "游戏崩溃了",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.ERROR_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            if (choice == 0) {
                Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(new StringSelection(stackTrace), null);
                JOptionPane.showMessageDialog(null, "错误信息已复制到剪贴板");
            }

            System.exit(1);
        });
    }
}