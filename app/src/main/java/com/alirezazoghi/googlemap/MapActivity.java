package com.alirezazoghi.googlemap;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.SimpleOnSearchActionListener;
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;

    private static final String USER_LOCATION = "My Location";

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1313;
    private static final int ERROR_DIALOG_REQUEST = 1234;

    private static final float DEFAULT_ZOOM = 15f;

    private static final int MAP_TYPE_NORMAL = 1;
    private static final int MAP_TYPE_SATELLITE = 2;

    private boolean mLocationPermissionsGranted = false;
    private int mapType = MAP_TYPE_NORMAL;

    private GoogleMap mMap;
    private PlacesClient placesClient;
    private AutocompleteSessionToken token;

    private List<AutocompletePrediction> predictionList;

    private ImageView mapTypeSwitch;

    private SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        if (isServiceSdk()) {
            init();
            getLocationPermission();
        }
    }

    private boolean isServiceSdk() {
        Log.e(TAG, "isServiceSdk: check google service version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);

        if (available == ConnectionResult.SUCCESS) {
            Log.e(TAG, "isServiceSdk: google play service is working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            Log.e(TAG, "isServiceSdk: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "we cant make map request", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void init() {
        ImageView gpsImageView = findViewById(R.id.ic_gps);
        mapTypeSwitch = findViewById(R.id.satellite);
        MaterialSearchBar materialSearchBar = findViewById(R.id.searchBar);

        mapTypeSwitch.setOnClickListener(view -> initMap());

        materialSearchBar.setOnSearchActionListener(new SimpleOnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                super.onSearchStateChanged(enabled);
            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                //geoLocate(text.toString());
                startSearch(text.toString(), true, null, true);
            }

            @Override
            public void onButtonClicked(int buttonCode) {
                if (buttonCode == MaterialSearchBar.BUTTON_BACK) {
                    materialSearchBar.disableSearch();
                }
            }
        });
        materialSearchBar.addTextChangeListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                if (i2 == 0) {
                    predictionList = new ArrayList<>();
                    if (!materialSearchBar.isSuggestionsVisible()) {
                        materialSearchBar.hideSuggestionsList();
                    }
                } else {
                    FindAutocompletePredictionsRequest predictionsRequest = FindAutocompletePredictionsRequest.builder()
                            .setTypeFilter(TypeFilter.ADDRESS)
                            .setSessionToken(token)
                            .setQuery(s.toString())
                            .build();

                    placesClient.findAutocompletePredictions(predictionsRequest).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FindAutocompletePredictionsResponse predictionsResponse = task.getResult();
                            if (predictionsResponse != null) {
                                predictionList = predictionsResponse.getAutocompletePredictions();
                                List<String> suggestionList = new ArrayList<>();

                                for (int j = 0; j < predictionList.size(); j++) {
                                    AutocompletePrediction prediction = predictionList.get(j);
                                    suggestionList.add(prediction.getFullText(null).toString());
                                }
                                Log.e(TAG, "ok : " + s + " list" + predictionList.size());
                                materialSearchBar.updateLastSuggestions(suggestionList);
                                if (!materialSearchBar.isSuggestionsVisible()) {
                                    materialSearchBar.showSuggestionsList();
                                }
                            }
                        } else {
                            Log.e(TAG, "error : " + s);
                        }
                    }).addOnFailureListener(e -> Log.e(TAG, "onFailure: " + e.getMessage()));
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        materialSearchBar.setSuggestionsClickListener(new SuggestionsAdapter.OnItemViewClickListener() {
            @Override
            public void OnItemClickListener(int position, View v) {
                if (position >= predictionList.size()) {
                    return;
                }

                AutocompletePrediction autocompletePrediction = predictionList.get(position);
                String suggestion = materialSearchBar.getLastSuggestions().get(position).toString();
                materialSearchBar.setText(suggestion);
                materialSearchBar.clearSuggestions();
                hideSoftKeyboard();
                String placeId = autocompletePrediction.getPlaceId();
                List<Place.Field> placeField = Collections.singletonList(Place.Field.LAT_LNG);

                FetchPlaceRequest fetchPlaceRequest = FetchPlaceRequest.builder(placeId, placeField).build();
                placesClient.fetchPlace(fetchPlaceRequest).addOnSuccessListener(fetchPlaceResponse -> {

                    Place place = fetchPlaceResponse.getPlace();
                    Log.e(TAG, "place found " + place.getName());
                    String placeName = place.getName();
                    LatLng latLngOfPlace = place.getLatLng();
                    if (latLngOfPlace != null) {
                        moveCamera(latLngOfPlace, DEFAULT_ZOOM, placeName == null ? materialSearchBar.getText() : placeName);
                    }
                }).addOnFailureListener(e -> {
                    if (e instanceof ApiException) {
                        ApiException apiException = (ApiException) e;
                        int statusCode = apiException.getStatusCode();
                        Log.e(TAG, "place not found " + e.getMessage());
                        Log.e(TAG, "status code " + statusCode);
                    }
                });
            }

            @Override
            public void OnItemDeleteListener(int position, View v) {

            }
        });

        MaterialSearchBar.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(20, getStatusBarHeight(), 20, 0);
        materialSearchBar.setLayoutParams(layoutParams);

        gpsImageView.setOnClickListener(view -> {
            Toast.makeText(MapActivity.this, "find my location", Toast.LENGTH_SHORT).show();
            getDeviceLocation();
        });

    }

    private void getLocationPermission() {
        String[] permissions = {FINE_LOCATION,
                COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(MapActivity.this, FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(MapActivity.this, COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void initMap() {
        if (mapFragment == null)
            mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    private void getDeviceLocation() {
        FusedLocationProviderClient mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        Places.initialize(this, getString(R.string.api_key));
        placesClient = Places.createClient(this);
        token = AutocompleteSessionToken.newInstance();

        try {
            if (mLocationPermissionsGranted) {
                mFusedLocationProviderClient.getLastLocation()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.e(TAG, "onComplete: found location");
                                Location currentLocation = task.getResult();

                                if (currentLocation != null) {
                                    moveCamera(new LatLng(currentLocation.getLatitude(),
                                                    currentLocation.getLongitude()),
                                            DEFAULT_ZOOM, USER_LOCATION);
                                }

                            } else {
                                Log.e(TAG, "onComplete: current location is null");
                                Toast.makeText(MapActivity.this, "unable to get location", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(e -> Toast.makeText(MapActivity.this, "error on get map - " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        } catch (Exception e) {
            Log.e(TAG, "getDeviceLocation: SecurityException" + e);
        }
    }

    private void moveCamera(LatLng latLng, float zoom, String title) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        mMap.clear();
        if (!title.equals(USER_LOCATION)) {
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title);
            mMap.addMarker(options);
        }
    }

    private void geoLocate(String name) {
        try {
            Geocoder geocoder = new Geocoder(MapActivity.this, Locale.getDefault());
            List<Address> list = geocoder.getFromLocationName(name, 1);
            if (list.size() > 0) {
                Address address = list.get(0);
                Log.e(TAG, "geoLocate: " + address.toString());
                moveCamera(new LatLng(address.getLatitude(), address.getLongitude()),
                        DEFAULT_ZOOM,
                        address.getAddressLine(0));
            }
        } catch (IOException e) {
            Log.e(TAG, "geoLocate: error=" + e);
        }
        hideSoftKeyboard();
    }

    private void hideSoftKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        /*View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }*/
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        googleMap.setMapType(getMapType());

        mMap = googleMap;

        if (mLocationPermissionsGranted) {
            getDeviceLocation();

            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    private int getMapType() {
        if (mapType == MAP_TYPE_NORMAL) {
            mapType = MAP_TYPE_SATELLITE;
            mapTypeSwitch.setImageResource(R.drawable.ic_satelite);
        } else {
            mapType = MAP_TYPE_NORMAL;
            mapTypeSwitch.setImageResource(R.drawable.ic_map);
        }
        return mapType;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionsGranted = false;
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        mLocationPermissionsGranted = false;
                        return;
                    }
                }
                mLocationPermissionsGranted = true;
                initMap();
            }
        }
    }

    //google autoComplete searchView
    private void getAutoCompleteFragment() {
         /*AutocompleteSupportFragment autocompleteSupportFragment
                = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.LAT_LNG, Place.Field.NAME));

        autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                LatLng latLng = place.getLatLng();
                Log.e(TAG, "onPlaceSelected: " + latLng.latitude + "\n" + latLng.longitude);
            }

            @Override
            public void onError(@NonNull Status status) {

            }
        });*/
    }
}
