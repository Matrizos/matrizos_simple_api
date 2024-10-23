package com.matrizos.matrizos_simple_api.ui;

import com.matrizos.matrizos_simple_api.data.DrawableBuilder;
import com.matrizos.matrizos_simple_api.data.JSONMetaData;
import com.matrizos.matrizos_simple_api.data.UtilMethods;
import lombok.Getter;
import org.json.JSONObject;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

public class ButtonUI {
    @Getter private final FrameUI frameUI;
    private final Consumer<MouseEvent> pressedFunction, releasedFunction;
    @Getter private ColorModelUI colorModelUI;
    @Getter protected boolean pressed = false, enabled = true;
    @Getter private RoundRectangle2D.Float bounds;
    private ScheduledFuture<?> animation = null;
    private DoubleSupplier x, y, width, height;

    public ButtonUI(FrameUI frameUI, JSONObject paint, Object x, Object y, Object width, Object height,
                    Consumer<MouseEvent> pressedFunction, Consumer<MouseEvent> releasedFunction) {
        this.frameUI = frameUI;
        this.pressedFunction = pressedFunction;
        this.releasedFunction = releasedFunction;
        HashMap<String, DrawableBuilder.Value> fields = new HashMap<>();
        HashMap<String, Class<?>> refs = new HashMap<>();
        this.bounds = new RoundRectangle2D.Float(
            (float)(this.x = JSONMetaData.calcDouble(getClass(), x, this, fields, refs)).getAsDouble(),
            (float)(this.y = JSONMetaData.calcDouble(getClass(), y, this, fields, refs)).getAsDouble(),
            (float)(this.width = JSONMetaData.calcDouble(getClass(), width, this, fields, refs)).getAsDouble(),
            (float)(this.height = JSONMetaData.calcDouble(getClass(), height, this, fields, refs)).getAsDouble(),
            10,
            10
        );
        this.colorModelUI = UtilMethods.getColorModelUI(paint, this);
    }

    public void set(JSONObject jo) {
        HashMap<String, DrawableBuilder.Value> fields = new HashMap<>();
        HashMap<String, Class<?>> refs = new HashMap<>();
        bounds.x = (float)(this.x = JSONMetaData.calcDouble(getClass(), x, this, fields, refs)).getAsDouble();
        bounds.y = (float)(this.y = JSONMetaData.calcDouble(getClass(), y, this, fields, refs)).getAsDouble();
        bounds.width = (float)(this.width = JSONMetaData.calcDouble(getClass(), width, this, fields, refs)).getAsDouble();
        bounds.height = (float)(this.height = JSONMetaData.calcDouble(getClass(), height, this, fields, refs)).getAsDouble();
        colorModelUI.set(UtilMethods.getPaint(jo.getJSONObject("paint"), this), getBounds());
    }

    public void updateBounds() {
        bounds.x = (float)x.getAsDouble();
        bounds.y = (float)y.getAsDouble();
    }

    public void mousePressed(MouseEvent e) {
        pressedFunction.accept(e);
    }

    public void mouseReleased(MouseEvent e) {
        releasedFunction.accept(e);
    }

    public void mouseMoved(MouseEvent e) {
//        if(animation == null)
//            animation = frameUI.getPool().schedule(() -> {}, 100, TimeUnit.MILLISECONDS);
    }
}
