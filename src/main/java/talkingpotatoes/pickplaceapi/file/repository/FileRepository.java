package talkingpotatoes.pickplaceapi.file.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import talkingpotatoes.pickplaceapi.file.domain.entity.File;

/**
 * 파일 Repository
 *
 * @author : 박지혁
 * @since : 2026/04/12
 */
public interface FileRepository extends JpaRepository<File, Long> {

    @Query("SELECT f FROM File f WHERE f.fileManageSrl =: uuid")
    List<File> findByUUID(String uuid);
}
