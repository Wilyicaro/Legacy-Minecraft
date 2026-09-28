package wily.legacy.client.control;

final class GyroInput {
    private long lastSampleTime;
    private boolean wasSteering;
    private double pitchBias;
    private double yawBias;
    private double pitchSum;
    private double yawSum;
    private double stillTime;
    double pitchDelta;
    double yawDelta;

    void reset() {
        pitchBias = yawBias = 0;
        stop();
    }

    void stop() {
        lastSampleTime = 0;
        wasSteering = false;
        pitchDelta = yawDelta = 0;
        clearCalibration();
    }

    void update(float pitch, float yaw, float roll, long now, boolean steering) {
        pitchDelta = yawDelta = 0;
        double seconds = lastSampleTime == 0 ? 0 : (now - lastSampleTime) / 1_000_000_000.0;
        lastSampleTime = now;
        if (!Float.isFinite(pitch) || !Float.isFinite(yaw) || !Float.isFinite(roll) || seconds <= 0 || seconds > 0.1) {
            wasSteering = false;
            clearCalibration();
            return;
        }
        if (!steering) {
            wasSteering = false;
            if (Math.abs(pitch) < 0.02 && Math.abs(yaw) < 0.02 && Math.abs(roll) < 0.02) {
                pitchSum += pitch * seconds;
                yawSum += yaw * seconds;
                stillTime += seconds;
                if (stillTime >= 1.0) {
                    pitchBias = pitchSum / stillTime;
                    yawBias = yawSum / stillTime;
                    clearCalibration();
                }
            } else clearCalibration();
            return;
        }
        clearCalibration();
        if (!wasSteering) {
            wasSteering = true;
            return;
        }
        pitchDelta = -Math.toDegrees(removeDeadZone(pitch - pitchBias)) * seconds;
        yawDelta = -Math.toDegrees(removeDeadZone(yaw - yawBias)) * seconds;
    }

    private void clearCalibration() {
        pitchSum = yawSum = stillTime = 0;
    }

    private static double removeDeadZone(double velocity) {
        return Math.copySign(Math.max(0, Math.abs(velocity) - 0.01), velocity);
    }
}
