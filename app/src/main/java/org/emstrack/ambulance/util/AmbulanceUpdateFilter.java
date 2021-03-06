package org.emstrack.ambulance.util;

import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.emstrack.ambulance.util.LatLon.calculateDistanceAndBearing;
import static org.emstrack.ambulance.util.LatLon.stationaryRadius;
import static org.emstrack.ambulance.util.LatLon.stationaryVelocity;

/**
 * Created by mauricio on 3/22/2018.
 */

public class AmbulanceUpdateFilter {

    private static final String TAG = AmbulanceUpdateFilter.class.getSimpleName();

    private AmbulanceUpdate currentAmbulanceUpdate;
    private List<AmbulanceUpdate> filteredAmbulanceUpdates;

    public AmbulanceUpdateFilter() {
        this(null);
    }

    public AmbulanceUpdateFilter(AmbulanceUpdate location) {
        this.currentAmbulanceUpdate = location;
        this.filteredAmbulanceUpdates = new ArrayList<>();
    }

    public void setCurrentAmbulanceUpdate(AmbulanceUpdate currentAmbulanceUpdate) {
        this.currentAmbulanceUpdate = currentAmbulanceUpdate;
    }

    public List<AmbulanceUpdate> getFilteredAmbulanceUpdates() {
        return this.filteredAmbulanceUpdates;
    }

    public boolean hasUpdates() {
        return this.filteredAmbulanceUpdates.size() > 0;
    }

    public void reset() {
        this.filteredAmbulanceUpdates = new ArrayList<>();
    }

    public void sort() {
        Collections.sort(this.filteredAmbulanceUpdates,
                new AmbulanceUpdate.SortByAscendingOrder());
    }

    /**
     * Update current position based on a new measurement
     *
     * @param update the updateAmbulance
     */
    private void _update(Location update) {

        // return if null
        if (currentAmbulanceUpdate.getLocation() == null) {
            // Log.d(TAG, "Null location, skipping...");
            return;
        }

        // elapsed time
        double dt = update.getTime() - currentAmbulanceUpdate.getTimestamp().getTime();

        // Predict next currentAmbulanceUpdate
        // GPSLocation prediction = updateLocation(currentAmbulanceUpdate, bearing, velocity * dt);

        // measure velocity and bearing
        double[] dandb = calculateDistanceAndBearing(currentAmbulanceUpdate.getLocation(), update);
        double distance = dandb[0];
        double brn = dandb[1];
        double vel = currentAmbulanceUpdate.getVelocity();
        if (dt > 0)
            vel = distance / dt;

        // ambulanceUpdateFilter velocity
        double Kv = 0.9;
        double velocity = currentAmbulanceUpdate.getVelocity();
        velocity += Kv * (vel - velocity);
        currentAmbulanceUpdate.setVelocity(velocity);

        // ambulanceUpdateFilter bearing
        double Kb = 0.9;
        double bearing = currentAmbulanceUpdate.getBearing();
        bearing += Kb * (brn - bearing);
        currentAmbulanceUpdate.setBearing(bearing);

        if ((velocity > stationaryVelocity && distance > stationaryRadius) ||
                (velocity <= stationaryVelocity && distance > 3 * stationaryRadius)) {

            // updateAmbulance currentAmbulanceUpdate
            currentAmbulanceUpdate.setLocation(update);
            currentAmbulanceUpdate.setTimestamp(new Date(update.getTime()));

            // add currentAmbulanceUpdate to filtered locations
            filteredAmbulanceUpdates.add(new AmbulanceUpdate(currentAmbulanceUpdate));

        }

        // Log.i(TAG, "velocity = " + velocity + ", distance = " + distance + ", bearing = " + bearing + "(" + update.getBearing() + ")");

    }

    public void update(String status) {

        this.filteredAmbulanceUpdates.add(new AmbulanceUpdate(status));

    }

    public void update(String status, Date timestamp) {

        this.filteredAmbulanceUpdates.add(new AmbulanceUpdate(status, timestamp));

    }

    public void update(Location location) {

        // initialize
        if (this.currentAmbulanceUpdate == null) {
            // use first record
            this.currentAmbulanceUpdate = new AmbulanceUpdate(location);
            return;
        }

        // update records
        _update(location);

    }

    public void update(List<Location> locations) {

        // Fast return if no updates
        if (locations == null || locations.size() == 0)
            return;

        // initialize
        if (currentAmbulanceUpdate == null)
            // use first record
            currentAmbulanceUpdate = new AmbulanceUpdate(locations.get(0));

        // loop through records
        for (Location location : locations)
            _update(location);

    }

    /**
     * Model is that of a constant forward velocity and constant angular velocity
     *
     * xDot(t) = v(t) cos(theta(t))
     * yDot(t) = v(t) cos(theta(t))
     * thetaDot(t) = thetaDot(t)
     * thetaDotDot(t) = 0
     * vDot(t) = 0
     *
     * Discretizing at t+ = tk + dt, t = tk, dt = t+ - t, we obtain the model:
     *
     * x(tk+), y(tk+) = f(x(tk), y(tk), theta(tk-), v(tk) dt)
     * theta(tk+) = theta(tk) + thetaDot(tk) dt
     * thetaDot(tk+) = thetaDot(tk)
     * v(tk+) = v(tk)
     *
     * or
     *
     * X = (x, y, theta, thetaDot, v)
     * X(tk+) = F(X(tk),tk),
     * Z(tk) = G(X(tk),tk)
     *
     * Partials:
     *
     * f1: x(tk+) = x(tk) + dt v(tk) cos(theta(tk))
     * f2: y(tk+) = y(tk) + dt v(tk) sin(theta(tk))
     * f3: theta(tk+) = theta(tk) + thetaDot(tk) dt
     * f4: thetaDot(tk+) = thetaDot(tk)
     * f5: v(tk+) = v(tk)
     *
     * Fk = dF/dx
     *    = [1, 0, -dt*v(tk)*sin(theta(tk)), 0, dt*cos(theta(tk));
     *       0, 1, dt*v(tk)*cos(theta(tk)), 0, dt*sin(theta(tk));
     *       0, 0, 1, dt, 0;
     *       0, 0, 0, 1, 0;
     *       0, 0, 0, 0, 1]
     *
     * g1: z1(tk) = x(tk)
     * g2: z2(tk) = y(tk)
     *
     * Hk = dG/dx
     *    = [1, 0, 0, 0;
     *       0, 1, 0, 0];
     *
     *
     * Extended Kalman ambulanceUpdateFilter
     *
     * Prediction:
     *
     * xHat(tk+|tk), yHat(tk+|tk) = f(xHat(tk), yHat(tk), thetaHat(tk), vHat(tk) tk)
     * thetaHat(tk+|tk) = thetaHat(tk) + thetaDotHat(tk) dt
     * thetaDotHat(tk+|tk) = thetaDotHat(tk)
     * vHat(tk+|tk) = vHat(tk)
     *
     * or
     *
     * XHat(tk+|tk) = F(XHat(tk),tk)
     *
     * Covariance prediction:
     *
     * P(tk+|tk) = Fk P(tk) Fk' + Qk
     *
     * At time tk we obtain a measurement ot z(tk) = (x(tk), y(tk))
     *
     * Update:
     *
     * Sk = Hk P(tk|tk) Hk' + Rk
     * Kk = P(tk+|tk) Hk' inv(Sk)
     *
     * XHat(tk+) = XHat(tk+|tk) + K (z(tk) - zHat(tk))
     *
     * Covariance updateAmbulance:
     *
     * P(tk+ = P(tk+|tk+) = (I - Kk Hk) P(tk+|tk)
     *
     */


}
