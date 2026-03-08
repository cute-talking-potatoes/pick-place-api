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
import talkingpotatoes.pickplaceapi.global.domain.UserStatus;
import talkingpotatoes.pickplaceapi.global.domain.entity.BaseEntity;
import talkingpotatoes.pickplaceapi.user.domain.entity.User;

/**
 * 채팅방 참여자 엔티티
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Table(name = "tb_chat_user")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_user_srl")
    private Long chatUserSrl; // 채팅 참여자 NO (PK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_srl")
    private User user; // 회원 (생성자)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatroom_srl")
    private Chatroom chatroom; // 채팅방

    @Column(name = "chat_user_sts")
    @Enumerated(value = EnumType.STRING)
    private UserStatus chatUserSts; // 채팅 참여자 상태
}
