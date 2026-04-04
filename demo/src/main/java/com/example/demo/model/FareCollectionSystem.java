package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fare_collection_systems")
public class FareCollectionSystem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "system_code", nullable = false, unique = true)
    private String systemCode;

    @Column(name = "equipment_id")
    private String equipmentId;

    @Column(name = "vehicle_num")
    private String vehicleNum;

    public FareCollectionSystem() {
    }

    public FareCollectionSystem(String systemCode, String equipmentId, String vehicleNum) {
        this.systemCode = systemCode;
        this.equipmentId = equipmentId;
        this.vehicleNum = vehicleNum;
    }

    public Long getId() {
        return id;
    }

    public String getSystemCode() {
        return systemCode;
    }

    public void setSystemCode(String systemCode) {
        this.systemCode = systemCode;
    }

    public String getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(String equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getVehicleNum() {
        return vehicleNum;
    }

    public void setVehicleNum(String vehicleNum) {
        this.vehicleNum = vehicleNum;
    }
}
