package talkingpotatoes.pickplaceapi.place.domain.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import talkingpotatoes.pickplaceapi.global.exception.PlaceException;
import talkingpotatoes.pickplaceapi.place.dto.PlaceRequest;

/**
 * @author : 박지혁
 * @since : 2026/06/21
 */
class PlaceTest {

    @Test
    void 장소를_생성한다() {
        PlaceRequest request = new PlaceRequest("경복궁", "", "서울 종로구 사직로 161 경복궁", "", "03045", 37.5786111111,
                126.9772222222);
        Place place = new Place(request);
        assertEquals("경복궁", place.getPlNm());
    }

    @Test
    void 장소명이유효하지않아_예외가발생한다() {
        PlaceRequest request = new PlaceRequest("", "", "서울 종로구 사직로 161 경복궁", "", "03045", 37.5786111111,
                126.9772222222);

        assertThrows(PlaceException.class, () -> new Place(request));
    }

    @Test
    void 위도가널값으로넘어와_예외가발생한다() {
        PlaceRequest request = new PlaceRequest("경복궁", "", "서울 종로구 사직로 161 경복궁", "", "03045", null,
                126.9772222222);

        assertThrows(PlaceException.class, () -> new Place(request));
    }

    @Test
    void 위도의값이최소값을넘어_예외가발생한다() {
        PlaceRequest request = new PlaceRequest("경복궁", "", "서울 종로구 사직로 161 경복궁", "", "03045", -90.1,
                126.9772222222);

        assertThrows(PlaceException.class, () -> new Place(request));
    }

    @Test
    void 위도의값이최소값으로_생성한다() {
        PlaceRequest request = new PlaceRequest("경복궁", "", "서울 종로구 사직로 161 경복궁", "", "03045", -90.0,
                126.9772222222);

        Place place = new Place(request);
        assertEquals(-90, place.getPlLat());
    }

    @Test
    void 위도의값이최대값을넘어_예외가발생한다() {
        PlaceRequest request = new PlaceRequest("경복궁", "", "서울 종로구 사직로 161 경복궁", "", "03045", 90.1,
                126.9772222222);

        assertThrows(PlaceException.class, () -> new Place(request));
    }

    @Test
    void 위도의값이최대값으로_생성한다() {
        PlaceRequest request = new PlaceRequest("경복궁", "", "서울 종로구 사직로 161 경복궁", "", "03045", 90.0,
                126.9772222222);

        Place place = new Place(request);
        assertEquals(90, place.getPlLat());
    }

    @Test
    void 경도가널값으로넘어가_예외가발생한다() {
        PlaceRequest request = new PlaceRequest("경복궁", "", "서울 종로구 사직로 161 경복궁", "", "03045", 37.5786111111,
                null);

        assertThrows(PlaceException.class, () -> new Place(request));
    }

    @Test
    void 경도의값이최소값을넘어_예외가발생한다() {
        PlaceRequest request = new PlaceRequest("경복궁", "", "서울 종로구 사직로 161 경복궁", "", "03045", 37.5786111111,
                -180.1);

        assertThrows(PlaceException.class, () -> new Place(request));
    }

    @Test
    void 경도의값이최소값으로_생성한다() {
        PlaceRequest request = new PlaceRequest("경복궁", "", "서울 종로구 사직로 161 경복궁", "", "03045", 37.5786111111,
                -180.0);

        Place place = new Place(request);
        assertEquals(-180.0, place.getPlLng());
    }

    @Test
    void 경도의값이최대값을넘어_예외가발생한다() {
        PlaceRequest request = new PlaceRequest("경복궁", "", "서울 종로구 사직로 161 경복궁", "", "03045", 37.5786111111,
                180.1);

        assertThrows(PlaceException.class, () -> new Place(request));
    }

    @Test
    void 경도의값이최대값으로_생성한다() {
        PlaceRequest request = new PlaceRequest("경복궁", "", "서울 종로구 사직로 161 경복궁", "", "03045", 37.5786111111,
                180.0);

        Place place = new Place(request);
        assertEquals(180.0, place.getPlLng());
    }
}
