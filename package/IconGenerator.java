import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import javax.imageio.ImageIO;

/**
 * Generates app icon in .ico format for jpackage on Windows.
 * Run: java -cp target\classes-icon IconGenerator
 * Output: package/windows/icon.ico (256×256, 32-bit ARGB)
 *
 * <p>Writes a valid .ico file with embedded PNG data — the standard
 * format for modern Windows icons and accepted by jpackage.</p>
 */
public final class IconGenerator {

    private static final int SIZE = 256;
    private static final Color BG      = new Color(0x16, 0x21, 0x3e);   // deep navy
    private static final Color GOLD    = new Color(0xe6, 0xc2, 0x00);   // warm gold
    private static final Color GOLD_DIM = new Color(0xc4, 0xa0, 0x00);  // muted gold
    private static final Color WHITE   = new Color(0xf0, 0xf0, 0xf5);

    public static void main(String[] args) throws Exception {
        // ── Draw the icon into a BufferedImage ──
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // background: rounded rectangle
            g.setColor(BG);
            g.fill(new RoundRectangle2D.Double(8, 8, SIZE - 16, SIZE - 16, 40, 40));

            // sound-wave bars
            int barW = 14;
            int gap = 10;
            int baseY = 158;
            int[] barHeights = {28, 52, 74, 52, 28};
            int totalW = barHeights.length * barW + (barHeights.length - 1) * gap;
            int startX = (SIZE - totalW) / 2;

            for (int i = 0; i < barHeights.length; i++) {
                int x = startX + i * (barW + gap);
                int h = barHeights[i];
                int y = baseY - h;
                GradientPaint grad = new GradientPaint(x, y, GOLD, x, baseY, GOLD_DIM);
                g.setPaint(grad);
                g.fill(new RoundRectangle2D.Double(x, y, barW, h, barW / 2f, barW / 2f));
            }

            // "AI" text
            g.setColor(WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 52));
            FontMetrics fm = g.getFontMetrics();
            String text = "AI";
            int textW = fm.stringWidth(text);
            g.drawString(text, (SIZE - textW) / 2, 210);

            // bottom accent line
            g.setColor(GOLD);
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int lineY = 224;
            int lineW = 60;
            g.draw(new Line2D.Double((SIZE - lineW) / 2.0, lineY, (SIZE + lineW) / 2.0, lineY));
        } finally {
            g.dispose();
        }

        // ── Encode as PNG bytes ──
        ByteArrayOutputStream pngBytes = new ByteArrayOutputStream();
        ImageIO.write(img, "png", pngBytes);
        byte[] pngData = pngBytes.toByteArray();

        // ── Write .ico file ──
        File out = new File("package/windows/icon.ico");
        out.getParentFile().mkdirs();

        try (FileOutputStream fos = new FileOutputStream(out)) {
            // ICO header: reserved(2) + type=1(2) + count=1(2)
            writeLE16(fos, 0);    // reserved
            writeLE16(fos, 1);    // type: icon
            writeLE16(fos, 1);    // count: 1 image

            // ICO directory entry (16 bytes)
            int dataOffset = 6 + 16;  // header + 1 entry
            fos.write(0);              // width  (0 = 256)
            fos.write(0);              // height (0 = 256)
            fos.write(0);              // color palette
            fos.write(0);              // reserved
            writeLE16(fos, 1);        // planes
            writeLE16(fos, 32);       // bpp
            writeLE32(fos, pngData.length);  // image size
            writeLE32(fos, dataOffset);       // image offset

            // PNG image data
            fos.write(pngData);
        }

        System.out.println("Icon written to " + out.getAbsolutePath());
    }

    private static void writeLE16(FileOutputStream fos, int value) throws Exception {
        fos.write(value & 0xFF);
        fos.write((value >> 8) & 0xFF);
    }

    private static void writeLE32(FileOutputStream fos, int value) throws Exception {
        fos.write(value & 0xFF);
        fos.write((value >> 8) & 0xFF);
        fos.write((value >> 16) & 0xFF);
        fos.write((value >> 24) & 0xFF);
    }
}
