package com.example.mybestlocation.ui.dashboard;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.mybestlocation.Location;
import com.example.mybestlocation.LocationAdapter;
import com.example.mybestlocation.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import android.telephony.SmsManager;

public class DashboardFragment extends Fragment implements LocationAdapter.OnLocationActionListener {

    private RecyclerView recyclerView;
    private LocationAdapter locationAdapter;
    private RequestQueue requestQueue;
    private static final String BASE_URL = "http://192.168.1.16/myapp/api/locations.php";

    // SMS Permission request code
    private static final int SMS_PERMISSION_REQUEST_CODE = 101;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Initialize Volley request queue
        requestQueue = Volley.newRequestQueue(getContext());

        // Initialize RecyclerView
        recyclerView = root.findViewById(R.id.recycler_view_locations);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize the adapter and set it to the RecyclerView
        locationAdapter = new LocationAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(locationAdapter);

        // Fetch locations from the server
        fetchLocationsFromServer();

        return root;
    }

    private void fetchLocationsFromServer() {
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, BASE_URL, null,
                response -> {
                    try {
                        // Parse and update the locations list
                        List<Location> locations = parseLocationResponse(response);
                        if (locations != null) {
                            locationAdapter.updateLocations(locations);
                            locationAdapter.notifyDataSetChanged(); // Ensure UI updates
                        }
                    } catch (Exception e) {
                        Log.e("FetchLocations", "Error parsing locations: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Error parsing locations: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // Handle and log errors
                    Log.e("FetchLocations", "Error fetching locations: " + error.getMessage(), error);
                    Toast.makeText(getContext(), "Failed to fetch locations. Please try again.", Toast.LENGTH_SHORT).show();
                });

        // Add the request to the request queue
        requestQueue.add(jsonArrayRequest);
    }

    private List<Location> parseLocationResponse(JSONArray response) {
        List<Location> locations = new ArrayList<>();
        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject obj = response.getJSONObject(i);
                Location location = new Location(
                        obj.getInt("id"),
                        obj.getString("name"),
                        obj.getString("pseudo"),
                        obj.getDouble("latitude"),
                        obj.getDouble("longitude")
                );
                locations.add(location);
            }
        } catch (JSONException e) {
            Toast.makeText(getContext(), "Error parsing locations: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return locations;
    }

    @Override
    public void onEditLocation(Location location) {
        showEditLocationDialog(location);
    }

    @Override
    public void onDeleteLocation(Location location) {
        StringRequest stringRequest = new StringRequest(Request.Method.DELETE, BASE_URL,
                response -> {
                    Toast.makeText(getContext(), "Location deleted", Toast.LENGTH_SHORT).show();
                    fetchLocationsFromServer(); // Refresh the list
                },
                error -> Toast.makeText(getContext(), "Failed to delete location: " + error.getMessage(), Toast.LENGTH_SHORT).show()) {
            @Override
            public byte[] getBody() {
                return ("{\"id\":" + location.getId() + "}").getBytes();
            }
        };

        requestQueue.add(stringRequest);
    }

    @Override
    public void onSendSms(Location location) {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission if not already granted
            requestSmsPermission();
        } else {
            // Permission granted, send SMS
            showSmsDialog(location);
        }
    }

    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(
                getActivity(),
                new String[]{Manifest.permission.SEND_SMS},
                SMS_PERMISSION_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "SMS permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showEditLocationDialog(Location location) {
        // Implementation for editing a location (not shown for brevity)
    }

    private void showSmsDialog(Location location) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Send SMS");

        final EditText inputPhoneNumber = new EditText(getContext());
        inputPhoneNumber.setHint("Enter phone number");
        inputPhoneNumber.setInputType(InputType.TYPE_CLASS_PHONE);

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(inputPhoneNumber);

        builder.setView(layout);
        builder.setPositiveButton("Send", (dialog, which) -> {
            String phoneNumber = inputPhoneNumber.getText().toString();
            sendSms(location, phoneNumber);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void sendSms(Location location, String phoneNumber) {
        try {
            String message = "My position is: Latitude " + location.getLatitude() +
                    ", Longitude " + location.getLongitude();
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(getContext(), "SMS sent successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
