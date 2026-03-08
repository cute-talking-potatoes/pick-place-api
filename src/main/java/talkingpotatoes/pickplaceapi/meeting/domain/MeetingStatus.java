package talkingpotatoes.pickplaceapi.meeting.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 모임 상태
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Getter
@AllArgsConstructor
public enum MeetingStatus {
    RECRUITING("모집중"),
    FULL("모집완료"),
    CLOSED("모집마감"),
    IN_PROGRESS("진행중"),
    COMPLETED("종료"),
    CANCELLED("취소"),
    ;

    private final String korStatus;
}
