package talkingpotatoes.pickplaceapi.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 채팅 메시지 타입
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Getter
@AllArgsConstructor
public enum ChatMessageType {
    TEXT("텍스트"),
    IMAGE("이미지"),
    FILE("파일"),
    SYSTEM("시스템"),
    ;

    private final String korType;
}
