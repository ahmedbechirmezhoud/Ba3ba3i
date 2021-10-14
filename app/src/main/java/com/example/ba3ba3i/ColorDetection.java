package com.example.ba3ba3i;


import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.RequiresApi;


public class ColorDetection{
    @RequiresApi(api = Build.VERSION_CODES.Q)
    static boolean detect(Bitmap bitmap){
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();

        int x1 = (int) (width / 4);
        int x2 = (int) (width*3 / 4);
        int x3 = (int) (width / 2);

        int y1 = (int) (height / 4);
        int y2 = (int) (height*3 / 4);
        int y3 = (int) (height / 2);

        Color color1 = bitmap.getColor(x1, y1);
        Color color2 = bitmap.getColor(x1, y2);
        Color color3 = bitmap.getColor(x2, y1);
        Color color4 = bitmap.getColor(x2, y2);
        Color color5 = bitmap.getColor(x1, y3);
        Color color6 = bitmap.getColor(x2, y3);
        Color color7 = bitmap.getColor(x3, y1);
        Color color8 = bitmap.getColor(x3, y2);
        Color color9 = bitmap.getColor(x3, y3);

        int red = (int) (color1.red() + color2.red() + color3.red() +
                        color4.red() + color5.red() + color6.red() +
                        color7.red() + color8.red() + color9.red());

        int blue = (int) (color1.blue() + color2.blue() + color3.blue() +
                        color4.blue() + color5.blue() + color6.blue() +
                        color7.blue() + color8.blue() + color9.blue());


        return(blue <= red);
    }
}