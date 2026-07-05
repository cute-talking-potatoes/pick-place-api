package talkingpotatoes.pickplaceapi.place.dto;

import java.util.List;

import talkingpotatoes.pickplaceapi.file.domain.entity.PhotoFile;
import talkingpotatoes.pickplaceapi.place.domain.entity.Place;

/**
 * 지도 마커 표시용 응답
 *
 * @author : 박지혁
 * @since : 2026/07/02
 */
public record PlaceMarkerResponse(
        Long plSrl, // 위치 NO (PK)
        String plNm, // 위치 이름
        String plDesc, // 위치 설명
        String plAddr1, // 위치 주소 1 (기본)
        String plAddr2, // 위치 주소 2 (상세)
        String plCd, // 위치 우편번호
        Double plLat, // 위도
        Double plLng, // 경도
        List<PlacePhotoResponse> photos // 등록 사진
        // TODO: 2026/07/2 좋아요 개수, 취향 추가 필요
) {
    public PlaceMarkerResponse(Place place) {
        this(place, List.of());
    }

    public PlaceMarkerResponse(Place place, List<PhotoFile> photoFiles) {
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
                        .toList()
        );
    }

    public record PlacePhotoResponse(
            Long photoFileSrl,
            Long fileSrl,
            String fileName,
            String fileExtension,
            String imageUrl
    ) {
        public PlacePhotoResponse(PhotoFile photoFile) {
            this(
                    photoFile.getPhotoFileSrl(),
                    photoFile.getFile().getFileSrl(),
                    photoFile.getFile().getFileNm(),
                    photoFile.getFile().getFileExtension(),
                    "/api/place/photos/" + photoFile.getPhotoFileSrl()
            );
        }
    }
}
