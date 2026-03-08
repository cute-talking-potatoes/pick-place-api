package talkingpotatoes.pickplaceapi.user.domain.entity;

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
import talkingpotatoes.pickplaceapi.place.domain.entity.Place;

/**
 * 북마크 엔티티
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Table(name = "tb_bookmark")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bmk_srl")
    private Long bmkSrl; // 북마크 NO (PK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_srl")
    private User user; // 회원

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pl_srl")
    private Place place; // 위치

    @Column(name = "bmk_memo", columnDefinition = "TEXT")
    private String bmkMemo; // 북마크 메모

    @Column(name = "bmk_rating", precision = 5, scale = 2)  // DECIMAL(5, 2), 전체 5자리 (숫자3, 소수점2)
    private Double bmkRating; // 북마크 평가
}
