package talkingpotatoes.pickplaceapi.place.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import talkingpotatoes.pickplaceapi.global.domain.entity.BaseEntity;
import talkingpotatoes.pickplaceapi.global.exception.ExceptionCode;
import talkingpotatoes.pickplaceapi.global.exception.PlaceException;
import talkingpotatoes.pickplaceapi.place.dto.PlaceRequest;

/**
 * 위치 엔티티
 *
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Table(name = "tb_place")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseEntity {

    private final static double MIN_LATITUDE = -90; // 최소 위도
    private final static double MAX_LATITUDE = 90; // 최대 위도
    private final static double MIN_LONGITUDE = -180; // 최소 경도
    private final static double MAX_LONGITUDE = 180; // 최대 경도

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pl_srl")
    private Long plSrl; // 위치 NO (PK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pref_srl")
    private Prefer prefer; // 취향 (이 위치가 어떤 타입인지) / 위치별 여러 취향이 가능하다면 위치 <-> 취향 사이에 테이블 하나 더 추가 필요

    @Column(name = "pl_nm")
    private String plNm; // 위치 이름

    @Column(name = "pl_desc")
    private String plDesc; // 위치 설명

    @Column(name = "pl_addr1")
    private String plAddr1; // 위치 주소 1 (기본)

    @Column(name = "pl_addr2", length = 50)
    private String plAddr2; // 위치 주소 2 (상세)

    @Column(name = "pl_cd", length = 10)
    private String plCd; // 위치 우편번호

    @Column(name = "pl_lat")
    private Double plLat; // 위도

    @Column(name = "pl_lng")
    private Double plLng; // 경도

    public Place(PlaceRequest request) {
        validCheck(request);
        // this.prefer = prefer; TODO: 취향 추가하기
        this.plNm = request.getPlNm();
        this.plDesc = request.getPlDesc();
        this.plAddr1 = request.getPlAddr1();
        this.plAddr2 = request.getPlAddr2();
        this.plCd = request.getPlCd();
        this.plLat = request.getPlLat();
        this.plLng = request.getPlLng();
    }

    public void validCheck(PlaceRequest request) {
        if (request.getPlNm() == null || request.getPlNm().isBlank()) { // TODO: 글자수도 여기서 넣기 or 요청에서 검증하기?
            throw new PlaceException(ExceptionCode.ERR_PLACE_INVALID_TITLE);
        }
        isValidLatitude(request.getPlLat());
        isValidLongitude(request.getPlLng());
    }

    public static void isValidLatitude(Double latitude) {
        if (latitude == null || latitude < MIN_LATITUDE || latitude > MAX_LATITUDE) {
            throw new PlaceException(ExceptionCode.ERR_PLACE_INVALID_LATITUDE);
        }
    }

    public static void isValidLongitude(Double longitude) {
        if (longitude == null || longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE) {
            throw new PlaceException(ExceptionCode.ERR_PLACE_INVALID_LONGITUDE);
        }
    }

    public void updatePlace(PlaceRequest request) {
        if (request.getPlNm() != null && !request.getPlNm().isBlank()) {
            this.plNm = request.getPlNm();
        }
        if (request.getPlDesc() != null && !request.getPlDesc().isBlank()) {
            this.plDesc = request.getPlDesc();
        }
        if (request.getPlAddr1() != null && !request.getPlAddr1().isBlank()) {
            this.plAddr1 = request.getPlAddr1();
        }
        if (request.getPlAddr2() != null && !request.getPlAddr2().isBlank()) {
            this.plAddr2 = request.getPlAddr2();
        }
        if (request.getPlCd() != null && !request.getPlCd().isBlank()) {
            this.plCd = request.getPlCd();
        }
        if (request.getPlLat() != null) {
            isValidLatitude(request.getPlLat());
            this.plLat = request.getPlLat();
        }
        if (request.getPlLng() != null) {
            isValidLongitude(request.getPlLng());
            this.plLng = request.getPlLng();
        }
    }
}
