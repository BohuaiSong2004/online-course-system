package com.bohuai.ocs.service;

import com.bohuai.ocs.model.AssignmentRow;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AssignmentServiceTest {

    private final AssignmentService service = new AssignmentService(null, null, null,
            Clock.fixed(Instant.parse("2026-11-20T12:00:00Z"), ZoneOffset.UTC));

    private String status(String enrollment, String due, int done) {
        AssignmentRow r = new AssignmentRow();
        r.setEnrollmentStatus(enrollment);
        r.setDueDate(due == null ? null : LocalDate.parse(due));
        r.setCompletedChapters(done);
        r.setTotalChapters(4);
        service.decorate(r);
        return r.getStatus();
    }

    @Test
    void derivesStatus() {
        assertThat(status("COMPLETED", "2026-11-01", 4)).isEqualTo("COMPLETED"); // completed late is still completed
        assertThat(status("ACTIVE", "2026-11-19", 2)).isEqualTo("OVERDUE");
        assertThat(status("ACTIVE", "2026-11-20", 0)).isEqualTo("NOT_STARTED"); // due today is not overdue
        assertThat(status("ACTIVE", null, 1)).isEqualTo("IN_PROGRESS");
    }

    @Test
    void csvCellsAreEscapedAndFormulaSafe() {
        assertThat(AssignmentService.cell("Smith, Jane")).isEqualTo("\"Smith, Jane\"");
        assertThat(AssignmentService.cell("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(AssignmentService.cell("=HYPERLINK(\"x\")")).startsWith("\"'=HYPERLINK");
        assertThat(AssignmentService.cell(null)).isEmpty();
    }
}
