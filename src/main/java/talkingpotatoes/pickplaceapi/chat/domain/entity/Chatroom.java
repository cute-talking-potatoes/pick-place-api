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
import talkingpotatoes.pickplaceapi.chat.domain.ChatType;
import talkingpotatoes.pickplaceapi.global.domain.entity.BaseEntity;
import talkingpotatoes.pickplaceapi.user.domain.entity.User;

/**
 * 채팅방 엔티티
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Table(name = "tb_chatroom")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chatroom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chatroom_srl")
    private Long chatroomSrl; // 채팅방 NO (PK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_srl")
    private User user; // 회원 (생성자)

    @Column(name = "chatroom_title")
    private String chatroomTitle;

    @Column(name = "chatroom_type")
    @Enumerated(value = EnumType.STRING)
    private ChatType chatroomType;
}
