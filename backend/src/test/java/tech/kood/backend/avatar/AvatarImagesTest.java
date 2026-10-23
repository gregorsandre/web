package tech.kood.backend.avatar;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AvatarImagesTest {

    static byte[] bytes(int... values) {
        byte[] out = new byte[values.length];
        for (int i = 0; i < values.length; i++) out[i] = (byte) values[i];
        return out;
    }

    @Test
    void recognisesJpegPngAndWebp() {
        assertEquals(Optional.of("image/jpeg"), AvatarImages.detectType(bytes(0xFF, 0xD8, 0xFF, 0xE0, 0, 0)));
        assertEquals(Optional.of("image/png"), AvatarImages.detectType(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0)));
        assertEquals(Optional.of("image/webp"), AvatarImages.detectType("RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1)));
    }

    @Test
    void refusesEverythingElse() {
        assertEquals(Optional.empty(), AvatarImages.detectType(new byte[0]));
        assertEquals(Optional.empty(), AvatarImages.detectType("hello, not a picture".getBytes(StandardCharsets.UTF_8)));
        assertEquals(Optional.empty(), AvatarImages.detectType("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8)));
        assertEquals(Optional.empty(), AvatarImages.detectType("RIFF\0\0\0\0WAVE".getBytes(StandardCharsets.ISO_8859_1)));
        assertEquals(Optional.empty(), AvatarImages.detectType(bytes(0xFF, 0xD8)));
    }
}
