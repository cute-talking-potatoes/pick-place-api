package talkingpotatoes.pickplaceapi.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/**
 * 로그인 요청
 * @author : 이나영
 * @since : 2026/05/19
 */
@Getter
public class AuthRequest {

    @JsonProperty("user_id")
    @NotBlank(message = "아이디를 입력해 주세요.")
    private String userId;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    private String password;
}
