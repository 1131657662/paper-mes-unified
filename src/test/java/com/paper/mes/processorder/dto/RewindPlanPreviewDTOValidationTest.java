package com.paper.mes.processorder.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RewindPlanPreviewDTOValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validate_whenRepeatCountExceeds500_rejectsRequest() {
        RewindPlanPreviewDTO dto = validDto();
        dto.getSegments().getFirst().setRepeatCount(501);

        assertThat(validator.validate(dto)).anyMatch(violation ->
                violation.getPropertyPath().toString().equals("segments[0].repeatCount"));
    }

    @Test
    void validate_whenLayoutQuantityExceeds500_rejectsRequest() {
        RewindPlanPreviewDTO dto = validDto();
        dto.getSegments().getFirst().getLayoutItems().getFirst().setQuantity(501);

        assertThat(validator.validate(dto)).anyMatch(violation ->
                violation.getPropertyPath().toString().equals("segments[0].layoutItems[0].quantity"));
    }

    private RewindPlanPreviewDTO validDto() {
        RewindPlanPreviewDTO.RewindLayoutItemDTO item = new RewindPlanPreviewDTO.RewindLayoutItemDTO();
        item.setWidth(1000);
        item.setQuantity(1);
        RewindPlanPreviewDTO.RewindSegmentDTO segment = new RewindPlanPreviewDTO.RewindSegmentDTO();
        segment.setRepeatCount(1);
        segment.setLayoutItems(List.of(item));
        RewindPlanPreviewDTO dto = new RewindPlanPreviewDTO();
        dto.setRewindMode(1);
        dto.setSegments(List.of(segment));
        return dto;
    }
}
