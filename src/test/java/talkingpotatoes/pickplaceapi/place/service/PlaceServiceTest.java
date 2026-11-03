package talkingpotatoes.pickplaceapi.place.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static talkingpotatoes.pickplaceapi.global.exception.ExceptionCode.ERR_AUTH_ACCESS_DENIED;
import static talkingpotatoes.pickplaceapi.global.exception.ExceptionCode.ERR_PLACE_NOT_FOUND;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import talkingpotatoes.pickplaceapi.file.domain.entity.File;
import talkingpotatoes.pickplaceapi.file.domain.entity.PhotoFile;
import talkingpotatoes.pickplaceapi.file.repository.PhotoFileRepository;
import talkingpotatoes.pickplaceapi.file.service.FileDownloadService;
import talkingpotatoes.pickplaceapi.file.service.FileService;
import talkingpotatoes.pickplaceapi.global.exception.AuthException;
import talkingpotatoes.pickplaceapi.global.exception.PlaceException;
import talkingpotatoes.pickplaceapi.global.security.UserInfoProvider;
import talkingpotatoes.pickplaceapi.place.domain.entity.Place;
import talkingpotatoes.pickplaceapi.place.dto.MapBound;
import talkingpotatoes.pickplaceapi.place.dto.PlaceDetailResponse;
import talkingpotatoes.pickplaceapi.place.dto.PlaceMarkerResponse;
import talkingpotatoes.pickplaceapi.place.dto.PlaceRequest;
import talkingpotatoes.pickplaceapi.place.repository.PlaceRepository;

@DisplayName("장소 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    private static final String WRITER_ID = "writer";

    private PlaceService placeService;

    @Mock
    private UserInfoProvider userInfoProvider;
    @Mock
    private PlaceRepository placeRepository;
    @Mock
    private PhotoFileRepository photoFileRepository;
    @Mock
    private FileService fileService;
    @Mock
    private FileDownloadService fileDownloadService;

    @BeforeEach
    void setUp() {
        placeService = new PlaceService(
                userInfoProvider,
                placeRepository,
                photoFileRepository,
                fileService,
                fileDownloadService
        );
    }

    @Test
    void 장소를_등록하고_첨부파일을_업로드한다() {
        // Given
        PlaceRequest request = createRequest("경복궁", 37.5786111111, 126.9772222222);
        List<MultipartFile> files = List.of(createFile("photo.jpg"));

        // When
        placeService.createPlace(request, files);

        // Then
        ArgumentCaptor<Place> placeCaptor = ArgumentCaptor.forClass(Place.class);
        then(placeRepository).should().save(placeCaptor.capture());

        Place savedPlace = placeCaptor.getValue();
        assertAll(
                () -> assertEquals("경복궁", savedPlace.getPlNm()),
                () -> assertEquals(37.5786111111, savedPlace.getPlLat()),
                () -> assertEquals(126.9772222222, savedPlace.getPlLng())
        );
        then(fileService).should().uploadPhoto(same(files), same(savedPlace));
    }

    @Test
    void 첨부파일이_없어도_장소를_등록한다() {
        // Given
        PlaceRequest request = createRequest("경복궁", 37.5786111111, 126.9772222222);

        // When
        placeService.createPlace(request, List.of());

        // Then
        then(placeRepository).should().save(any(Place.class));
        then(fileService).should(never()).uploadPhoto(any(), any());
    }

    @Test
    void 장소를_수정한다() {
        // Given
        Place place = createPlace(1L, WRITER_ID, createRequest("경복궁", 37.5786111111, 126.9772222222));
        PlaceRequest request = new PlaceRequest(
                "창덕궁", "수정 설명", "서울 종로구 율곡로 99", "돈화문", "03072", 37.5794444444, 126.9911111111
        );
        given(placeRepository.findByIdForUpdate(1L)).willReturn(Optional.of(place));

        // When
        placeService.updatePlace(1L, request, List.of());

        // Then
        then(userInfoProvider).should().checkUserAuthorization(WRITER_ID);
        assertAll(
                () -> assertEquals("창덕궁", place.getPlNm()),
                () -> assertEquals("수정 설명", place.getPlDesc()),
                () -> assertEquals("서울 종로구 율곡로 99", place.getPlAddr1()),
                () -> assertEquals("돈화문", place.getPlAddr2()),
                () -> assertEquals("03072", place.getPlCd()),
                () -> assertEquals(37.5794444444, place.getPlLat()),
                () -> assertEquals(126.9911111111, place.getPlLng())
        );
        then(placeRepository).should(never()).save(any(Place.class));
        then(fileService).should(never()).uploadPhoto(any(), any());
    }

    @Test
    void 장소수정시_첨부파일이_있으면_사진파일을_업로드한다() {
        // Given
        Place place = createPlace(1L, WRITER_ID, createRequest("경복궁", 37.5786111111, 126.9772222222));
        PlaceRequest request = createRequest("창덕궁", 37.5794444444, 126.9911111111);
        List<MultipartFile> files = List.of(createFile("photo.jpg"));
        given(placeRepository.findByIdForUpdate(1L)).willReturn(Optional.of(place));

        // When
        placeService.updatePlace(1L, request, files);

        // Then
        then(userInfoProvider).should().checkUserAuthorization(WRITER_ID);
        then(fileService).should().uploadPhoto(same(files), same(place));
        then(placeRepository).should(never()).findById(any()); // 사진 추가는 장소 행 락을 잡은 상태에서 수행한다
    }

    @Test
    void 존재하지않는_장소를_수정하면_예외가발생한다() {
        // Given
        given(placeRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

        // When & Then
        assertThrows(PlaceException.class, () -> placeService.updatePlace(
                1L,
                createRequest("창덕궁", 37.5794444444, 126.9911111111),
                List.of()
        ));
        then(userInfoProvider).should(never()).checkUserAuthorization(any());
        then(fileService).should(never()).uploadPhoto(any(), any());
    }

    @Test
    void 작성자가_아닌_사용자가_장소를_수정하면_예외가발생한다() {
        // Given
        Place place = createPlace(1L, WRITER_ID, createRequest("경복궁", 37.5786111111, 126.9772222222));
        given(placeRepository.findByIdForUpdate(1L)).willReturn(Optional.of(place));
        willThrow(new AuthException(ERR_AUTH_ACCESS_DENIED))
                .given(userInfoProvider)
                .checkUserAuthorization(WRITER_ID);

        // When & Then
        assertThrows(AuthException.class, () -> placeService.updatePlace(
                1L,
                createRequest("창덕궁", 37.5794444444, 126.9911111111),
                List.of(createFile("photo.jpg"))
        ));
        assertEquals("경복궁", place.getPlNm());
        then(fileService).should(never()).uploadPhoto(any(), any());
    }

    @Test
    void 장소를_삭제한다() {
        // Given
        Place place = createPlace(1L, WRITER_ID, createRequest("경복궁", 37.5786111111, 126.9772222222));
        given(placeRepository.findByIdForUpdate(1L)).willReturn(Optional.of(place));

        // When
        placeService.deletePlace(1L);

        // Then - 권한 확인 → 사진·파일 정리 → 장소 삭제 순서로 수행된다
        InOrder inOrder = inOrder(userInfoProvider, fileService, placeRepository);
        inOrder.verify(userInfoProvider).checkUserAuthorization(WRITER_ID);
        inOrder.verify(fileService).deletePlacePhotos(place);
        inOrder.verify(placeRepository).delete(place);
        then(placeRepository).should(never()).findById(any()); // 락 없는 조회를 쓰지 않는다
    }

    @Test
    void 존재하지않는_장소를_삭제하면_예외가발생한다() {
        // Given
        given(placeRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

        // When & Then
        assertThrows(PlaceException.class, () -> placeService.deletePlace(1L));
        then(userInfoProvider).should(never()).checkUserAuthorization(any());
        then(fileService).should(never()).deletePlacePhotos(any());
        then(placeRepository).should(never()).delete(any(Place.class));
    }

    @Test
    void 작성자가_아닌_사용자가_장소를_삭제하면_예외가발생한다() {
        // Given
        Place place = createPlace(1L, WRITER_ID, createRequest("경복궁", 37.5786111111, 126.9772222222));
        given(placeRepository.findByIdForUpdate(1L)).willReturn(Optional.of(place));
        willThrow(new AuthException(ERR_AUTH_ACCESS_DENIED))
                .given(userInfoProvider)
                .checkUserAuthorization(WRITER_ID);

        // When & Then
        assertThrows(AuthException.class, () -> placeService.deletePlace(1L));
        then(fileService).should(never()).deletePlacePhotos(any());
        then(placeRepository).should(never()).delete(any(Place.class));
    }

    @Test
    void 지도영역안의_장소를_조회한다() {
        // Given
        MapBound bound = new MapBound(37.0, 126.0, 38.0, 127.0);
        Place place1 = createPlace(2L, WRITER_ID, createRequest("경복궁", 37.5786111111, 126.9772222222));
        Place place2 = createPlace(1L, WRITER_ID, createRequest("창덕궁", 37.5794444444, 126.9911111111));
        PhotoFile photoFile1 = createPhotoFile(11L, createFileEntity(101L, "gyeongbokgung-1.jpg", ".jpg"), place1);
        PhotoFile photoFile2 = createPhotoFile(12L, createFileEntity(102L, "gyeongbokgung-2.png", ".png"), place1);
        given(placeRepository.findVisiblePlacesInBounds(bound)).willReturn(List.of(place1, place2));
        given(photoFileRepository.findByPlaceInWithFile(List.of(place1, place2)))
            .willReturn(List.of(photoFile1, photoFile2));

        // When
        List<PlaceMarkerResponse> result = placeService.findPlacesInMap(bound);

        // Then
        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertEquals(2L, result.get(0).plSrl()),
                () -> assertEquals("경복궁", result.get(0).plNm()),
                () -> assertEquals(2, result.get(0).photos().size()),
                () -> assertEquals(11L, result.get(0).photos().get(0).photoFileSrl()),
                () -> assertEquals(101L, result.get(0).photos().get(0).fileSrl()),
                () -> assertEquals("gyeongbokgung-1.jpg", result.get(0).photos().get(0).fileName()),
                () -> assertEquals("/api/place/photos/11", result.get(0).photos().get(0).imageUrl()),
                () -> assertEquals(1L, result.get(1).plSrl()),
                () -> assertEquals("창덕궁", result.get(1).plNm()),
                () -> assertTrue(result.get(1).photos().isEmpty())
        );
    }

    @Test
    void 장소사진을_화면표시용_리소스로_조회한다() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        File file = createFileEntity(101L, "gyeongbokgung.jpg", ".jpg");
        PhotoFile photoFile = createPhotoFile(11L, file,
            createPlace(1L, WRITER_ID, createRequest("경복궁", 37.5786111111, 126.9772222222)));
        Resource resource = mock(Resource.class);
        given(photoFileRepository.findByPhotoFileSrlWithFile(11L)).willReturn(Optional.of(photoFile));
        given(fileDownloadService.displayImage(file, headers)).willReturn(resource);

        // When
        Resource result = placeService.displayPhoto(11L, headers);

        // Then
        assertSame(resource, result);
        then(fileDownloadService).should().displayImage(file, headers);
    }

    @Test
    void 존재하지않는_장소사진을_조회하면_예외가발생한다() {
        // Given
        given(photoFileRepository.findByPhotoFileSrlWithFile(11L)).willReturn(Optional.empty());

        // When & Then
        assertThrows(PlaceException.class, () -> placeService.displayPhoto(11L, new HttpHeaders()));
        then(fileDownloadService).should(never()).displayImage(any(), any());
    }

    @Test
    void 작성자가_장소상세를_조회하면_사진과_함께_isOwner가_true다() {
        // Given
        Place place = createPlace(1L, WRITER_ID, new PlaceRequest(
                "경복궁", "조선 법궁", "서울 종로구 사직로 161", "광화문", "03045", 37.5786111111, 126.9772222222
        ));
        LocalDateTime createdAt = LocalDateTime.of(2026, 11, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 11, 2, 11, 30);
        ReflectionTestUtils.setField(place, "createdAt", createdAt);
        ReflectionTestUtils.setField(place, "updatedAt", updatedAt);
        PhotoFile photoFile1 = createPhotoFile(11L, createFileEntity(101L, "gyeongbokgung-1.jpg", ".jpg"), place);
        PhotoFile photoFile2 = createPhotoFile(12L, createFileEntity(102L, "gyeongbokgung-2.png", ".png"), place);
        given(placeRepository.findById(1L)).willReturn(Optional.of(place));
        given(photoFileRepository.findByPlaceWithFile(place)).willReturn(List.of(photoFile1, photoFile2));
        given(userInfoProvider.getUserId()).willReturn(WRITER_ID);

        // When
        PlaceDetailResponse result = placeService.findPlaceDetail(1L);

        // Then
        assertAll(
                () -> assertEquals(1L, result.plSrl()),
                () -> assertEquals("경복궁", result.plNm()),
                () -> assertEquals("조선 법궁", result.plDesc()),
                () -> assertEquals("서울 종로구 사직로 161", result.plAddr1()),
                () -> assertEquals("광화문", result.plAddr2()),
                () -> assertEquals("03045", result.plCd()),
                () -> assertEquals(37.5786111111, result.plLat()),
                () -> assertEquals(126.9772222222, result.plLng()),
                () -> assertEquals(createdAt, result.createdAt()),
                () -> assertEquals(updatedAt, result.updatedAt()),
                () -> assertTrue(result.isOwner()),
                () -> assertEquals(2, result.photos().size()),
                () -> assertEquals(11L, result.photos().get(0).photoFileSrl()),
                () -> assertEquals(101L, result.photos().get(0).fileSrl()),
                () -> assertEquals("gyeongbokgung-1.jpg", result.photos().get(0).fileName()),
                () -> assertEquals(".jpg", result.photos().get(0).fileExtension()),
                () -> assertEquals("/api/place/photos/11", result.photos().get(0).imageUrl()),
                () -> assertEquals(12L, result.photos().get(1).photoFileSrl()),
                () -> assertEquals("/api/place/photos/12", result.photos().get(1).imageUrl())
        );
        then(placeRepository).should(never()).findByIdForUpdate(any()); // 조회는 락을 쓰지 않는다
        then(photoFileRepository).should(never()).findByPlaceInWithFile(any());
    }

    @Test
    void 작성자가_아닌_사용자가_장소상세를_조회하면_isOwner가_false다() {
        // Given
        Place place = createPlace(1L, WRITER_ID, createRequest("경복궁", 37.5786111111, 126.9772222222));
        given(placeRepository.findById(1L)).willReturn(Optional.of(place));
        given(photoFileRepository.findByPlaceWithFile(place)).willReturn(List.of());
        given(userInfoProvider.getUserId()).willReturn("other");

        // When
        PlaceDetailResponse result = placeService.findPlaceDetail(1L);

        // Then
        assertFalse(result.isOwner());
        then(userInfoProvider).should(never()).checkUserAuthorization(any()); // 타인도 조회는 가능하다
    }

    @Test
    void 작성자정보가_없는_장소상세를_조회하면_isOwner가_false다() {
        // Given
        Place place = createPlace(1L, null, createRequest("경복궁", 37.5786111111, 126.9772222222));
        given(placeRepository.findById(1L)).willReturn(Optional.of(place));
        given(photoFileRepository.findByPlaceWithFile(place)).willReturn(List.of());
        given(userInfoProvider.getUserId()).willReturn(WRITER_ID);

        // When
        PlaceDetailResponse result = placeService.findPlaceDetail(1L);

        // Then
        assertFalse(result.isOwner());
    }

    @Test
    void 사진이_없는_장소상세를_조회하면_빈_사진목록을_반환한다() {
        // Given
        Place place = createPlace(1L, WRITER_ID, createRequest("경복궁", 37.5786111111, 126.9772222222));
        given(placeRepository.findById(1L)).willReturn(Optional.of(place));
        given(photoFileRepository.findByPlaceWithFile(place)).willReturn(List.of());
        given(userInfoProvider.getUserId()).willReturn(WRITER_ID);

        // When
        PlaceDetailResponse result = placeService.findPlaceDetail(1L);

        // Then
        assertNotNull(result.photos());
        assertTrue(result.photos().isEmpty());
    }

    @Test
    void 존재하지않는_장소상세를_조회하면_예외가발생한다() {
        // Given
        given(placeRepository.findById(1L)).willReturn(Optional.empty());

        // When & Then
        PlaceException exception = assertThrows(PlaceException.class, () -> placeService.findPlaceDetail(1L));
        assertEquals(ERR_PLACE_NOT_FOUND.name(), exception.getCode());
        then(photoFileRepository).should(never()).findByPlaceWithFile(any());
    }

    @Test
    void 장소상세조회는_읽기전용_트랜잭션으로_수행된다() throws NoSuchMethodException {
        // When
        Transactional transactional = PlaceService.class
                .getMethod("findPlaceDetail", Long.class)
                .getAnnotation(Transactional.class);

        // Then
        assertNotNull(transactional);
        assertTrue(transactional.readOnly());
    }

    private PlaceRequest createRequest(String name, Double lat, Double lng) {
        return new PlaceRequest(name, "설명", "서울 종로구 사직로 161 경복궁", "", "03045", lat, lng);
    }

    private Place createPlace(Long placeId, String createdBy, PlaceRequest request) {
        Place place = new Place(request);
        ReflectionTestUtils.setField(place, "plSrl", placeId);
        ReflectionTestUtils.setField(place, "createdBy", createdBy);
        return place;
    }

    private MultipartFile createFile(String filename) {
        return new MockMultipartFile(
                "files",
                filename,
                "image/jpeg",
                "data".getBytes(StandardCharsets.UTF_8)
        );
    }

    private File createFileEntity(Long fileSrl, String fileName, String extension) {
        return File.builder()
                .fileSrl(fileSrl)
                .fileNm(fileName)
                .fileExtension(extension)
                .fileManageSrl("uuid")
                .fileSeq(1L)
                .fileSrc("/tmp/" + fileName)
                .build();
    }

    private PhotoFile createPhotoFile(Long photoFileSrl, File file, Place place) {
        return PhotoFile.builder()
                .photoFileSrl(photoFileSrl)
                .file(file)
                .place(place)
                .build();
    }
}
