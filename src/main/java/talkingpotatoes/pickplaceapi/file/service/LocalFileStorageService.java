package talkingpotatoes.pickplaceapi.file.service;

import static talkingpotatoes.pickplaceapi.global.exception.ExceptionCode.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import talkingpotatoes.pickplaceapi.file.domain.FileMetadata;
import talkingpotatoes.pickplaceapi.file.domain.prop.FileProp;
import talkingpotatoes.pickplaceapi.global.exception.FileException;

/**
 * 로컬 파일 저장
 * <pre>
 *  - 클라이언트로 나가는 예외 메시지에는 서버 경로나 IOException 메시지를 넣지 않는다.
 *  - 상세 원인(경로, 예외)은 서버 로그로만 남긴다.
 * </pre>
 *
 * @author : 박지혁
 * @since : 2026/07/02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageService {

    private final FileProp fileProp;

    /**
     * 파일 관리번호(UUID)에 해당하는 저장 디렉토리 경로를 반환한다. (디렉토리를 만들지 않는다)
     */
    public Path resolveDirectory(String uuid) {
        return Paths.get(fileProp.getFilePath(), uuid);
    }

    public Path createDirectory(String uuid) {
        Path dirPath = resolveDirectory(uuid);

        if (Files.exists(dirPath)) {
            return dirPath;
        }

        try {
            Files.createDirectories(dirPath);
            return dirPath;
        } catch (IOException e) {
            log.error("파일 폴더 생성에 실패했습니다. 경로: {}", dirPath, e);
            throw new FileException(ERR_FILE_CREATE_DIRECTORY);
        }
    }

    public void save(FileMetadata fileMetadata) {
        try {
            Files.write(fileMetadata.filePath(), fileMetadata.file().getBytes());
        } catch (IOException e) {
            log.error("파일 저장에 실패했습니다. 경로: {}", fileMetadata.filePath(), e);
            throw new FileException(ERR_FILE_SAVE);
        }
    }

    public void delete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("파일 삭제에 실패했습니다. 경로: {}", path, e);
            throw new FileException(ERR_FILE_DELETE);
        }
    }

    public void deleteAll(List<Path> paths) {
        paths.forEach(this::delete);
    }

    /**
     * 파일 삭제를 시도하고, 실패하면 예외를 던지지 않고 로그만 남긴다.
     * <pre>
     *  - 트랜잭션 커밋 이후 정리 작업처럼 실패가 요청 결과에 영향을 주면 안 되는 경우에 사용한다.
     * </pre>
     *
     * @return 삭제 성공(또는 이미 없음) 여부
     */
    public boolean deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
            return true;
        } catch (IOException | RuntimeException e) {
            log.warn("파일 삭제에 실패했습니다. 수동 정리가 필요합니다. 경로: {}", path, e);
            return false;
        }
    }

    /**
     * 디렉토리가 비어있으면 삭제한다. 실패해도 예외를 던지지 않고 로그만 남긴다.
     * <pre>
     *  - 업로드 롤백 시 새로 만든 빈 UUID 디렉토리를 정리할 때 사용한다.
     * </pre>
     */
    public void deleteDirectoryIfEmpty(Path dirPath) {
        if (!Files.isDirectory(dirPath)) {
            return;
        }
        try (Stream<Path> children = Files.list(dirPath)) {
            if (children.findAny().isPresent()) {
                return;
            }
            Files.deleteIfExists(dirPath);
        } catch (IOException | RuntimeException e) {
            log.warn("빈 디렉토리 삭제에 실패했습니다. 경로: {}", dirPath, e);
        }
    }

    public void validateExists(Path path) {
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            log.warn("요청한 파일이 존재하지 않습니다. 경로: {}", path);
            throw new FileException(ERR_FILE_NOT_EXIST);
        }
    }
}
