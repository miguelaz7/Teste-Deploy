package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "routes")
public class Route {

    @Id
    @Column(name = "route_id")
    private Long routeId;

    @Column(name = "agency_id")
    private String agencyId;

    @Column(name = "route_short_name")
    private String routeShortName;

    @Column(name = "route_long_name")
    private String routeLongName;

    @Column(name = "route_type")
    private Integer routeType;

    public Route() {}

    public Route(Long routeId, String agencyId, String routeShortName, String routeLongName, Integer routeType) {
        this.routeId = routeId;
        this.agencyId = agencyId;
        this.routeShortName = routeShortName;
        this.routeLongName = routeLongName;
        this.routeType = routeType;
    }

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public String getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
    }

    public String getRouteShortName() {
        return routeShortName;
    }

    public void setRouteShortName(String routeShortName) {
        this.routeShortName = routeShortName;
    }

    public String getRouteLongName() {
        return routeLongName;
    }

    public void setRouteLongName(String routeLongName) {
        this.routeLongName = routeLongName;
    }

    public Integer getRouteType() {
        return routeType;
    }

    public void setRouteType(Integer routeType) {
        this.routeType = routeType;
    }
}
