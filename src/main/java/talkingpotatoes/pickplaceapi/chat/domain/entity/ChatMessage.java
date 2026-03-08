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
import talkingpotatoes.pickplaceapi.chat.domain.ChatMessageType;
import talkingpotatoes.pickplaceapi.global.domain.ActiveStatus;
import talkingpotatoes.pickplaceapi.global.domain.entity.BaseEntity;
import talkingpotatoes.pickplaceapi.user.domain.entity.User;

/**
 * 채팅 메시지 엔티티
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Table(name = "tb_chat_message")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_msg_srl")
    private Long chatMsgSrl; // 채팅 메시지 NO (PK)

    @Column(name = "chat_msg_cont")
    private String chatMsgCont; // 채팅 메시지 내용

    @Column(name = "chat_msg_type")
    @Enumerated(value = EnumType.STRING)
    private ChatMessageType chatMsgType; // 채팅 메시지 타입

    @Column(name = "chat_msg_sts")
    @Enumerated(value = EnumType.STRING)
    private ActiveStatus chatMsgSts; // 채팅 메시지 상태

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_srl")
    private User user; // 회원

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatroom_srl")
    private Chatroom chatroom; // 채팅방
}
