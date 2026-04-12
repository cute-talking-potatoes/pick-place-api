package talkingpotatoes.pickplaceapi.file.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 파일 구분값
 *
 * @author : 박지혁
 * @since : 2026/04/12
 */
@Getter
@AllArgsConstructor
public enum FileType {
    PHOTO("사진첩 파일"),
    USER("회원 파일"),
    ;
    private final String korType;
}
