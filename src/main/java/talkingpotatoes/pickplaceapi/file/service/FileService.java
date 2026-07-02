package talkingpotatoes.pickplaceapi.file.service;

import static talkingpotatoes.pickplaceapi.global.exception.ExceptionCode.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import talkingpotatoes.pickplaceapi.file.domain.FileMetadata;
import talkingpotatoes.pickplaceapi.file.domain.FileType;
import talkingpotatoes.pickplaceapi.file.domain.entity.File;
import talkingpotatoes.pickplaceapi.file.domain.prop.FileProp;
import talkingpotatoes.pickplaceapi.file.repository.FileRepository;
import talkingpotatoes.pickplaceapi.global.exception.ExceptionCode;
import talkingpotatoes.pickplaceapi.global.exception.FileException;
import talkingpotatoes.pickplaceapi.global.security.UserInfoProvider;
import talkingpotatoes.pickplaceapi.place.domain.entity.Place;

/**
 * 파일 Service
 *
 * @author : 박지혁
 * @since : 2026/04/12
 */
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileProp fileProp;
    private final UserInfoProvider userInfoProvider;
    private final FileRepository fileRepository;
    private final LocalFileStorageService localFileStorageService;
    private final FileMetadataService fileMetadataService;
    private final FileDownloadService fileDownloadService;

    @Transactional
    public void upload(List<MultipartFile> files) {
        String uuid = UUID.randomUUID().toString();
        saveFile(uuid, 1, files, fileMetadataService::saveUserFile);
    }

    @Transactional
    public void uploadPhoto(List<MultipartFile> files, Place place) {
        String uuid = UUID.randomUUID().toString();
        saveFile(uuid, 1, files, fileMetadata -> fileMetadataService.savePhotoFile(fileMetadata, place));
    }

    @Transactional
    public void update(List<MultipartFile> files, String uuid) {
        checkUUID(uuid);

        long sequence = getFileSequence(uuid);
        saveFile(uuid, sequence, files, fileMetadataService::saveUserFile);
    }

    @Transactional
    public void updatePhoto(List<MultipartFile> files, String uuid, Place place) {
        checkUUID(uuid);

        long sequence = getFileSequence(uuid);
        saveFile(uuid, sequence, files, fileMetadata -> fileMetadataService.savePhotoFile(fileMetadata, place));
    }

    // 파일 삭제
    @Transactional
    public void delete(String uuid, List<Long> fileSeqList) {
        List<File> filesToDelete = fileRepository.findByUUIDAndFileSeq(uuid, fileSeqList);

        if (filesToDelete.isEmpty()) {
            return;
        }

        filesToDelete.forEach(this::deleteFile);
    }

    // 파일 다운로드
    public Resource download(String uuid, List<Long> fileSeqList, HttpHeaders headers) {
        List<File> filesToDownload = fileRepository.findByUUIDAndFileSeq(uuid, fileSeqList);
        return fileDownloadService.download(uuid, fileSeqList, filesToDownload, headers);
    }

    private void saveFile(String uuid, long sequence, List<MultipartFile> files, Consumer<FileMetadata> saveMethod) {
        Path dirPath = localFileStorageService.createDirectory(uuid);
        List<Path> savedFilePaths = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                FileMetadata fileMetadata = FileMetadata.create(file, dirPath, uuid, sequence, FileType.USER);
                validate(fileMetadata.extension());

                savedFilePaths.add(fileMetadata.filePath());
                localFileStorageService.save(fileMetadata);
                saveMethod.accept(fileMetadata);

                sequence++;
            }
        } catch (RuntimeException e) {
            localFileStorageService.deleteAll(savedFilePaths);
            throw e;
        }
    }

    private long getFileSequence(String uuid) {
        List<File> fileList = fileRepository.findByUUID(uuid);

        return fileList.stream() // 존재한다면 해당 fileSeq + 1 존재하지 않는다면 수정할 수 없는 파일 / 최초 업로드부터 수행 필수
                .max(Comparator.comparing(File::getFileSeq)) // 저장된 fileSeq 중 가장 큰 값을 찾기
                .map(f -> {
                    validateOwner(f, ERR_FILE_UPDATE, "파일을 추가할 권한이 존재하지 않습니다.");
                    return f.getFileSeq() + 1;
                })
                .orElseThrow(() -> new FileException(ERR_FILE_UPDATE));
    }

    private void deleteFile(File file) {
        validateOwner(file, ERR_FILE_DELETE, "파일을 삭제할 권한이 존재하지 않습니다.");
        localFileStorageService.delete(Path.of(file.getFileSrc()));
        fileMetadataService.delete(file);
    }

    private void checkUUID(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            throw new FileException(ERR_FILE_UPDATE);
        }
        try {
            UUID.fromString(uuid); // UUID가 유효한지 검증
        } catch (IllegalArgumentException e) { // 유효하지 않다면 예외 발생
            throw new FileException(ERR_FILE_UPDATE);
        }
    }

    private void validateOwner(File file, ExceptionCode exceptionCode, String message) {
        if (!userInfoProvider.getUserId().equals(file.getUser().getUserId())) {
            throw new FileException(exceptionCode, message);
        }
    }

    private void validate(String extension) {
        if (!fileProp.getFileTypes().contains(extension)) {
            throw new FileException(ERR_FILE_EXTENSION_NOT_MATCH);
        }
    }
}
