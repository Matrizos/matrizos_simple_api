package com.matrizos.matrizos_simple_api.data;

import com.matrizos.matrizos_simple_api.ui.ButtonUI;
import com.matrizos.matrizos_simple_api.ui.ColorModelUI;
import com.matrizos.matrizos_simple_api.ui.FrameUI;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.awt.geom.Point2D;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Optional;

public class UtilMethods {
    public static ColorModelUI getColorModelUI(JSONObject jo, Object source) {
//      linearGradientPaint
        HashMap<String, DrawableBuilder.Value> fields = new HashMap<>();
        HashMap<String, Class<?>> refs = new HashMap<>();
        return new ColorModelUI(getPaint(jo, source),
            source instanceof FrameUI frameUI ? frameUI.getBounds() :
            source instanceof ButtonUI buttonUI ? buttonUI.getBounds() : new Rectangle(
            JSONMetaData.calcInt(source.getClass(), jo.get("x"), source, fields, refs).getAsInt(),
            JSONMetaData.calcInt(source.getClass(), jo.get("y"), source, fields, refs).getAsInt(),
            JSONMetaData.calcInt(source.getClass(), jo.get("width"), source, fields, refs).getAsInt(),
            JSONMetaData.calcInt(source.getClass(), jo.get("height"), source, fields, refs).getAsInt()
        ));
    }

    public static Paint getPaint(JSONObject jo, Object source) {
        HashMap<String, DrawableBuilder.Value> fields = new HashMap<>();
        HashMap<String, Class<?>> refs = new HashMap<>();
        return switch(jo.getString("type")) {
            case "normalPaint" -> getColorOf(jo.get("color"));
            case "linearGradientPaint" -> new LinearGradientPaint(
                (float)JSONMetaData.calcDouble(source.getClass(), jo.get("startX"), source, fields, refs).getAsDouble(),
                (float)JSONMetaData.calcDouble(source.getClass(), jo.get("startY"), source, fields, refs).getAsDouble(),
                (float)JSONMetaData.calcDouble(source.getClass(), jo.get("endX"), source, fields, refs).getAsDouble(),
                (float)JSONMetaData.calcDouble(source.getClass(), jo.get("endY"), source, fields, refs).getAsDouble(),
                getFractionsOf(jo.getJSONArray("fractions")),
                getColorsOf(jo.getJSONArray("colors"))
            );
            case "radialGradientPaint" -> new RadialGradientPaint(
                new Point2D.Float(
                    (float)JSONMetaData.calcDouble(source.getClass(), jo.get("centerX"), source, fields, refs).getAsDouble(),
                    (float)JSONMetaData.calcDouble(source.getClass(), jo.get("centerY"), source, fields, refs).getAsDouble()
                ),
                Math.abs((float)JSONMetaData.calcDouble(source.getClass(), jo.get("radius"), source, fields, refs).getAsDouble()),
                new Point2D.Float(
                    (float)JSONMetaData.calcDouble(source.getClass(), Optional.of(jo.opt("focusX")).orElse(jo.get("centerX")), source, fields, refs).getAsDouble(),
                    (float)JSONMetaData.calcDouble(source.getClass(), Optional.of(jo.opt("focusY")).orElse(jo.get("centerY")), source, fields, refs).getAsDouble()
                ),
                getFractionsOf(jo.getJSONArray("fractions")),
                getColorsOf(jo.getJSONArray("colors")),
                jo.optEnum(MultipleGradientPaint.CycleMethod.class, "cycleMethod", MultipleGradientPaint.CycleMethod.NO_CYCLE)
            );
            case null, default -> throw new RuntimeException("not a valid paint: " + jo.getString("type"));
        };
    }

    public static Color[] getColorsOf(JSONArray array) {
        Color[] colors = new Color[array.length()];
        for(int i = 0; i < array.length(); i++)
            colors[i] = getColorOf(array.get(i));
        return colors;
    }

    public static Color getColorOf(Object source) {
        return switch(source) {
            case Integer i -> new Color(i);
            case BigInteger bi -> new Color(bi.intValue());
            case String s -> new Color(Integer.parseInt(
                s.startsWith("0x") ? s.substring(2) : s.startsWith("#") ? s.substring(1) : s, 16
            ));
            case JSONArray ja -> new Color(ja.getInt(0), ja.getInt(1), ja.getInt(2), ja.optInt(3, 255));
            default -> throw new IllegalStateException("Unexpected value: " + source);
        };
    }

    public static float[] getFractionsOf(JSONArray array) {
        float[] r = new float[array.length()];
        for(int i = 0; i < r.length; i++)
            r[i] = array.getFloat(i);
        return r;
    }
}
