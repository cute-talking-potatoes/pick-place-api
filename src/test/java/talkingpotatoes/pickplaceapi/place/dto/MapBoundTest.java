package talkingpotatoes.pickplaceapi.place.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import talkingpotatoes.pickplaceapi.global.exception.PlaceException;

@DisplayName("지도 영역 요청 테스트")
class MapBoundTest {

    @Test
    void 지도영역을_생성한다() {
        MapBound bound = new MapBound(37.0, 126.0, 38.0, 127.0);

        assertAll(
                () -> assertEquals(37.0, bound.swLat()),
                () -> assertEquals(126.0, bound.swLng()),
                () -> assertEquals(38.0, bound.neLat()),
                () -> assertEquals(127.0, bound.neLng())
        );
    }

    @Test
    void 좌표가_널이면_예외가발생한다() {
        assertThrows(PlaceException.class, () -> new MapBound(null, 126.0, 38.0, 127.0));
    }

    @Test
    void 위도가_유효범위를_벗어나면_예외가발생한다() {
        assertAll(
                () -> assertThrows(PlaceException.class, () -> new MapBound(-90.1, 126.0, 38.0, 127.0)),
                () -> assertThrows(PlaceException.class, () -> new MapBound(37.0, 126.0, 90.1, 127.0))
        );
    }

    @Test
    void 경도가_유효범위를_벗어나면_예외가발생한다() {
        assertAll(
                () -> assertThrows(PlaceException.class, () -> new MapBound(37.0, -180.1, 38.0, 127.0)),
                () -> assertThrows(PlaceException.class, () -> new MapBound(37.0, 126.0, 38.0, 180.1))
        );
    }

    @Test
    void 남서좌표가_북동좌표보다_크면_예외가발생한다() {
        assertAll(
                () -> assertThrows(PlaceException.class, () -> new MapBound(38.0, 126.0, 37.0, 127.0)),
                () -> assertThrows(PlaceException.class, () -> new MapBound(37.0, 127.0, 38.0, 126.0))
        );
    }
}
