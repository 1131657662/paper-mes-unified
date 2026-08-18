package com.paper.mes.ai.process.credential.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
public class AiProviderKeyUpdateRequest {

    @NotBlank
    @Size(min = 8, max = 512)
    @ToString.Exclude
    private String apiKey;
}
