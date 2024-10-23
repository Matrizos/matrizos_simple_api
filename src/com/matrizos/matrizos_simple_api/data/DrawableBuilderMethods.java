package com.matrizos.matrizos_simple_api.data;

import org.json.JSONArray;

import java.lang.reflect.Method;
import java.util.function.DoubleSupplier;

public class DrawableBuilderMethods {
    public static DoubleSupplier mathCalc(Class<?> clazz, JSONArray array) {
        String first = array.getString(0);
        boolean hasArgs = first.contains("(");
        String name = first.substring(5, hasArgs ? first.indexOf("(") : first.length());
        String stringArgs = "";
        if(hasArgs)
            stringArgs = DrawableBuilder.enterParenteses(0, first.substring(first.indexOf("(")), false);
        String[] splitValues = stringArgs.split(",");
        Class<?>[] classes = hasArgs ? JSONMetaData.getArgsClasses(splitValues) : new Class[splitValues.length];
        Object object = array.get(1);
        DrawableBuilder.Value[] args = object instanceof JSONArray ja ?
            JSONMetaData.getArgs(clazz, ja, classes, DrawableBuilder.currentSource, DrawableBuilder.fields, DrawableBuilder.refs) :
            new DrawableBuilder.Value[] { JSONMetaData.getField(clazz, object, DrawableBuilder.currentSource, DrawableBuilder.fields, DrawableBuilder.refs) };
        Method method;
        try {
            method = Math.class.getDeclaredMethod(name, classes);
        } catch(NoSuchMethodException e) {
            throw new NullPointerException("the can't get the method " + name);
        }
        Method finalMethod = method;
        return () -> {
            try {
                Object[] argValues = new Object[args.length];
                for(int j = 0; j < argValues.length; j++)
                    argValues[j] = args[j].getValue();
                return (double)finalMethod.invoke(null, argValues);
            } catch(Exception e) {
                System.err.println(e.getMessage());
                return 0;
            }
        };
    }

    public static Object getObjectFromSource(Class<?> clazz, Object object, Object source) {
        return switch(object) {
            case String string -> {
                if(string.startsWith(DrawableBuilder.Tags.FIELD))
                    yield JSONMetaData.getField(clazz, object, DrawableBuilder.currentSource, DrawableBuilder.fields, DrawableBuilder.refs);
                else if(string.startsWith(DrawableBuilder.Tags.CLASS))
                    yield JSONMetaData.clazz(string);
                yield string;
            }
            case DrawableBuilder.Value v -> v.getValue();
            default -> object;
        };
    }

    public static Class<?> byType(String string) {
        return switch(string) {
            case "int", "int.class" -> int.class;
            case "float", "float.class" -> float.class;
            case "double", "double.class" -> double.class;
            case "long", "long.class" -> long.class;
            case "char", "char.class" -> char.class;
            case "short", "short.class" -> short.class;
            case "byte", "byte.class" -> byte.class;
            case "boolean", "boolean.class" -> boolean.class;
            default -> {
                try {
                    yield DrawableBuilderMethods.class.getClassLoader().loadClass(string);
                } catch(ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
