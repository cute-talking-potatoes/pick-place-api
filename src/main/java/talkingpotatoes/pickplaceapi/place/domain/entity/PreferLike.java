package talkingpotatoes.pickplaceapi.place.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import talkingpotatoes.pickplaceapi.global.domain.entity.BaseEntity;
import talkingpotatoes.pickplaceapi.user.domain.entity.User;

/**
 * 취향 좋아요 엔티티
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Table(name = "tb_prefer_like")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PreferLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pref_like_srl")
    private Long prefLikeSrl; // 취향 좋아요 NO (PK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_srl")
    private User user; // 회원

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pref_srl")
    private Prefer prefer; // 취향
}
