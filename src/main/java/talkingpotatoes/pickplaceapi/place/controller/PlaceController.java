package talkingpotatoes.pickplaceapi.place.controller;

import static talkingpotatoes.pickplaceapi.global.api.SuccessCode.*;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import talkingpotatoes.pickplaceapi.global.api.ApiResponse;
import talkingpotatoes.pickplaceapi.place.dto.MapBound;
import talkingpotatoes.pickplaceapi.place.dto.PlaceMarkerResponse;
import talkingpotatoes.pickplaceapi.place.dto.PlaceRequest;
import talkingpotatoes.pickplaceapi.place.service.PlaceService;

/**
 * 장소 컨트롤러
 *
 * @author : 박지혁
 * @since : 2026/07/05
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/place")
public class PlaceController {

    private final PlaceService placeService;

    @PostMapping("/map")
    public ResponseEntity<ApiResponse<List<PlaceMarkerResponse>>> findPlacesInMap(@RequestBody MapBound mapBound) {
        return ApiResponse.ok(SUC_PLACE_FIND, placeService.findPlacesInMap(mapBound));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> createPlace(
            @RequestPart("request") PlaceRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        placeService.createPlace(request, files);
        return ApiResponse.ok(SUC_PLACE_CREATE);
    }

    @PatchMapping(value = "/{plSrl}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updatePlace(
            @PathVariable Long plSrl,
            @RequestPart("request") PlaceRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        placeService.updatePlace(plSrl, request, files);
        return ApiResponse.ok(SUC_PLACE_UPDATE);
    }

    @DeleteMapping("/{plSrl}")
    public ResponseEntity<ApiResponse<Void>> deletePlace(
            @PathVariable Long plSrl
    ) {
        placeService.deletePlace(plSrl);
        return ApiResponse.ok(SUC_PLACE_DELETE);
    }

    @GetMapping("/photos/{photoFileSrl}")
    public ResponseEntity<Resource> displayPhoto(@PathVariable Long photoFileSrl) {
        HttpHeaders headers = new HttpHeaders();
        Resource resource = placeService.displayPhoto(photoFileSrl, headers);

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

}
