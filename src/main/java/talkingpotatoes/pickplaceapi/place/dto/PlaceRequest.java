package talkingpotatoes.pickplaceapi.place.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장소 요청
 *
 * @author : 박지혁
 * @since : 2026/06/21
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceRequest {
    private String plNm; // 장소 제목
    private String plDesc; // 장소 설명
    private String plAddr1; // 위치 주소 1 (기본)
    private String plAddr2; // 위치 주소 2 (상세)
    private String plCd; // 위치 우편번호
    private Double plLat; // 위도
    private Double plLng; // 경도
}
