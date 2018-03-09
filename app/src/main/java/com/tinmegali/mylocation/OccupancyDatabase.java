package com.tinmegali.mylocation;

/**
 * Created by akankanh on 3/8/18.
 */

public class OccupancyDatabase {
    String geoFenceId;
    int max_occupancy;
    int current_occupancy;
    double latitude;
    double longitude;
    double radius;

    OccupancyDatabase(String geoFenceId,
                      int max_occupancy,
                      int current_occupancy,
                      double latitude,
                      double longitude,
                      double radius) {

        this.geoFenceId = geoFenceId;
        this.max_occupancy = max_occupancy;
        this.current_occupancy = current_occupancy;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }

    public String toString() {
        return this.geoFenceId;
    }
}
