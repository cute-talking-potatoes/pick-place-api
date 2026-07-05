package talkingpotatoes.pickplaceapi.place.dto;

import static talkingpotatoes.pickplaceapi.global.exception.ExceptionCode.*;

import talkingpotatoes.pickplaceapi.global.exception.PlaceException;
import talkingpotatoes.pickplaceapi.place.domain.entity.Place;

/**
 * 현재 지도 화면의 사각형 영역
 * sw = South West, 남서쪽 좌표
 * ne = North East, 북동쪽 좌표
 *
 * @author : 박지혁
 * @since : 2026/07/02
 */
public record MapBound(
        Double swLat,
        Double swLng,
        Double neLat,
        Double neLng
) {
    public MapBound {
        if (swLat == null || swLng == null || neLat == null || neLng == null) {
            throw new PlaceException(ERR_PLACE_INVALID_MAP);
        }

        Place.isValidLatitude(swLat);
        Place.isValidLatitude(neLat);
        Place.isValidLongitude(swLng);
        Place.isValidLongitude(neLng);

        if (swLat > neLat) {
            throw new PlaceException(ERR_PLACE_INVALID_LATITUDE);
        }

        if (swLng > neLng) {
            throw new PlaceException(ERR_PLACE_INVALID_LONGITUDE);
        }
    }
}
