package org.emstrack.ambulance.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.emstrack.ambulance.AmbulanceApp;
import org.emstrack.ambulance.AmbulanceListActivity;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.LoginActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.services.OnServiceComplete;
import org.emstrack.mqtt.MqttProfileClient;

/**
 * Created by devinhickey on 5/24/17.
 */

public class LogoutDialog extends DialogFragment {

    final String TAG = "LogoutDialog";

    public LogoutDialog() {}

    public static LogoutDialog newInstance() {
        return new LogoutDialog();
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        System.out.println("Logout Dialog onCreateDialog");

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());

        alertBuilder.setTitle(getResources().getString(R.string.alert_warning_title));
        alertBuilder.setMessage(getResources().getString(R.string.logout_confirm));

        // Create the OK button that logs user out
        alertBuilder.setNeutralButton(getResources().getString(R.string.alert_button_positive_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.i(TAG,"LogoutDialog: OK Button Clicked");

                // Stop foreground activity
                Intent intent = new Intent(getActivity().getBaseContext(), AmbulanceForegroundService.class);
                intent.setAction(AmbulanceForegroundService.Actions.LOGOUT);

                // What to do when service completes?
                new OnServiceComplete(getActivity(),
                        AmbulanceForegroundService.BroadcastActions.SUCCESS,
                        AmbulanceForegroundService.BroadcastActions.FAILURE,
                        intent) {

                    @Override
                    public void onSuccess(Bundle extras) {
                        Log.i(TAG, "onSuccess");

                        // Start login activity
                        Intent loginIntent = new Intent(LogoutDialog.this.getActivity(), LoginActivity.class);
                        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(loginIntent);

                    }

                }
                        .setFailureMessage(getString(R.string.couldNotLogout))
                        .setAlert(new AlertSnackbar(getActivity()));

            }
        });

        // Create the Cancel Button
        alertBuilder.setNegativeButton(getResources().getString(R.string.alert_button_negative_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.i(TAG, "LogoutDialog: Cancel Button Clicked");

                // dismiss
                dialog.dismiss();
            }
        });

        return alertBuilder.create();

    }

}
