package talkingpotatoes.pickplaceapi.place.dto;

import talkingpotatoes.pickplaceapi.place.domain.entity.Place;

/**
 * 장소 등록 응답
 * <pre>
 *  - 등록 직후 상세 조회·사진 관리로 이동할 수 있도록 생성된 장소 번호만 내려준다.
 * </pre>
 *
 * @author : 박지혁
 * @since : 2026/11/03
 */
public record PlaceCreateResponse(
        Long plSrl // 생성된 위치 NO (PK)
) {
    public PlaceCreateResponse(Place place) {
        this(place.getPlSrl());
    }
}
