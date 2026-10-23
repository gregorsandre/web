package tech.kood.backend.avatar;

import java.util.Optional;

// Works out a picture's type from its first bytes. The file name and content type
// the browser sends are not trusted, and SVG (which can carry scripts) never matches.
final class AvatarImages {
    static final long MAX_BYTES = 2 * 1024 * 1024;

    private AvatarImages() {}

    static Optional<String> detectType(byte[] data) {
        if (startsWith(data, 0, 0xFF, 0xD8, 0xFF)) {
            return Optional.of("image/jpeg");
        }
        if (startsWith(data, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return Optional.of("image/png");
        }
        if (startsWith(data, 0, 'R', 'I', 'F', 'F') && startsWith(data, 8, 'W', 'E', 'B', 'P')) {
            return Optional.of("image/webp");
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] data, int offset, int... expected) {
        if (data.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((data[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }
}
