package com.example.jaineek.meeplemain.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.jaineek.meeplemain.FeedActivity;
import com.example.jaineek.meeplemain.R;
import com.example.jaineek.meeplemain.ViewPostActivity;
import com.example.jaineek.meeplemain.adapters_and_holders.MeepleInfoWindowAdapter;
import com.example.jaineek.meeplemain.model.Post;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

import static android.content.Context.MODE_PRIVATE;
import static com.example.jaineek.meeplemain.FeedActivity.PATH_TO_GEOFIRE;
import static com.example.jaineek.meeplemain.FeedActivity.PATH_TO_POSTS;

/**
 * Created by Krishnak97 on 7/5/2016.
 */

public class MyMapFragment extends Fragment implements MeepleFragment,
        OnMapReadyCallback, ValueEventListener {
    // Container fragment and handler for GoogleMap SupportMapFragment

    public static final String TAG = "FRAGMENT_MAP";
    private static String title_map_fragment = "My Location";
    private static int drawable_icon_id = R.drawable.ic_location_on_white_48dp;


    private MapView mMapView;
    private GoogleMap mMap;
    private Location mLastLocation;
    private SharedPreferences mSharedPreferences;

    private DatabaseReference mPostsReference;
    private DatabaseReference mGeoFireReference;
    private double queryRadius;
    private HashMap<Marker, Post> mMarkersToPosts;

    private LayoutInflater mLayoutInflater;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Save all instance information
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_map, container, false);
        mLayoutInflater = inflater;

        mMapView = (MapView) v.findViewById(R.id.google_mapView);
        mMapView.onCreate(savedInstanceState);  // Must be forwarded

        mPostsReference = FirebaseDatabase.getInstance().getReference(PATH_TO_POSTS);
        mGeoFireReference = FirebaseDatabase.getInstance().getReference(PATH_TO_GEOFIRE);

        mMarkersToPosts = new HashMap<>();

        // Save radius
        mSharedPreferences = getActivity().getSharedPreferences("preferences", MODE_PRIVATE);
        queryRadius = mSharedPreferences.getFloat("key_change_radius",
                (float) FeedActivity.DEFAULT_RADIUS);


        // Create geoQuery for user location
        mLastLocation = ((FeedActivity) getActivity()).getmLastLocation();

        // Set onMapCallBack
        mMapView.getMapAsync(this);

        return v;
    }

    private void setupGeoQueryWithMarkers(GeoQuery geoQuery) {
        // Adds event listeners for drawing markers
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                mPostsReference.child(key).addValueEventListener(MyMapFragment.this);
            }

            @Override
            public void onKeyExited(String key) {
                mPostsReference.child(key).removeEventListener(MyMapFragment.this);
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                onKeyExited(key);
                onKeyEntered(key, location);
            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    /* FIREBASE VALUE EVENT LISTENER METHODS */

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        // Draw map marker
        Post post = dataSnapshot.getValue(Post.class);
        double lat = post.eventLocation.getLatitude();
        double lon = post.eventLocation.getLongitude();
        Marker marker = mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
                .title(post.eventTitle).snippet(post.eventDesc));

        // Add to Marker HashMap
        mMarkersToPosts.put(marker, post);
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }


    /* MEEPLE FRAGMENT METHODS */

    @Override
    public String getTitle() {
        // Returns title of fragment
        return title_map_fragment;
    }

    @Override
    public String getFragmentTag() {
        return TAG;
    }

    @Override
    public int getDrawableIconId() {
        // Returns Drawable tab icon for this page
        return drawable_icon_id;
    }

    /* GOOGLE MAPS METHODS */

//    @Override
//    public void onMapReady(GoogleMap map) {
//        // Once map is ready, perform setup
//        mMap = map;
//        // Enables MyLocation button on map
//        if (ContextCompat.checkSelfPermission(getActivity(),
//                android.Manifest.permission.ACCESS_FINE_LOCATION)
//                == PackageManager.PERMISSION_GRANTED) {
//            // Check if Lcoation services are enabled
//            mMap.setMyLocationEnabled(true);
//            mMap.getUiSettings().setMyLocationButtonEnabled(true);
//
//            // Allow custom Info Windows
//            mMap.setInfoWindowAdapter(new MeepleInfoWindowAdapter(mLayoutInflater, mMarkersToPosts));
//
//            // Allow Info Window clicks
//            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
//                @Override
//                public void onInfoWindowClick(Marker marker) {
//                    // Send this post to ViewPostActivity
//                    Post post = mMarkersToPosts.get(marker);
//                    Intent toViewPostActivity = new Intent(getActivity(), ViewPostActivity.class);
//                    toViewPostActivity.putExtra(ViewPostActivity.KEY_EXTRA_POST, post);
//                    startActivity(toViewPostActivity);
//                }
//            });
//
//            if (mLastLocation != null) {
//                GeoLocation geoCenter = new GeoLocation(mLastLocation.getLatitude(),
//                        mLastLocation.getLongitude());
//                GeoFire geoFire = new GeoFire(mGeoFireReference);
//                GeoQuery geoQuery = geoFire.queryAtLocation(geoCenter, queryRadius);
//
//                setupGeoQueryWithMarkers(geoQuery);
//            }
//
//        } else {
//             // Notify the user that ACCESS LOCATION permission is disabled
//            Toast.makeText(getActivity(), getString(R.string.error_location_not_supported),
//                    Toast.LENGTH_LONG).show();
//        }
//    }

    @Override
    public void onMapReady(GoogleMap map) {
        // Once map is ready, perform setup
        mMap = map;
        // Enables MyLocation button on map
        if (ContextCompat.checkSelfPermission(getActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Check if Lcoation services are enabled
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);

            // Allow custom Info Windows
            mMap.setInfoWindowAdapter(new MeepleInfoWindowAdapter(mLayoutInflater, mMarkersToPosts));

            // Allow Info Window clicks
            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                    // Send this post to ViewPostActivity
                    Post post = mMarkersToPosts.get(marker);
                    Intent toViewPostActivity = new Intent(getActivity(), ViewPostActivity.class);
                    toViewPostActivity.putExtra(ViewPostActivity.KEY_EXTRA_POST, post);
                    startActivity(toViewPostActivity);
                }
            });

            if (mLastLocation != null) {
                // Create circle around user location
                double userLat = mLastLocation.getLatitude();
                double userLon = mLastLocation.getLongitude();
                LatLng center = new LatLng(userLat, userLon);
                Circle localCircle = mMap.addCircle(new CircleOptions().center(center)
                    .radius(kmRadiusToCircleRadius(queryRadius))
                        // Color hex format: 2 bit % transparency + 6 bit color
                        .fillColor(0x401BB3F5)
                        .strokeColor(0x883369E8));

                Toast.makeText(getActivity(), TAG + ": " + Double.toString(queryRadius),
                        Toast.LENGTH_SHORT).show();


                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center,
                        getZoomLevel(localCircle)));

                // Setup GeoFire query
                GeoLocation geoCenter = new GeoLocation(mLastLocation.getLatitude(),
                        mLastLocation.getLongitude());
                GeoFire geoFire = new GeoFire(mGeoFireReference);
                GeoQuery geoQuery = geoFire.queryAtLocation(geoCenter, queryRadius);
                setupGeoQueryWithMarkers(geoQuery);
            }

        } else {
            // Notify the user that ACCESS LOCATION permission is disabled
            Toast.makeText(getActivity(), getString(R.string.error_location_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private float getZoomLevel(Circle circle) {
        // Returns proper zoom level for circle
        float zoomLevel = 11;
        if (circle != null) {
            double radius = circle.getRadius() + circle.getRadius() / 2;
            double scale = radius / 500;
            zoomLevel = (float) (16 - Math.log(scale) / Math.log(2));
        }

        float padding = (float) 0.25;
        return zoomLevel - padding;
    }

    private double kmRadiusToCircleRadius(double km) {
        // Returns radius in meters for CircleOptions
        return km * 1000;
    }

    /* LIFECYCLE METHODS  -
    *       MapView requires all lifecycle methods
    *       to be forwarded from the hosting activity /
    *       fragment
    */

    @Override
    public void onResume() {
        // Must be forwarded
        super.onResume();
        mMapView.onResume();

        //New runnable for faster loading of the map
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MapView mapView = new MapView(getContext());
                    mapView.onCreate(null);
                    mapView.onPause();
                    mapView.onDestroy();
                }catch (Exception ignored){

                }
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        // Must be forwarded
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        // Must be forwarded
        super.onLowMemory();
        mMapView.onLowMemory();
    }
}
