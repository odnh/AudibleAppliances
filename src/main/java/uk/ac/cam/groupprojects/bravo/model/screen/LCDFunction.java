package uk.ac.cam.groupprojects.bravo.model.screen;

/**
 * Created by david on 12/02/2018.
 */
public enum LCDFunction {
    MANUAL(
            "10000000 10000000 10000000 10000000 10000000 10000000 10000000 10000000 10000000 10000000",
            "11000000 11000000 11000000 11000000 11000000 11000000 11000000 11000000 11000000 11000000",
            "11100000 11100000 11100000 11100000 11100000 11100000 11100000 11100000 11100000 11100000",
            "11110000 11110000 11110000 11110000 11110000 11110000 11110000 11110000 11110000 11110000",
            "11111000 11111000 11111000 11111000 11111000 11111000 11111000 11111000 11111000 11111000",
            "11111100 11111100 11111100 11111100 11111100 11111100 11111100 11111100 11111100 11111100",
            "11111110 11111110 11111110 11111110 11111110 11111110 11111110 11111110 11111110 11111110",
            "11111111 11111111 11111111 11111111 11111111 11111111 11111111 11111111 11111111 11111111",
            "10000000 10000000 11000000 11100000 11100000 11100000 11100001 11100000 11110000 11111000"
    ),
    STEPS (
            "10000000 10000000 11000000 11000000 11100000 11100000 11110000 11110000 11111000 11111000",
            "11000000 11000000 11100000 11100000 11110000 11110000 11111000 11111000 11111100 11111100",
            "11100000 11100000 11110000 11110000 11111000 11111000 11111100 11111100 11111110 11111110",
            "11110000 11110000 11111000 11111000 11111100 11111100 11111110 11111110 11111111 11111111",
            "11111000 11111000 11111100 11111100 11111110 11111110 11111111 11111111 11111111 11111111",
            "11111100 11111100 11111110 11111110 11111111 11111111 11111111 11111111 11111111 11111111",
            "11111110 11111110 11111111 11111111 11111111 11111111 11111111 11111111 11111111 11111111"
    ),
    HILL (
            "11100000 11100000 11110000 11110000 11111000 11111000 11110000 11110000 11100000 11100000",
            "11110000 11110000 11111000 11111000 11111100 11111100 11111000 11111000 11110000 11110000",
            "11111000 11111000 11111100 11111100 11111110 11111110 11111100 11111100 11111000 11111000",
            "11111100 11111100 11111110 11111110 11111111 11111111 11111110 11111110 11111100 11111100"
    ),
    NOT_DEF

    ;


    private String[] values;

    LCDFunction( String... values ){
        this.values = values;
    }

}
