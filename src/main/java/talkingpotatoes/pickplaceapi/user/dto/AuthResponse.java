package talkingpotatoes.pickplaceapi.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import talkingpotatoes.pickplaceapi.user.domain.entity.User;

/**
 * 인증 성공 응답
 * @author : 이나영
 * @since : 2026/05/19
 */
@Getter
@Builder
public class AuthResponse {
    @JsonProperty("user_id")
    private String userId;
    private String nickname;
    private String email;

    public static AuthResponse from(User user) {
        return AuthResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .build();
    }
}