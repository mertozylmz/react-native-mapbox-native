package com.reactlibrary;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.util.Log;

import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.MapboxDirections;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.OnLocationClickListener;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.core.constants.Constants.PRECISION_6;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineCap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineJoin;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

public class MapboxNativeManager extends SimpleViewManager implements OnMapReadyCallback, LocationEngineListener, OnLocationClickListener {
    public static final String REACT_CLASS = "MapboxNative";
    private static final String TAG = "MainActivity";

    public Boolean isHotspotActive = false;

    private static final String GEOJSON_SOURCE = "geojson-source-lineLayer";
    private static final String DIRECTIONS_LAYER = "directions-layer";
    private static final String POLYGON_LAYER = "polygon-layer";
    private static final String HOTSPOT_POLYGON_LAYER = "hotspot-polygon-layer";

    private static final String MARKER_SOURCE = "marker-source";
    private static final String MARKER_STYLE_LAYER = "marker-style-layer";
    private static final String MARKER_IMAGE = "custom-marker";
    private static final String POLYGON_SOURCE = "polygon-source";
    private static final String HOTSPOT_POLYGON_SOURCE = "hotspot-polygon-source";


    private static ThemedReactContext mContext;
    private LocationEngine locationEngine;
    private static MapboxMap mapboxMap;
    public static MapView mapView;

    private Location originLocation;
    private FeatureCollection featureCollection;
    private FeatureCollection featureCollectionMarker;
    private FeatureCollection featureCollectionPolygon;
    private FeatureCollection featureCollectionHotspotPolygon;

    private static final List<List<Point>> POINTS = new ArrayList<>();
    private static final List<Point> OUTER_POINTS = new ArrayList<>();
    private static final List<List<Point>> HOTSPOT_POINTS = new ArrayList<>();
    private static final List<Point> HOTSPOT_OUTER_POINTS = new ArrayList<>();

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public MapView createViewInstance(ThemedReactContext reactContext) {
        mContext = reactContext;
        Mapbox.getInstance(mContext, "pk.eyJ1IjoiYmVyeW1vMTIzIiwiYSI6ImNqbjNoZXNraTAwNzQza3J4YnB4MjN5bDAifQ.G3ZaO08XKciKI3WXhnj4xA");
        mapView = new MapView(mContext);
        mapView.findViewById(R.id.mapView);
        mapView.getMapAsync(this);

        return mapView;
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        MapboxNativeManager.mapboxMap = mapboxMap;
        initializeLocationEngine();
        initializeLocationLayer();

        Bitmap icon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_marker);
        mapboxMap.addImage(MARKER_IMAGE, icon);
        createRouteLayer();
    }

    @SuppressLint("MissingPermission")
    private void initializeLocationLayer() {
        LocationComponentOptions options = LocationComponentOptions.builder(mContext)
                .trackingGesturesManagement(true)
                .bearingDrawable(R.drawable.user_compass_triangle)
                .backgroundDrawable(R.drawable.user_compass_background)
                .backgroundDrawableStale(R.drawable.user_compass_background)
                .foregroundTintColor(Color.WHITE)
                .foregroundStaleTintColor(Color.WHITE)
                .build();

        LocationComponent locationComponent = mapboxMap.getLocationComponent();
        locationComponent.activateLocationComponent(mContext, options);
        locationComponent.setLocationComponentEnabled(true);
        locationComponent.setRenderMode(RenderMode.COMPASS);
        locationComponent.addOnLocationClickListener(this);
    }

    @SuppressWarnings("MissingPermission")
    private void initializeLocationEngine() {
        locationEngine = new LocationEngineProvider(mContext).obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            originLocation = lastLocation;
            setCameraPosition(lastLocation);
        } else {
            locationEngine.addLocationEngineListener(this);
        }
    }

    private void setCameraPosition(Location location) {
        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13.0), 2000);
        final LocationComponent locationComponent = mapboxMap.getLocationComponent();
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                locationComponent.setCameraMode(CameraMode.TRACKING_COMPASS);
            }
        }, 5000);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (originLocation == null) {
            originLocation = location;
            setCameraPosition(location);
        }
    }

    @Override
    public void onLocationComponentClick() {
        LocationComponent locationComponent = mapboxMap.getLocationComponent();
        locationComponent.setCameraMode(CameraMode.TRACKING_COMPASS);
    }

    private void createRouteLayer() {
        featureCollection = FeatureCollection.fromFeatures(new Feature[] {});
        featureCollectionMarker = FeatureCollection.fromFeatures(new Feature[] {});
        featureCollectionPolygon = FeatureCollection.fromFeatures(new Feature[] {});
        featureCollectionHotspotPolygon = FeatureCollection.fromFeatures(new Feature[] {});

        mapboxMap.addSource(new GeoJsonSource(GEOJSON_SOURCE, featureCollection));
        mapboxMap.addSource(new GeoJsonSource(MARKER_SOURCE, featureCollectionMarker));
        mapboxMap.addSource(new GeoJsonSource(POLYGON_SOURCE, featureCollectionPolygon));
        mapboxMap.addSource(new GeoJsonSource(HOTSPOT_POLYGON_SOURCE, featureCollectionHotspotPolygon));
    }

    // REACT NATIVE ----------------- BRIDGE -----------------
    @ReactMethod
    public void drawPolygon(@Nullable ReadableArray coordinates, final Boolean isHotspot) {
        if (isHotspot) {
            isHotspotActive = true;

            for (int i = 0; i<coordinates.size(); i++) {
                HOTSPOT_OUTER_POINTS.add(Point.fromLngLat(coordinates.getArray(i).getDouble(0), coordinates.getArray(i).getDouble(1)));
            }
            HOTSPOT_POINTS.add(HOTSPOT_OUTER_POINTS);

            mContext.getCurrentActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    GeoJsonSource source = mapboxMap.getSourceAs(HOTSPOT_POLYGON_SOURCE);

                    source.setGeoJson(Polygon.fromLngLats(HOTSPOT_POINTS));
                    mapboxMap.addLayerBelow(new FillLayer(HOTSPOT_POLYGON_LAYER, HOTSPOT_POLYGON_SOURCE).withProperties(
                            fillOpacity(0.2f),
                            fillColor(Color.parseColor("#017aff"))
                    ), "road-label-small");
                }
            });
        } else {
            for (int i = 0; i<coordinates.size(); i++) {
                OUTER_POINTS.add(Point.fromLngLat(coordinates.getArray(i).getDouble(0), coordinates.getArray(i).getDouble(1)));
            }
            POINTS.add(OUTER_POINTS);

            mContext.getCurrentActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    GeoJsonSource source = mapboxMap.getSourceAs(POLYGON_SOURCE);

                    source.setGeoJson(Polygon.fromLngLats(POINTS));
                    mapboxMap.addLayerBelow(new FillLayer(POLYGON_LAYER, POLYGON_SOURCE).withProperties(
                            fillOpacity(0.1f),
                            fillColor(Color.parseColor("#017aff"))
                    ), "road-label-small");
                }
            });
        }
    }

    @ReactMethod
    public void addPoint(@Nullable final ReadableArray coordinates) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                List<Feature> features = new ArrayList<>();
                features.add(Feature.fromGeometry(Point.fromLngLat(coordinates.getDouble(0), coordinates.getDouble(1))));
                featureCollectionMarker = FeatureCollection.fromFeatures(features);
                GeoJsonSource source = mapboxMap.getSourceAs(MARKER_SOURCE);

                Layer layer = mapboxMap.getLayerAs(MARKER_STYLE_LAYER);

                if (source != null) {
                    source.setGeoJson(featureCollectionMarker);
                }
                if (layer == null) {
                    Float[] floats = {0f, -20f};
                    SymbolLayer markerStyleLayer = new SymbolLayer(MARKER_STYLE_LAYER, MARKER_SOURCE)
                            .withProperties(
                                    PropertyFactory.iconOffset(floats),
                                    PropertyFactory.iconAllowOverlap(true),
                                    PropertyFactory.iconImage(MARKER_IMAGE)
                            );
                    mapboxMap.addLayer(markerStyleLayer);
                }
            }
        });
    }

    @ReactMethod
    public void setCoordinates(@Nullable ReadableArray coordinates) {
        getRoute(
                Point.fromLngLat(
                        coordinates.getArray(0).getDouble(0),
                        coordinates.getArray(0).getDouble(1)
                ),
                Point.fromLngLat(
                        coordinates.getArray(1).getDouble(0),
                        coordinates.getArray(1).getDouble(1)
                ));
    }

    private void getRoute(final Point origin, final Point destination) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Layer layer = mapboxMap.getLayerAs(DIRECTIONS_LAYER);

                if (layer == null) {
                    featureCollection = FeatureCollection.fromFeatures(new Feature[] {});
                    mapboxMap.addLayerBelow(new LineLayer(DIRECTIONS_LAYER, GEOJSON_SOURCE).withProperties(
                            lineCap(Property.LINE_CAP_ROUND),
                            lineJoin(Property.LINE_JOIN_ROUND),
                            lineWidth(4f),
                            lineColor(Color.parseColor("#5c5c5c"))
                    ), "road-label-small");
                }


                MapboxDirections client = MapboxDirections.builder()
                        .accessToken("pk.eyJ1IjoiYmVyeW1vMTIzIiwiYSI6ImNqbjNoZXNraTAwNzQza3J4YnB4MjN5bDAifQ.G3ZaO08XKciKI3WXhnj4xA")
                        .origin(origin)
                        .destination(destination)
                        .overview(DirectionsCriteria.OVERVIEW_FULL)
                        .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                        .build();

                client.enqueueCall(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if (response.body() == null) {
                            Log.e(TAG, "No routes found, check right user and access token");
                            return;
                        } else if (response.body().routes().size() == 0) {
                            Log.e(TAG, "No routes found" );
                            return;
                        }

                        List<Feature> directionsRouteFeatureList = new ArrayList<>();
                        LineString lineString = LineString.fromPolyline(response.body().routes().get(0).geometry(), PRECISION_6);
                        List<Point> coordinates = lineString.coordinates();
                        for (int i = 0; i<coordinates.size(); i++) {
                            directionsRouteFeatureList.add(Feature.fromGeometry(LineString.fromLngLats(coordinates)));
                        }
                        featureCollection = FeatureCollection.fromFeatures(directionsRouteFeatureList);
                        GeoJsonSource source = mapboxMap.getSourceAs(GEOJSON_SOURCE);
                        if (source != null) {
                            source.setGeoJson(featureCollection);
                        }
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        Log.e(TAG, "Error: " + t.getMessage());
                    }
                });
            }
        });
    }

    @ReactMethod
    public void clearMapItems() {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mapboxMap.removeLayer(DIRECTIONS_LAYER);
                mapboxMap.removeLayer(MARKER_STYLE_LAYER);
            }
        });
    }
}
