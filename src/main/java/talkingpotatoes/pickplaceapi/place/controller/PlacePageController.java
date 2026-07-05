package talkingpotatoes.pickplaceapi.place.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 장소 테스트 페이지 컨트롤러
 *
 * @author : 박지혁
 * @since : 2026/07/05
 */
// TODO: 2026/07/5 이 컨트롤러는 테스트용임. 삭제 필요!
@Controller
@RequestMapping("/test/place")
public class PlacePageController {

    @GetMapping
    public String placeTestPage() {
        return "forward:/test/place.html";
    }
}
