package com.ssafy.linkcare.group.entity;

import com.ssafy.linkcare.group.enums.GroupType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "`groups`")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_seq")
    private Long groupSeq;

    @Column(name = "group_name", nullable = false, length = 50)
    private String groupName;

    @Column(name = "group_description", columnDefinition = "TEXT")
    private String groupDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private GroupType type;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Group(String groupName, String groupDescription, GroupType type, Integer capacity, String imageUrl) {
        this.groupName = groupName;
        this.groupDescription = groupDescription;
        this.type = type;
        this.capacity = capacity;
        this.imageUrl = imageUrl;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void updateGroupInfo(String groupName, String groupDescription) {
        this.groupName = groupName;
        this.groupDescription = groupDescription;
    }
}
