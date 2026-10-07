package edu.liceo.ugoautomate.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class QrCodes {
    private QrCodes() {
    }

    public static BufferedImage generate(String text, int size) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        try {
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints);
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (WriterException e) {
            throw new IllegalArgumentException("Text cannot be encoded as a QR code: " + e.getMessage(), e);
        }
    }

    public static BitMatrix moduleMatrix(String text) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 0);
        try {
            return new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints);
        } catch (WriterException e) {
            throw new IllegalArgumentException("Text cannot be encoded as a QR code: " + e.getMessage(), e);
        }
    }

    public static Optional<String> decode(BufferedImage image) {
        if (image == null) {
            return Optional.empty();
        }
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE));
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        MultiFormatReader reader = new MultiFormatReader();
        reader.setHints(hints);
        for (BinaryBitmap bitmap : List.of(new BinaryBitmap(new HybridBinarizer(source)),
                new BinaryBitmap(new GlobalHistogramBinarizer(source)))) {
            try {
                Result result = reader.decodeWithState(bitmap);
                return Optional.of(result.getText());
            } catch (NotFoundException e) {
                reader.reset();
            }
        }
        return Optional.empty();
    }

    public static Optional<String> decode(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("\"" + file.getName() + "\" is not a supported image (use PNG, JPG, BMP, or GIF).");
        }
        return decode(image);
    }

    public static void savePng(BufferedImage image, File file) throws IOException {
        if (!ImageIO.write(image, "png", file)) {
            throw new IOException("PNG writer not available");
        }
    }
}
