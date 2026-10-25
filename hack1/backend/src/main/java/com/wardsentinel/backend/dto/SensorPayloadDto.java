package com.wardsentinel.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class SensorPayloadDto {

    @NotNull
    @JsonProperty("heartRateBpm")
    private List<Double> heartRateBpm;

    @NotNull
    @JsonProperty("spo2Pct")
    private List<Double> spo2Pct;

    @NotNull
    @JsonProperty("respRateBreaths")
    private List<Double> respRateBreaths;

    @NotNull
    @JsonProperty("skinTempC")
    private List<Double> skinTempC;

    @NotNull
    @JsonProperty("motionG")
    private List<Double> motionG;

    @NotNull
    @JsonProperty("ambientNoiseDb")
    private List<Double> ambientNoiseDb;

    @NotNull
    @JsonProperty("lightLux")
    private List<Double> lightLux;

    @JsonProperty("torsoElevation")
    private List<Double> torsoElevation;

    @JsonProperty("edgeProximity")
    private List<Double> edgeProximity;

    @JsonProperty("lowerBodyMovement")
    private List<Double> lowerBodyMovement;

    @JsonProperty("bedPressureShift")
    private List<Double> bedPressureShift;

    @JsonProperty("occupancyConfidence")
    private List<Double> occupancyConfidence;

    public SensorPayloadDto() {}

    public List<Double> getHeartRateBpm() { return heartRateBpm; }
    public void setHeartRateBpm(List<Double> heartRateBpm) { this.heartRateBpm = heartRateBpm; }

    public List<Double> getSpo2Pct() { return spo2Pct; }
    public void setSpo2Pct(List<Double> spo2Pct) { this.spo2Pct = spo2Pct; }

    public List<Double> getRespRateBreaths() { return respRateBreaths; }
    public void setRespRateBreaths(List<Double> respRateBreaths) { this.respRateBreaths = respRateBreaths; }

    public List<Double> getSkinTempC() { return skinTempC; }
    public void setSkinTempC(List<Double> skinTempC) { this.skinTempC = skinTempC; }

    public List<Double> getMotionG() { return motionG; }
    public void setMotionG(List<Double> motionG) { this.motionG = motionG; }

    public List<Double> getAmbientNoiseDb() { return ambientNoiseDb; }
    public void setAmbientNoiseDb(List<Double> ambientNoiseDb) { this.ambientNoiseDb = ambientNoiseDb; }

    public List<Double> getLightLux() { return lightLux; }
    public void setLightLux(List<Double> lightLux) { this.lightLux = lightLux; }

    public List<Double> getTorsoElevation() { return torsoElevation; }
    public void setTorsoElevation(List<Double> torsoElevation) { this.torsoElevation = torsoElevation; }

    public List<Double> getEdgeProximity() { return edgeProximity; }
    public void setEdgeProximity(List<Double> edgeProximity) { this.edgeProximity = edgeProximity; }

    public List<Double> getLowerBodyMovement() { return lowerBodyMovement; }
    public void setLowerBodyMovement(List<Double> lowerBodyMovement) { this.lowerBodyMovement = lowerBodyMovement; }

    public List<Double> getBedPressureShift() { return bedPressureShift; }
    public void setBedPressureShift(List<Double> bedPressureShift) { this.bedPressureShift = bedPressureShift; }

    public List<Double> getOccupancyConfidence() { return occupancyConfidence; }
    public void setOccupancyConfidence(List<Double> occupancyConfidence) { this.occupancyConfidence = occupancyConfidence; }
}
