package talkingpotatoes.pickplaceapi.file.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 업로드를 허용하는 이미지 형식과 파일 시그니처(매직 바이트)
 * <pre>
 *  - 확장자만으로는 파일 형식을 신뢰할 수 없으므로 파일 앞부분 바이트로 실제 형식을 판별한다.
 *  - 판별한 형식이 파일 확장자와 일치해야 이미지로 인정한다.
 * </pre>
 *
 * @author : 박지혁
 * @since : 2026/11/03
 */
public enum ImageFormat {
    JPEG(Set.of(".jpg", ".jpeg")) {
        @Override
        boolean matches(byte[] header) {
            return startsWith(header, 0, new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF});
        }
    },
    PNG(Set.of(".png")) {
        @Override
        boolean matches(byte[] header) {
            return startsWith(header, 0, new byte[] {(byte)0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        }
    },
    GIF(Set.of(".gif")) {
        @Override
        boolean matches(byte[] header) {
            return startsWith(header, 0, new byte[] {'G', 'I', 'F', '8', '7', 'a'})
                    || startsWith(header, 0, new byte[] {'G', 'I', 'F', '8', '9', 'a'});
        }
    },
    WEBP(Set.of(".webp")) {
        @Override
        boolean matches(byte[] header) { // RIFF????WEBP
            return startsWith(header, 0, new byte[] {'R', 'I', 'F', 'F'})
                    && startsWith(header, 8, new byte[] {'W', 'E', 'B', 'P'});
        }
    };

    /**
     * 형식 판별에 필요한 최대 헤더 길이 (WEBP: 12바이트)
     */
    public static final int HEADER_LENGTH = 12;

    private final Set<String> extensions;

    ImageFormat(Set<String> extensions) {
        this.extensions = extensions;
    }

    abstract boolean matches(byte[] header);

    /**
     * 파일 앞부분 바이트로 이미지 형식을 판별한다.
     *
     * @param header 파일 앞부분 바이트 (최대 {@link #HEADER_LENGTH})
     * @return 판별된 이미지 형식, 허용하는 이미지가 아니면 빈 값
     */
    public static Optional<ImageFormat> detect(byte[] header) {
        if (header == null || header.length == 0) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(format -> format.matches(header))
                .findFirst();
    }

    /**
     * 확장자가 이 형식에 해당하는지 확인한다. (대소문자 무시)
     */
    public boolean supportsExtension(String extension) {
        return extension != null && extensions.contains(extension.toLowerCase(Locale.ROOT));
    }

    private static boolean startsWith(byte[] header, int offset, byte[] signature) {
        if (header.length < offset + signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (header[offset + i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
