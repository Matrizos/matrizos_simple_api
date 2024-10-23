package com.matrizos.matrizos_simple_api.ui;

import com.matrizos.matrizos_simple_api.data.UtilMethods;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

import java.awt.*;

@Getter @Setter
public class ColorModelUI {
    private Paint paint;
    private Shape bounds;

    public ColorModelUI(Paint paint, Shape bounds) {
        this.paint = paint;
        this.bounds = bounds;
    }

    public void set(Paint paint, Shape bounds) {
        this.paint = paint;
        this.bounds = bounds;
    }

    public void update(JSONObject jo, Object source) {
        long oldTime = System.currentTimeMillis();
        paint = UtilMethods.getPaint(jo, source);
        long newTime = System.currentTimeMillis();
        System.out.println("updated in " + (newTime - oldTime) + "ms");
    }

    public void update(Shape bounds) {
        this.bounds = bounds;
        if(paint instanceof LinearGradientPaint)
            paint = new LinearGradientPaint(bounds.getBounds().width / 2f, 0f, bounds.getBounds().width / 2f, bounds.getBounds().height,
                ((LinearGradientPaint)paint).getFractions(), ((LinearGradientPaint)paint).getColors());
    }

    private static float[] defaultFractions(int length) {
        float[] fractions = new float[length];
        for(int i = 0; i < fractions.length; i++)
            fractions[i] = (float)(i + 1) / length;
        return fractions;
    }
}
