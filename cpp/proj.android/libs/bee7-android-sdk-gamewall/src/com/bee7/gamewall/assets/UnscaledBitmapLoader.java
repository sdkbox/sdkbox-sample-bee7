package com.bee7.gamewall.assets;

/**
 * Created by Bee7 on 02/12/14.
 */
public class UnscaledBitmapLoader {
    public enum ScreenDPI {
        DENSITY_LDPI(120), DENSITY_MDPI(160), DENSITY_TVDPI(213), DENSITY_HDPI(240), DENSITY_XHDPI(320), DENSITY_XXHDPI(
                480), DENSITY_XXXHDPI(640);

        private int density;

        private ScreenDPI(int density) {
            this.density = density;
        }

        public int getDensity() {
            return density;
        }

        public static ScreenDPI parseDensity(String density) {
            if (density.equalsIgnoreCase("ldpi")) {
                return DENSITY_LDPI;
            } else if (density.equalsIgnoreCase("mdpi")) {
                return DENSITY_MDPI;
            } else if (density.equalsIgnoreCase("hdpi")) {
                return DENSITY_HDPI;
            } else if (density.equalsIgnoreCase("xhdpi")) {
                return DENSITY_XHDPI;
            } else if (density.equalsIgnoreCase("xxhdpi")) {
                return DENSITY_XXHDPI;
            } else if (density.equalsIgnoreCase("xxxhdpi")) {
                return DENSITY_XXXHDPI;
            } else {
                throw new RuntimeException("Unable to parse density: " + density);
            }
        }
    }

}
