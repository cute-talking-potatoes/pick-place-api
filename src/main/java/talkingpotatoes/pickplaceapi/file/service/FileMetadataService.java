package talkingpotatoes.pickplaceapi.file.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import talkingpotatoes.pickplaceapi.file.domain.FileMetadata;
import talkingpotatoes.pickplaceapi.file.domain.entity.File;
import talkingpotatoes.pickplaceapi.file.domain.entity.PhotoFile;
import talkingpotatoes.pickplaceapi.file.domain.entity.UserFile;
import talkingpotatoes.pickplaceapi.file.repository.FileRepository;
import talkingpotatoes.pickplaceapi.file.repository.PhotoFileRepository;
import talkingpotatoes.pickplaceapi.file.repository.UserFileRepository;
import talkingpotatoes.pickplaceapi.global.security.UserInfoProvider;
import talkingpotatoes.pickplaceapi.place.domain.entity.Place;
import talkingpotatoes.pickplaceapi.user.domain.entity.User;

/**
 * 파일 메타데이터 저장/삭제
 *
 * @author : 박지혁
 * @since : 2026/07/02
 */
@Service
@RequiredArgsConstructor
public class FileMetadataService {

    private final UserInfoProvider userInfoProvider;
    private final FileRepository fileRepository;
    private final PhotoFileRepository photoFileRepository;
    private final UserFileRepository userFileRepository;

    public void saveUserFile(FileMetadata fileMetadata) {
        User user = userInfoProvider.getUser();
        File fileEntity = saveFileEntity(fileMetadata, user);

        UserFile userFile = UserFile.builder()
                .file(fileEntity)
                .user(user)
                .build();
        userFileRepository.save(userFile);
    }

    public void savePhotoFile(FileMetadata fileMetadata, Place place) {
        User user = userInfoProvider.getUser();
        File fileEntity = saveFileEntity(fileMetadata, user);

        PhotoFile photoFile = PhotoFile.builder()
                .file(fileEntity)
                .user(user)
                .place(place)
                .build();
        photoFileRepository.save(photoFile);
    }

    public void delete(File file) {
        userFileRepository.deleteByFileSrl(file.getFileSrl());
        photoFileRepository.deleteByFileSrl(file.getFileSrl());
        fileRepository.delete(file);
    }

    private File saveFileEntity(FileMetadata fileMetadata, User user) {
        File fileEntity = File.builder()
                .user(user)
                .fileNm(fileMetadata.originalName())
                .fileSrc(fileMetadata.filePath().toString())
                .fileExtension(fileMetadata.extension())
                .uploadedAt(LocalDateTime.now())
                .fileManageSrl(fileMetadata.uuid())
                .fileSeq(fileMetadata.sequence())
                .build();
        return fileRepository.save(fileEntity);
    }
}
