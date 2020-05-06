/*
* Copyright (C) 2017 Paranoid Android
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.android.systemui.secureapps;

import android.annotation.ColorInt;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

public class ColorUtils {

    public static int getIconColorFromDrawable(Drawable drawable) {
        if (drawable == null) {
            return -3;
        }
        if (drawable.getConstantState() == null) {
            return -3;
        }
        Drawable copyDrawable = drawable.getConstantState().newDrawable();
        if (copyDrawable == null) {
            return -3;
        }
        if (copyDrawable instanceof ColorDrawable) {
            return ((ColorDrawable) drawable).getColor();
        }
        Bitmap bitmap = drawableToBitmap(copyDrawable);
        if (bitmap == null) {
            return -3;
        }
        return getDominantColor(bitmap);
    }

    public static int getDominantColor(Bitmap bitmap) {
        if (bitmap == null) {
            return -3;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = width * height;
        int pixels[] = new int[size];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        int r = 0;
        int g = 0;
        int b = 0;
        int count = 0;
        for (int i = 0; i < pixels.length; i++) {
            int color = pixels[i];
            if (Color.alpha(color) > 0) {
                r += Color.red(color);
                g += Color.green(color);
                b += Color.blue(color);
                count++;
            }
        }
        return Color.rgb(r / count, g / count, b / count);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static boolean isColorDark(@ColorInt int color) {
        return Color.luminance(color) < 0.5f;
    }
}