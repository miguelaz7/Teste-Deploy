package pt.tub.ticketub.p8_alertas_e_excecoes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "alert_configuration")
public class AlertConfigurationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "detection_window_minutes", nullable = false)
    private int detectionWindowMinutes = 60;

    @Column(name = "repeat_card_threshold", nullable = false)
    private int repeatCardThreshold = 3;

    @Column(name = "volume_spike_threshold", nullable = false)
    private int volumeSpikeThreshold = 25;

    @Column(name = "critical_invalid_ratio", nullable = false)
    private double criticalInvalidRatio = 0.35d;

    @Column(name = "min_events_for_ratio", nullable = false)
    private int minEventsForRatio = 8;

    public Long getId() {
        return id;
    }

    public int getDetectionWindowMinutes() {
        return detectionWindowMinutes;
    }

    public void setDetectionWindowMinutes(int detectionWindowMinutes) {
        this.detectionWindowMinutes = detectionWindowMinutes;
    }

    public int getRepeatCardThreshold() {
        return repeatCardThreshold;
    }

    public void setRepeatCardThreshold(int repeatCardThreshold) {
        this.repeatCardThreshold = repeatCardThreshold;
    }

    public int getVolumeSpikeThreshold() {
        return volumeSpikeThreshold;
    }

    public void setVolumeSpikeThreshold(int volumeSpikeThreshold) {
        this.volumeSpikeThreshold = volumeSpikeThreshold;
    }

    public double getCriticalInvalidRatio() {
        return criticalInvalidRatio;
    }

    public void setCriticalInvalidRatio(double criticalInvalidRatio) {
        this.criticalInvalidRatio = criticalInvalidRatio;
    }

    public int getMinEventsForRatio() {
        return minEventsForRatio;
    }

    public void setMinEventsForRatio(int minEventsForRatio) {
        this.minEventsForRatio = minEventsForRatio;
    }
}