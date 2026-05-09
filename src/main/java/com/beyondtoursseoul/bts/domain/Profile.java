package com.beyondtoursseoul.bts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    private String nickname;

    @Column(name = "preferred_language")
    private String preferredLanguage;

    @Column(name = "visit_count")
    private Integer visitCount;

    @Column(name = "local_preference")
    private String localPreference;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    /**
     * auth.users에만 있고 profiles 행이 아직 없는 계정용 (저장함 등 첫 API에서 보강).
     */
    public static Profile createForUser(UUID id) {
        Profile p = new Profile();
        OffsetDateTime now = OffsetDateTime.now();
        p.id = id;
        p.visitCount = 0;
        p.createdAt = now;
        p.updatedAt = now;
        return p;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateLocalPreference(String localPreference) {
        this.localPreference = localPreference;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updatePreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
        this.updatedAt = OffsetDateTime.now();
    }

}
