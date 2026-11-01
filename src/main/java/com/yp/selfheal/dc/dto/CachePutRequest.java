package com.yp.selfheal.dc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CachePutRequest(
    @NotBlank(message = "Key cannot be blank")
    String key,

    @NotNull(message = "Value cannot be null")
    Object value,

    Long ttlSeconds
) {}
