package talkingpotatoes.pickplaceapi.file.service;

import static talkingpotatoes.pickplaceapi.global.exception.ExceptionCode.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import talkingpotatoes.pickplaceapi.file.domain.FileMetadata;
import talkingpotatoes.pickplaceapi.file.domain.FileType;
import talkingpotatoes.pickplaceapi.file.domain.ImageFormat;
import talkingpotatoes.pickplaceapi.file.domain.entity.File;
import talkingpotatoes.pickplaceapi.file.domain.entity.PhotoFile;
import talkingpotatoes.pickplaceapi.file.domain.prop.FileProp;
import talkingpotatoes.pickplaceapi.file.repository.FileRepository;
import talkingpotatoes.pickplaceapi.file.repository.PhotoFileRepository;
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
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileProp fileProp;
    private final UserInfoProvider userInfoProvider;
    private final FileRepository fileRepository;
    private final LocalFileStorageService localFileStorageService;
    private final FileMetadataService fileMetadataService;
    private final FileDownloadService fileDownloadService;
    private final PhotoFileRepository photoFileRepository;

    @Transactional
    public void upload(List<MultipartFile> files) {
        String uuid = UUID.randomUUID().toString();
        saveFile(uuid, 1, files, this::validate, fileMetadataService::saveUserFile);
    }

    @Transactional
    public void uploadPhoto(List<MultipartFile> files, Place place) {
        String uuid = UUID.randomUUID().toString();
        saveFile(uuid, 1, files, this::validateImage,
                fileMetadata -> fileMetadataService.savePhotoFile(fileMetadata, place));
    }

    @Transactional
    public void update(List<MultipartFile> files, String uuid) {
        checkUUID(uuid);

        long sequence = getFileSequence(uuid);
        saveFile(uuid, sequence, files, this::validate, fileMetadataService::saveUserFile);
    }

    @Transactional
    public void updatePhoto(List<MultipartFile> files, String uuid, Place place) {
        checkUUID(uuid);

        long sequence = getFileSequence(uuid);
        saveFile(uuid, sequence, files, this::validateImage,
                fileMetadata -> fileMetadataService.savePhotoFile(fileMetadata, place));
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

    /**
     * 장소에 연결된 사진(tb_photo_file)과 파일(tb_file)을 삭제한다.
     * <pre>
     *  - 이 메서드는 자체 인가 검사를 하지 않는다. 호출자가 먼저 장소 작성자 권한을 확인해야 한다.
     *    (장소 사진은 장소 작성자만 등록할 수 있다. 현재 호출자: PlaceService.deletePlace)
     *  - 동시 사진 추가를 막으려면 호출자가 장소 행을 쓰기 락으로 조회한 상태여야 한다.
     *  - DB 행은 현재 트랜잭션 안에서 삭제하고, 디스크 파일은 트랜잭션이 커밋된 이후에만 삭제한다.
     *  - 롤백되면 디스크 파일은 그대로 남는다.
     *  - 커밋 이후 디스크 파일 삭제 실패는 로그만 남기고 요청은 성공으로 처리한다.
     *  - DB 행은 테이블당 벌크 삭제 1회로 지운다. 벌크 삭제 후 영속성 컨텍스트가 비워지므로
     *    호출자가 들고 있던 엔티티(Place 등)는 준영속 상태가 된다.
     * </pre>
     *
     * @param place 삭제할 장소
     */
    @Transactional
    public void deletePlacePhotos(Place place) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) { // 커밋 시점을 알 수 없으면 아무것도 지우지 않는다
            throw new FileException(ERR_FILE_DELETE_INCORRECT_TRANSACTION);
        }

        List<File> filesToDelete = photoFileRepository.findByPlaceWithFile(place).stream()
                .map(PhotoFile::getFile)
                .toList();

        if (filesToDelete.isEmpty()) {
            return;
        }

        List<Path> filePaths = filesToDelete.stream()
                .map(file -> Path.of(file.getFileSrc()))
                .toList();

        registerAfterCommitFileCleanup(filePaths); // 커밋 후 디스크 파일 삭제 콜백 등록
        fileMetadataService.deleteAll(filesToDelete.stream().map(File::getFileSrl).toList());
    }

    // 파일 다운로드
    public Resource download(String uuid, List<Long> fileSeqList, HttpHeaders headers) {
        List<File> filesToDownload = fileRepository.findByUUIDAndFileSeq(uuid, fileSeqList);
        return fileDownloadService.download(uuid, fileSeqList, filesToDownload, headers);
    }

    private void saveFile(
            String uuid, long sequence, List<MultipartFile> files,
            Consumer<FileMetadata> validator, Consumer<FileMetadata> saveMethod
    ) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new FileException(ERR_FILE_SAVE_INCORRECT_TRANSACTION);
        }

        // 1. 디스크에 쓰기 전에 모든 파일을 검증한다. (거부된 업로드는 디렉토리도 만들지 않는다)
        Path dirPath = localFileStorageService.resolveDirectory(uuid);
        List<FileMetadata> metadataList = new ArrayList<>();
        for (MultipartFile file : files) {
            FileMetadata fileMetadata = FileMetadata.create(file, dirPath, uuid, sequence, FileType.USER);
            validator.accept(fileMetadata);
            metadataList.add(fileMetadata);
            sequence++;
        }

        // 2. 검증을 모두 통과한 뒤에만 디렉토리를 만들고 저장한다.
        localFileStorageService.createDirectory(uuid);
        List<Path> savedFilePaths = new ArrayList<>();
        registerRollbackFileCleanup(dirPath, savedFilePaths); // 롤백 발생 시 파일·빈 디렉토리 삭제 콜백 등록

        for (FileMetadata fileMetadata : metadataList) {
            savedFilePaths.add(fileMetadata.filePath());
            localFileStorageService.save(fileMetadata);
            saveMethod.accept(fileMetadata);
        }
    }

    private void registerRollbackFileCleanup(Path dirPath, List<Path> savedFilePaths) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) { // 커밋이 아닐땐 모두 롤백처리
                    savedFilePaths.forEach(localFileStorageService::deleteQuietly);
                    localFileStorageService.deleteDirectoryIfEmpty(dirPath); // 이번 업로드로 비게 된 디렉토리 정리
                }
            }
        });
    }

    private void registerAfterCommitFileCleanup(List<Path> filePaths) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() { // 커밋된 경우에만 디스크 파일 삭제, 실패해도 예외를 던지지 않는다
                filePaths.forEach(localFileStorageService::deleteQuietly);
            }
        });
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

    private void validate(FileMetadata fileMetadata) {
        if (!fileProp.getFileTypes().contains(fileMetadata.extension())) {
            throw new FileException(ERR_FILE_EXTENSION_NOT_MATCH);
        }
    }

    /**
     * 장소 사진용 이미지 검증
     * <pre>
     *  1. 확장자가 이미지 화이트리스트(file.imageTypes)에 있는지 확인한다. (대소문자 무시, 미설정 시 모두 거부)
     *  2. 파일 앞부분 시그니처로 실제 이미지 형식을 판별하고, 확장자와 일치하는지 확인한다.
     * </pre>
     */
    private void validateImage(FileMetadata fileMetadata) {
        String extension = fileMetadata.extension().toLowerCase(Locale.ROOT);
        List<String> imageTypes = fileProp.getImageTypes();
        boolean allowed = imageTypes != null && imageTypes.stream()
                .anyMatch(imageType -> imageType != null && imageType.toLowerCase(Locale.ROOT).equals(extension));

        if (extension.isEmpty() || !allowed) {
            throw new FileException(ERR_FILE_EXTENSION_NOT_MATCH);
        }

        byte[] header = readHeader(fileMetadata.file());
        boolean validImage = ImageFormat.detect(header)
                .filter(format -> format.supportsExtension(extension))
                .isPresent();

        if (!validImage) {
            throw new FileException(ERR_FILE_INVALID_IMAGE);
        }
    }

    private byte[] readHeader(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return inputStream.readNBytes(ImageFormat.HEADER_LENGTH);
        } catch (IOException e) {
            log.warn("업로드 파일 헤더를 읽는데 실패했습니다.", e);
            throw new FileException(ERR_FILE_INVALID_IMAGE);
        }
    }
}
