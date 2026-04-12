package talkingpotatoes.pickplaceapi.file.service;

import static talkingpotatoes.pickplaceapi.global.exception.ExceptionCode.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import talkingpotatoes.pickplaceapi.file.domain.FileType;
import talkingpotatoes.pickplaceapi.file.domain.entity.File;
import talkingpotatoes.pickplaceapi.file.domain.prop.FileProp;
import talkingpotatoes.pickplaceapi.file.repository.FileRepository;
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

    private final FileRepository fileRepository;
    private final FileProp fileProp;

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
        }
    }

    private String getFileType(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return ""; // 확장자가 없을 경우 빈 문자열 반환
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
