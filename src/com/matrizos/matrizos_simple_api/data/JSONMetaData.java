package com.matrizos.matrizos_simple_api.data;

import com.matrizos.matrizos_simple_api.ui.ButtonUI;
import com.matrizos.matrizos_simple_api.ui.FrameUI;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.awt.*;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Objects;
import java.util.Vector;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

import static com.matrizos.matrizos_simple_api.data.DrawableBuilder.*;

@Getter
public class JSONMetaData extends JSONObject {
    private final String path;
    private Runtime.Version version;
    private String type;
    @Setter private Object source;

    public static boolean isMetaData(JSONObject o) {
        return o instanceof JSONMetaData || o.has("version") || o.has("type");
    }

    public JSONMetaData(JSONTokener x) throws JSONException {
        this(x, null);
    }

    public JSONMetaData(JSONTokener x, String path) throws JSONException {
        this(new JSONObject(x), path);
        version();
    }

    public JSONMetaData(JSONObject jo) {
        this(jo, null);
    }

    public JSONMetaData(JSONObject jo, String path) {
        super(jo.toMap());
        this.path = path;
        version();
    }

    public JSONMetaData(String source) throws JSONException {
        super(source);
        path = null;
        version();
    }

    @SneakyThrows
    public void updateFromDisk() {
        System.out.println("path = " + path);
        if(path == null)
            return;
        clear();
        JSONObject jo = new JSONObject(new JSONTokener(new FileInputStream(path)));
        jo.toMap().forEach(this::put);
        if(Objects.requireNonNull(source) instanceof FrameUI frameUI) {
            frameUI.getColorModelUI().update(jo.getJSONObject("paint"), frameUI);
            if(jo.has("buttons")) {
                JSONArray buttons = jo.getJSONArray("buttons");
                for(int i = 0; i < buttons.length(); i++) {
                    JSONObject obj = buttons.getJSONObject(i);
                    if(i < frameUI.getButtons().size())
                        frameUI.getButtons().get(i).set(obj);
                    else
                        frameUI.getButtons().add(new ButtonUI(frameUI, obj.getJSONObject("paint"),
                        obj.get("x"), obj.get("y"), obj.get("width"), obj.get("height"),
                        ignored -> System.out.println("Pressed"), ignored -> System.out.println("Released")));
                }
            }
        }
    }

    public static Value getField(Class<?> clazz, Object object, Object source, HashMap<String, Value> fields, HashMap<String, Class<?>> refs) {
        Class<?> currentClass = clazz;
        Member current = null;
        int arrayIndex = 0;
        Value[] args = new Value[0];
        String string = "", link = "";

        if(object instanceof JSONArray array) {
            string = array.getString(0);
            args = getArgs(clazz, array, enterParenteses(0, string.substring(string.indexOf("(")), false), source, fields, refs);
        } else if(object instanceof String s)
            string = s;

        if(string.startsWith(Tags.FIELD))
            string = string.substring(Tags.FIELD.length());
        else if(string.startsWith(Tags.CLASS_1)) {
            string = string.substring(Tags.FIELD.length());
            return new Value(clazz(string));
        }
        if(fields.containsKey(string))
            return fields.get(string);

        boolean first = true;
        String[] split = string.split("\\.");
        for(String splitValue : split)
            breakable:{
                if(fields.containsKey(splitValue)) {
                    Value field = fields.get(splitValue);
                    currentClass = field.getClassOfValue();
                    source = field;
                    link += (first ? "" : ".") + splitValue;
                    if(first)
                        first = false;
                    break breakable;
                } else if(!first && refs.containsKey(link))
                    currentClass = refs.get(link);

                boolean getter = splitValue.endsWith("()") || splitValue.endsWith("(") || splitValue.endsWith(")");
                if(getter)
                    splitValue = splitValue.substring(0, splitValue.indexOf('('));

                try {
                    if("getClass".equals(splitValue))
                        throw new IllegalArgumentException("getClass() can't be used");
                    link += (first ? "" : ".") + splitValue;
                    if(first)
                        first = false;
                    else
                        source = new GettableValue(current, source, arrayIndex, args);
                    current = getter ? currentClass.getMethod(splitValue) : currentClass.getField(splitValue);
                    currentClass = getter ? ((Method)current).getReturnType() : ((Field)current).getType();
                } catch(NoSuchMethodException e) {
                    throw new NullPointerException("the method " + splitValue + "[" + string + "] has not found in " + currentClass.getName());
                } catch(NoSuchFieldException e) {
                    throw new NullPointerException("the field " + splitValue + "[" + string + "] has not found in " + currentClass.getName());
                }
            }

        Value value = new GettableValue(current, source, arrayIndex, args);
        fields.put(string, value);
        return value;
    }

    public static Class<?> clazz(String string) {
        if(string.startsWith(Tags.CLASS))
            string = string.substring(Tags.CLASS.length());

        try {
            return DrawableBuilder.class.getClassLoader().loadClass(string);
        } catch(ClassNotFoundException e) {
            System.err.println(string);
            throw new RuntimeException(e);
        }
    }

    public static IntSupplier calcInt(Class<?> clazz, Object object, Object source, HashMap<String, Value> fields, HashMap<String, Class<?>> refs) {
        String currentString = "";
        IntSupplier intSupplier;
        if(object instanceof JSONArray array) {
            Object firstObj = array.get(0);
            if(firstObj instanceof String string && string.startsWith("Math.")) {
                DoubleSupplier mathCalc = DrawableBuilderMethods.mathCalc(clazz, array);
                return () -> (int)mathCalc.getAsDouble();
            }
            intSupplier = intValueOf(clazz, firstObj, source, fields, refs);
            for(int i = 1; i < array.length(); i++) {
                Object obj = array.get(i);
                boolean different = false;
                IntSupplier finalIntSupplier = intSupplier;
                if(Objects.requireNonNull(obj) instanceof String string) {
                    switch(string) {
                        case "+", "-", "*", "/", "%", "&", "|", "^", ">>", "<<" -> currentString = string;
                        default -> different = true;
                    }
                } else {
                    different = true;
                }
                if(different) {
                    IntSupplier calc = intValueOf(clazz, array.get(i), source, fields, refs);
                    intSupplier = switch(currentString) {
                        case "+" -> () -> finalIntSupplier.getAsInt() + calc.getAsInt();
                        case "-" -> () -> finalIntSupplier.getAsInt() - calc.getAsInt();
                        case "*" -> () -> finalIntSupplier.getAsInt() * calc.getAsInt();
                        case "/" -> () -> finalIntSupplier.getAsInt() / calc.getAsInt();
                        case "%" -> () -> finalIntSupplier.getAsInt() % calc.getAsInt();
                        case "&" -> () -> finalIntSupplier.getAsInt() & calc.getAsInt();
                        case "|" -> () -> finalIntSupplier.getAsInt() | calc.getAsInt();
                        case "^" -> () -> finalIntSupplier.getAsInt() ^ calc.getAsInt();
                        case ">>" -> () -> finalIntSupplier.getAsInt() >> calc.getAsInt();
                        case "<<" -> () -> finalIntSupplier.getAsInt() << calc.getAsInt();
                        default -> throw new IllegalArgumentException("invalid type: " + currentString);
                    };
                }
            }
        } else
            return intValueOf(clazz, object, source, fields, refs);
        return intSupplier;
    }

    public static DoubleSupplier calcDouble(Class<?> clazz, Object object, Object source, HashMap<String, Value> fields, HashMap<String, Class<?>> refs) {
        String currentString = "";
        DoubleSupplier doubleSupplier;
        if(object instanceof JSONArray array) {
            Object firstObj = array.get(0);
            if(firstObj instanceof String string && string.startsWith("Math."))
                return DrawableBuilderMethods.mathCalc(clazz, array);
            doubleSupplier = doubleValueOf(clazz, firstObj, source, fields, refs);
            for(int i = 1; i < array.length(); i++) {
                Object obj = array.get(i);
                boolean different = false;
                DoubleSupplier finalDoubleSupplier = doubleSupplier;
                if(Objects.requireNonNull(obj) instanceof String string) {
                    switch(string) {
                        case "+", "-", "*", "/", "%" -> currentString = string;
                        default -> different = true;
                    }
                } else {
                    different = true;
                }
                if(different) {
                    DoubleSupplier calc = doubleValueOf(clazz, array.get(i), source, fields, refs);
                    doubleSupplier = switch(currentString) {
                        case "+" -> () -> finalDoubleSupplier.getAsDouble() + calc.getAsDouble();
                        case "-" -> () -> finalDoubleSupplier.getAsDouble() - calc.getAsDouble();
                        case "*" -> () -> finalDoubleSupplier.getAsDouble() * calc.getAsDouble();
                        case "/" -> () -> finalDoubleSupplier.getAsDouble() / calc.getAsDouble();
                        case "%" -> () -> finalDoubleSupplier.getAsDouble() % calc.getAsDouble();
                        default -> throw new IllegalArgumentException("invalid type: " + currentString);
                    };
                }
            }
        } else
            return doubleValueOf(clazz, object, source, fields, refs);
        return doubleSupplier;
    }

    public static IntSupplier intValueOf(Class<?> clazz, Object object, Object source, HashMap<String, Value> fields, HashMap<String, Class<?>> refs) {
//        final IntSupplier[] intSupplier = {null};
        return switch(object) {
            case Number n -> n::intValue;
            case Boolean b -> b ? () -> 1 : () -> 0;
            case Value value -> intValueOf(clazz, value.getValue(), source, fields, refs);
            case JSONArray ja -> {
                Object obj = ja.get(0);
                if(obj instanceof String string && string.startsWith("Math.")) {
                    final DoubleSupplier mathSupplier = DrawableBuilderMethods.mathCalc(clazz, ja);
                    yield () -> (int)mathSupplier.getAsDouble();
                }
                yield calcInt(clazz, ja, source, fields, refs);
            }
            case String string -> {
                if(string.startsWith(Tags.FIELD))
                    yield intValueOf(clazz, getField(clazz, string.substring(Tags.FIELD.length()), source, fields, refs), source, fields, refs);
                throw new IllegalArgumentException("\"" + string + "\" is not an valid argument, try use \"@field:\" for use fields");
            }
            default -> throw new IllegalArgumentException("invalid type: " + object);
        };
    }

    public static DoubleSupplier doubleValueOf(Class<?> clazz, Object object, Object source, HashMap<String, Value> fields, HashMap<String, Class<?>> refs) {
//        final DoubleSupplier[] doubleSupplier = {null};
        return switch(object) {
            case Number n -> n::doubleValue;
            case Boolean b -> b ? () -> 1 : () -> 0;
            case Value value -> doubleValueOf(clazz, value.getValue(), source, fields, refs);
            case JSONArray ja -> {
                Object obj = ja.get(0);
                if(obj instanceof String string && string.startsWith("Math."))
                    yield DrawableBuilderMethods.mathCalc(clazz, ja);
                yield calcDouble(clazz, ja, source, fields, refs);
            }
            case String string -> {
                if(string.startsWith(Tags.FIELD))
                    yield doubleValueOf(clazz, getField(clazz, string.substring(Tags.FIELD.length()), source, fields, refs), source, fields, refs);
                throw new IllegalArgumentException("\"" + string + "\" is not an valid argument, try use \"@field:\" for use fields");
            }
            default -> throw new IllegalArgumentException("invalid argument: " + object);
        };
    }

    public static Value[] getArgs(Class<?> clazz, JSONArray array, String stringArgs, Object source, HashMap<String, Value> fields, HashMap<String, Class<?>> refs) {
        return stringArgs.isEmpty() ? new Value[0] : getArgs(clazz, array, stringArgs.replace(" ", "").split(","), source, fields, refs);
    }

    public static Value[] getArgs(Class<?> clazz, JSONArray array, String[] types, Object source, HashMap<String, Value> fields, HashMap<String, Class<?>> refs) {
        return getArgs(clazz, array, getArgsClasses(types), source, fields, refs);
    }

    public static Value[] getArgs(Class<?> clazz, JSONArray array, Class<?>[] types, Object source, HashMap<String, Value> fields, HashMap<String, Class<?>> refs) {
        if(array == null || array.isEmpty())
            return new Value[0];
        int length = Math.min(types.length, array.length());
        Vector<Value> args = new Vector<>(length);
        for(int i = 0; i < length; i++) {
            Object obj = types.length == 1 ? array : array.get(i);
            if(types[i] == int.class)
                args.add(new GettableValue(calcInt(clazz, obj, source, fields, refs), source));
            else if(types[i] == double.class)
                args.add(new GettableValue(calcDouble(clazz, obj, source, fields, refs), source));
            else if(types[i] == boolean.class)
                args.add(new GettableValue(condition(clazz, (JSONArray)obj, source, fields, refs), source));
            else if(types[i] == String.class && obj instanceof String value)
                args.add(getField(clazz, value, source, fields, refs));
            else {
                args.add(new GettableValue(obj, source));
            }
        }
        return args.toArray(new Value[0]);
    }

    public static Class<?>[] getArgsClasses(String[] splitValues) {
        Class<?>[] classes = new Class[splitValues.length];
        int i = 0;
        for(String string : splitValues) {
            classes[i] = DrawableBuilderMethods.byType(string);
            i++;
        }
        return classes;
    }

    private void version() {
        type = has("type") ? getString("type") : "?";
        version = Runtime.Version.parse(has("version") ?
            switch(get("version")) {
                case String string -> string;
                case Number n -> String.valueOf(n.intValue());
                case JSONArray array -> {
                    String string = "";
                    for(Object o : array)
                        string += o;
                    yield string;
                }
                case null, default -> "1";
            } : "1");
    }

    public static BooleanSupplier condition(Class<?> clazz, JSONArray array, Object source, HashMap<String, Value> fields, HashMap<String, Class<?>> refs) {
        BooleanSupplier booleanSupplier = () -> true;
        String currentString = "";
        Object firstObj = array.get(0);
        Value backObj = new Value(firstObj), nextObj = null;

        for(int i = 1; i < array.length(); i++) {
            Object obj = array.get(i);
            boolean differentValue = false;
            BooleanSupplier finalBooleanSupplier = booleanSupplier;
            switch(obj) {
                case Boolean b -> booleanSupplier = () -> b;
                case JSONArray ja -> {
                    BooleanSupplier newSupplier = condition(clazz, ja, source, fields, refs);
                    booleanSupplier = () -> finalBooleanSupplier.getAsBoolean() && newSupplier.getAsBoolean();
                }
                case String string -> {
                    Value finalBackObj = backObj;
                    switch(string) {
                        case "&&", "||", "==", "!=", "!", "<", ">", "<=", ">=", "instanceof" -> currentString = string;
                        default -> {
                            if(string.startsWith(Tags.FIELD)) {
                                if(backObj == null) {
                                    backObj = getField(clazz, string, source, fields, refs);
                                } else {
                                    nextObj = getField(clazz, string, source, fields, refs);
                                    differentValue = true;
                                }
                            } else if("instanceof".equals(currentString)) {
                                Class<?> clazz0 = clazz(string);
                                BooleanSupplier newBooleanSupplier = () -> clazz0.isInstance(finalBackObj.getValue());
                                booleanSupplier = () -> finalBooleanSupplier.getAsBoolean() && newBooleanSupplier.getAsBoolean();
                                backObj = null;
                                nextObj = null;
                            }
                        }
                    }
                }
                case null, default -> {
                    Object arrayObj = array.get(i);
                    if(backObj == null) {
                        backObj = new GettableValue(DrawableBuilderMethods.getObjectFromSource(clazz, arrayObj, arrayObj), null);
                        if(backObj.getValueTrue() instanceof Boolean b) {
                            if("&&".equals(currentString))
                                booleanSupplier = () -> finalBooleanSupplier.getAsBoolean() && b;
                            else if("||".equals(currentString))
                                booleanSupplier = () -> finalBooleanSupplier.getAsBoolean() || b;
                            backObj = null;
                        }
                    } else {
                        nextObj = new GettableValue(DrawableBuilderMethods.getObjectFromSource(clazz, arrayObj, arrayObj), null);
                        differentValue = true;
                    }
                }
            }
            if(differentValue) {
                Value finalBackObj = backObj, finalNextObj = nextObj;
                switch(currentString) {
                    case "&&", "||", "==", "!=", "!" -> {
                        switch(currentString) {
                            case "&&" ->
                                booleanSupplier = () -> finalBooleanSupplier.getAsBoolean() && (boolean)finalNextObj.getValue();
                            case "||" ->
                                booleanSupplier = () -> finalBooleanSupplier.getAsBoolean() || (boolean)finalNextObj.getValue();
                            case "==", "!=" -> {
                                if(obj == JSONObject.NULL) {
                                    booleanSupplier = Objects.equals(currentString, "==") ?
                                        () -> finalBooleanSupplier.getAsBoolean() && finalBackObj.getValue() == null :
                                        () -> finalBooleanSupplier.getAsBoolean() && finalBackObj.getValue() != null;
                                } else {
                                    booleanSupplier = Objects.equals(currentString, "==") ?
                                        () -> finalBooleanSupplier.getAsBoolean() && finalBackObj.getValue() == finalNextObj.getValue() :
                                        () -> finalBooleanSupplier.getAsBoolean() && finalBackObj.getValue() != finalNextObj.getValue();
                                }
                            }
                            case "!" ->
                                booleanSupplier = () -> finalBooleanSupplier.getAsBoolean() && !(boolean)finalNextObj.getValue();
                            default -> throw new IllegalStateException("Unexpected value: " + currentString);
                        }
                    }
                    case "<", ">", "<=", ">=" -> {
                        DoubleSupplier calc1 = calcDouble(clazz, backObj, source, fields, refs), calc2 = calcDouble(clazz, nextObj, source, fields, refs);
                        BooleanSupplier newBooleanSupplier = switch(currentString) {
                            case "<" -> () -> calc1.getAsDouble() < calc2.getAsDouble();
                            case ">" -> () -> calc1.getAsDouble() > calc2.getAsDouble();
                            case "<=" -> () -> calc1.getAsDouble() <= calc2.getAsDouble();
                            case ">=" -> () -> calc1.getAsDouble() >= calc2.getAsDouble();
                            case null, default ->
                                throw new IllegalArgumentException("not a statement: " + currentString);
                        };
                        booleanSupplier = () -> finalBooleanSupplier.getAsBoolean() && newBooleanSupplier.getAsBoolean();
                    }
                }
                backObj = null;
                nextObj = null;
            }
        }
        return booleanSupplier;
    }
}
