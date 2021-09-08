package com.vegazsdev.bobobot.core.bot;

/**
 * That class show version info about build.
 */
public record BuildInfo(boolean doStable) {
    public String getVersion() {
        Variables variables = new Variables();

        if (doStable) {
            return "v" + variables.VERSION + "-" + variables.STABLE + "-" + "[" + variables.CODENAME + "]";
        } else {
            return variables.VERSION + variables.STAGING;
        }
    }

    private static class Variables {
        public final String VERSION = "1.7.0";
        public final String STABLE = "STABLE";
        public final String STAGING = "BETA";
        public final String CODENAME = "Tsuki";
    }
}