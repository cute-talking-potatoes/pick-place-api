package talkingpotatoes.pickplaceapi.file.service;

import static talkingpotatoes.pickplaceapi.global.exception.ExceptionCode.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import talkingpotatoes.pickplaceapi.file.domain.FileMetadata;
import talkingpotatoes.pickplaceapi.file.domain.prop.FileProp;
import talkingpotatoes.pickplaceapi.global.exception.FileException;

/**
 * 로컬 파일 저장
 *
 * @author : 박지혁
 * @since : 2026/07/02
 */
@Service
@RequiredArgsConstructor
public class LocalFileStorageService {

    private final FileProp fileProp;

    public Path createDirectory(String uuid) {
        Path dirPath = Paths.get(fileProp.getFilePath(), uuid);

        if (Files.exists(dirPath)) {
            return dirPath;
        }

        try {
            Files.createDirectories(dirPath);
            return dirPath;
        } catch (IOException e) {
            throw new FileException(ERR_FILE_CREATE_DIRECTORY, e.getMessage());
        }
    }

    public void save(FileMetadata fileMetadata) {
        try {
            Files.write(fileMetadata.filePath(), fileMetadata.file().getBytes());
        } catch (IOException e) {
            throw new FileException(ERR_FILE_SAVE, e.getMessage());
        }
    }

    public void delete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new FileException(ERR_FILE_DELETE, e.getMessage());
        }
    }

    public void deleteAll(List<Path> paths) {
        paths.forEach(this::delete);
    }

    public void validateExists(Path path) {
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new FileException(ERR_FILE_NOT_EXIST, path + "에 파일이 존재하지 않습니다.");
        }
    }
}
