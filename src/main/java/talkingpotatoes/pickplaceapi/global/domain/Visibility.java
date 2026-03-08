package talkingpotatoes.pickplaceapi.global.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 공개 범위
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Getter
@AllArgsConstructor
public enum Visibility {
    PRIVATE("비공개"),
    FRIENDS("친구공개"),
    PUBLIC("전체공개"),
    ;

    private final String korVisibility;
}
