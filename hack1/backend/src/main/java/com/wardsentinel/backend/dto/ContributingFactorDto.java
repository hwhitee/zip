package com.wardsentinel.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContributingFactorDto {

    @JsonProperty("feature_name")
    private String featureName;

    @JsonProperty("value")
    private Double value;

    @JsonProperty("weight")
    private Double weight;

    public ContributingFactorDto() {}

    public ContributingFactorDto(String featureName, Double value, Double weight) {
        this.featureName = featureName;
        this.value = value;
        this.weight = weight;
    }

    public String getFeatureName() { return featureName; }
    public void setFeatureName(String featureName) { this.featureName = featureName; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
}
