package com.skarm.launcher.bootstrap;

import java.awt.Color;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;

/** Checks the actual embedded runtime before downloading or entering the game. */
public final class Arm32Checks {
    private Arm32Checks() {}

    public static void run() throws Exception {
        var vm = java.lang.management.ManagementFactory.getPlatformMXBean(
                com.sun.management.HotSpotDiagnosticMXBean.class);
        System.out.println("[Arm32Checks] VM mode=" + System.getProperty("java.vm.info")
                + ", UseCompiler=" + vm.getVMOption("UseCompiler").getValue()
                + ", TieredStopAtLevel=" + vm.getVMOption("TieredStopAtLevel").getValue()
                + ", InlineMathNatives=" + vm.getVMOption("InlineMathNatives").getValue());
        checkMath();
        checkWarmMath();
        checkPixels();
        checkFonts();
        // Discord's optional desktop SDK uses FFM after character selection.
        Preferences prefs = Preferences.userRoot().node("projectx");
        prefs.putBoolean("discord", false);
        prefs.flush();
        System.out.println("[Arm32Checks] PASS: math, direct buffers, PNG and font raster; Discord disabled");
    }

    public static void checkMath() {
        near("pow", Math.pow(4.0, 3.0), 64.0);
        near("Vorbis table size", Math.rint(Math.pow(10.0, 2.0)), 100.0);
        near("sqrt", Math.sqrt(81.0), 9.0);
        near("sin", Math.sin(Math.PI / 2.0), 1.0);
        near("cos", Math.cos(0.0), 1.0);
        near("tan", Math.tan(Math.PI / 4.0), 1.0);
        near("log", Math.log(Math.E), 1.0);
        near("exp", Math.exp(0.0), 1.0);
        near("abs", Math.abs(-0.75), 0.75);
        near("rint", Math.rint(85.75), 86.0);
        near("float bits", Float.intBitsToFloat(0x3f400000), 0.75);
        near("double bits", Double.longBitsToDouble(0x3fe8000000000000L), 0.75);
        System.out.println("[Arm32Checks] math PASS");
    }

    private static void near(String name, double actual, double expected) {
        double difference = actual - expected;
        if (!Double.isFinite(actual) || difference < -0.000001 || difference > 0.000001) {
            throw new IllegalStateException(name + ": expected " + expected + ", got " + actual);
        }
    }

    public static void checkWarmMath() {
        // Repeated calls exercise transitions from interpreted to compiled code.
        long start = System.nanoTime();
        for (int i = 0; i < 20000; i++) checkMathInput((i & 15) + 1.0);
        var compiler = java.lang.management.ManagementFactory.getCompilationMXBean();
        System.out.println("[Arm32Checks] warm math PASS: elapsedMs="
                + (System.nanoTime() - start) / 1_000_000L + ", compiler="
                + (compiler == null ? "none" : compiler.getName())
                + ", compilationMs=" + (compiler == null || !compiler.isCompilationTimeMonitoringSupported()
                ? -1 : compiler.getTotalCompilationTime()));
    }

    private static void checkMathInput(double value) {
        near("warm pow", Math.pow(value, 2.0), value * value);
        near("warm sqrt", Math.sqrt(value * value), value);
        near("warm sin", Math.sin(value) * Math.sin(value) + Math.cos(value) * Math.cos(value), 1.0);
        near("warm bits", Double.longBitsToDouble(Double.doubleToRawLongBits(value)), value);
    }

    public static void checkPixels() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
        buffer.putFloat(0, 0.75f);
        buffer.putDouble(8, 64.0);
        near("direct float", buffer.getFloat(0), 0.75);
        near("direct double", buffer.getDouble(8), 64.0);
        BufferedImage original = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        original.setRGB(0, 0, 0x804080c0);
        original.setRGB(1, 1, 0xff123456);
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        if (!ImageIO.write(original, "png", encoded)) throw new IllegalStateException("PNG writer missing");
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(encoded.toByteArray()));
        if (decoded == null || decoded.getRGB(0, 0) != 0x804080c0 || decoded.getRGB(1, 1) != 0xff123456) {
            throw new IllegalStateException("PNG pixel roundtrip failed");
        }
        System.out.println("[Arm32Checks] pixel buffers and PNG PASS");
    }

    public static void checkFonts() {
        Font font = new Font("Dialog", Font.PLAIN, 18);
        char[] text = "Spiral Knights AV ñ".toCharArray();
        var context = new FontRenderContext(new AffineTransform(), true, true);
        var glyphs = font.layoutGlyphVector(context, text, 0, text.length, Font.LAYOUT_LEFT_TO_RIGHT);
        double width = glyphs.getLogicalBounds().getWidth();
        if (!Double.isFinite(width) || width <= 0 || width > 1024) {
            throw new IllegalStateException("Invalid font width: " + width);
        }
        BufferedImage raster = new BufferedImage(512, 40, BufferedImage.TYPE_INT_ARGB);
        var graphics = raster.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.drawGlyphVector(glyphs, 2, 25);
        } finally {
            graphics.dispose();
        }
        int visible = 0;
        for (int y = 0; y < raster.getHeight(); y++) {
            for (int x = 0; x < raster.getWidth(); x++) {
                if ((raster.getRGB(x, y) >>> 24) != 0) visible++;
            }
        }
        if (visible == 0) throw new IllegalStateException("Font raster is empty");
        System.out.println("[Arm32Checks] font PASS: width=" + width + ", visible pixels=" + visible);
    }

    public static void checkGameResources(ClassLoader loader, String appDir) {
        // Keep silent mode unless this device decodes the actual game files.
        System.setProperty("disable_sound", "true");
        try {
            Class<?> decoderClass = loader.loadClass("com.threerings.openal.k");
            for (String sound : new String[]{"feedback/chat_open.ogg", "music/title.ogg"}) {
                Path path = Path.of(appDir, "rsrc", "sound", sound);
                Object decoder = decoderClass.getConstructor().newInstance();
                try (var input = Files.newInputStream(path)) {
                    decoderClass.getMethod("h", java.io.InputStream.class).invoke(decoder, input);
                    ByteBuffer pcm = ByteBuffer.allocateDirect(65536).order(ByteOrder.nativeOrder());
                    int size = (Integer) decoderClass.getMethod("b", ByteBuffer.class).invoke(decoder, pcm);
                    if (size <= 0 || size > pcm.capacity()) throw new IllegalStateException("Invalid audio sample size: " + size);
                    System.out.println("[Arm32Checks] decoded " + sound + ": " + size + " bytes");
                }
            }
            System.setProperty("disable_sound", "false");
            System.out.println("[Arm32Checks] game audio PASS; sound enabled");
        } catch (ReflectiveOperationException | java.io.IOException | RuntimeException | LinkageError error) {
            System.err.println("[Arm32Checks] game audio failed; keeping silent mode");
            error.printStackTrace();
        }
        try {
            BufferedImage button = ImageIO.read(Path.of(appDir, "rsrc", "ui", "button", "blue", "up.png").toFile());
            if (button == null) throw new IllegalStateException("Button image could not be decoded");
            int visible = 0;
            for (int y = 0; y < button.getHeight(); y++) {
                for (int x = 0; x < button.getWidth(); x++) {
                    if ((button.getRGB(x, y) >>> 24) != 0) visible++;
                }
            }
            System.out.println("[Arm32Checks] game button texture " + button.getWidth() + "x" + button.getHeight() + ": " + visible + " visible pixels");
        } catch (java.io.IOException | RuntimeException error) {
            System.err.println("[Arm32Checks] game button texture check failed: " + error);
        }
    }
}
