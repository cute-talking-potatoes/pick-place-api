package talkingpotatoes.pickplaceapi.place.dto;

import java.time.LocalDateTime;
import java.util.List;

import talkingpotatoes.pickplaceapi.file.domain.entity.PhotoFile;
import talkingpotatoes.pickplaceapi.place.domain.entity.Place;
import talkingpotatoes.pickplaceapi.place.dto.PlaceMarkerResponse.PlacePhotoResponse;

/**
 * 장소 상세 조회 응답
 * <pre>
 *  - 지도 마커 클릭 후 보여줄 장소 상세 정보다.
 *  - 작성자 아이디(createdBy/updatedBy)는 노출하지 않고, 현재 사용자가 작성자인지(isOwner)만 내려준다.
 * </pre>
 *
 * @author : 박지혁
 * @since : 2026/11/03
 */
public record PlaceDetailResponse(
        Long plSrl, // 위치 NO (PK)
        String plNm, // 위치 이름
        String plDesc, // 위치 설명
        String plAddr1, // 위치 주소 1 (기본)
        String plAddr2, // 위치 주소 2 (상세)
        String plCd, // 위치 우편번호
        Double plLat, // 위도
        Double plLng, // 경도
        List<PlacePhotoResponse> photos, // 등록 사진 (photoFileSrl 오름차순)
        LocalDateTime createdAt, // 등록일시
        LocalDateTime updatedAt, // 수정일시
        boolean isOwner // 현재 로그인 사용자가 작성자인지 여부
) {
    public PlaceDetailResponse(Place place, List<PhotoFile> photoFiles, boolean isOwner) {
        this(
                place.getPlSrl(),
                place.getPlNm(),
                place.getPlDesc(),
                place.getPlAddr1(),
                place.getPlAddr2(),
                place.getPlCd(),
                place.getPlLat(),
                place.getPlLng(),
                photoFiles.stream()
                        .map(PlacePhotoResponse::new)
                        .toList(),
                place.getCreatedAt(),
                place.getUpdatedAt(),
                isOwner
        );
    }
}
