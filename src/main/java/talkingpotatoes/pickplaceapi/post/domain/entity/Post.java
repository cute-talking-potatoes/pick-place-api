package talkingpotatoes.pickplaceapi.post.domain.entity;

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
import lombok.Setter;
import talkingpotatoes.pickplaceapi.global.domain.ActiveStatus;
import talkingpotatoes.pickplaceapi.global.domain.entity.BaseEntity;
import talkingpotatoes.pickplaceapi.post.domain.PostType;
import talkingpotatoes.pickplaceapi.user.domain.entity.User;

/**
 * 게시글 엔티티
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Table(name = "tb_post")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_srl")
    private Long postStl; // 게시글 NO (PK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_srl")
    private User user; // 회원

    @Column(name = "post_type")
    @Enumerated(value = EnumType.STRING)
    private PostType postType; // 게시글 타입

    @Column(name = "post_title")
    private String postTitle; // 게시글 제목

    @Column(name = "post_cont", columnDefinition = "TEXT")
    private String postCont; // 게시글 본문

    @Setter
    @Column(name = "post_view_cnt", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long postViewCnt = 0L; // 게시글 조회수 / 기본값 0

    @Column(name = "post_sts")
    @Enumerated(value = EnumType.STRING)
    private ActiveStatus postSts; // 게시글 상태

    @Column(name = "is_replied", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean isReplied = false; // 게시글 답변 여부 (질문게시글) / 기본값 false (사용하지 않아도 false 설정)
}
