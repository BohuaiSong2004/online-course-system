package com.bohuai.ocs.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugifyTest {

    @Test
    void slugifiesTitles() {
        assertThat(CourseService.slugify("Java & Spring Boot 101")).isEqualTo("java-spring-boot-101");
        assertThat(CourseService.slugify("  Café Résumé  ")).isEqualTo("cafe-resume");
        assertThat(CourseService.slugify("!!!")).isEqualTo("course");
        assertThat(CourseService.slugify("a".repeat(200))).hasSize(120);
    }
}
