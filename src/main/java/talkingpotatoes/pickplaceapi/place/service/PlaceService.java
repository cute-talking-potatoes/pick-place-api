package talkingpotatoes.pickplaceapi.place.service;

import static talkingpotatoes.pickplaceapi.global.exception.ExceptionCode.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import talkingpotatoes.pickplaceapi.file.domain.entity.PhotoFile;
import talkingpotatoes.pickplaceapi.file.repository.PhotoFileRepository;
import talkingpotatoes.pickplaceapi.file.service.FileDownloadService;
import talkingpotatoes.pickplaceapi.file.service.FileService;
import talkingpotatoes.pickplaceapi.global.exception.PlaceException;
import talkingpotatoes.pickplaceapi.global.security.UserInfoProvider;
import talkingpotatoes.pickplaceapi.place.domain.entity.Place;
import talkingpotatoes.pickplaceapi.place.dto.MapBound;
import talkingpotatoes.pickplaceapi.place.dto.PlaceMarkerResponse;
import talkingpotatoes.pickplaceapi.place.dto.PlaceRequest;
import talkingpotatoes.pickplaceapi.place.repository.PlaceRepository;

/**
 * 장소 서비스
 *
 * @author : 박지혁
 * @since : 2026/06/21
 */
@Service
@RequiredArgsConstructor
public class PlaceService {

    private final UserInfoProvider userInfoProvider;
    private final PlaceRepository placeRepository;
    private final PhotoFileRepository photoFileRepository;
    private final FileService fileService;
    private final FileDownloadService fileDownloadService;

    @Transactional
    public void createPlace(PlaceRequest request, List<MultipartFile> files) {
        Place place = new Place(request);
        placeRepository.save(place);
        if (hasFiles(files)) {
            fileService.uploadPhoto(files, place);
        }
    }

    @Transactional
    public void updatePlace(Long plSrl, PlaceRequest request, List<MultipartFile> files) {
        Place place = getPlace(plSrl);
        userInfoProvider.checkUserAuthorization(place.getCreatedBy()); // 작성자와 수정자가 동일한지 확인
        place.updatePlace(request); // TODO: 2026/07/2 더티체킹이 정상적으로 발생하여 업데이트가 되는지 확인하기
        if (hasFiles(files)) {
            fileService.uploadPhoto(files, place);
        }
    }

    @Transactional
    public void deletePlace(Long plSrl) {
        Place place = getPlace(plSrl);
        userInfoProvider.checkUserAuthorization(place.getCreatedBy()); // 작성자와 수정자가 동일한지 확인
        placeRepository.delete(place);
    }

    public List<PlaceMarkerResponse> findPlacesInMap(MapBound bound) {
        List<Place> places = placeRepository.findVisiblePlacesInBounds(bound);
        Map<Long, List<PhotoFile>> photoFileMap = findPhotoFilesByPlace(places);

        return places.stream()
                .map(place -> new PlaceMarkerResponse(
                        place,
                        photoFileMap.getOrDefault(place.getPlSrl(), List.of())
                ))
                .toList();
    }

    public Resource displayPhoto(Long photoFileSrl, HttpHeaders headers) {
        PhotoFile photoFile = photoFileRepository.findByPhotoFileSrlWithFile(photoFileSrl)
                .orElseThrow(() -> new PlaceException(ERR_PLACE_NOT_FOUND));
        return fileDownloadService.displayImage(photoFile.getFile(), headers);
    }

    private Place getPlace(Long placeId) {
        return placeRepository.findById(placeId).orElseThrow(() -> new PlaceException(ERR_PLACE_NOT_FOUND));
    }

    private boolean hasFiles(List<MultipartFile> files) {
        return files != null && !files.isEmpty();
    }

    private Map<Long, List<PhotoFile>> findPhotoFilesByPlace(List<Place> places) {
        if (places.isEmpty()) {
            return Collections.emptyMap();
        }

        return photoFileRepository.findByPlaceInWithFile(places).stream()
                .collect(Collectors.groupingBy(photoFile -> photoFile.getPlace().getPlSrl()));
    }
}
