package talkingpotatoes.pickplaceapi.meeting.domain.entity;

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
import talkingpotatoes.pickplaceapi.global.domain.Visibility;
import talkingpotatoes.pickplaceapi.global.domain.entity.BaseEntity;
import talkingpotatoes.pickplaceapi.meeting.domain.MeetingStatus;
import talkingpotatoes.pickplaceapi.user.domain.entity.User;

/**
 * 모임 엔티티
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Table(name = "tb_meeting")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meeting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mt_srl")
    private Long mtSrl; // 모임 NO (PK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_srl")
    private User user; // 회원 (모임 개설자)

    @Column(name = "mt_title")
    private String mtTitle; // 모임 제목

    @Column(name = "mt_desc")
    private String mtDesc; // 모임 설명

    @Column(name = "mt_max_user")
    private Integer mtMaxUser; // 모임 최대 회원 수

    @Column(name = "mt_sts")
    @Enumerated(value = EnumType.STRING)
    private MeetingStatus mtSts; // 모임 상태

    @Column(name = "mt_visibility")
    @Enumerated(value = EnumType.STRING)
    private Visibility mtVisibility; // 모임 공개 범위
}
