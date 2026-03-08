package talkingpotatoes.pickplaceapi.global.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 활성화 상태
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Getter
@AllArgsConstructor
public enum ActiveStatus {
    ACTIVE("활성화"),
    DEACTIVATE("비활성화"),
    HIDDEN("숨김"),
    DELETED("삭제됨"),
    ;

    private final String korStatus;
}
