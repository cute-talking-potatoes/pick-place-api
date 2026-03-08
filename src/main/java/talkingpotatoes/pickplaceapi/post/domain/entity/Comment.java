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
import talkingpotatoes.pickplaceapi.global.domain.ActiveStatus;
import talkingpotatoes.pickplaceapi.global.domain.entity.BaseEntity;
import talkingpotatoes.pickplaceapi.user.domain.entity.User;

/**
 * 댓글 엔티티
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Table(name = "tb_comment")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cmt_srl")
    private Long cmtSrl; // 댓글 NO (PK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_srl")
    private User user; // 회원

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_srl")
    private Post post; // 게시글

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cmt_parent")
    private Comment parent; // 댓글 부모

    @Column(name = "cmt_cont", columnDefinition = "TEXT")
    private String cmtCont; // 댓글 내용

    @Column(name = "cmt_sts")
    @Enumerated(value = EnumType.STRING)
    private ActiveStatus cmtSts; // 댓글 상태

}
