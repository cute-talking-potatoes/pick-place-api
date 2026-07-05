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

    @Test
    void 장소정보를_수정한다() {
        Place place = new Place(new PlaceRequest(
                "경복궁", "기존 설명", "서울 종로구 사직로 161 경복궁", "", "03045", 37.5786111111, 126.9772222222
        ));
        PlaceRequest updateRequest = new PlaceRequest(
                "창덕궁", "수정 설명", "서울 종로구 율곡로 99", "돈화문", "03072", 37.5794444444, 126.9911111111
        );

        place.updatePlace(updateRequest);

        assertAll(
                () -> assertEquals("창덕궁", place.getPlNm()),
                () -> assertEquals("수정 설명", place.getPlDesc()),
                () -> assertEquals("서울 종로구 율곡로 99", place.getPlAddr1()),
                () -> assertEquals("돈화문", place.getPlAddr2()),
                () -> assertEquals("03072", place.getPlCd()),
                () -> assertEquals(37.5794444444, place.getPlLat()),
                () -> assertEquals(126.9911111111, place.getPlLng())
        );
    }

    @Test
    void 장소수정시_널값은_기존값을_유지한다() {
        Place place = new Place(new PlaceRequest(
                "경복궁", "기존 설명", "서울 종로구 사직로 161 경복궁", "상세", "03045", 37.5786111111, 126.9772222222
        ));
        PlaceRequest updateRequest = new PlaceRequest(
                "창덕궁", null, null, null, null, null, null
        );

        place.updatePlace(updateRequest);

        assertAll(
                () -> assertEquals("창덕궁", place.getPlNm()),
                () -> assertEquals("기존 설명", place.getPlDesc()),
                () -> assertEquals("서울 종로구 사직로 161 경복궁", place.getPlAddr1()),
                () -> assertEquals("상세", place.getPlAddr2()),
                () -> assertEquals("03045", place.getPlCd()),
                () -> assertEquals(37.5786111111, place.getPlLat()),
                () -> assertEquals(126.9772222222, place.getPlLng())
        );
    }

    @Test
    void 장소수정시_빈장소명이면_기존값을_유지한다() {
        Place place = new Place(new PlaceRequest(
                "경복궁", "기존 설명", "서울 종로구 사직로 161 경복궁", "", "03045", 37.5786111111, 126.9772222222
        ));
        PlaceRequest updateRequest = new PlaceRequest(
                " ", "수정 설명", "서울 종로구 율곡로 99", "", "03072", 37.5794444444, 126.9911111111
        );

        place.updatePlace(updateRequest);
        assertAll(
                () -> assertEquals("경복궁", place.getPlNm()),
                () -> assertEquals("수정 설명", place.getPlDesc()),
                () -> assertEquals("서울 종로구 율곡로 99", place.getPlAddr1()),
                () -> assertEquals("", place.getPlAddr2()),
                () -> assertEquals("03072", place.getPlCd()),
                () -> assertEquals(37.5794444444, place.getPlLat()),
                () -> assertEquals(126.9911111111, place.getPlLng())
        );
    }
}
