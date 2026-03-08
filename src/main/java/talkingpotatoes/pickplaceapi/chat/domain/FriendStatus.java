package talkingpotatoes.pickplaceapi.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 친구 상태
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Getter
@AllArgsConstructor
public enum FriendStatus {
    BLOCKED("차단"),
    HIDDEN("숨김"),
    ;

    private final String korStatus;
}
