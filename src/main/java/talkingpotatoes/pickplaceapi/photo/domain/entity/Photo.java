package talkingpotatoes.pickplaceapi.photo.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import talkingpotatoes.pickplaceapi.global.domain.Visibility;
import talkingpotatoes.pickplaceapi.global.domain.entity.BaseEntity;
import talkingpotatoes.pickplaceapi.place.domain.entity.Place;
import talkingpotatoes.pickplaceapi.user.domain.entity.User;

/**
 * 사진첩 엔티티
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Table(name = "tb_photo")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Photo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_srl")
    private Long photoSrl; // 사진 NO (PK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_srl")
    private User user; // 회원

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pl_srl")
    private Place place; // 위치

    @Column(name = "photo_title")
    private String photoTitle; // 사진 제목

    @Column(name = "photo_desc")
    private String photoDesc; // 사진 설명

    @Column(name = "photo_visibility")
    @Enumerated(value = EnumType.STRING)
    private Visibility photoVisibility; // 사진 공개범위

}
