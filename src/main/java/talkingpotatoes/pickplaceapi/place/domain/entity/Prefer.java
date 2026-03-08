package talkingpotatoes.pickplaceapi.place.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import talkingpotatoes.pickplaceapi.global.domain.entity.BaseEntity;

/**
 * 취향 엔티티
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Table(name = "tb_prefer")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Prefer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pref_srl")
    private Long prefSrl; // 취향 NO (PK)

    @Column(name = "pref_cate", length = 100)
    private String prefCate; // 취향 카테고리

    @Column(name = "pref_Type", length = 100)
    private String prefType; // 취향 타입
}
