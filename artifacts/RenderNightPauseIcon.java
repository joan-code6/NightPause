import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

/** Code-only preview renderer for the Night Pause adaptive launcher icon. */
public final class RenderNightPauseIcon {
    private static final Color BG = new Color(0x10182B);

    private static RoundRectangle2D.Float rr(float x, float y, float w, float h, float r) {
        return new RoundRectangle2D.Float(x, y, w, h, r, r);
    }

    private static void drawIcon(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(BG);
        g.fillRect(0, 0, 256, 256);

        // Slim, quiet strap; the circular case carries the mark.
        g.setColor(new Color(0x40527A));
        g.fill(rr(109, 37, 38, 66, 26));
        g.fill(rr(109, 153, 38, 66, 26));
        // Circular Galaxy Watch-like case and a restrained double bezel.
        g.setColor(new Color(0xD7E0FF));
        g.fill(new Ellipse2D.Float(53, 53, 150, 150));
        g.setColor(new Color(0x8799D0));
        g.fill(new Ellipse2D.Float(59, 59, 138, 138));
        g.setColor(new Color(0x1A2A4B));
        g.fill(new Ellipse2D.Float(66, 66, 124, 124));

        // Round the two ends after subtracting the circles; without this the
        // intersecting arcs leave a needle-like point at the top and bottom.
        Area moon = new Area(new Ellipse2D.Float(78, 83, 86, 86));
        moon.subtract(new Area(new Ellipse2D.Float(105, 83, 86, 86)));
        g.setColor(new Color(0xD5DEFF));
        g.fill(moon);
        g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(moon);

        g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(155, 110, 155, 146));
        g.draw(new Line2D.Float(173, 110, 173, 146));

        // Small crown, tucked into the case edge.
        g.setColor(new Color(0x7184BC));
        g.fill(rr(198, 119, 9, 18, 5));
    }

    public static void main(String[] args) throws Exception {
        int scale = 4;
        BufferedImage image = new BufferedImage(256 * scale, 256 * scale, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.scale(scale, scale);
        drawIcon(g);
        g.dispose();
        File output = new File(args.length == 0 ? "artifacts/nightpause-icon-refined.png" : args[0]);
        output.getParentFile().mkdirs();
        ImageIO.write(image, "png", output);
        System.out.println(output.getAbsolutePath());
    }
}
