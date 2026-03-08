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

/**
 * 회원 상세 엔티티
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Table(name = "tb_user_details")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDetails extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_dtl_srl")
    private Long userDtlSrl; // 회원 상세 NO (PK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_srl")
    private User user; // 회원 정보

    @Column(name = "preferred_transport", length = 100)
    private String preferredTransport; // 선호 교통 수단 / Enum으로 변경해도 괜찮을지도?

    @Column(name = "start_point")
    private String startPoint; // 출발지 주소

    @Column(name = "start_cd", length = 10)
    private String startCd; // 출발지 우편번호

}
