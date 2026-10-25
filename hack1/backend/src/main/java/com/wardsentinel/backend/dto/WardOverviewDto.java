package com.wardsentinel.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class WardOverviewDto {

    @JsonProperty("wardId")
    private String wardId;

    @JsonProperty("totalBeds")
    private Integer totalBeds;

    @JsonProperty("occupiedBeds")
    private Integer occupiedBeds;

    @JsonProperty("activeEventsCount")
    private Integer activeEventsCount;

    @JsonProperty("criticalEventsCount")
    private Integer criticalEventsCount;

    @JsonProperty("onShiftStaffCount")
    private Integer onShiftStaffCount;

    @JsonProperty("beds")
    private List<BedOverviewDto> beds;

    public WardOverviewDto() {}

    public String getWardId() { return wardId; }
    public void setWardId(String wardId) { this.wardId = wardId; }

    public Integer getTotalBeds() { return totalBeds; }
    public void setTotalBeds(Integer totalBeds) { this.totalBeds = totalBeds; }

    public Integer getOccupiedBeds() { return occupiedBeds; }
    public void setOccupiedBeds(Integer occupiedBeds) { this.occupiedBeds = occupiedBeds; }

    public Integer getActiveEventsCount() { return activeEventsCount; }
    public void setActiveEventsCount(Integer activeEventsCount) { this.activeEventsCount = activeEventsCount; }

    public Integer getCriticalEventsCount() { return criticalEventsCount; }
    public void setCriticalEventsCount(Integer criticalEventsCount) { this.criticalEventsCount = criticalEventsCount; }

    public Integer getOnShiftStaffCount() { return onShiftStaffCount; }
    public void setOnShiftStaffCount(Integer onShiftStaffCount) { this.onShiftStaffCount = onShiftStaffCount; }

    public List<BedOverviewDto> getBeds() { return beds; }
    public void setBeds(List<BedOverviewDto> beds) { this.beds = beds; }
}
