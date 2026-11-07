package com.corectdelay;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class CorectDelayConfig {
    public static boolean rmbTweak = true;
    public static boolean lmbTweakWithItem = true;
    public static boolean lmbTweakWithoutItem = true;
    public static boolean wheelTweak = true;
    public static boolean wheelSearchOrder = true; // true = od ostatniego do pierwszego
    public static int wheelScrollDirection = 0;    // 0 normalnie, 1 odwrócone, 2 zależne od pozycji

    private static final Path FILE =
            FabricLoader.getInstance().getConfigDir().resolve("CorectDelay.cfg");

    public static void load() {
        Properties p = new Properties();
        if (Files.exists(FILE)) {
            try (InputStream in = Files.newInputStream(FILE)) {
                p.load(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        rmbTweak = readBool(p, "RMBTweak", true);
        lmbTweakWithItem = readBool(p, "LMBTweakWithItem", true);
        lmbTweakWithoutItem = readBool(p, "LMBTweakWithoutItem", true);
        wheelTweak = readBool(p, "WheelTweak", true);
        wheelSearchOrder = readBool(p, "WheelSearchOrder", true);
        wheelScrollDirection = readInt(p, "WheelScrollDirection", 0, 0, 2);
        save(); // dopisuje brakujące wartości
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("RMBTweak", rmbTweak ? "1" : "0");
        p.setProperty("LMBTweakWithItem", lmbTweakWithItem ? "1" : "0");
        p.setProperty("LMBTweakWithoutItem", lmbTweakWithoutItem ? "1" : "0");
        p.setProperty("WheelTweak", wheelTweak ? "1" : "0");
        p.setProperty("WheelSearchOrder", wheelSearchOrder ? "1" : "0");
        p.setProperty("WheelScrollDirection", String.valueOf(wheelScrollDirection));
        try (OutputStream out = Files.newOutputStream(FILE)) {
            p.store(out, "CorectDelay config");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean readBool(Properties p, String key, boolean def) {
        return "1".equals(p.getProperty(key, def ? "1" : "0").trim());
    }

    private static int readInt(Properties p, String key, int def, int min, int max) {
        try {
            int v = Integer.parseInt(p.getProperty(key, String.valueOf(def)).trim());
            return Math.max(min, Math.min(max, v));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
