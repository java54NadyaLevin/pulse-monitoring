package telran.pulse.monitoring;

public record PulseData(int patientId, int pulseValue, long timestamp){

    public int patientId() {
        return patientId;
    }

    public int pulseValue() {
        return pulseValue;
    }

    public long timestamp() {
        return timestamp;
    }
	
}