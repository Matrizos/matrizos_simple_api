package com.matrizos.matrizos_simple_api.math;

import java.awt.*;
import java.awt.geom.Area;

public final class ForShapes {
    public static Area ofIntersections(Shape... shapes) {
        Area area = new Area();
        for(Shape rhs : shapes)
            area.add(new Area(rhs));
        return area;
    }
}
