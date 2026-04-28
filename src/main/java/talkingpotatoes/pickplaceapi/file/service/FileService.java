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
import talkingpotatoes.pickplaceapi.file.domain.FileType;
import talkingpotatoes.pickplaceapi.file.domain.entity.File;
import talkingpotatoes.pickplaceapi.file.domain.entity.PhotoFile;
import talkingpotatoes.pickplaceapi.file.domain.entity.UserFile;
import talkingpotatoes.pickplaceapi.file.domain.prop.FileProp;
import talkingpotatoes.pickplaceapi.file.repository.FileRepository;
import talkingpotatoes.pickplaceapi.file.repository.PhotoFileRepository;
import talkingpotatoes.pickplaceapi.file.repository.UserFileRepository;
import talkingpotatoes.pickplaceapi.global.exception.FileException;

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

    private final FileRepository fileRepository;
    private final PhotoFileRepository photoFileRepository;
    private final UserFileRepository userFileRepository;

    /**
     * 파일 업로드
     *
     * @param files    업로드할 파일들
     * @param fileType 업로드할 파일 구분
     * @param uuid     업로드할 파일 UUID (존재하고, 해당 UUID로 저장된 파일이 존재한다면 파일 수정, 없거나 파일이 존재하지 않는다면 새로운 파일 업로드)
     */
    @Transactional
    public void upload(List<MultipartFile> files, FileType fileType, String uuid) {
        long sequence = 1;
        if (uuid == null || uuid.isBlank()) {
            uuid = UUID.randomUUID().toString(); // 파일 관리 번호
        } else {
            try {
                UUID.fromString(uuid); // UUID가 유효한지 검증
            } catch (IllegalArgumentException e) { // 유효하지 않다면 새로운 UUID 생성
                uuid = UUID.randomUUID().toString();
            }
            List<File> fileList = fileRepository.findByUUID(uuid);
            sequence = fileList.stream()
                    .max(Comparator.comparing(File::getFileSeq)) // 저장된 fileSeq 중 가장 큰 값을 찾기
                    .map(f -> f.getFileSeq() + 1).orElse(1L); // 존재한다면 해당 fileSeq + 1 없다면 다시 1부터 시작
        }

        Path dirPath = Paths.get(fileProp.getFilePath(), uuid); // UUID 폴더

        try {
            if (!Files.exists(dirPath)) { // 폴더가 존재하지 않는다면
                Files.createDirectories(dirPath); // 폴더 생성
            }
        } catch (IOException e) {
            throw new FileException(ERR_FILE_CREATE_DIRECTORY, e.getMessage());
        }

        // 저장에 성공한 파일 경로 (한건이라도 실패하면 모두 삭제하기 위해 사용)
        List<Path> successfullySavedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename(); // 원본파일명
            String storedName = uuid + "_" + sequence; // 저장할 파일명 (UUID_순번)
            String fileExt = getFileType(originalName);
            boolean checkExt = fileProp.getFileTypes().contains(fileExt);

            if (!checkExt) {
                // 파일 롤백 - 이미 저장된 파일 삭제
                successfullySavedFiles.forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException e) {
                        throw new FileException(ERR_FILE_DELETE, e.getMessage());
                    }
                });
                // 파일 에러 호출
                throw new FileException(ERR_FILE_EXTENSION_NOT_MATCH);
            }

            Path filePath = dirPath.resolve(storedName);

            try {
                Files.write(filePath, file.getBytes()); // 파일 저장
                successfullySavedFiles.add(filePath); // 성공한 파일에 추가
            } catch (IOException e) {
                throw new FileException(ERR_FILE_SAVE, e.getMessage());
            }

            File fileEntity = File.builder()
                    // .user() TODO: 2026/04/12 회원 기능이 개발되면 회원에 관련된 로직 추가하기
                    .fileNm(originalName)
                    .fileSrc(filePath.toString())
                    .fileExtension(fileExt)
                    .uploadedAt(LocalDateTime.now())
                    .fileManageSrl(uuid)
                    .fileSeq(sequence)
                    .build();

            fileRepository.save(fileEntity);
            sequence++; // 파일 순번 증가

            if (fileType == FileType.PHOTO) { // 사진 파일이라면
                PhotoFile photoFile = PhotoFile.builder()
                        .file(fileEntity)
                        // .user() TODO: 2026/04/28 회원 기능이 개발되면 회원에 관련된 로직 추가하기
                        .build();
                photoFileRepository.save(photoFile);
            } else if (fileType == FileType.USER) { // 회원 파일이라면
                UserFile userFile = UserFile.builder()
                        .file(fileEntity)
                        // .user() TODO: 2026/04/28 회원 기능이 개발되면 회원에 관련된 로직 추가하기
                        .build();
                userFileRepository.save(userFile);
            }
        }
    }

    // 파일 삭제
    @Transactional
    public void delete(String uuid, List<Long> fileSeqList) {
        List<File> filesToDelete = fileRepository.findByUUIDAndFileSeq(uuid, fileSeqList);

        if (filesToDelete.isEmpty()) { // 삭제할 파일이 존재하지 않는다면
            return;
        }

        // 파일 삭제
        filesToDelete.forEach(this::deleteFile);
    }

    // 파일 다운로드
    public Resource download(String uuid, List<Long> fileSeqList, HttpHeaders headers) {
        List<File> filesToDownload = fileRepository.findByUUIDAndFileSeq(uuid, fileSeqList);

        if (filesToDownload.isEmpty()) {
            throw new FileException(ERR_FILE_NOT_EXIST);
        }

        if (fileSeqList.size() != filesToDownload.size()) {
            throw new FileException(ERR_FILE_CANNOT_DOWNLOAD, "요청한 파일과 실제 저장된 파일의 개수가 일치하지 않습니다.");
        }

        if (filesToDownload.size() == 1) { // 다운로드 할 파일이 하나라면
            File file = filesToDownload.get(0);

            Path path = Paths.get(file.getFileSrc());

            if (!Files.exists(path) || !Files.isRegularFile(path)) { // 파일이 존재하지 않거나 파일 형태가 아니라면
                throw new FileException(ERR_FILE_NOT_EXIST, path + "에 파일이 존재하지 않습니다.");
            }

            try {
                Resource resource = new UrlResource(path.toUri());

                String encodedFileName = URLEncoder.encode(file.getFileNm(), StandardCharsets.UTF_8)
                        .replace("+", "%20");

                // 원본 파일명으로 다운로드 할 수 있게 설정
                headers.add(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFileName + "\"");
                headers.add(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
                headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(Files.size(path)));

                return resource;
            } catch (IOException e) {
                throw new FileException(ERR_FILE_FAIL_DOWNLOAD, e.getMessage());
            }
        }

        // 다운로드 할 파일이 여러개라면 zip 형식으로 반환
        try {
            Path tempZipPath = Files.createTempFile(uuid, ".zip");

            try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(tempZipPath))) {
                Map<String, Integer> fileNameCountMap = new HashMap<>();
                byte[] buffer = new byte[8192];

                for (File file : filesToDownload) {
                    Path filePath = Paths.get(file.getFileSrc());

                    if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) { // 파일이 존재하지 않거나 파일 형태가 아니라면
                        throw new FileException(ERR_FILE_NOT_EXIST, filePath + "에 파일이 존재하지 않습니다.");
                    }

                    String fileName = file.getFileNm();

                    // 중복 파일명 처리
                    if (fileNameCountMap.containsKey(fileName)) {
                        int count = fileNameCountMap.get(fileName);
                        fileNameCountMap.put(fileName, count + 1);

                        int dotIndex = fileName.lastIndexOf(".");
                        if (dotIndex > 0) {
                            String name = fileName.substring(0, dotIndex);
                            String ext = fileName.substring(dotIndex);
                            fileName = name + "(" + count + ")" + ext;
                        } else {
                            fileName = fileName + "(" + count + ")";
                        }
                    } else {
                        fileNameCountMap.put(fileName, 1);
                    }

                    zipOutputStream.putNextEntry(new ZipEntry(fileName));

                    try (InputStream inputStream = Files.newInputStream(filePath)) {
                        int bytesRead;

                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            zipOutputStream.write(buffer, 0, bytesRead);
                        }
                    }
                    zipOutputStream.closeEntry();
                }
                zipOutputStream.finish();
            } catch (IOException e) {
                throw new FileException(ERR_FILE_FAIL_DOWNLOAD, e.getMessage());
            }

            String zipFileName = URLEncoder.encode(UUID.randomUUID() + ".zip", StandardCharsets.UTF_8)
                    .replace("+", "%20");

            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFileName + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/zip");
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(Files.size(tempZipPath)));

            return new UrlResource(tempZipPath.toUri());
        } catch (IOException e) {
            throw new FileException(ERR_FILE_FAIL_DOWNLOAD, e.getMessage());
        }
    }

    /**
     * 파일의 확장자를 분리해오는 메서드
     *
     * @param fileName 전체 파일명
     * @return 분리된 파일 확장자
     */
    private String getFileType(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return ""; // 확장자가 없을 경우 빈 문자열 반환
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * 파일 삭제
     *
     * @param file 파일 엔티티
     */
    private void deleteFile(File file) {
        Path path = Paths.get(file.getFileSrc());

        try {
            Files.delete(path); // 물리적인 파일 삭제
            deleteDBFile(file); // 매핑된 DB 데이터 삭제
        } catch (IOException e) {
            throw new FileException(ERR_FILE_DELETE, e.getMessage());
        }
    }

    /**
     * DB 데이터 삭제
     *
     * @param file 파일 엔티티
     */
    private void deleteDBFile(File file) {
        // UUID는 각 타입간 중복될 수 없으므로 존재하는 하나의 타입에 대해서만 삭제처리
        userFileRepository.deleteByFileSrl(file.getFileSrl()); // 회원 파일일 경우 삭제
        photoFileRepository.deleteByFileSrl(file.getFileSrl()); // 사진 파일일 경우 삭제

        fileRepository.delete(file); // 파일 테이블 데이터 삭제
    }
}
