package com.example.mybestlocation.ui.home;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.mybestlocation.R;
import com.example.mybestlocation.databinding.FragmentHomeBinding;
import com.example.mybestlocation.Location; // Import the Location model
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private FragmentHomeBinding binding;
    private FusedLocationProviderClient fusedLocationClient;

    private RequestQueue requestQueue; // For making API calls
    private GoogleMap mGoogleMap; // Store the GoogleMap instance

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.maps);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize the request queue for API calls
        requestQueue = Volley.newRequestQueue(requireContext());

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mGoogleMap = googleMap; // Store the GoogleMap instance

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            getDeviceLocation(googleMap);

            // Fetch and display saved locations from the API
            displaySavedLocations(googleMap);

            // Set up an OnMapClickListener to capture user clicks on the map
            googleMap.setOnMapClickListener(latLng -> {
                // Show a dialog to allow the user to add this location to their favorites
                showAddLocationDialog(latLng.latitude, latLng.longitude);
            });
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void getDeviceLocation(GoogleMap googleMap) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.addMarker(new MarkerOptions().position(currentLocation).title("You are here"));
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                } else {
                    Toast.makeText(getContext(), "Unable to get current location", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void showAddLocationDialog(double latitude, double longitude) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add New Location");

        final EditText inputName = new EditText(getContext());
        final EditText inputPseudo = new EditText(getContext());

        inputName.setHint("Enter location name");
        inputPseudo.setHint("Enter your pseudo");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(inputName);
        layout.addView(inputPseudo);

        builder.setView(layout);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = inputName.getText().toString();
            String pseudo = inputPseudo.getText().toString();

            // Log the data for debugging
            Log.d("AddLocation", "Adding favorite location: name=" + name + ", pseudo=" + pseudo +
                    ", latitude=" + latitude + ", longitude=" + longitude);

            // Call the function to save this favorite location
            addFavoriteLocation(name, pseudo, latitude, longitude);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void addFavoriteLocation(String name, String pseudo, double latitude, double longitude) {
        // Your server URL to insert the favorite location
        String serverUrl = "http://192.168.1.16/myapp/api/insert_favorite_location.php";  // Change this URL

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    // Set up the URL connection
                    URL url = new URL(serverUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);

                    // Prepare the POST data
                    String data = "name=" + URLEncoder.encode(name, "UTF-8") +
                            "&pseudo=" + URLEncoder.encode(pseudo, "UTF-8") +
                            "&latitude=" + URLEncoder.encode(String.valueOf(latitude), "UTF-8") +
                            "&longitude=" + URLEncoder.encode(String.valueOf(longitude), "UTF-8");

                    // Send the data
                    OutputStream os = connection.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    writer.write(data);
                    writer.flush();
                    writer.close();
                    os.close();

                    // Get the server response
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        return "Location added successfully!";
                    } else {
                        return "Error adding location: " + responseCode;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return "Exception: " + e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String result) {
                // Show the result of the operation
                Log.d("AddLocation", result);
                if (result.equals("Location added successfully!")) {
                    // Optionally, you can update the map with the new marker here
                    addMarkerToMap(latitude, longitude, name);
                }
            }
        }.execute();
    }

    private void addMarkerToMap(double latitude, double longitude, String title) {
        if (mGoogleMap != null) {
            // Add marker on the map for the new favorite location
            LatLng location = new LatLng(latitude, longitude);
            mGoogleMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title(title));
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
        } else {
            Log.e("HomeFragment", "GoogleMap is not ready yet!");
        }
    }

    private void displaySavedLocations(GoogleMap googleMap) {
        String url = "http://192.168.1.16/myapp/api/locations.php";  // Update this URL

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d("API Response", "Received response: " + response.toString());
                    try {
                        List<Location> locationList = parseLocationsFromResponse(response);

                        // Add markers to the map
                        if (locationList != null && !locationList.isEmpty()) {
                            for (Location location : locationList) {
                                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                googleMap.addMarker(new MarkerOptions().position(latLng).title(location.getName()));
                            }
                        } else {
                            Log.e("API Error", "No locations found.");
                            Toast.makeText(getContext(), "No locations found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("API Error", "Error parsing response.");
                    }
                },
                error -> Log.e("API Error", "Error fetching data: " + error.getMessage())
        );

        requestQueue.add(jsonArrayRequest);
    }

    private List<Location> parseLocationsFromResponse(JSONArray response) {
        List<Location> locations = new ArrayList<>();
        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject jsonObject = response.getJSONObject(i);
                String name = jsonObject.getString("name");
                String pseudo = jsonObject.getString("pseudo");  // Extract the pseudo
                double latitude = jsonObject.getDouble("latitude");
                double longitude = jsonObject.getDouble("longitude");

                // Pass the pseudo along with the other parameters to the Location constructor
                Location location = new Location(name, pseudo, latitude, longitude);
                locations.add(location);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return locations;
    }

}
