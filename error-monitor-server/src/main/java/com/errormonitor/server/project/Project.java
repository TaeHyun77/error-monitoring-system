package com.errormonitor.server.project;

import com.errormonitor.server.common.BaseTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "projects")
public class Project extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String projectId;

    @Column(unique = true, nullable = false)
    private String apiKey;

    @Column(nullable = false)
    private String name;

    private String description;

    @Builder
    public Project(String projectId, String apiKey, String name, String description) {
        this.projectId = projectId;
        this.apiKey = apiKey;
        this.name = name;
        this.description = description;
    }
}
