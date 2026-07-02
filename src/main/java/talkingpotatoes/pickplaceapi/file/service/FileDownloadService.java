package talkingpotatoes.pickplaceapi.file.service;

import static talkingpotatoes.pickplaceapi.global.exception.ExceptionCode.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

import lombok.RequiredArgsConstructor;
import talkingpotatoes.pickplaceapi.file.domain.entity.File;
import talkingpotatoes.pickplaceapi.global.exception.FileException;

/**
 * 파일 다운로드 처리
 *
 * @author : 박지혁
 * @since : 2026/07/02
 */
@Service
@RequiredArgsConstructor
public class FileDownloadService {

    private static final int BUFFER_SIZE = 8192;

    private final LocalFileStorageService localFileStorageService;

    public Resource download(String uuid, List<Long> fileSeqList, List<File> filesToDownload, HttpHeaders headers) {
        validateDownloadFiles(fileSeqList, filesToDownload);

        if (filesToDownload.size() == 1) {
            return downloadSingleFile(headers, filesToDownload.get(0));
        }

        return downloadZipFile(uuid, headers, filesToDownload);
    }

    private Resource downloadSingleFile(HttpHeaders headers, File file) {
        Path path = Path.of(file.getFileSrc());
        localFileStorageService.validateExists(path);
        return createDownloadResource(headers, path, file.getFileNm(), "application/octet-stream");
    }

    private UrlResource downloadZipFile(String uuid, HttpHeaders headers, List<File> filesToDownload) {
        try {
            Path tempZipPath = Files.createTempFile(uuid, ".zip");
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
            byte[] buffer = new byte[BUFFER_SIZE];

            for (File file : filesToDownload) {
                Path filePath = Path.of(file.getFileSrc());
                localFileStorageService.validateExists(filePath);

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

    private Resource createDownloadResource(HttpHeaders headers, Path path, String filename, String contentType) {
        try {
            Resource resource = new UrlResource(path.toUri());
            setUpDownloadHeader(filename, headers, path);
            headers.add(HttpHeaders.CONTENT_TYPE, contentType);
            return resource;
        } catch (IOException e) {
            throw new FileException(ERR_FILE_FAIL_DOWNLOAD, e.getMessage());
        }
    }

    private void setUpDownloadHeader(String file, HttpHeaders headers, Path path) throws IOException {
        String filename = URLEncoder.encode(file, StandardCharsets.UTF_8).replace("+", "%20");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(Files.size(path)));
    }

    private void validateDownloadFiles(List<Long> fileSeqList, List<File> filesToDownload) {
        if (filesToDownload.isEmpty()) {
            throw new FileException(ERR_FILE_NOT_EXIST);
        }

        if (fileSeqList.size() != filesToDownload.size()) {
            throw new FileException(ERR_FILE_CANNOT_DOWNLOAD, "요청한 파일과 실제 저장된 파일의 개수가 일치하지 않습니다.");
        }
    }

    private String generateUniqueFilename(File file, Map<String, Integer> fileMap) {
        String filename = file.getFileNm();

        if (fileMap.containsKey(filename)) {
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
