package uk.ac.cam.groupprojects.bravo.main;

import uk.ac.cam.groupprojects.bravo.config.BikeField;
import uk.ac.cam.groupprojects.bravo.config.ConfigData;
import uk.ac.cam.groupprojects.bravo.imageProcessing.ScreenBox;
import uk.ac.cam.groupprojects.bravo.main.threads.SegmentActiveThread;
import uk.ac.cam.groupprojects.bravo.main.threads.SegmentRecogniserThread;
import uk.ac.cam.groupprojects.bravo.model.LCDState;
import uk.ac.cam.groupprojects.bravo.model.menu.*;
import uk.ac.cam.groupprojects.bravo.model.numbers.*;
import uk.ac.cam.groupprojects.bravo.ocr.SegmentRecogniser;
import uk.ac.cam.groupprojects.bravo.ocr.UnrecognisedDigitException;
import uk.ac.cam.groupprojects.bravo.tts.Synthesiser;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static uk.ac.cam.groupprojects.bravo.main.ApplicationConstants.DEBUG;

/**
 * Created by david on 13/02/2018.
 */
public class BikeStateTracker {

    class StateTime {
        public LocalDateTime addedTime;
        public Set<ScreenBox> activeBoxes;
        public int recognisedTime;

        public StateTime(LocalDateTime addedTime, Set<ScreenBox> activeBoxes, int recognisedTime) {
            this.addedTime = addedTime;
            this.activeBoxes = activeBoxes;
        }
    }

    // Necessary class state
    private final Map<ScreenEnum, BikeScreen> screens = new HashMap<>();
    private final ConfigData configData;
    private final Synthesiser synthesiser;

    // Current Screen state
    private static BikeScreen currentScreen;

    private static long lastSpeakTime = 0;

    /**
     * Current state that we are tracking on the bike
     */
    private Map<BikeField, ScreenNumber> currentFields;
    // Back of the list is the most recent state added
    // We keep 2 seconds of state, so we can determine if time is changing
    // We only look at 1 second of the state to determine if a segment is blinking,
    // otherwise we introduce a lot of latency when changing state
    private final LinkedList<StateTime> history;

    // State over time of the LCDs - 
    private final Map<ScreenBox, LCDState> boxStates;
    private boolean timeChanging;


    private static final ScreenBox[] thread1_boxes = new ScreenBox[]{
            ScreenBox.LCD1,
            ScreenBox.LCD2,
            ScreenBox.LCD3,
            ScreenBox.LCD4,
            ScreenBox.LCD5,
            ScreenBox.LCD6,

            ScreenBox.LCD_TEXT_1,
            ScreenBox.LCD_TEXT_2,
            ScreenBox.LCD_TEXT_3,
            ScreenBox.LCD_TEXT_4,
            ScreenBox.LCD_TEXT_5_TOP,
            ScreenBox.LCD_TEXT_5_BOTTOM,

    };

    private static final ScreenBox[] thread2_boxes = new ScreenBox[]{
            ScreenBox.GRAPH,
            ScreenBox.RPM,
            ScreenBox.WATT,
            ScreenBox.SPEED,
            ScreenBox.LOAD,

            ScreenBox.LCD_TEXT_6,
            ScreenBox.LCD_TEXT_7,
            ScreenBox.LCD_TEXT_8,
            ScreenBox.LCD_TEXT_9,
            ScreenBox.LCD_TEXT_10,
            ScreenBox.LCD_TEXT_11,
            ScreenBox.LCD_TEXT_12,
    };

    public BikeStateTracker(ConfigData config, Synthesiser synth) {
        history = new LinkedList<>();
        boxStates = new HashMap<>();
        timeChanging = false;
        configData = config;
        synthesiser = synth;

        // Initialise currentFields;
        currentFields = new HashMap<>();
        currentFields.put(BikeField.CAL, new Calories());
        currentFields.put(BikeField.DISTANCE, new Distance());
        currentFields.put(BikeField.PULSE, new Pulse());
        currentFields.put(BikeField.RPM, new RPM());
        currentFields.put(BikeField.LOAD, new Load());
        currentFields.put(BikeField.SPEED, new Speed());
        currentFields.put(BikeField.TIME, new Time());
        currentFields.put(BikeField.WATT, new Watt());

        // Initialise screens
        screens.put(ScreenEnum.OFF_SCREEN, new OffScreen());
        screens.put(ScreenEnum.ERROR_SCREEN, new ErrorScreen());
        screens.put(ScreenEnum.INITIAL_SCREEN, new InitialScreen());
        screens.put(ScreenEnum.RUNNING_SCREEN, new RunningScreen());
        screens.put(ScreenEnum.PAUSED_SCREEN, new PausedScreen());
        screens.put(ScreenEnum.PROGRAM, new ProgramScreen());
        screens.put(ScreenEnum.SELECT_MANUAL, new SelectManualScreen());
        screens.put(ScreenEnum.SELECT_HRC, new SelectHRCScreen());
        screens.put(ScreenEnum.SELECT_USER_PROGRAM, new SelectUserProgramScreen());
        screens.put(ScreenEnum.SELECT_WATTS, new SelectWattScreen());
        screens.put(ScreenEnum.SELECT_PROGRAM, new SelectProgramScreen());

        // Set default screen
        currentScreen = screens.get(ScreenEnum.OFF_SCREEN);
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

        //////////////////////////////////////////
        // Update the screen (ie overall state) //
        //////////////////////////////////////////

        if (DEBUG)
            System.out.println("DETECTING CHANGE SCREEN STATE");

        BikeScreen newScreen = null;
        BikeScreen oldScreen = currentScreen;

        for (BikeScreen screen : screens.values()) {
            boolean inState = screen.isActiveScreen(this);
            if (inState) {
                newScreen = screen;
                break;
            }
        }

        if (newScreen != null) {
            currentScreen = newScreen;
            if (newScreen != oldScreen && newScreen.isSpeakFirst())
                newScreen.speakItems(this, synthesiser);
        }
        else {
            if (DEBUG)
                System.out.println("Failed to recognise state.");
        }

        System.out.println("Establishing bike state is " + currentScreen.getEnum().toString());
        System.out.println();

        ////////////////////////////////////////
        // Update the state (ie field values) //
        ////////////////////////////////////////

        LocalDateTime currentTime = LocalDateTime.now();
        Set<ScreenBox> activeSegs = new HashSet<>();

        // Map each screen region to the image of that region
        // Compute which LCD segments are lit up (active)

        Thread segmentActive1 = new Thread(
                new SegmentActiveThread( thread1_boxes, activeSegs, imgSegs )
        );

        Thread segmentActive2 = new Thread(
                new SegmentActiveThread( thread2_boxes, activeSegs, imgSegs )
        );

        segmentActive1.start();
        segmentActive2.start();

        try {
            //Block until these have finished
            segmentActive1.join();
            segmentActive2.join();
        } catch (InterruptedException e) {
            //Lol help us if this happens
            if ( DEBUG )
                e.printStackTrace();
        }

        // Recognise the text in each region of the screen
        if (currentScreen.getEnum() == ScreenEnum.RUNNING_SCREEN || currentScreen.getEnum() == ScreenEnum.PROGRAM) {
            Thread segmentRecogniser1 = new Thread(
                    new SegmentRecogniserThread( thread1_boxes, activeSegs, imgSegs, currentFields )
            );
            Thread segmentRecogniser2 = new Thread(
                    new SegmentRecogniserThread( thread2_boxes, activeSegs, imgSegs, currentFields )
            );

            try {
                segmentRecogniser1.join();
                segmentRecogniser2.join();
            }catch (InterruptedException e){
                //Lol help us even more if this happens
                if ( DEBUG )
                    e.printStackTrace();
            }

        } else {
            if ( DEBUG ){
                try {
                    System.out.println("Running OCR for TIME" );
                    long startTime = System.currentTimeMillis();
                    currentFields.get(BikeField.TIME).setValue(SegmentRecogniser.recogniseInt(imgSegs.get(ScreenBox.LCD1)));
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    System.out.println("That took " + elapsedTime);
                }catch ( Exception e ){
                    System.out.println("OCR crashed");
                }
            }else {
                try {
                    currentFields.get(BikeField.TIME).setValue(SegmentRecogniser.recogniseInt(imgSegs.get(ScreenBox.LCD1)));
                }catch ( Exception e ){
                    //I don't care
                }
            }
        }
        
        // Remove state information that's older than two complete blink cycles (2s) ago
        while (history.size() > 0) {
            if (currentTime.minus(4 * ApplicationConstants.BLINK_FREQ, ChronoUnit.MILLIS)
                           .isAfter(history.getFirst().addedTime)) {
                history.removeFirst();
            } else {
                break;
            }                
        }

        if (DEBUG) {
            System.out.println("Current state snapshots:");
            for (StateTime s : history) {
                System.out.println(s.addedTime.get(ChronoField.MILLI_OF_DAY));
            }
            System.out.println();
        }

        // Store the new state, with the time we recognised at the moment
        history.add(new StateTime(currentTime, activeSegs, currentFields.get(BikeField.TIME).getValue()));

        // Update which LCDs we know are solid/blinking
        updateSolidBlinking(currentTime);
        updateTimeChanging();

        if (DEBUG) {
            System.out.println("Current State:");
            System.out.println("Time Changing: " + isTimeChanging());
            for (ScreenBox box : ScreenBox.values()) {
                System.out.println(box.toString() + ": " + getBoxState(box).toString());
            }
            System.out.println();
        }

        // Check if time to speak, and if yes then speak
        if (System.currentTimeMillis() - lastSpeakTime > currentScreen.getSpeakDelay()) {
            currentScreen.speakItems(this, synthesiser);
            lastSpeakTime = System.currentTimeMillis();
            if (DEBUG)
                System.out.println("Spoke");
        }

    }

    // Update which boxes are blinking and which are solid
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
            } else if (active) {
                boxStates.put(box, LCDState.SOLID_ON);
            } else {
                boxStates.put(box, LCDState.SOLID_OFF);
            }
        }
    }

    private void updateTimeChanging() {
        if (history.size() == 0) {
            return;
        }

        int currentTime = history.getLast().recognisedTime;
        timeChanging = history.stream()
                              .allMatch(s -> s.recognisedTime == currentTime);
    }

    public ConfigData getConfig() {
        return configData;
    }

    public ScreenNumber getFieldValue(BikeField field) {
        return currentFields.get(field);
    }

    public LCDState getBoxState(ScreenBox box) {
        return boxStates.get(box);
    }

    public boolean isTimeChanging() {
        return timeChanging;
    }

    // Returns true iff the given box is lit in the latest received image
    public boolean isBoxActiveNow(ScreenBox box) {
        return history.getLast().activeBoxes.contains(box);
    }
}
