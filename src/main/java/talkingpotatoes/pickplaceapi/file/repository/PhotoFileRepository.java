package talkingpotatoes.pickplaceapi.file.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import talkingpotatoes.pickplaceapi.file.domain.entity.PhotoFile;
import talkingpotatoes.pickplaceapi.place.domain.entity.Place;

/**
 * 사진첩 파일 Repository
 *
 * @author : 박지혁
 * @since : 2026/04/28
 */
public interface PhotoFileRepository extends JpaRepository<PhotoFile, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM PhotoFile f WHERE f.file.fileSrl = :fileSrl")
    void deleteByFileSrl(Long fileSrl);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PhotoFile f WHERE f.file.fileSrl IN :fileSrls")
    int deleteAllByFileSrlIn(@Param("fileSrls") List<Long> fileSrls);

    @Query("""
                SELECT pf
                FROM PhotoFile pf
                JOIN FETCH pf.file
                JOIN FETCH pf.place
                WHERE pf.place IN :places
                ORDER BY pf.photoFileSrl ASC
            """)
    List<PhotoFile> findByPlaceInWithFile(@Param("places") List<Place> places);

    @Query("""
                SELECT pf
                FROM PhotoFile pf
                JOIN FETCH pf.file
                WHERE pf.place = :place
                ORDER BY pf.photoFileSrl ASC
            """)
    List<PhotoFile> findByPlaceWithFile(@Param("place") Place place);

    @Query("""
                SELECT pf
                FROM PhotoFile pf
                JOIN FETCH pf.file
                WHERE pf.photoFileSrl = :photoFileSrl
            """)
    Optional<PhotoFile> findByPhotoFileSrlWithFile(@Param("photoFileSrl") Long photoFileSrl);

    /**
     * 지정한 장소에 속한 사진만 파일과 함께 조회한다.
     * <pre>
     *  - 다른 장소의 사진 번호를 넣어도 조회되지 않으므로 사진 번호만으로 남의 사진에 접근하는 것을 막는다.
     * </pre>
     */
    @Query("""
                SELECT pf
                FROM PhotoFile pf
                JOIN FETCH pf.file
                WHERE pf.photoFileSrl = :photoFileSrl
                  AND pf.place = :place
            """)
    Optional<PhotoFile> findByPhotoFileSrlAndPlaceWithFile(
            @Param("photoFileSrl") Long photoFileSrl,
            @Param("place") Place place
    );
}
