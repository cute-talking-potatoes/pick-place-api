package talkingpotatoes.pickplaceapi.place.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
