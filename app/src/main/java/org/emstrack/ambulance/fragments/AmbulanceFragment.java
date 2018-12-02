package org.emstrack.ambulance.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulanceCall;
import org.emstrack.models.AmbulancePermission;
import org.emstrack.models.Call;
import org.emstrack.models.CallStack;
import org.emstrack.models.Location;
import org.emstrack.models.Patient;
import org.emstrack.models.Waypoint;
import org.emstrack.mqtt.MqttProfileClient;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class AmbulanceFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private static final String TAG = AmbulanceFragment.class.getSimpleName();;

    private static DecimalFormat df = new DecimalFormat();

    private View view;

    private Spinner statusSpinner;

    private TextView capabilityText;

    private TextView commentText;

    private TextView updatedOnText;

    private Map<String,String> ambulanceStatus;
    private List<String> ambulanceStatusList;
    private ArrayList<Integer> ambulanceStatusColorList;
    private Map<String,String> ambulanceCapabilities;
    private List<String> ambulanceCapabilityList;

    private AmbulancesUpdateBroadcastReceiver receiver;
    private LinearLayout callResumeLayout;
    private LinearLayout callLayout;
    private int currentCallId;
    private TextView callDescriptionTextView;
    private Button callPriorityButton;
    private TextView callAddressTextView;
    private Button callEndButton;
    private TextView callPatientsTextView;
    private TextView callDistanceTextView;
    private Button callAddWaypointButton;
    private TextView callNextWaypointTextView;
    private TextView callNumberWayointsView;
    private TextView callInformationText;

    public class AmbulancesUpdateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent ) {
            if (intent != null) {
                final String action = intent.getAction();
                if (action.equals(AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE)) {

                    Log.i(TAG, "AMBULANCE_UPDATE");
                    updateAmbulance(AmbulanceForegroundService.getCurrentAmbulance());

                } else if (action.equals(AmbulanceForegroundService.BroadcastActions.CALL_UPDATE)) {

                    Log.i(TAG, "CALL_UPDATE");
                    if (currentCallId > 0)
                        updateCall(AmbulanceForegroundService.getCurrentCall());

                } else if (action.equals(AmbulanceForegroundService.BroadcastActions.CALL_ONGOING)) {

                    Log.i(TAG, "CALL_ONGOING");

                    // Toast to warn user
                    Toast.makeText(getContext(), R.string.CallStarted, Toast.LENGTH_LONG).show();

                    int callId = intent.getIntExtra("CALL_ID", -1);
                    currentCallId = -1;
                    updateCall(AmbulanceForegroundService.getCurrentCall());

                } else if (action.equals(AmbulanceForegroundService.BroadcastActions.CALL_FINISHED)) {

                    Log.i(TAG, "CALL_FINISHED");

                    // Toast to warn user
                    Toast.makeText(getContext(), R.string.CallFinished, Toast.LENGTH_LONG).show();

                    int callId = intent.getIntExtra("CALL_ID", -1);
                    if (currentCallId == callId)
                        updateCall(null);

                }

            }
        }
    };

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // set formatter
        df.setMaximumFractionDigits(3);

        // inflate view
        view = inflater.inflate(R.layout.fragment_ambulance, container, false);

        // get callResumeLayout
        callResumeLayout = view.findViewById(R.id.callResumeLayout);

        // get callLayout
        callLayout = view.findViewById(R.id.callLayout);

        // setup as no current call
        // View child = getLayoutInflater().inflate(R.layout.no_calls, null);
        // callLayout.addView(child);
        currentCallId = -1;

        try {

            // Get settings, status and capabilities
            final MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient();

            ambulanceStatus = profileClient.getSettings().getAmbulanceStatus();
            ambulanceStatusList = new ArrayList<>(ambulanceStatus.values());
            Collections.sort(ambulanceStatusList);

            ambulanceStatusColorList = new ArrayList<>();
            for (String value : ambulanceStatusList)
                for (Map.Entry<String,String> entry : ambulanceStatus.entrySet())
                    if (value.equals(entry.getValue()))
                        ambulanceStatusColorList.add(getResources().getColor(Ambulance.statusColorList.get(entry.getKey())));

            ambulanceCapabilities = profileClient.getSettings().getAmbulanceCapability();
            ambulanceCapabilityList = new ArrayList<>(ambulanceCapabilities.values());
            Collections.sort(ambulanceCapabilityList);

        } catch (AmbulanceForegroundService.ProfileClientException e) {

            ambulanceStatusList = new ArrayList<>();

        }

        // Other text
        callInformationText = view.findViewById(R.id.callInformationText);
        capabilityText = view.findViewById(R.id.capabilityText);
        commentText = view.findViewById(R.id.commentText);
        updatedOnText = view.findViewById(R.id.updatedOnText);

        // Set status spinner
        statusSpinner = view.findViewById(R.id.statusSpinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> statusSpinnerAdapter =
                new ArrayAdapter<String>(getContext(),
                        R.layout.status_spinner_item,
                        ambulanceStatusList) {

                    @Override
                    public View getView(int pos, View view, ViewGroup parent)
                    {
                        Context context = AmbulanceFragment.this.getContext();
                        LayoutInflater inflater = LayoutInflater.from(context);
                        view = inflater.inflate(R.layout.status_spinner_dropdown_item, null);
                        TextView textView = view.findViewById(R.id.statusSpinnerDropdownItemText);
                        textView.setBackgroundColor(ambulanceStatusColorList.get(pos));
                        textView.setTextSize(context.getResources().getDimension(R.dimen.statusTextSize));
                        textView.setText(ambulanceStatusList.get(pos));
                        return view;
                    }

                    @Override
                    public View getDropDownView(int pos, View view, ViewGroup parent) {
                        return getView(pos, view, parent);
                    }

                };
        statusSpinner.setAdapter(statusSpinnerAdapter);

        // Update ambulance
        Ambulance ambulance = AmbulanceForegroundService.getCurrentAmbulance();
        if (ambulance != null)
            updateAmbulance(ambulance);

        // Are there calls?
        Call call = AmbulanceForegroundService.getCurrentCall();
        if (call != null)
            updateCall(call);

        // Process change of status
        statusSpinner.setOnItemSelectedListener(this);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Update ambulance
        Ambulance ambulance = AmbulanceForegroundService.getCurrentAmbulance();
        if (ambulance != null)
            updateAmbulance(ambulance);

        // Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_UPDATE);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_ONGOING);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_FINISHED);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_DECLINED);
        receiver = new AmbulanceFragment.AmbulancesUpdateBroadcastReceiver();
        getLocalBroadcastManager().registerReceiver(receiver, filter);

    }

    @Override
    public void onPause() {
        super.onPause();

        // Unregister receiver
        if (receiver != null) {
            getLocalBroadcastManager().unregisterReceiver(receiver);
            receiver = null;
        }

    }

    public void updateCall(final Call call) {

        if ((call == null ^ currentCallId <= 0)) {

            // destroy current view
            callLayout.removeAllViews();

            // create new layout
            if (call == null) {

                // child = getLayoutInflater().inflate(R.layout.no_calls, null);
                currentCallId = -1;

            } else {

                View child = getLayoutInflater().inflate(R.layout.calls, null);
                callPriorityButton = child.findViewById(R.id.callPriorityButton);
                callDescriptionTextView = child.findViewById(R.id.callDetailsText);
                callNextWaypointTextView = child.findViewById(R.id.callWaypointTypeText);
                callAddressTextView = child.findViewById(R.id.callAddressText);
                callPatientsTextView = child.findViewById(R.id.callPatientsText);
                callNumberWayointsView = child.findViewById(R.id.callNumberWaypointsText);
                callDistanceTextView = child.findViewById(R.id.callDistanceText);

                callEndButton = child.findViewById(R.id.callEndButton);
                callEndButton.setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Prompt end of call
                                ((MainActivity) getActivity()).promptEndCallDialog(call.getId());
                            }
                        }
                );

                callAddWaypointButton = child.findViewById(R.id.callAddWaypointButton);
                callAddWaypointButton.setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Prompt add new waypoint
                                ((MainActivity) getActivity()).promptNextWaypointDialog(call.getId());
                            }
                        }
                );

                currentCallId = call.getId();

                callLayout.addView(child);

            }

        }

        // Update call content
        if (call != null) {

            // get ambulanceCall
            AmbulanceCall ambulanceCall = call.getCurrentAmbulanceCall();
            if (ambulanceCall == null)
                Log.d(TAG, "Call does not have a current ambulance!");

            Waypoint waypoint = null;
            if (ambulanceCall != null)
                waypoint = ambulanceCall.getNextWaypoint();
            else
                Log.d(TAG, "Call does not have a next waypoint!");

            callPriorityButton.setText(call.getPriority());
            callPriorityButton.setBackgroundColor(((MainActivity) getActivity()).getCallPriorityBackgroundColors().get(call.getPriority()));
            callPriorityButton.setTextColor(((MainActivity) getActivity()).getCallPriorityForegroundColors().get(call.getPriority()));

            callDescriptionTextView.setText(call.getDetails());

            // patients
            List<Patient> patients = call.getPatientSet();
            if (patients != null && patients.size() > 0) {
                String text = "";
                for (Patient patient: patients)  {
                    if (!text.isEmpty())
                        text += ", ";
                    text += patient.getName();
                    if (patient.getAge() != null)
                        text += " (" + patient.getAge() + ")";
                }

                callPatientsTextView.setText(text);
            } else
                callPatientsTextView.setText(R.string.noPatientAvailable);

            int numberOfWaypoints = ambulanceCall == null ? 0 : ambulanceCall.getWaypointSet().size();
            callNumberWayointsView.setText(String.valueOf(numberOfWaypoints));

            if (waypoint != null) {

                // Get Location
                Location location = waypoint.getLocation();

                // Update waypoint type
                callNextWaypointTextView.setText(Location.typeLabel.get(location.getType()));

                // Update address
                callAddressTextView.setText(location.toString());

                // Update call distance to next waypoint
                callDistanceTextView.setText(updateCallDistance(location));

            } else {

                callNextWaypointTextView.setText("Next waypoint hasn't been set yet.");
                callAddressTextView.setText("---");
                callDistanceTextView.setText("---");

            }

        }

    }

    public void updateAmbulance(Ambulance ambulance) {

        // set spinner only if position changed
        // this helps to prevent a possible server loop
        int position = ambulanceStatusList.indexOf(ambulanceStatus.get(ambulance.getStatus()));
        int currentPosition = statusSpinner.getSelectedItemPosition();
        if (currentPosition != position) {

            Log.i(TAG,"Spinner changed from " + currentPosition + " to " + position);

            // set spinner
            setSpinner(position);

        } else {

            Log.i(TAG, "Spinner continues to be at position " + position + ". Skipping updateAmbulance");

        }

        // set identifier
        ((MainActivity) getActivity()).setAmbulanceButtonText(ambulance.getIdentifier());

        Log.d(TAG,"Sumarizing pending calls");

        // Set calls info
        Map<String, Integer> callSummary = AmbulanceForegroundService.getPendingCalls().summary(ambulance.getId());
        Log.d(TAG, "Call summary = " + callSummary.toString());
        final String summaryText = String.format("requested (%1$d), suspended (%2$d)",
                callSummary.get(AmbulanceCall.STATUS_REQUESTED),
                callSummary.get(AmbulanceCall.STATUS_SUSPENDED));
        callInformationText.setText(summaryText);

        // destroy current views
        callResumeLayout.removeAllViews();

        // deploy resume panel
        if ((callSummary.get(AmbulanceCall.STATUS_REQUESTED) + callSummary.get(AmbulanceCall.STATUS_SUSPENDED)) > 0) {

            Log.d(TAG,"Will add resume call view");

            // Expand resume call view
            View child = getLayoutInflater().inflate(R.layout.no_calls, null);

            // List of calls
            ArrayList<String> pendingCallList = new ArrayList<>();

            // Create lists of suspended calls
            final ArrayList<Pair<Call,AmbulanceCall>> suspendedCallList = new ArrayList<>();
            if (callSummary.get(AmbulanceCall.STATUS_SUSPENDED) > 0) {
                for (Map.Entry<Integer, Pair<Call,AmbulanceCall>> ambulanceCallEntry : AmbulanceForegroundService
                        .getPendingCalls()
                        .filterByStatus(ambulance.getId(),
                                AmbulanceCall.STATUS_SUSPENDED)
                        .entrySet()) {
                    Pair<Call,AmbulanceCall> ambulanceCall = ambulanceCallEntry.getValue();
                    suspendedCallList.add(ambulanceCall);
                    pendingCallList.add("(S) " + ambulanceCall.second.getCreatedAt());
                }
            }

            // Create lists of requested calls
            final ArrayList<Pair<Call,AmbulanceCall>> requestedCallList = new ArrayList<>();
            if (callSummary.get(AmbulanceCall.STATUS_REQUESTED) > 0) {
                for (Map.Entry<Integer, Pair<Call,AmbulanceCall>> ambulanceCallEntry : AmbulanceForegroundService
                        .getPendingCalls()
                        .filterByStatus(ambulance.getId(),
                                AmbulanceCall.STATUS_REQUESTED)
                        .entrySet()) {
                    Pair<Call,AmbulanceCall> ambulanceCall = ambulanceCallEntry.getValue();
                    requestedCallList.add(ambulanceCall);
                    pendingCallList.add("(R) " + ambulanceCall.second.getCreatedAt());
                }
            }

                        // Create the spinner adapter
            ArrayAdapter<String> pendingCallListAdapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_dropdown_item, pendingCallList);
            pendingCallListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            // Retrieve parts
            final Spinner callResumeSpinner = child.findViewById(R.id.callResumeSpinner);

            // set adapter
            callResumeSpinner.setAdapter(pendingCallListAdapter);

            final Button callResumeButton= child.findViewById(R.id.callResumeButton);
            callResumeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // retrieve spinner selection
                    int position = callResumeSpinner.getSelectedItemPosition();
                    Call call;
                    if (position < suspendedCallList.size())
                        call = suspendedCallList.get(position).first;
                    else
                        call = requestedCallList.get(position - suspendedCallList.size()).first;

                    // prompt user
                    Log.d(TAG,"Will prompt user to resume call");
                    ((MainActivity) getActivity()).promptAcceptCallDialog(call.getId());

                }
            });

            // add view to panel
            callResumeLayout.addView(child);

        }

        // update call distance?
        if (currentCallId > 0) {

            AmbulanceCall ambulanceCall = AmbulanceForegroundService.getCurrentAmbulanceCall();
            if (ambulanceCall != null) {

                Waypoint waypoint = ambulanceCall.getNextWaypoint();
                if (waypoint != null) {

                    String distanceText = updateCallDistance(waypoint.getLocation());
                    callDistanceTextView.setText(distanceText);
                } else
                    callDistanceTextView.setText("---");

            } else
                callDistanceTextView.setText("---");

        }

        // set status and comment
        commentText.setText(ambulance.getComment());
        updatedOnText.setText(ambulance.getUpdatedOn().toString());

        // set capability
        capabilityText.setText(ambulanceCapabilities.get(ambulance.getCapability()));

    }

    public String updateCallDistance(Location location) {

        if (location == null)
            return null;

        Log.d(TAG,"Will calculate distance");

        // Get current location
        android.location.Location lastLocation = AmbulanceForegroundService.getLastLocation();
        Log.d(TAG,"last location = " + lastLocation);

        // Calculate distance to patient
        float distance = -1;
        if (location != null)
            distance = lastLocation.distanceTo(location.getLocation().toLocation()) / 1000;
        String distanceText = getString(R.string.noDistanceAvailable);
        Log.d(TAG,"Distance = " + distance);
        if (distance > 0) {
            distanceText = df.format(distance) + " km";
        }

        return distanceText;

    }

    public void setSpinner(int position) {

        // temporarily disconnect listener to prevent loop
        Log.i(TAG, "Suppressing listener");
        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                /* do nothing */
                Log.i(TAG,"Ignoring change in spinner. Position '" + position + "' selected.");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.i(TAG,"Ignoring change in spinner. Nothing selected.");
            }
        });

        // updateAmbulance spinner
        statusSpinner.setSelection(position, false);

        // connect listener
        // this is tricky, see
        // https://stackoverflow.com/questions/2562248/how-to-keep-onitemselected-from-firing-off-on-a-newly-instantiated-spinner
        statusSpinner.post(new Runnable() {
            public void run() {
                Log.i(TAG, "Restoring listener");
                statusSpinner.setOnItemSelectedListener(AmbulanceFragment.this);
            }
        });

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        Log.i(TAG, "Item '" + position + "' selected.");

        // Should only updateAmbulance on server as a result of user interaction
        // Otherwise this will create a loop with mqtt updating ambulance
        // TODO: Debug spinner multiple updates
        // This may not be easy with the updates being called from a service

        Log.i(TAG, "Processing status spinner updateAmbulance.");

        Ambulance ambulance = AmbulanceForegroundService.getCurrentAmbulance();
        if (ambulance == null) {
            Log.i(TAG, "Ambulance is null. This should never happen!");
            return;
        }

        if ((ambulance != null) && !((MainActivity) getActivity()).canWrite()) {

            // Toast to warn user
            Toast.makeText(getContext(), R.string.cantModifyAmbulance, Toast.LENGTH_LONG).show();

            // set spinner
            int oldPosition = ambulanceStatusList.indexOf(ambulanceStatus.get(ambulance.getStatus()));
            setSpinner(oldPosition);

            // Return
            return;

        }

        // Get status from spinner
        String status = (String) parent.getItemAtPosition(position);

        // Search for entry in ambulanceStatus map
        String statusCode = "";
        for (Map.Entry<String, String> entry : ambulanceStatus.entrySet()) {
            if (status.equals(entry.getValue())) {
                statusCode = entry.getKey();
                break;
            }
        }

        // format timestamp
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = df.format(new Date());

        // Set updateAmbulance string
        String updateString = "{\"status\":\"" + statusCode + "\",\"timestamp\":\"" + timestamp + "\"}";

        // Update on server
        // TODO: Update along with locations because it will be recorded with
        //       the wrong location on the server
        Intent intent = new Intent(getContext(), AmbulanceForegroundService.class);
        intent.setAction(AmbulanceForegroundService.Actions.UPDATE_AMBULANCE);
        Bundle bundle = new Bundle();
        bundle.putInt("AMBULANCE_ID", ambulance.getId());
        bundle.putString("UPDATE", updateString);
        intent.putExtras(bundle);
        getActivity().startService(intent);

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.i(TAG, "Nothing selected: this should never happen.");
    }

    /**
     * Get LocalBroadcastManager
     *
     * @return the LocalBroadcastManager
     */
    private LocalBroadcastManager getLocalBroadcastManager() {
        return LocalBroadcastManager.getInstance(getContext());
    }

}