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

/**
 * 위치 엔티티
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Table(name = "tb_place")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseEntity {

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

    @Column(name = "pl_lat", precision = 10, scale = 7) // DECIMAL(10, 7), 전체 10자리 (숫자3, 소수점7)
    private Double plLat; // 위도

    @Column(name = "pl_lng", precision = 10, scale = 7) // DECIMAL(10, 7), 전체 10자리 (숫자3, 소수점7)
    private Double plLng; // 경도
}
