package talkingpotatoes.pickplaceapi.global.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 회원 상태
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Getter
@AllArgsConstructor
public enum UserStatus {
    JOINED("참여중"),
    LEFT("나감"),
    KICKED("강퇴"),
    BANNED("차단됨"),
    WAITING("승인대기"),
    ;

    private final String korStatus;
}
