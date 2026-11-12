package com.infotact.aerosaga.model;

import java.io.Serializable;

/**
 * Meteorological evaluation result for airspace safety.
 */
public class WeatherReport implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean safeForFlight;
    private double windSpeedKnots;
    private double gustSpeedKnots;
    private double visibilityKm;
    private double precipitationMm;
    private boolean thunderstormAlert;
    private String conditionsSummary;

    public WeatherReport() {
    }

    public WeatherReport(boolean safeForFlight, double windSpeedKnots, double gustSpeedKnots,
                         double visibilityKm, double precipitationMm, boolean thunderstormAlert,
                         String conditionsSummary) {
        this.safeForFlight = safeForFlight;
        this.windSpeedKnots = windSpeedKnots;
        this.gustSpeedKnots = gustSpeedKnots;
        this.visibilityKm = visibilityKm;
        this.precipitationMm = precipitationMm;
        this.thunderstormAlert = thunderstormAlert;
        this.conditionsSummary = conditionsSummary;
    }

    public boolean isSafeForFlight() {
        return safeForFlight;
    }

    public void setSafeForFlight(boolean safeForFlight) {
        this.safeForFlight = safeForFlight;
    }

    public double getWindSpeedKnots() {
        return windSpeedKnots;
    }

    public void setWindSpeedKnots(double windSpeedKnots) {
        this.windSpeedKnots = windSpeedKnots;
    }

    public double getGustSpeedKnots() {
        return gustSpeedKnots;
    }

    public void setGustSpeedKnots(double gustSpeedKnots) {
        this.gustSpeedKnots = gustSpeedKnots;
    }

    public double getVisibilityKm() {
        return visibilityKm;
    }

    public void setVisibilityKm(double visibilityKm) {
        this.visibilityKm = visibilityKm;
    }

    public double getPrecipitationMm() {
        return precipitationMm;
    }

    public void setPrecipitationMm(double precipitationMm) {
        this.precipitationMm = precipitationMm;
    }

    public boolean isThunderstormAlert() {
        return thunderstormAlert;
    }

    public void setThunderstormAlert(boolean thunderstormAlert) {
        this.thunderstormAlert = thunderstormAlert;
    }

    public String getConditionsSummary() {
        return conditionsSummary;
    }

    public void setConditionsSummary(String conditionsSummary) {
        this.conditionsSummary = conditionsSummary;
    }
}
