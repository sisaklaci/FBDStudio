package com.sisak.fbdstudio.ui;

import com.sisak.fbdstudio.i18n.Language;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/** Small flags drawn procedurally (no bundled image assets) for the language picker. */
public final class FlagIcon {

    private static final double W = 22;
    private static final double H = 15;

    private FlagIcon() {
    }

    public static Canvas create(Language lang) {
        Canvas canvas = new Canvas(W, H);
        GraphicsContext g = canvas.getGraphicsContext2D();
        switch (lang) {
            case HU -> horizontalStripes(g, Color.web("#CE2939"), Color.WHITE, Color.web("#477050"));
            case DE -> horizontalStripes(g, Color.web("#1a1a1a"), Color.web("#DD0000"), Color.web("#FFCE00"));
            case PL -> horizontalStripes(g, Color.WHITE, Color.web("#DC143C"));
            case ES -> spain(g);
            case FR -> verticalStripes(g, Color.web("#0055A4"), Color.WHITE, Color.web("#EF4135"));
            case ZH -> china(g);
            case JA -> japan(g);
            case EN -> unionJack(g);
        }
        g.setStroke(Color.web("#000000", 0.25));
        g.setLineWidth(1);
        g.strokeRect(0.5, 0.5, W - 1, H - 1);
        return canvas;
    }

    private static void horizontalStripes(GraphicsContext g, Color... colors) {
        double stripeH = H / colors.length;
        for (int i = 0; i < colors.length; i++) {
            g.setFill(colors[i]);
            g.fillRect(0, i * stripeH, W, stripeH);
        }
    }

    private static void verticalStripes(GraphicsContext g, Color... colors) {
        double stripeW = W / colors.length;
        for (int i = 0; i < colors.length; i++) {
            g.setFill(colors[i]);
            g.fillRect(i * stripeW, 0, stripeW, H);
        }
    }

    private static void spain(GraphicsContext g) {
        g.setFill(Color.web("#AA151B"));
        g.fillRect(0, 0, W, H);
        g.setFill(Color.web("#F1BF00"));
        g.fillRect(0, H / 4, W, H / 2);
    }

    private static void japan(GraphicsContext g) {
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, W, H);
        g.setFill(Color.web("#BC002D"));
        double r = H * 0.3;
        g.fillOval(W / 2 - r, H / 2 - r, r * 2, r * 2);
    }

    private static void china(GraphicsContext g) {
        g.setFill(Color.web("#DE2910"));
        g.fillRect(0, 0, W, H);
        g.setFill(Color.web("#FFDE00"));
        drawStar(g, W * 0.28, H * 0.32, H * 0.28, H * 0.11);
    }

    private static void drawStar(GraphicsContext g, double cx, double cy, double outerR, double innerR) {
        double[] xs = new double[10];
        double[] ys = new double[10];
        for (int i = 0; i < 10; i++) {
            double r = (i % 2 == 0) ? outerR : innerR;
            double angle = Math.toRadians(-90 + i * 36);
            xs[i] = cx + r * Math.cos(angle);
            ys[i] = cy + r * Math.sin(angle);
        }
        g.fillPolygon(xs, ys, 10);
    }

    /** Simplified Union Jack: recognizable approximation at icon size, not heraldically exact. */
    private static void unionJack(GraphicsContext g) {
        g.setFill(Color.web("#00247D"));
        g.fillRect(0, 0, W, H);

        g.setStroke(Color.WHITE);
        g.setLineWidth(3.2);
        g.strokeLine(0, 0, W, H);
        g.strokeLine(0, H, W, 0);

        g.setStroke(Color.web("#CF142B"));
        g.setLineWidth(1.4);
        g.strokeLine(0, 0, W, H);
        g.strokeLine(0, H, W, 0);

        g.setStroke(Color.WHITE);
        g.setLineWidth(4.5);
        g.strokeLine(W / 2, 0, W / 2, H);
        g.strokeLine(0, H / 2, W, H / 2);

        g.setStroke(Color.web("#CF142B"));
        g.setLineWidth(2.2);
        g.strokeLine(W / 2, 0, W / 2, H);
        g.strokeLine(0, H / 2, W, H / 2);
    }
}
