package talkingpotatoes.pickplaceapi.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;

/**
 * 회원가입 요청
 * @author : 이나영
 * @since : 2026/05/19
 */
@Getter
public class SignUpRequest {

    @JsonProperty("user_id")
    @NotBlank(message = "사용할 아이디를 입력해 주세요.")
    @Size(max = 50, message = "아이디는 50자 이하로 입력해 주세요.")
    @Pattern(regexp = "^[a-z0-9]+$", message = "아이디는 영문 소문자와 숫자만 사용할 수 있습니다.")
    private String userId;

    @NotBlank(message = "이메일을 입력해 주세요.")
    @Email(message = "올바른 이메일 형식을 입력해 주세요.")
    private String email;

    @NotBlank(message = "닉네임을 입력해 주세요.")
    @Size(max = 50, message = "닉네임은 50자 이하로 입력해 주세요.")
    private String nickname;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하로 입력해 주세요.")
    @Pattern(
            regexp = "[a-zA-Z0-9`~!@#$%^&*()_=+|{};:,.<>/?]*$",
            message = "비밀번호 형식이 일치하지 않습니다"
    )
    private String password;

    @JsonProperty("password_confirm")
    @NotBlank(message = "비밀번호 확인을 입력해 주세요.")
    private String passwordConfirm;
}
