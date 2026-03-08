package talkingpotatoes.pickplaceapi.post.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 게시글 타입
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Getter
@AllArgsConstructor
public enum PostType {
    NOTICE("공지"),
    QUESTION("문의"),
    GENERAL("일반"),
    ;

    private final String korPostType;
}
