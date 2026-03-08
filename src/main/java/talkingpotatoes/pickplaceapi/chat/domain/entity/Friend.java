package talkingpotatoes.pickplaceapi.chat.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 친구 엔티티
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Table(name = "tb_friend")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friend extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "frd_srl")
    private Long frdSrl; // 친구 NO (PK)

    private String frdNm; // 친구 이름

    @Column(name = "frd_sts")
    @Enumerated(value = EnumType.STRING)
    private FetchType frdSts; // 친구 상태

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_srl")
    private User user; // 회원 (나)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "frd_user_srl")
    private User friend; // 친구 (상대)
}
