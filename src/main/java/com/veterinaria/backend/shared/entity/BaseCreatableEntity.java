package com.veterinaria.backend.shared.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@MappedSuperclass
public abstract class BaseCreatableEntity extends BaseEntity {

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
