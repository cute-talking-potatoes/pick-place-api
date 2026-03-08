package talkingpotatoes.pickplaceapi.meeting.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 모임 권한
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Getter
@AllArgsConstructor
public enum MeetingRole {
    HOST("방장"),
    MANAGER("운영자"),
    MEMBER("참여자"),
    ;

    private final String korRole;
}
