package talkingpotatoes.pickplaceapi.alert.domain.entity;

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
import talkingpotatoes.pickplaceapi.alert.domain.AlertType;
import talkingpotatoes.pickplaceapi.global.domain.entity.BaseEntity;
import talkingpotatoes.pickplaceapi.user.domain.entity.User;

/**
 * 알림 엔티티
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Table(name = "tb_alert")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Alert extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_srl")
    private Long alertSrl; // 알림 NO (PK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_srl")
    private User user; // 회원

    @Column(name = "alert_type")
    @Enumerated(value = EnumType.STRING)
    private AlertType alertType; // 알림 타입

    @Column(name = "alert_target_srl")
    private Long alertTargetSrl; // 알림 대상 ID (외래키이지만 외래키로 설정하지 않음, 애플리케이션 로직단에서 처리 필수 / 게시글, 댓글, 모임 등의 PK값)

    @Column(name = "alert_is_read", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean isRead; // 알림 읽음 여부
}
