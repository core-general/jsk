package sk.utils.statics;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import sk.utils.functional.O;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.*;
import java.util.Arrays;

@Slf4j
public final class Im/*ages*/ {
    public static O<BufferedImage> readImage(String file) {
        return readImage(new File(file));
    }

    public static O<BufferedImage> readImage(File input) {
        try {
            return O.of(ImageIO.read(input));
        } catch (IOException e) {
            return O.empty();
        }
    }

    public static O<BufferedImage> readImage(byte[] image) {
        return readImage(new ByteArrayInputStream(image));
    }

    public static O<BufferedImage> readImage(InputStream is) {
        try {
            return O.of(ImageIO.read(is));
        } catch (IOException e) {
            return O.empty();
        }
    }

    public static BufferedImage resize(BufferedImage img, int newW, int newH) {
        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }

    public static BufferedImage resizeMaxDimension(BufferedImage img, int newMaxDimension) {
        if (img.getWidth() > img.getHeight()) {
            return resize(img, newMaxDimension, img.getHeight() * newMaxDimension / img.getWidth());
        } else {
            return resize(img, img.getWidth() * newMaxDimension / img.getHeight(), newMaxDimension);
        }
    }


    /**
     * @param im
     * @param blurSize some value between 2-20 depending on the input image, try a few variants
     * @return
     */
    private static BufferedImage blur(BufferedImage im, int blurSize) {
        int radius = blurSize;
        int size = radius * radius + 1;
        float weight = 1.0f / (size * size);
        float[] data = new float[size * size];

        Arrays.fill(data, weight);
        Kernel kernel = new Kernel(size, size, data);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        final BufferedImage bi = op.filter(im, null);
        final BufferedImage subimage = bi.getSubimage(size / 2, size / 2, bi.getWidth() - size, bi.getHeight() - size);
        return resize(subimage, im.getWidth(), im.getHeight());
    }



    //region JPEG
    public static boolean saveJpegToFile(String path, BufferedImage resultImage) {
        return saveJpegToFile(resultImage, path, 0.9f);
    }

    /** Quality 0f-1f */
    @SneakyThrows
    public static boolean saveJpegToFile(BufferedImage image, String path, float quality) {
        File f = new File(path);
        //noinspection ResultOfMethodCallIgnored
        f.getParentFile().mkdirs();
        return saveJpegToOutStream(image, new FileImageOutputStream(f), quality);
    }

    /** Quality 0f-1f */
    @SneakyThrows
    public static byte[] saveJpegToArray(BufferedImage image, float quality) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            MemoryCacheImageOutputStream os = new MemoryCacheImageOutputStream(baos);
            saveJpegToOutStream(image, os, quality);
            baos.close();
            return baos.toByteArray();
        }
    }

    @SneakyThrows
    public static InputStream saveJpegToInputStream(BufferedImage image, float quality) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        saveJpegToOutStream(image, os, quality);
        return new ByteArrayInputStream(os.toByteArray());
    }

    /** Quality 0f-1f */
    @SneakyThrows
    public static boolean saveJpegToOutStream(BufferedImage image, OutputStream output, float quality) {
        return saveJpegToOutStream(image, new MemoryCacheImageOutputStream(output), quality);
    }

    /** Quality 0f-1f */

    public static boolean saveJpegToOutStream(BufferedImage image, ImageOutputStream output, float quality) {
        JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
        jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpegParams.setCompressionQuality(quality);

        if (image.getColorModel().hasAlpha()) {
            //if image has alpha, we have to convert it to non alpha, otherwise writing throws exception
            BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = copy.createGraphics();
            try {
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, copy.getWidth(), copy.getHeight());
                g2d.drawImage(image, 0, 0, null);
                image = copy;
            } finally {
                g2d.dispose();
            }
        }

        try (output) {
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            writer.setOutput(output);
            writer.write(null, new IIOImage(image, null, null), jpegParams);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    //endregion


    //region PNG
    @SneakyThrows
    public static void savePngToFile(String file, BufferedImage image) {
        final File f = new File(file);
        f.getParentFile().mkdirs();
        ImageIO.write(image, "png", f);
    }


    public static byte[] savePngToBytes(BufferedImage bi) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             ImageOutputStream imo = new MemoryCacheImageOutputStream(stream)) {
            ImageIO.write(bi, "png", imo);
            stream.close();
            return stream.toByteArray();
        } catch (Exception e) {
            return Ex.thRow(e);
        }
    }
    //endregion


    public static String toHexColor(Color color, boolean withAlpha) {
        final String hexColorAlphaFirst = Integer.toHexString(color.getRGB());
        if (withAlpha) {
            return "#" + hexColorAlphaFirst.substring(hexColorAlphaFirst.length() - 6) + hexColorAlphaFirst.substring(0, 2);
        } else {
            return "#" + hexColorAlphaFirst.substring(2);
        }
    }

    public enum Format {

    }
}
