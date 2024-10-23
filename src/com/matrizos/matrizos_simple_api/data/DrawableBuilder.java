package com.matrizos.matrizos_simple_api.data;

import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Objects;
import java.util.Vector;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import static com.matrizos.matrizos_simple_api.data.JSONMetaData.condition;

// my heard burned more than my computer
// this is a programmer's sanity sacrifice

public final class DrawableBuilder {
    static final HashMap<String, Drawable> savedDrawables = new HashMap<>();
    static HashMap<String, Value> fields = new HashMap<>();
    static HashMap<String, Class<?>> refs = new HashMap<>();
    static Object currentSource;

    @SuppressWarnings("all")
    public static String enterParenteses(int i, String string, boolean already) {
        String sub = "";
        int parenteses = already ? 1 : 0;
        char c;
        while(i < string.length()) breakable: {
            c = string.charAt(i);
            i++;
            if(c == '(') {
                parenteses++;
                if(parenteses == 1)
                    break breakable;
            } else if(c == ')') {
                parenteses--;
                if(parenteses <= 0)
                    return sub;
            }
            if(parenteses != 0)
                sub += c;
        }
        return sub;
    }

    private interface DrawableConsumer {
        void accept(Graphics2D g2, int offX, int offY);
    }

    //
    // if you judge me because this static method can be used only by one thread per time,
    // you aren't conscience of the stress of the programmer's life, my head is a head, not a furnace,
    // for knows, be one, easy is talk about me, but to do what I do, is hard?
    //

    public static synchronized Drawable createDrawable(Object source, JSONObject jo) throws NullPointerException {
        Class<?> clazz = (currentSource = source).getClass();
        JSONArray newFieldsArray = null, classReference = null, draw;

        if(jo.has("new_fields"))
            newFieldsArray = jo.getJSONArray("new_fields");

        if(jo.has("class_ref"))
            classReference = jo.getJSONArray("class_ref");

        if(jo.has("draw"))
            draw = jo.getJSONArray("draw");
        else
            throw new NullPointerException("none for draw");

        fields = new HashMap<>();
        refs = new HashMap<>();
        if(classReference != null) {
            for(int i = 0; i < classReference.length(); i++) {
                String fullString = classReference.getString(i);
                String[] values = fullString.split("=");
                for(String name : values[0].replace(" ", "").split(","))
                    refs.put(name, JSONMetaData.clazz(values[1]));
            }
        }
        if(newFieldsArray != null)
            for(int i = 0; i < newFieldsArray.length(); i++) {
                String fullString = newFieldsArray.getString(i);
                String[] split = fullString.split("=");
                try {
                    fields.put(split[0], JSONMetaData.getField(clazz, split[1], DrawableBuilder.currentSource, DrawableBuilder.fields, DrawableBuilder.refs));
                } catch(NullPointerException e) {
                    System.err.println("for " + split[0]);
                    throw e;
                }
            }
        CodeBlock codeBlock = newBlock(() -> true, clazz, draw);
        return new Drawable(0, 0) {
            @Override
            public void toDraw(Graphics2D g, int offX, int offY) {
                codeBlock.accept(g, offX, offY);
            }
        };
    }

    private static CodeBlock newBlock(BooleanSupplier thisCan, Class<?> clazz, JSONArray array) {
        Vector<DrawableConsumer> consumers = new Vector<>();
        BooleanSupplier can = null;
        for(int i = 0; i < array.length(); i++) {
            Object obj = array.get(i);
            if(obj instanceof JSONArray fa) {
                Object obj1 = fa.get(0);
                if(obj1 instanceof String string) {
                    switch(string) {
                        case "if" -> {
                            BooleanSupplier finalCan = can;
                            BooleanSupplier canSupplier = condition(clazz, fa.getJSONArray(i + 1), DrawableBuilder.currentSource, DrawableBuilder.fields, refs);
                            can = can != null ? () -> finalCan.getAsBoolean() && canSupplier.getAsBoolean() : canSupplier;
                        }
                        case "newBlock" -> {
                            consumers.add(newBlock(can != null ? can : () -> true, clazz, fa));
                            can = null;
                        }
                        default -> consumers.add(getConsumer(clazz, fa));
                    }
                }
            }
        }
        return new CodeBlock(thisCan, consumers);
    }

    private static DrawableConsumer getConsumer(Class<?> clazz, JSONArray array) {
        String currentString;
        return switch(currentString = array.getString(0)) {
            case "drawShape", "fillShape" -> {
                String string = currentString;
                Shape shape = switch(currentString = array.getString(1)) {
                    case "Rectangle", "Ellipse" -> switch(currentString) {
                        case "Rectangle" -> new Rectangle2D.Float(array.getFloat(2), array.getFloat(3), array.getFloat(4), array.getFloat(5));
                        case "Ellipse" -> new Ellipse2D.Float(array.getFloat(2), array.getFloat(3), array.getFloat(4), array.getFloat(5));
                        default -> throw new IllegalStateException("Unexpected value: " + currentString);
                    };
                    case "RoundRectangle" -> new RoundRectangle2D.Float(array.getFloat(2), array.getFloat(3), array.getFloat(4), array.getFloat(5), array.getFloat(6), array.getFloat(7));
                    default -> throw new IllegalStateException("Unexpected value: " + currentString);
                };
                yield "drawShape".equals(string) ? (g2, _, _) -> g2.draw(shape) : (g2, _, _) -> g2.fill(shape);
            }
            case "drawImage" -> {
                BufferedImage img = array.get(1) instanceof String s ? (BufferedImage)JSONMetaData.getField(clazz, s, DrawableBuilder.currentSource, DrawableBuilder.fields, DrawableBuilder.refs).getValue() : null;
                IntSupplier x = JSONMetaData.calcInt(clazz, array.get(2), currentSource, fields, refs);
                IntSupplier y = JSONMetaData.calcInt(clazz, array.get(3), currentSource, fields, refs);
                yield (g2, offX, offY) -> g2.drawImage(img, x.getAsInt() + offX, y.getAsInt() + offY, null);
            }
            case "transform=translate" -> {
                DoubleSupplier x = JSONMetaData.calcDouble(clazz, array.get(1), currentSource, fields, refs), y = JSONMetaData.calcDouble(clazz, array.get(2), currentSource, fields, refs);
                yield (g2, _, _) -> {
                    AffineTransform at = new AffineTransform(g2.getTransform());
                    at.translate(x.getAsDouble(), y.getAsDouble());
                    g2.setTransform(at);
                };
            }
            case "transform=rotate" -> {
                DoubleSupplier rotation = JSONMetaData.calcDouble(clazz, array.get(1), currentSource, fields, refs),
                    x = JSONMetaData.calcDouble(clazz, array.get(2), currentSource, fields, refs),
                    y = JSONMetaData.calcDouble(clazz, array.get(3), currentSource, fields, refs);
                yield (g2, offX, offY) -> {
                    AffineTransform at = new AffineTransform(g2.getTransform());
                    at.rotate(rotation.getAsDouble(), x.getAsDouble() + offX, y.getAsDouble() + offY);
                    g2.setTransform(at);
                };
            }
            case "transform=shear" -> {
                DoubleSupplier x = JSONMetaData.calcDouble(clazz, array.get(1), currentSource, fields, refs), y = JSONMetaData.calcDouble(clazz, array.get(2), currentSource, fields, refs);
                yield (g2, _, _) -> {
                    AffineTransform at = new AffineTransform(g2.getTransform());
                    at.shear(x.getAsDouble(), y.getAsDouble());
                    g2.setTransform(at);
                };
            }
            case "resetTransform" -> (g2, _, _) -> g2.setTransform(new AffineTransform());
            case "for" -> {
                JSONArray ja = array.getJSONArray(1);
                IntSupplier j = JSONMetaData.calcInt(clazz, ja.get(0), currentSource, fields, refs);
                BooleanSupplier condition = condition(clazz, ja.getJSONArray(1), DrawableBuilder.currentSource, DrawableBuilder.fields, refs);
                CodeBlock newBlock = newBlock(() -> true, clazz, array.getJSONArray(2));
                yield (g2, offX, offY) -> {
                    Value value = new Value(0);
                    fields.put("i", value);
                    for(int i = j.getAsInt(); condition.getAsBoolean(); i++, value.value = i)
                        newBlock.accept(g2, offX, offY);
                };
            }
            default -> (_, _, _) -> {};
        };
    }

    @SuppressWarnings("unused")
    public static class Tags {
        public static final String CALC_INT = "@calc_int:", CALC_DOUBLE = "@calc_double:",
            CONDITION = "@condition:", FIELD = "@field:", CLASS = "#default:", CLASS_1 = "@class:", ENUM = "enum:";
    }

    record CodeBlock(BooleanSupplier booleanSupplier, Vector<DrawableConsumer> consumers) implements DrawableConsumer {
        @Override
        public void accept(Graphics2D g2, int offX, int offY) {
            if(booleanSupplier.getAsBoolean())
                consumers.forEach(g -> g.accept(g2, offX, offY));
        }
    }

    @Getter
    public static sealed class Value permits GettableValue {
        Object value;

        public Value(Object value) {
            this.value = value;
        }

        public final Object getValueTrue() {
            return value;
        }

        public Class<?> getClassOfValue() {
            return value.getClass();
        }
    }

    public static final class GettableValue extends Value {
        Value[] args;
        Object source;
        int arrayIndex;

        public GettableValue(Object value, Object source) {
            this(value, source, 0);
        }

        public GettableValue(Object value, Object source, int arrayIndex) {
            this(value, source, arrayIndex, new Value[0]);
        }

        @SuppressWarnings("unused")
        public GettableValue(Object value, Object source, Value[] args) {
            this(value, source, 0, args);
        }

        public GettableValue(Object value, Object source, int arrayIndex, Value[] args) {
            super(value);
            this.source = source;
            this.arrayIndex = arrayIndex;
            this.args = args;
        }

        @Override
        public Object getValue() {
            return switch(value) {
                case null -> null;
                case IntSupplier intSupplier -> intSupplier.getAsInt();
                case DoubleSupplier doubleSupplier -> doubleSupplier.getAsDouble();
                case LongSupplier longSupplier -> longSupplier.getAsLong();
                case BooleanSupplier booleanSupplier -> booleanSupplier.getAsBoolean();
                case Method method -> getMethodValue(method);
                case Field field -> getFieldValue(field);
                default -> getValueOfValue();
            };
        }

        public Object getValueOfValue() {
            Object obj = value instanceof Value valueOfValue ? valueOfValue.getValue() : value;
            return obj != null && obj.getClass().isArray() ? Array.get(obj, arrayIndex) : obj;
        }

        public Object getMethodValue(Method method) {
            Objects.requireNonNull(source);
            try {
                if(args.length == 0) {
                    Object value = source instanceof Value v ? v.getValue() : source;
                    return method.invoke(Objects.requireNonNull(value));
                } else {
                    Object[] argValues = new Object[args.length];
                    for(int j = 0; j < argValues.length; j++)
                        argValues[j] = args[j].getValue();
                    return method.invoke(source instanceof Value v ? v.getValue() : source, argValues);
                }
            } catch(Exception e) {
                e.printStackTrace();
                throw new NullPointerException("the game can't invoke the method " + method.getName());
            }
        }

        public Object getFieldValue(Field field) {
            try {
                return field.get(currentSource);
            } catch(IllegalAccessException e) {
                throw new NullPointerException("the game can't get the value of the field " + field.getName());
            }
        }

        @Override
        public Class<?> getClassOfValue() {
            return switch(value) {
                case null -> null;
                case IntSupplier ignored -> int.class;
                case DoubleSupplier ignored -> double.class;
                case LongSupplier ignored -> long.class;
                case BooleanSupplier ignored -> boolean.class;
                case Field field -> field.getType();
                case Method method -> method.getReturnType();
                default -> Objects.requireNonNull(getValue()).getClass();
            };
        }
    }
}
