package uk.ac.cam.groupprojects.bravo.imageProcessing;

import java.awt.image.BufferedImage;

/**
 * Created by david on 03/03/2018.
 *
 * Works on threshold-ed images (white on black backgrounds)
 * Basically removes anything at the top of the screen, i.e. the bottom of a label
 */
public class IntelligentCropping {

    private static final int THRESHOLD_LIMIT = 40;
    private static final int SEARCH_LIMIT = 2;

    public static BufferedImage intelligentCrop( BufferedImage image ){
        FastRGB rgb = new FastRGB( image );

        int imageHeight = image.getHeight();

        for ( int i = 0; i < image.getWidth(); i++ ){
            int start = -1;
            int height = 0;
            int limit = SEARCH_LIMIT;
            for ( int j = 0; j < limit; j++ ){
                if ( rgb.getRGB(i, j) > THRESHOLD_LIMIT ){
                    if ( start == -1 )
                        start = j;

                    limit++;
                    height++;
                }
            }
            if ( height > 0 && height < ( imageHeight * 0.1 ) ){
                image.setRGB( i, start, 1, height, new int[ height ], 0, 1 );
            }
        }

        return image;
    }

}