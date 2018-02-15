package uk.ac.cam.groupprojects.bravo.main;

import uk.ac.cam.groupprojects.bravo.imageProcessing.CameraException;
import uk.ac.cam.groupprojects.bravo.imageProcessing.ConfigException;
import uk.ac.cam.groupprojects.bravo.imageProcessing.ImageSegments;
import uk.ac.cam.groupprojects.bravo.imageProcessing.PiCamera;
import uk.ac.cam.groupprojects.bravo.tts.FestivalMissingException;
import uk.ac.cam.groupprojects.bravo.tts.Synthesiser;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static uk.ac.cam.groupprojects.bravo.main.ApplicationConstants.*;

/**
 * Created by david on 13/02/2018.
 */
public class AudibleAppliances {

    private static Synthesiser synthesiser;
    private static ImageSegments segments;
    private static BikeStateTracker bikeStateTracker;
    private static boolean running = false;

    private static int timeTracker = 0;

    public static void main(String[] args) {
        printHeader();

        //Load config
        System.out.println("Loading in config from " + PATH_TO_CONFIG );
        try {

            //segments = new ImageSegments( PATH_TO_CONFIG );
            System.out.println("Config loaded successfully");
            System.out.println("Setting up required components");
            bikeStateTracker = new BikeStateTracker( segments );
            synthesiser = new Synthesiser();
            System.out.println("Components set up successfully!");

            running = true;
            new Thread( runTracker ).start();

            Thread inputThread = new Thread( handleInput );
            inputThread.setDaemon(true);
            inputThread.start();
        } catch( FestivalMissingException e ){
            if ( DEBUG )
                e.printStackTrace();
            System.out.println("FATAL ERROR: Could not load voice library!");
            printFooter();
        } catch (Exception e) {
            if ( DEBUG )
                e.printStackTrace();
            System.out.println("FATAL ERROR: Could not load in config");
            printFooter();
        }
    }

    static Runnable handleInput = () -> {
        try {
            System.out.println("Type 'exit' to close the application");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader( System.in ) );
            while ( running ){
                String input = reader.readLine();
                if ( input.trim().length() > 0 && input.compareToIgnoreCase("exit") == 0 ){
                    running = false;
                    System.out.println("ENDING Audible Appliances");
                }else {
                    System.out.println("Type 'exit' to close the application!");
                }
            }
            System.out.println("DONE1");
        }catch( IOException e ){
            if ( DEBUG )
                e.printStackTrace();
            System.out.println("FATAL ERROR: Cannot read from System.in");
            printFooter();
        }
    };

    static Runnable runTracker = () ->{
        System.out.println();
        System.out.println("Starting the bike state tracker! ");
        synthesiser.speak("Welcome to Audible Appliances");

        while( running ){
            try {
                Thread.sleep( UPDATE_FREQ );
                timeTracker += UPDATE_FREQ;
            } catch (InterruptedException e) {
                if ( DEBUG )
                    e.printStackTrace();
            }

            try {
                bikeStateTracker.processNewImage( PiCamera.takeImage() );
            } catch (CameraException e) {
                if ( DEBUG )
                    e.printStackTrace();
            }

            if ( timeTracker == SPEAK_FREQ ){
                timeTracker = 0;
                bikeStateTracker.speakItems();
            }
        }
        synthesiser.speak("Goodbye! Hope to see you again soon!");
        synthesiser.close();
        printFooter();
    };

    private static void printHeader(){
        System.out.println("|----------------------------------------|");
        System.out.println("|----------------------------------------|");
        System.out.println("|----------------------------------------|");
        System.out.println("|---------- AUDIBLE APPLIANCES ----------|");
        System.out.println("|-------------- VERSION " + VERSION_NO + " -------------|");
        System.out.println("|------------- DEVELOPED BY: ------------|");
        System.out.println("|------------- Oliver Hope --------------|");
        System.out.println("|------------ Cameron Ramsay ------------|");
        System.out.println("|----------- Keith Collister ------------|");
        System.out.println("|------------ David Adeboye -------------|");
        System.out.println("|------------ Patryk Balicki ------------|");
        System.out.println("|------------ Tom Strudwick -------------|");
        System.out.println("|----------------------------------------|");
        System.out.println("|----------------------------------------|");
        System.out.println();
    }

    private static void printFooter(){
        System.out.println();
        System.out.println("|----------------------------------------|");
        System.out.println("|---------- AUDIBLE APPLIANCES ----------|");
        System.out.println("|----------------------------------------|");
    }

}