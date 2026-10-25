package com.wardsentinel.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wardsentinel.backend.domain.model.SignalQuality;

public class SensorMetadataDto {

    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("firmwareVer")
    private String firmwareVer;

    @JsonProperty("signalQuality")
    private SignalQuality signalQuality = SignalQuality.GOOD;

    public SensorMetadataDto() {}

    public SensorMetadataDto(String deviceId, String firmwareVer, SignalQuality signalQuality) {
        this.deviceId = deviceId;
        this.firmwareVer = firmwareVer;
        this.signalQuality = signalQuality != null ? signalQuality : SignalQuality.GOOD;
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getFirmwareVer() { return firmwareVer; }
    public void setFirmwareVer(String firmwareVer) { this.firmwareVer = firmwareVer; }

    public SignalQuality getSignalQuality() { return signalQuality; }
    public void setSignalQuality(SignalQuality signalQuality) { this.signalQuality = signalQuality; }
}
