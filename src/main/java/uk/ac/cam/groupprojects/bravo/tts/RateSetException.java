package uk.ac.cam.groupprojects.bravo.tts;

/*
    Thrown if the festival program produces invalid output when the rate's being changed.
*/
public class RateSetException extends Exception {
    public RateSetException(String message) {
        super(message);
    }
}