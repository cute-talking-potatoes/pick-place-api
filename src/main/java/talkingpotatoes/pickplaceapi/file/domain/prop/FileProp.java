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
    private List<String> fileTypes;
}
