/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient;

import javax.swing.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    public static void main(String[] args) throws UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        Path gpPath = Paths.get("gradle.properties");
        String gradleProperties = Files.readString(gpPath);
        AtomicReference<String> mcVersion = new AtomicReference<>();
        gradleProperties.lines().forEach(l -> {
            if (l.startsWith("minecraft_version=")) mcVersion.set(l.replace("minecraft_version=", ""));
        });
        int option = JOptionPane.showOptionDialog(
                null,
                "To install Meteor Client, you need to put it in your mods folder and run Fabric for " + mcVersion.get() + "\nNeed help? Join our Discord",
                "Meteor Client",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                new String[] { "Open Wiki", "Open Mods Folder", "Join our Discord" },
                null
        );

        switch (option) {
            case 0: getOS().open("https://meteorclient.com/faq/installation"); break;
            case 1: {
                String path;

                switch (getOS()) {
                    case WINDOWS: path = System.getenv("AppData") + "/.minecraft/mods"; break;
                    case OSX:     path = System.getProperty("user.home") + "/Library/Application Support/minecraft/mods"; break;
                    default:      path = System.getProperty("user.home") + "/.minecraft/mods"; break;
                }

                File mods = new File(path);
                if (!mods.exists()) mods.mkdirs();

                getOS().open(mods);
                break;
            }
            case 2: getOS().open("https://meteorclient.com/discord"); break;
        }
    }

    private static OperatingSystem getOS() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);

        if (os.contains("linux") || os.contains("unix"))  return OperatingSystem.LINUX;
        if (os.contains("mac")) return OperatingSystem.OSX;
        if (os.contains("win")) return OperatingSystem.WINDOWS;

        return OperatingSystem.UNKNOWN;
    }

    private enum OperatingSystem {
        LINUX,
        WINDOWS {
            @Override
            protected String[] getURLOpenCommand(URL url) {
                return new String[] { "rundll32", "url.dll,FileProtocolHandler", url.toString() };
            }
        },
        OSX {
            @Override
            protected String[] getURLOpenCommand(URL url) {
                return new String[] { "open", url.toString() };
            }
        },
        UNKNOWN;

        public void open(URL url) {
            try {
                Runtime.getRuntime().exec(getURLOpenCommand(url));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void open(String url) {
            try {
                open(new URL(url));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        public void open(File file) {
            try {
                open(file.toURI().toURL());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        protected String[] getURLOpenCommand(URL url) {
            String string = url.toString();

            if ("file".equals(url.getProtocol())) {
                string = string.replace("file:", "file://");
            }

            return new String[] { "xdg-open", string };
        }
    }
}

