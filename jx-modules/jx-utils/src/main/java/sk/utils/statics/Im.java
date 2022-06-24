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
import lombok.extern.log4j.Log4j2;
import sk.utils.functional.O;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;

@Log4j2
public final class Im {
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
    //endregion

}
