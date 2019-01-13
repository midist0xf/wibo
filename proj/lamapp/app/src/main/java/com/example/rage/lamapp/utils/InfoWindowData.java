package com.example.rage.lamapp.utils;

public class InfoWindowData {

    private String mgrs;
    private String occurrences;
    private String avgSignalStrength;

    public InfoWindowData(String mgrs, String occurrences, String avgSignalStrength) {
        this.mgrs = mgrs;
        this.occurrences = occurrences;
        this.avgSignalStrength = avgSignalStrength;
    }

    public String getMgrs() {
        return mgrs;
    }

    public void setMgrs(String mgrs) {
        this.mgrs = mgrs;
    }

    public String getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(String occurrences) {
        this.occurrences = occurrences;
    }

    public String getAvgSignalStrength() {
        return avgSignalStrength;
    }

    public void setAvgSignalStrength(String avgSignalStrength) {
        this.avgSignalStrength = avgSignalStrength;
    }
}
