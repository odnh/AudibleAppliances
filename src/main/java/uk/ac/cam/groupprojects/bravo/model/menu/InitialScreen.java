package uk.ac.cam.groupprojects.bravo.model.menu;

import java.util.Arrays;
import java.util.List;

import uk.ac.cam.groupprojects.bravo.imageProcessing.ScreenBox;
import uk.ac.cam.groupprojects.bravo.main.BikeStateTracker;
import uk.ac.cam.groupprojects.bravo.model.LCDState;

public class InitialScreen extends BikeScreen {
    @Override
    public boolean isActiveScreen(BikeStateTracker state) {

        return state.getBoxState(ScreenBox.LCD_TEXT_1) == LCDState.SOLID_OFF &&
               state.getBoxState(ScreenBox.LCD_TEXT_3) == LCDState.SOLID_OFF &&
               state.getBoxState(ScreenBox.LCD_TEXT_4) == LCDState.SOLID_OFF &&
               state.getBoxState(ScreenBox.LCD_TEXT_5_TOP) == LCDState.SOLID_ON &&
               state.getBoxState(ScreenBox.LCD_TEXT_9) == LCDState.SOLID_OFF &&
               state.getBoxState(ScreenBox.LCD6) == LCDState.BLINKING;
    }

    @Override
    public ScreenEnum getEnum() {
        return ScreenEnum.INITIAL_SCREEN;
    }

    @Override
    public List<String> formatSpeech(BikeStateTracker bikeStateTracker) {
        return Arrays.asList("Please press the start button");
    }

    @Override
    public int getSpeakDelay() {
        return 20000;
    }

    @Override
    public boolean isSpeakFirst() {
        return true;
    }
}
