package com.wardsentinel.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wardsentinel.backend.dto.InferenceResponseDto;
import com.wardsentinel.backend.dto.TelemetryWindowRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
public class InferenceClientService {

    private static final Logger log = LoggerFactory.getLogger(InferenceClientService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${inference.service.url:http://localhost:8001}")
    private String inferenceServiceUrl;

    public InferenceClientService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<InferenceResponseDto> callPredict(TelemetryWindowRequest request) {
        String endpoint = inferenceServiceUrl.replaceAll("/+$", "") + "/predict";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            HttpEntity<TelemetryWindowRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<InferenceResponseDto> response = restTemplate.postForEntity(
                    endpoint,
                    entity,
                    InferenceResponseDto.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                InferenceResponseDto body = response.getBody();
                if (body.isValid()) {
                    return Optional.of(body);
                } else {
                    log.error("Inference response failed semantic validation: missing required fields or invalid bounds");
                    return Optional.empty();
                }
            } else {
                log.warn("Inference service returned non-2xx code: {}", response.getStatusCode());
                return Optional.empty();
            }
        } catch (ResourceAccessException rae) {
            log.error("Network or timeout error calling Python inference service at {}: {}", endpoint, rae.getMessage());
            return Optional.empty();
        } catch (HttpStatusCodeException hse) {
            log.error("Inference service HTTP error {}: {}", hse.getStatusCode(), hse.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception ex) {
            log.error("Unexpected error communicating with inference service: {}", ex.getMessage(), ex);
            return Optional.empty();
        }
    }
}
