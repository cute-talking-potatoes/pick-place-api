package talkingpotatoes.pickplaceapi.file.domain.prop;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * 파일 Prop
 *
 * @author : 박지혁
 * @since : 2026/04/12
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "file")
public class FileProp {
    private String filePath;
    private List<String> fileTypes; // 일반 파일 업로드 허용 확장자
    private List<String> imageTypes; // 장소 사진 업로드 허용 이미지 확장자
}
