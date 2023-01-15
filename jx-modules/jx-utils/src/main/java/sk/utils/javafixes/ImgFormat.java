package sk.utils.javafixes;

import lombok.AllArgsConstructor;
import sk.utils.functional.F1;
import sk.utils.statics.Im;

import java.awt.image.BufferedImage;

@AllArgsConstructor
public enum ImgFormat {
    JPG(img -> Im.saveJpegToArray(img, 0.9f)),
    PNG(img -> Im.savePngToBytes(img));

    private F1<BufferedImage, byte[]> imgToBytesConverter;

    public byte[] toBytes(BufferedImage bi) {
        return imgToBytesConverter.apply(bi);
    }
}
