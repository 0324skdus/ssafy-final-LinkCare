package com.ssafy.linkcare.health.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class DataSource {

    @Column(name = "data_source_a")
    private String a;

    @Column(name = "data_source_b")
    private String b;
}
