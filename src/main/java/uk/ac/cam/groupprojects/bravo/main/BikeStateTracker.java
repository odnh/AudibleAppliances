package uk.ac.cam.groupprojects.bravo.main;

import uk.ac.cam.groupprojects.bravo.config.BikeField;
import uk.ac.cam.groupprojects.bravo.config.ConfigData;
import uk.ac.cam.groupprojects.bravo.imageProcessing.ScreenBox;
import uk.ac.cam.groupprojects.bravo.imageProcessing.ImageSegments;
import uk.ac.cam.groupprojects.bravo.model.LCDState;
import uk.ac.cam.groupprojects.bravo.model.menu.*;
import uk.ac.cam.groupprojects.bravo.model.numbers.*;
import uk.ac.cam.groupprojects.bravo.ocr.SegmentActive;
import uk.ac.cam.groupprojects.bravo.ocr.SegmentRecogniser;
import uk.ac.cam.groupprojects.bravo.ocr.UnrecognisedDigitException;
import uk.ac.cam.groupprojects.bravo.tts.Synthesiser;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * Created by david on 13/02/2018.
 */
public class BikeStateTracker {
    // Holds data about the bike at a given time (enough to compute the state, so LCD and the recognised time)
    class StateTime {
        public LocalDateTime addedTime;
        public Set<ScreenBox> activeBoxes;
        public int bikeTime;

        public StateTime(LocalDateTime addedTime, Set<ScreenBox> activeBoxes, int bikeTime) {
            this.addedTime = addedTime;
            this.activeBoxes = activeBoxes;
            this.bikeTime = bikeTime;
        }
    }
    // Holds the last image we saw for each segment *when it was active* - no blank images here
    class ImageTime {
        public LocalDateTime addedTime;
        public BufferedImage boxImage;
        public ScreenNumber recognisedValue;

        public ImageTime(LocalDateTime addedTime, BufferedImage boxImage) {
            this.addedTime = addedTime;
            this.boxImage = boxImage;

            recognisedValue = null;
        }

        public void setRecognisedValue(ScreenNumber value) {
            recognisedValue = value;
        }
    }

    // Necessary class state
    private final ConfigData configData;
    private final Synthesiser synthesiser;

    private static long lastSpeakTime = 0;

    // Back of the list is the most recent state added
    // We keep 2 seconds of state, so we can determine if time is changing
    // We only look at 1 second of the state to determine if a segment is blinking,
    // otherwise we introduce a lot of latency when changing state
    private final LinkedList<StateTime> history;

    // Current useful state of the bike, as inferred from the history    
    private final Map<ScreenBox, LCDState> boxStates; // Which LCDs are active?
    private final Map<ScreenBox, ImageTime> latestImages; // Latest non-blank images of each LCD
    private BikeScreen currentScreen; // What do we think the current state is?
    private boolean timeChanging; // Is the time LCD counting up/down?

    public BikeStateTracker(ConfigData config, Synthesiser synth) {
        history = new LinkedList<>();
        boxStates = new HashMap<>();
        latestImages = new HashMap<>();
        timeChanging = false;
        configData = config;
        synthesiser = synth;
    }

    /**
     * Update the current state using pre-segmented image
     *
     * @param imgSegs
     * @throws IOException
     * @throws UnrecognisedDigitException
     * @throws NumberFormatException
     */
    public void updateState(Map<ScreenBox, BufferedImage> imgSegs)
                throws IOException, UnrecognisedDigitException, NumberFormatException {
        LocalDateTime currentTime = LocalDateTime.now();
        Set<ScreenBox> activeSegs = new HashSet<>();

        // Compute which LCD segments are lit up (active)
        for (ScreenBox box : ScreenBox.values()) {
            long startTime = System.currentTimeMillis();

            BufferedImage boxImage = imgSegs.get(box);
            if (SegmentActive.segmentActive(box, boxImage)) {
                // If the LCD is active, record it and update the latest image we have of it
                activeSegs.add(box);
                latestImages.put(box, new ImageTime(currentTime, boxImage));
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            if (ApplicationConstants.DEBUG)
                System.out.println("segmentActive(" + box.toString() + ") took " + elapsedTime + "ms");
        }

        // Store the new state, with the time we recognised at the moment
        Time bikeTime = (Time)getFieldValue(BikeField.TIME);
        if (bikeTime != null) { // If this is null, it just means we've only just started and don't have enough data yet
            history.add(new StateTime(currentTime, activeSegs, bikeTime.getValue()));
        }

        // Remove state information that's older than two complete blink cycles (2s) ago
        removeOldHistory(currentTime);

        // Update which LCDs we know are solid/blinking
        updateSolidBlinking(currentTime);
        updateTimeChanging();

        // DEBUG: Output the state of every box
        if (ApplicationConstants.DEBUG) {
            System.out.println("Current State:");
            System.out.println("Time Changing: " + isTimeChanging());
            for (ScreenBox box : ScreenBox.values()) {
                System.out.println(box.toString() + ": " + getBoxState(box).toString());
            }
            System.out.println();
        }

        BikeScreen newScreen = null;
        for (ScreenEnum s : ScreenEnum.values()) {
            BikeScreen screen = s.getBikeScreen();
            boolean inState = screen.isActiveScreen(this);
            if (inState) {
                newScreen = screen;
                break;
            }
        }
        if (newScreen == null) {
            System.out.println("Failed to identify state");
        }

        currentScreen = newScreen;
        
        // Check if it's the time to speak, and if yes then speak
        if (System.currentTimeMillis() - lastSpeakTime > currentScreen.getSpeakDelay()) {
            currentScreen.speakItems(this, synthesiser);
            lastSpeakTime = System.currentTimeMillis();
            if (ApplicationConstants.DEBUG)
                System.out.println("Spoke");
        }
    }

    // Update which LCDs are blinking and which are solid
    private void updateSolidBlinking(LocalDateTime currentTime) {
        // Reset all
        boxStates.clear();

        for (ScreenBox box : ScreenBox.values()) {
            // Default to each box being solid off
            boolean blinking = false;
            boolean active = false;

            // Loop over the last second of history we have
            if (history.size() > 0) {
                boolean currentlyActive = isBoxActiveNow(box);
                boolean historyMatches =
                        history.stream()
                               .filter(s ->
                                   currentTime.minus(2 * ApplicationConstants.BLINK_FREQ, ChronoUnit.MILLIS)
                                              .isBefore(s.addedTime))
                               .allMatch(s ->
                                   s.activeBoxes.contains(box) == currentlyActive);

                if (!historyMatches) { // Changes over time => blinking
                    blinking = true;
                } else {
                    active = currentlyActive;
                }
            }

            if (blinking) {
                boxStates.put(box, LCDState.BLINKING);
            } else {
                boxStates.put(box, active ? LCDState.SOLID_ON : LCDState.SOLID_OFF);
            }
        }
    }

    // Update whether the time LCD is counting over time or not
    private void updateTimeChanging() {
        if (history.size() == 0) {
            return;
        }

        int currentBikeTime = history.getLast().bikeTime;
        timeChanging = !history.stream()
                               .allMatch(s -> s.bikeTime == currentBikeTime);
    }
    private void removeOldHistory(LocalDateTime currentTime) {
        while (history.size() > 0) {
            if (currentTime.minus(4 * ApplicationConstants.BLINK_FREQ, ChronoUnit.MILLIS)
                           .isAfter(history.getFirst().addedTime)) {
                history.removeFirst();
            } else {
                break;
            }                
        }
    }

    public ConfigData getConfig() {
        return configData;
    }

    public ScreenNumber getFieldValue(BikeField field) {
        ScreenBox containingBox = field.getScreenBox();

        ImageTime lastImage = latestImages.get(containingBox);
        if (lastImage == null || lastImage.boxImage == null) { // No images for this box yet
            return null;
        }

        // Not already run OCR on the image
        if (lastImage.recognisedValue == null) {
            long startTime = System.currentTimeMillis();

            try {
                int value = SegmentRecogniser.recogniseInt(lastImage.boxImage);
                ScreenNumber recognised = field.getScreenNumber();
                recognised.setValue(value);
                lastImage.recognisedValue = recognised;
            }
            catch (IOException | NumberFormatException | UnrecognisedDigitException e) {
                System.out.println("Failed to recognise digit for " + field.toString());
                return null;
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            if (ApplicationConstants.DEBUG) {
                System.out.println("OCR for " + field.toString() + " took " + elapsedTime + "ms");
            }
        }

        if (ApplicationConstants.DEBUG) {
            if (lastImage.recognisedValue == null)
                System.out.println("Value of " + field.toString() + ": lastImage.recognisedValue null");
            else
                System.out.println("Value of " + field.toString() + ": " + lastImage.recognisedValue.getValue());
        }
        return lastImage.recognisedValue;
    }

    public LCDState getBoxState(ScreenBox box) {
        return boxStates.get(box);
    }

    public boolean isTimeChanging() {
        return timeChanging;
    }

    // Returns true iff the given LCD is lit in the latest received image
    public boolean isBoxActiveNow(ScreenBox box) {
        return history.getLast().activeBoxes.contains(box);
    }
}
