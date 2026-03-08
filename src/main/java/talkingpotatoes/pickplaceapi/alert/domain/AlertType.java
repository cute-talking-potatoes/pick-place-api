package talkingpotatoes.pickplaceapi.alert.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 알림 타입
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Getter
@AllArgsConstructor
public enum AlertType {
    MEETING("모임"),
    CHAT("채팅"),
    POST("게시글"),
    COMMENT("댓글"),
    ;

    private final String korType;
}
