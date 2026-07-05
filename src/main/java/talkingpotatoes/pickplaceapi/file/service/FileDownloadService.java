package talkingpotatoes.pickplaceapi.file.service;

import static talkingpotatoes.pickplaceapi.global.exception.ExceptionCode.*;

import java.io.FilterInputStream;
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

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import talkingpotatoes.pickplaceapi.file.domain.entity.File;
import talkingpotatoes.pickplaceapi.global.exception.FileException;

/**
 * 파일 다운로드 처리
 *
 * @author : 박지혁
 * @since : 2026/07/02
 */
@Slf4j
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

    public Resource displayImage(File file, HttpHeaders headers) {
        Path path = Path.of(file.getFileSrc());
        localFileStorageService.validateExists(path);
        return createInlineResource(headers, path, file.getFileNm(), file.getFileExtension());
    }

    private Resource downloadSingleFile(HttpHeaders headers, File file) {
        Path path = Path.of(file.getFileSrc());
        localFileStorageService.validateExists(path);
        return createDownloadResource(headers, path, file.getFileNm(), "application/octet-stream");
    }

    private Resource downloadZipFile(String uuid, HttpHeaders headers, List<File> filesToDownload) {
        Path tempZipPath = null;
        try {
            tempZipPath = Files.createTempFile(uuid, ".zip");
            makeZipFile(filesToDownload, tempZipPath);

            String filename = UUID.randomUUID() + ".zip";
            setUpDownloadHeader(filename, headers, tempZipPath);
            headers.add(HttpHeaders.CONTENT_TYPE, "application/zip");
            return new DeleteOnCloseFileResource(tempZipPath);
        } catch (IOException e) {
            deleteTempZipFile(tempZipPath);
            throw new FileException(ERR_FILE_FAIL_DOWNLOAD, e.getMessage());
        } catch (RuntimeException e) {
            deleteTempZipFile(tempZipPath);
            throw e;
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

    private Resource createInlineResource(HttpHeaders headers, Path path, String filename, String extension) {
        try {
            Resource resource = new UrlResource(path.toUri());
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedFilename + "\"");
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(Files.size(path)));
            headers.add(HttpHeaders.CONTENT_TYPE, resolveContentType(path, extension));
            return resource;
        } catch (IOException e) {
            throw new FileException(ERR_FILE_FAIL_DOWNLOAD, e.getMessage());
        }
    }

    private String resolveContentType(Path path, String extension) throws IOException {
        String extensionContentType = resolveContentTypeByExtension(extension);
        if (extensionContentType != null) {
            return extensionContentType;
        }

        String contentType = Files.probeContentType(path);
        if (contentType == null || !contentType.startsWith("image/")) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return contentType;
    }

    private String resolveContentTypeByExtension(String extension) {
        if (extension == null) {
            return null;
        }

        return switch (extension.toLowerCase()) {
            case ".jpg", ".jpeg" -> MediaType.IMAGE_JPEG_VALUE;
            case ".png" -> MediaType.IMAGE_PNG_VALUE;
            case ".gif" -> MediaType.IMAGE_GIF_VALUE;
            case ".webp" -> "image/webp";
            default -> null;
        };
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

    private void deleteTempZipFile(Path tempZipPath) {
        if (tempZipPath == null) {
            return;
        }

        try {
            Files.deleteIfExists(tempZipPath);
        } catch (IOException e) {
            log.warn("임시 ZIP파일을 삭제하는데 실패했습니다. 임시 파일 경로: {}", tempZipPath, e);
        }
    }

    private class DeleteOnCloseFileResource extends FileSystemResource {

        private final Path path;

        DeleteOnCloseFileResource(Path path) {
            super(path);
            this.path = path;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new FilterInputStream(Files.newInputStream(path)) {
                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        deleteTempZipFile(path);
                    }
                }
            };
        }
    }
}
