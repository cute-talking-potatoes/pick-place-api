package talkingpotatoes.pickplaceapi.file.service;

import static talkingpotatoes.pickplaceapi.global.exception.ExceptionCode.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import talkingpotatoes.pickplaceapi.file.domain.FileMetadata;
import talkingpotatoes.pickplaceapi.file.domain.FileType;
import talkingpotatoes.pickplaceapi.file.domain.entity.File;
import talkingpotatoes.pickplaceapi.file.domain.entity.PhotoFile;
import talkingpotatoes.pickplaceapi.file.domain.entity.UserFile;
import talkingpotatoes.pickplaceapi.file.domain.prop.FileProp;
import talkingpotatoes.pickplaceapi.file.repository.FileRepository;
import talkingpotatoes.pickplaceapi.file.repository.PhotoFileRepository;
import talkingpotatoes.pickplaceapi.file.repository.UserFileRepository;
import talkingpotatoes.pickplaceapi.global.exception.FileException;
import talkingpotatoes.pickplaceapi.global.security.UserInfoProvider;
import talkingpotatoes.pickplaceapi.user.domain.entity.User;

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
    private final PhotoFileRepository photoFileRepository;
    private final UserFileRepository userFileRepository;

    /**
     * 파일 업로드
     *
     * @param files    업로드할 파일들
     * @param fileType 업로드할 파일 구분
     */
    @Transactional
    public void upload(List<MultipartFile> files, FileType fileType) {
        String uuid = UUID.randomUUID().toString(); // 파일 관리 번호

        Path dirPath = Paths.get(fileProp.getFilePath(), uuid); // UUID 폴더

        try {
            createDirPath(dirPath);
        } catch (IOException e) {
            throw new FileException(ERR_FILE_CREATE_DIRECTORY, e.getMessage());
        }

        saveFile(dirPath, uuid, 1, files, fileType);
    }

    /**
     * 파일 수정 (추가 업로드)
     *
     * @param files    업로드할 파일들
     * @param fileType 업로드할 파일 구분
     * @param uuid     업로드할 파일 UUID
     */
    @Transactional
    public void update(List<MultipartFile> files, FileType fileType, String uuid) {
        checkUUID(uuid);

        List<File> fileList = fileRepository.findByUUID(uuid);
        long sequence = fileList.stream()
                .max(Comparator.comparing(File::getFileSeq)) // 저장된 fileSeq 중 가장 큰 값을 찾기
                .map(f -> {
                    if (!hasAuth(f)) {
                        throw new FileException(ERR_FILE_UPDATE, "파일을 추가할 권한이 존재하지 않습니다.");
                    }
                    return f.getFileSeq() + 1;
                })
                .orElseThrow(() -> new FileException(
                        ERR_FILE_SAVE)); // 존재한다면 해당 fileSeq + 1 존재하지 않는다면 수정할 수 없는 파일 / 최초 업로드부터 수행 필수

        Path dirPath = Paths.get(fileProp.getFilePath(), uuid); // UUID 폴더

        try {
            createDirPath(dirPath);
        } catch (IOException e) {
            throw new FileException(ERR_FILE_CREATE_DIRECTORY, e.getMessage());
        }

        saveFile(dirPath, uuid, sequence, files, fileType);
    }

    // 파일 삭제
    @Transactional
    public void delete(String uuid, List<Long> fileSeqList) {
        List<File> filesToDelete = fileRepository.findByUUIDAndFileSeq(uuid, fileSeqList);

        if (filesToDelete.isEmpty()) { // 삭제할 파일이 존재하지 않는다면
            return;
        }

        filesToDelete.forEach(this::deleteFile); // 파일 삭제
    }

    // 파일 다운로드
    public Resource download(String uuid, List<Long> fileSeqList, HttpHeaders headers) {
        List<File> filesToDownload = fileRepository.findByUUIDAndFileSeq(uuid, fileSeqList);
        checkFileList(fileSeqList, filesToDownload);

        if (filesToDownload.size() == 1) { // 다운로드 할 파일이 하나라면
            return downloadFileSingle(headers, filesToDownload);
        }

        return downloadFileZip(uuid, headers, filesToDownload);
    }

    /**
     * 파일 저장 로직
     *
     * @param dirPath  파일 저장할 폴더 경로
     * @param uuid     파일 관리 번호
     * @param sequence 파일 순번
     * @param files    파일 리스트
     * @param fileType 저장할 파일 타입
     */
    private void saveFile(Path dirPath, String uuid, long sequence, List<MultipartFile> files, FileType fileType) {
        List<Path> successfullySavedFiles = new ArrayList<>(); // 저장에 성공한 파일 경로 (한건이라도 실패하면 모두 삭제하기 위해 사용)

        for (MultipartFile file : files) {
            FileMetadata fileMetadata = FileMetadata.create(file, dirPath, uuid, sequence, fileType);
            checkFileExtension(fileMetadata.extension(), successfullySavedFiles);
            savePhysicalFile(fileMetadata);
            saveFileMetadata(
                    fileMetadata); // TODO: 2026/05/25 여기서 예외가 발생하면 물리 파일 삭제 로직 필요 (스케줄러로 파일 정보 읽어서 매칭안되는 파일 제거하는 로직 추가?)
            successfullySavedFiles.add(fileMetadata.filePath());
            sequence++; // 파일 순번 증가
        }
    }

    private void saveFileMetadata(FileMetadata fileMetadata) {
        User user = userInfoProvider.getUser();
        File fileEntity = File.builder()
                .user(user)
                .fileNm(fileMetadata.originalName())
                .fileSrc(fileMetadata.filePath().toString())
                .fileExtension(fileMetadata.extension())
                .uploadedAt(LocalDateTime.now())
                .fileManageSrl(fileMetadata.uuid())
                .fileSeq(fileMetadata.sequence())
                .build();
        fileRepository.save(fileEntity);

        if (fileMetadata.fileType() == FileType.PHOTO) { // 사진 파일이라면
            PhotoFile photoFile = PhotoFile.builder()
                    .file(fileEntity)
                    .user(user)
                    .build();
            photoFileRepository.save(photoFile);
        } else if (fileMetadata.fileType() == FileType.USER) { // 회원 파일이라면
            UserFile userFile = UserFile.builder()
                    .file(fileEntity)
                    .user(user)
                    .build();
            userFileRepository.save(userFile);
        }
    }

    // 해당 경로에 파일이 존재하는지 확인하고 존재하지 않는다면 폴더를 생성한다.
    private void createDirPath(Path dirPath) throws IOException {
        if (!Files.exists(dirPath)) { // 폴더가 존재하지 않는다면
            Files.createDirectories(dirPath); // 폴더 생성
        }
    }

    private void savePhysicalFile(FileMetadata fileMetadata) {
        try {
            Files.write(fileMetadata.filePath(), fileMetadata.file().getBytes()); // 파일 저장
        } catch (IOException e) {
            throw new FileException(ERR_FILE_SAVE, e.getMessage());
        }
    }

    private void deleteFile(File file) {
        if (!hasAuth(file)) {
            throw new FileException(ERR_FILE_DELETE, "파일을 삭제할 권한이 존재하지 않습니다.");
        }
        Path path = Paths.get(file.getFileSrc());

        try {
            Files.deleteIfExists(path); // 물리적인 파일 삭제
            deleteDBFile(file); // 매핑된 DB 데이터 삭제
        } catch (IOException e) {
            throw new FileException(ERR_FILE_DELETE, e.getMessage());
        }
    }

    private boolean hasAuth(File file) {
        return userInfoProvider.getUserId().equals(file.getUser().getUserId());
    }

    private void deleteDBFile(File file) {
        // UUID는 각 타입간 중복될 수 없으므로 존재하는 하나의 타입에 대해서만 삭제처리
        userFileRepository.deleteByFileSrl(file.getFileSrl()); // 회원 파일일 경우 삭제
        photoFileRepository.deleteByFileSrl(file.getFileSrl()); // 사진 파일일 경우 삭제

        fileRepository.delete(file); // 파일 테이블 데이터 삭제
    }

    private void deletePhysicalFileList(List<Path> successfullySavedFiles) {
        successfullySavedFiles.forEach(this::deletePhysicalFile);
    }

    private void deletePhysicalFile(Path p) {
        try {
            Files.deleteIfExists(p); // 파일 롤백 - 이미 저장된 파일 삭제
        } catch (IOException e) {
            throw new FileException(ERR_FILE_DELETE, e.getMessage());
        }
    }

    private boolean isValidExtension(String fileExt) {
        return fileProp.getFileTypes().contains(fileExt);
    }

    private void checkFileExtension(String extension, List<Path> successfullySavedFiles) {
        if (isValidExtension(extension)) { // 유효한 파일 확장자라면 바로 리턴
            return;
        }
        deletePhysicalFileList(successfullySavedFiles); // 유효한 파일 확장자가 아니라면 삭제
        throw new FileException(ERR_FILE_EXTENSION_NOT_MATCH); // 파일 에러 호출
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

    private Resource downloadFileSingle(HttpHeaders headers, List<File> filesToDownload) {
        File file = filesToDownload.get(0);
        Path path = Paths.get(file.getFileSrc());
        checkFilePath(path);
        return downloadFile(headers, path, file);
    }

    private UrlResource downloadFileZip(String uuid, HttpHeaders headers, List<File> filesToDownload) {
        try {
            Path tempZipPath = Files.createTempFile(uuid,
                    ".zip"); // TODO: 2026/05/25 파일 다운로드 후 해당 파일 삭제 로직 추가 필요 (스케줄러)
            makeZipFile(filesToDownload, tempZipPath);
            String filename = UUID.randomUUID() + ".zip";
            setUpDownloadHeader(filename, headers, tempZipPath);
            headers.add(HttpHeaders.CONTENT_TYPE, "application/zip");
            return new UrlResource(tempZipPath.toUri());
        } catch (IOException e) {
            throw new FileException(ERR_FILE_FAIL_DOWNLOAD, e.getMessage());
        }
    }

    private void makeZipFile(List<File> filesToDownload, Path tempZipPath) {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(tempZipPath))) {
            Map<String, Integer> filenameCountMap = new HashMap<>();
            byte[] buffer = new byte[8192];

            for (File file : filesToDownload) {
                Path filePath = Paths.get(file.getFileSrc());
                checkFilePath(filePath);
                String fileName = generateUniqueFilename(file, filenameCountMap);
                writeZipStream(zipOutputStream, fileName, filePath, buffer);
            }
            zipOutputStream.finish();
        } catch (IOException e) {
            throw new FileException(ERR_FILE_FAIL_DOWNLOAD, e.getMessage());
        }
    }

    private void writeZipStream(
            ZipOutputStream zipOutputStream, String fileName, Path filePath, byte[] buffer
    ) throws IOException {

        zipOutputStream.putNextEntry(new ZipEntry(fileName));
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                zipOutputStream.write(buffer, 0, bytesRead);
            }
        }
        zipOutputStream.closeEntry();
    }

    private Resource downloadFile(HttpHeaders headers, Path path, File file) {
        try {
            Resource resource = new UrlResource(path.toUri());

            setUpDownloadHeader(file.getFileNm(), headers, path);
            headers.add(HttpHeaders.CONTENT_TYPE, "application/octet-stream");

            return resource;
        } catch (IOException e) {
            throw new FileException(ERR_FILE_FAIL_DOWNLOAD, e.getMessage());
        }
    }

    // 원본 파일명으로 다운로드 할 수 있게 헤더 설정
    private void setUpDownloadHeader(String file, HttpHeaders headers, Path path) throws IOException {
        String filename = URLEncoder.encode(file, StandardCharsets.UTF_8).replace("+", "%20");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(Files.size(path)));
    }

    private void checkFileList(List<Long> fileSeqList, List<File> filesToDownload) {
        if (filesToDownload.isEmpty()) {
            throw new FileException(ERR_FILE_NOT_EXIST);
        }

        if (fileSeqList.size() != filesToDownload.size()) {
            throw new FileException(ERR_FILE_CANNOT_DOWNLOAD, "요청한 파일과 실제 저장된 파일의 개수가 일치하지 않습니다.");
        }
    }

    private void checkFilePath(Path path) {
        if (!Files.exists(path) || !Files.isRegularFile(path)) { // 파일이 존재하지 않거나 파일 형태가 아니라면
            throw new FileException(ERR_FILE_NOT_EXIST, path + "에 파일이 존재하지 않습니다.");
        }
    }

    private String generateUniqueFilename(File file, Map<String, Integer> fileMap) {
        String filename = file.getFileNm();

        if (fileMap.containsKey(filename)) { // 중복 파일명 처리
            int count = fileMap.get(filename);
            fileMap.put(filename, count + 1);
            return appendDuplicateSuffix(filename, count);
        }

        fileMap.put(filename, 1);
        return filename;
    }

    private String appendDuplicateSuffix(String filename, int count) {
        int idx = filename.lastIndexOf(".");
        if (idx > 0) {
            return filename.substring(0, idx) + "(" + count + ")" + filename.substring(idx);
        }
        return filename + "(" + count + ")";
    }
}
