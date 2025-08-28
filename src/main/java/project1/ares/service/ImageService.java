package project1.ares.service;

import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ImageService {
    public Mono<byte[]> validateAndProcessImage(FilePart filePart) {
        return DataBufferUtils.join(filePart.content())
                .flatMap(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);

                    try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
                        BufferedImage image = ImageIO.read(inputStream);
                        if (image == null) {
                            return Mono.error(new RuntimeException("Invalid image file"));
                        }

                        // Verifica tamanho do arquivo
                        double sizeInMB = bytes.length / (1024.0 * 1024.0);
                        if (sizeInMB > 5.0) { // limite de 5MB
                            return Mono.error(new RuntimeException("File too large (> 5MB)"));
                        }

                        int width = image.getWidth();
                        int height = image.getHeight();

                        // Redimensiona se for maior que 500x500
                        if (width > 500 || height > 500) {
                            image = resizeImage(image, 500, 500);
                        }

                        // Sempre salva como PNG otimizado
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(image, "png", baos);

                        return Mono.just(baos.toByteArray());
                    } catch (IOException e) {
                        return Mono.error(new RuntimeException("Error processing image", e));
                    }
                });
    }



    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        Image resultingImage = originalImage.getScaledInstance(
                targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage outputImage = new BufferedImage(
                targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(resultingImage, 0, 0, null);
        g2d.dispose();

        return outputImage;
    }


    public Mono<Void> deleteCompanyFolder(String _path) {
        Path companyPath = Path.of(_path);
        return Mono.fromRunnable(() -> {
            try {
                if (Files.exists(companyPath)) {
                    // deleta recursivamente
                    Files.walk(companyPath)
                            .sorted((a, b) -> b.compareTo(a)) // primeiro filhos, depois pai
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException e) {
                                    throw new RuntimeException("Erro ao deletar: " + path, e);
                                }
                            });
                }
            } catch (IOException e) {
                throw new RuntimeException("Erro ao apagar pasta da empresa.", e);
            }
        });
    }
}
