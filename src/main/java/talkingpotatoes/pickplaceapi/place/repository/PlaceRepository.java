package talkingpotatoes.pickplaceapi.place.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import talkingpotatoes.pickplaceapi.place.domain.entity.Place;

/**
 * 장소 Repository
 *
 * @author : 박지혁
 * @since : 2026/06/21
 */
public interface PlaceRepository extends JpaRepository<Place, Long> {
}
