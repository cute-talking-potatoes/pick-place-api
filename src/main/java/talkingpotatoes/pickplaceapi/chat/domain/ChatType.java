package talkingpotatoes.pickplaceapi.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 채팅 타입
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Getter
@AllArgsConstructor
public enum ChatType {
    DIRECT("1:1채팅"),
    GROUP("그룹채팅"),
    OPEN("오픈채팅")
    ;

    private final String korType;
}
