package uk.ac.cam.groupprojects.bravo.model.menu;

import uk.ac.cam.groupprojects.bravo.imageProcessing.ScreenBox;
import uk.ac.cam.groupprojects.bravo.main.ApplicationConstants;
import uk.ac.cam.groupprojects.bravo.main.BikeStateTracker;
import uk.ac.cam.groupprojects.bravo.model.LCDState;

public class SelectManualScreen extends BikeScreen {
    @Override
    public boolean isActiveScreen(BikeStateTracker state) {

        return !state.isTimeChanging() &&
               state.getBoxState(ScreenBox.LCD_TEXT_1) == LCDState.SOLID_OFF &&
               state.getBoxState(ScreenBox.LCD_TEXT_3) == LCDState.SOLID_OFF &&
               state.getBoxState(ScreenBox.LCD_TEXT_4) == LCDState.BLINKING &&
               state.getBoxState(ScreenBox.LCD_TEXT_5_TOP) == LCDState.BLINKING &&
               state.getBoxState(ScreenBox.LCD_TEXT_9) == LCDState.BLINKING;
    }

    @Override
    public ScreenEnum getEnum() {
        return ScreenEnum.SELECT_MANUAL;
    }

    @Override
    public String formatSpeech(BikeStateTracker bikeStateTracker) {
        return "Press enter to cycle without program or rotate right to choose a program.";
    }

    @Override
    public int getSpeakDelay() {
        return ApplicationConstants.DEFAULT_SPEAK_FREQ / 2;
    }

    @Override
    public boolean isSpeakFirst() {
        return true;
    }
}
