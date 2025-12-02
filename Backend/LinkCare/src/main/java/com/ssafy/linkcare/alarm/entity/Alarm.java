package com.ssafy.linkcare.alarm.entity;

import com.ssafy.linkcare.alarm.enums.MessageType;
import com.ssafy.linkcare.group.entity.Group;
import com.ssafy.linkcare.user.entity.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "alarm")
public class Alarm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_pk", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_user_pk", nullable = false)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_seq", nullable = false)
    private Group group;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private boolean isRead = false;

    @Builder
    public Alarm(User sender, User receiver, Group group, MessageType messageType, String content) {
        this.sender = sender;
        this.receiver = receiver;
        this.group = group;
        this.messageType = messageType;
        this.content = content;
    }

    public void read() {
        this.isRead = true;
    }
}
