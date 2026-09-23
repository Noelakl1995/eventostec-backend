package com.eventostec.api.domain.event;


import org.springframework.web.multipart.MultipartFile;

public record EventRequestDTO(
    String name,
    String description,
    String location,
    Long date,
    String city,
    String state,
    Boolean remote,
    String eventUrl,
    MultipartFile[] image
) {
}