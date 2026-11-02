package com.yp.selfheal.dc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NodeRegisterRequest(
    @NotBlank(message = "nodeId cannot be blank")
    String nodeId,

    @NotBlank(message = "host cannot be blank")
    String host,

    @NotNull(message = "port cannot be null")
    Integer port
) {}
