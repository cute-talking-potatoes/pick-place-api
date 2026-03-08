package talkingpotatoes.pickplaceapi.place.domain.entity;

import java.time.LocalDateTime;

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
import talkingpotatoes.pickplaceapi.user.domain.entity.User;

/**
 * 위치 후기 엔티티
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Table(name = "tb_place_review")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pl_review_srl")
    private Long plReviewSrl; // 위치 후기 NO (PK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_srl")
    private User user; // 회원

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pl_srl")
    private Place place; // 위치

    @Column(name = "pl_review_cont", columnDefinition = "TEXT")
    private String plReviewCont; // 후기 내용

    @Column(name = "pl_review_rating", precision = 5, scale = 2)  // DECIMAL(5, 2), 전체 5자리 (숫자3, 소수점2)
    private Double plReviewRating; // 후기 별점

    @Column(name = "pl_review_sts")
    private String plReviewSts; // 위치 상태 (이게 뭐더라... enum으로 변경 가능할듯?)

    @Column(name = "pl_visited_at", columnDefinition = "DATETIME")
    private LocalDateTime plVisitedAt; // 위치 방문 일자
}
