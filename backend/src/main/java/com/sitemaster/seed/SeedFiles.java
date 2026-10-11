package com.sitemaster.seed;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;

/**
 * Real PDF and PNG files generated in memory for the demo data, so uploads go
 * through the same validation (extension, size, Apache Tika content check) as
 * files a user uploads.
 */
final class SeedFiles {

    private SeedFiles() {
        // utility class
    }

    static MultipartFile pdf(String fileName, String... lines) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            for (String line : lines) {
                document.add(new Paragraph(line));
            }
        } catch (DocumentException ex) {
            throw new IllegalStateException("Could not build demo PDF", ex);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return new InMemoryFile("file", fileName, "application/pdf", out.toByteArray());
    }

    /** A simple sky-and-building illustration used as a project cover photo. */
    static MultipartFile png(String fileName, String caption, Color accent) {
        BufferedImage image = new BufferedImage(1200, 675, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, new Color(125, 175, 230), 0, 675, new Color(235, 243, 250)));
            g.fillRect(0, 0, 1200, 675);
            g.setColor(new Color(90, 90, 90));
            g.fillRect(0, 560, 1200, 115);
            g.setColor(accent);
            g.fillRect(420, 190, 360, 370);
            g.setColor(new Color(255, 255, 255, 200));
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 4; col++) {
                    g.fillRect(445 + col * 85, 215 + row * 68, 55, 40);
                }
            }
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 44));
            g.drawString(caption, 60, 640);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return new InMemoryFile("file", fileName, "image/png", out.toByteArray());
    }

    /** Minimal MultipartFile backed by a byte array. */
    private record InMemoryFile(String name, String originalFilename, String contentType, byte[] bytes)
            implements MultipartFile {

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes.clone();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.write(dest.toPath(), bytes);
        }
    }
}
