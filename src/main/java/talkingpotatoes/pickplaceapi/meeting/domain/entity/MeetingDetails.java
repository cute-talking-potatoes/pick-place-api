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
import talkingpotatoes.pickplaceapi.global.domain.entity.BaseEntity;
import talkingpotatoes.pickplaceapi.meeting.domain.MeetingRole;
import talkingpotatoes.pickplaceapi.global.domain.UserStatus;
import talkingpotatoes.pickplaceapi.user.domain.entity.User;

/**
 * 모임 상세 엔티티
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Table(name = "tb_meeting_details")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingDetails extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mt_dtl_srl")
    private Long mtDtlSrl; // 모임 상세 NO (PK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_srl")
    private User user; // 회원 (모임 참여자)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mt_srl")
    private Meeting meeting; // 모임

    @Column(name = "mt_role")
    @Enumerated(value = EnumType.STRING)
    private MeetingRole mtRole; // 모임 권한

    @Column(name = "mt_user_sts")
    @Enumerated(value = EnumType.STRING)
    private UserStatus mtUserSts; // 모임 회원 상태
}
