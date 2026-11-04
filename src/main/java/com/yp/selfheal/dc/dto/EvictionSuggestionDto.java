package com.yp.selfheal.dc.dto;

import java.util.List;

public record EvictionSuggestionDto(
    List<String> suggestedEvictionKeys,
    String reasoning,
    boolean aiGuided
) {}
