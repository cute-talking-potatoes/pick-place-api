package talkingpotatoes.pickplaceapi.file.domain;

import java.nio.file.Path;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 메타데이터 정보를 담은 객체
 *
 * @param dirPath      파일 저장 상위 경로
 * @param filePath     파일 실제 저장 경로
 * @param uuid         파일 관리번호
 * @param sequence     파일 순번
 * @param file         파일 정보
 * @param fileType     파일 타입
 * @param originalName 원본 파일명
 * @param storedName   파일 저장명
 * @param extension    파일 확장자
 * @author : 박지혁
 * @since : 2026/05/25
 */
public record FileMetadata(
        Path dirPath,
        Path filePath,
        String uuid,
        long sequence,
        MultipartFile file,
        FileType fileType,
        String originalName,
        String storedName,
        String extension
) {

    public static FileMetadata create(MultipartFile file, Path dirPath, String uuid, long sequence, FileType fileType) {
        String originalName = file.getOriginalFilename(); // 원본파일명
        String storedName = uuid + "_" + sequence; // 저장할 파일명 (UUID_순번)
        String fileExtension = getFileExtension(originalName); // 파일 확장자
        Path filePath = dirPath.resolve(storedName); // 파일 저장 경로

        return new FileMetadata(
                dirPath, filePath, uuid, sequence, file, fileType, originalName, storedName, fileExtension
        );
    }

    private static String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return ""; // 확장자가 없을 경우 빈 문자열 반환
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
