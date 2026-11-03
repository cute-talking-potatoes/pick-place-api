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
import talkingpotatoes.pickplaceapi.place.dto.PlaceCreateResponse;
import talkingpotatoes.pickplaceapi.place.dto.PlaceDetailResponse;
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

    /**
     * 장소 등록
     * <pre>
     *  - 생성된 장소 번호(plSrl)를 응답으로 돌려준다. (IDENTITY 전략이라 save 시점에 PK가 채워진다)
     * </pre>
     */
    @Transactional
    public PlaceCreateResponse createPlace(PlaceRequest request, List<MultipartFile> files) {
        Place place = new Place(request);
        placeRepository.save(place);
        if (hasFiles(files)) {
            fileService.uploadPhoto(files, place);
        }
        return new PlaceCreateResponse(place);
    }

    @Transactional
    public void updatePlace(Long plSrl, PlaceRequest request, List<MultipartFile> files) {
        Place place = getPlaceForUpdate(plSrl); // 삭제와 동시에 사진이 추가되지 않도록 장소 행 락
        userInfoProvider.checkUserAuthorization(place.getCreatedBy()); // 작성자와 수정자가 동일한지 확인
        place.updatePlace(request); // TODO: 2026/07/2 더티체킹이 정상적으로 발생하여 업데이트가 되는지 확인하기
        if (hasFiles(files)) {
            fileService.uploadPhoto(files, place);
        }
    }

    /**
     * 장소 삭제
     * <pre>
     *  - 작성자 권한을 먼저 확인한다.
     *  - 장소 사진(tb_photo_file)과 파일(tb_file) 행을 삭제한 뒤 장소를 삭제한다.
     *  - 디스크 파일은 트랜잭션 커밋 이후에만 삭제된다. (롤백 시 유지)
     *  - 장소 행을 쓰기 락으로 조회해 동시에 실행되는 사진 추가(updatePlace)와 직렬화한다.
     * </pre>
     */
    @Transactional
    public void deletePlace(Long plSrl) {
        Place place = getPlaceForUpdate(plSrl);
        userInfoProvider.checkUserAuthorization(place.getCreatedBy()); // 작성자와 수정자가 동일한지 확인
        fileService.deletePlacePhotos(place); // 장소 사진·파일 정리 (FK 참조 행을 먼저 삭제)
        placeRepository.delete(place);
    }

    /**
     * 장소 사진 한 장 삭제
     * <pre>
     *  1. 장소 행을 쓰기 락으로 조회한다. (장소 삭제·사진 추가와 직렬화)
     *  2. 작성자 권한을 확인한다. 권한 확인 전에는 사진 존재 여부도 조회하지 않는다.
     *  3. 사진은 해당 장소 범위 안에서만 조회한다. 다른 장소의 사진 번호이거나 없으면 ERR_PLACE_PHOTO_NOT_FOUND.
     *  4. 사진·파일 행(tb_photo_file, tb_file, 연결된 tb_user_file)을 삭제한다.
     *  5. 디스크 파일은 트랜잭션 커밋 이후에만 삭제된다. (롤백 시 유지, 삭제 실패는 로그만 남긴다)
     * </pre>
     */
    @Transactional
    public void deletePlacePhoto(Long plSrl, Long photoFileSrl) {
        Place place = getPlaceForUpdate(plSrl);
        userInfoProvider.checkUserAuthorization(place.getCreatedBy()); // 작성자와 요청자가 동일한지 확인
        PhotoFile photoFile = photoFileRepository.findByPhotoFileSrlAndPlaceWithFile(photoFileSrl, place)
                .orElseThrow(() -> new PlaceException(ERR_PLACE_PHOTO_NOT_FOUND));
        fileService.deletePlacePhoto(photoFile);
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

    /**
     * 장소 상세 조회
     * <pre>
     *  - 락 없이 조회한다. (읽기 전용 트랜잭션)
     *  - 사진은 장소 1건 기준 JOIN FETCH 쿼리 1회로 photoFileSrl 오름차순 조회한다.
     *  - 작성자 아이디는 응답에 넣지 않고, 현재 사용자가 작성자인지만 isOwner로 내려준다.
     * </pre>
     */
    @Transactional(readOnly = true)
    public PlaceDetailResponse findPlaceDetail(Long plSrl) {
        Place place = getPlace(plSrl);
        List<PhotoFile> photoFiles = photoFileRepository.findByPlaceWithFile(place);
        boolean isOwner = userInfoProvider.getUserId().equals(place.getCreatedBy());
        return new PlaceDetailResponse(place, photoFiles, isOwner);
    }

    public Resource displayPhoto(Long photoFileSrl, HttpHeaders headers) {
        PhotoFile photoFile = photoFileRepository.findByPhotoFileSrlWithFile(photoFileSrl)
                .orElseThrow(() -> new PlaceException(ERR_PLACE_NOT_FOUND));
        return fileDownloadService.displayImage(photoFile.getFile(), headers);
    }

    private Place getPlace(Long placeId) {
        return placeRepository.findById(placeId).orElseThrow(() -> new PlaceException(ERR_PLACE_NOT_FOUND));
    }

    private Place getPlaceForUpdate(Long placeId) {
        return placeRepository.findByIdForUpdate(placeId)
                .orElseThrow(() -> new PlaceException(ERR_PLACE_NOT_FOUND));
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
