package com.projectx.common.geo;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/** WGS84 (SRID 4326) point helpers shared by every module that stores or compares locations. */
public final class GeoPoints {

    public static final int SRID_WGS84 = 4326;
    private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), SRID_WGS84);
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private GeoPoints() {}

    public static Point toPoint(GeoPoint geoPoint) {
        return toPoint(geoPoint.latitude(), geoPoint.longitude());
    }

    public static Point toPoint(double latitude, double longitude) {
        // JTS/PostGIS coordinate order is (x=lng, y=lat).
        return FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    public static GeoPoint toGeoPoint(Point point) {
        return new GeoPoint(point.getY(), point.getX());
    }

    /** Haversine great-circle distance in meters - used for in-memory checks (e.g. against the Redis cache). */
    public static double distanceMeters(Point a, Point b) {
        double lat1 = Math.toRadians(a.getY());
        double lat2 = Math.toRadians(b.getY());
        double deltaLat = Math.toRadians(b.getY() - a.getY());
        double deltaLng = Math.toRadians(b.getX() - a.getX());

        double h = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
        return EARTH_RADIUS_METERS * c;
    }
}
