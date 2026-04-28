package talkingpotatoes.pickplaceapi.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import talkingpotatoes.pickplaceapi.file.domain.entity.UserFile;

/**
 * 사용자 파일 Repository
 *
 * @author : 박지혁
 * @since : 2026/04/28
 */
public interface UserFileRepository extends JpaRepository<UserFile, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM UserFile f WHERE f.file.fileSrl = :fileSrl")
    void deleteByFileSrl(Long fileSrl);
}
