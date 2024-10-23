package com.matrizos.matrizos_simple_api;

import com.matrizos.matrizos_simple_api.data.JSONMetaData;
import com.matrizos.matrizos_simple_api.ui.FrameUI;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        String path = "/home/mathe/test/test.json";
        JSONMetaData data = new JSONMetaData(new JSONTokener(new FileInputStream(path)), path);
        FrameUI frameUI = new FrameUI(data);
        data.setSource(frameUI);
    }
}
