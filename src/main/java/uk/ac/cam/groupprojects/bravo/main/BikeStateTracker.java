package uk.ac.cam.groupprojects.bravo.main;

import uk.ac.cam.groupprojects.bravo.graphProcessing.Graph;
import uk.ac.cam.groupprojects.bravo.imageProcessing.BoxType;
import uk.ac.cam.groupprojects.bravo.imageProcessing.ImageSegments;
import uk.ac.cam.groupprojects.bravo.model.numbers.*;
import uk.ac.cam.groupprojects.bravo.model.screen.LCD;
import uk.ac.cam.groupprojects.bravo.ocr.SegmentRecogniser;
import uk.ac.cam.groupprojects.bravo.ocr.UnrecognisedDigitException;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Created by david on 13/02/2018.
 */
public class BikeStateTracker {

    /**
     * Current state that we are tracking on the bike
     */
    private Calories currentCalories;
    private Distance currentDistance;
    private Level currentLevel;
    private Pulse currentPulse;
    private Speed currentSpeed;
    private Time currentTime;

    private LCD lcdScreen;

    /**
     * Other state
     */
    private ImageSegments segments;

    public BikeStateTracker( ImageSegments segments ){
        currentCalories = new Calories();
        currentDistance = new Distance();
        currentLevel = new Level();
        currentPulse = new Pulse();
        currentSpeed = new Speed();
        currentTime = new Time();

        lcdScreen = new LCD();

        this.segments = segments;
    }

    public void processNewImage( BufferedImage newImage )
                throws IOException,UnrecognisedDigitException, NumberFormatException {
        BufferedImage temp;

        if ( segments == null ){
            System.out.println("Fatal Error: SEGMENTS IS NULL! ");
            throw new IOException();
        }

        if ( newImage == null ){
            System.out.println("Fatal Error: NewImage is NULL! ");
            throw new IOException();
        }

        temp = segments.getImageBox( BoxType.SPEED, newImage );
        currentSpeed.setValue(SegmentRecogniser.recogniseInt(temp));

        temp = segments.getImageBox( BoxType.TIME, newImage );
        currentTime.setValue(SegmentRecogniser.recogniseInt(temp));

        temp = segments.getImageBox( BoxType.DISTANCE, newImage );
        currentDistance.setValue(SegmentRecogniser.recogniseInt(temp));

        temp = segments.getImageBox( BoxType.PROGRAM, newImage );
        currentLevel.setValue(SegmentRecogniser.recogniseInt(temp));

        temp = segments.getImageBox( BoxType.CAL, newImage );
        currentCalories.setValue(SegmentRecogniser.recogniseInt(temp));

        temp = segments.getImageBox( BoxType.PULSE, newImage );
        currentPulse.setValue(SegmentRecogniser.recogniseInt(temp));

        temp = segments.getImageBox( BoxType.GRAPH, newImage );
        lcdScreen = new Graph( temp ).get();
    }

    public void speakItems(){
        //Will need to pass in some sort of config
    }


}
