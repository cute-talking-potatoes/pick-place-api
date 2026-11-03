package talkingpotatoes.pickplaceapi.place.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import talkingpotatoes.pickplaceapi.place.domain.entity.Place;
import talkingpotatoes.pickplaceapi.place.dto.MapBound;

/**
 * 장소 Repository
 *
 * @author : 박지혁
 * @since : 2026/06/21
 */
public interface PlaceRepository extends JpaRepository<Place, Long> {

    @Query("""
                SELECT DISTINCT p
                FROM Place p
                WHERE p.plLat BETWEEN :#{#bound.swLat} AND :#{#bound.neLat}
                  AND p.plLng BETWEEN :#{#bound.swLng} AND :#{#bound.neLng}
                ORDER BY p.plSrl DESC
            """)
    List<Place> findVisiblePlacesInBounds(@Param("bound") MapBound bound);

    /**
     * 장소를 쓰기 락(SELECT ... FOR UPDATE)으로 조회한다.
     * <pre>
     *  - 장소 수정(사진 추가)과 삭제가 동시에 실행될 때 한쪽이 대기하도록 해 고아 사진 행·파일을 막는다.
     *  - 트랜잭션 안에서만 호출해야 한다.
     * </pre>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Place p WHERE p.plSrl = :plSrl")
    Optional<Place> findByIdForUpdate(@Param("plSrl") Long plSrl);
}
