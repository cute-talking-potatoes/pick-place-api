package talkingpotatoes.pickplaceapi.place.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import talkingpotatoes.pickplaceapi.file.service.FileService;
import talkingpotatoes.pickplaceapi.place.domain.entity.Place;
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

    private final PlaceRepository placeRepository;
    private final FileService fileService;

    @Transactional
    public void createPlace(PlaceRequest request, List<MultipartFile> files) {
        Place place = new Place(request);
        fileService.uploadImage(files, place);
        placeRepository.save(place);
    }
}
