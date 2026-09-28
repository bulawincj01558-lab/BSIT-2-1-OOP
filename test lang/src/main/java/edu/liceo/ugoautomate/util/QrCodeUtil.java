package edu.liceo.ugoautomate.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * QR code generation and decoding with ZXing.
 */
public final class QrCodeUtil {

    private QrCodeUtil() {
    }

    /** Renders {@code text} as a square QR code image of {@code size} pixels. */
    public static BufferedImage generate(String text, int size) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 2);
        try {
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints);
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (WriterException e) {
            throw new AppException("Unable to generate the QR code.", e);
        }
    }

    public static void writePng(String text, int size, Path file) throws IOException {
        ImageIO.write(generate(text, size), "png", file.toFile());
    }

    /**
     * Decodes the first QR code found in an image.
     *
     * @param image     source image (file upload or camera frame)
     * @param tryHarder spend more time searching (use for still images, not live frames)
     * @return decoded text, or empty if no readable QR code was found
     */
    public static Optional<String> decode(BufferedImage image, boolean tryHarder) {
        if (image == null) {
            return Optional.empty();
        }
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE));
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
        if (tryHarder) {
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        }
        try {
            return Optional.of(new QRCodeReader().decode(bitmap, hints).getText());
        } catch (ReaderException e) {
            return Optional.empty();
        }
    }

    /**
     * Decodes a QR code from an image file (manual fallback when no camera is available).
     *
     * @throws IOException if the file cannot be read or is not a supported image
     */
    public static Optional<String> decode(Path imageFile) throws IOException {
        BufferedImage image = ImageIO.read(imageFile.toFile());
        if (image == null) {
            throw new IOException("The selected file is not a supported image (use PNG, JPG, GIF, or BMP).");
        }
        return decode(image, true);
    }
}
