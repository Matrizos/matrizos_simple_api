package com.matrizos.matrizos_simple_api.ui;

import com.matrizos.matrizos_simple_api.data.DrawableBuilder;
import com.matrizos.matrizos_simple_api.data.JSONMetaData;
import com.matrizos.matrizos_simple_api.data.UtilMethods;
import lombok.Getter;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

public class FrameUI {
    @Getter private final ColorModelUI colorModelUI;
    @Getter private ButtonUI currentButton; // shortcut for
    @Getter private final ArrayList<ButtonUI> buttons = new ArrayList<>();
    private final JFrame frameUI;
    @Getter private final ScheduledExecutorService pool;
    JSONMetaData jo;

    public FrameUI(JSONMetaData jo) {
        this(
            jo.optString("title", "Window"),
            jo.optInt("width", 100),
            jo.optInt("height", 100),
            jo.getJSONObject("paint")
        );
        if(jo.has("buttons")) {
            for(Object o : jo.getJSONArray("buttons")) {
                JSONObject obj = ((JSONObject)o);
                buttons.add(new ButtonUI(this, obj.getJSONObject("paint"),
                    obj.get("x"), obj.get("y"), obj.get("width"), obj.get("height"),
                    ignored -> System.out.println("Pressed"), ignored -> System.out.println("Released")));
            }
        }
        this.jo = jo;
    }

    public FrameUI(String title, int width, int height, JSONObject paint) {
        frameUI = new JFrame(title);
        pool = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 100);
//        pool.schedule(() -> {}, 100, TimeUnit.MILLISECONDS);
        frameUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frameUI.setResizable(true);
        Container c = getContainer(width, height);
        c.addMouseListener(new MouseListener() {
            @Override public void mouseClicked(MouseEvent e) {}
            @Override public void mousePressed(MouseEvent e) {buttons.forEach(b -> {if(b.getBounds().contains(e.getX(), e.getY())) b.mousePressed(e);});}
            @Override public void mouseReleased(MouseEvent e) {buttons.forEach(b -> {if(b.getBounds().contains(e.getX(), e.getY())) b.mouseReleased(e);});}
            @Override public void mouseEntered(MouseEvent e) {}
            @Override public void mouseExited(MouseEvent e) {}
        });
        c.addMouseMotionListener(new MouseMotionListener() {
            @Override public void mouseDragged(MouseEvent e) {}
            @Override public void mouseMoved(MouseEvent e) {buttons.forEach(b -> {if(b.getBounds().contains(e.getX(), e.getY())) b.mouseMoved(e);});}
        });
        frameUI.addKeyListener(new KeyListener() {
            @Override public void keyTyped(KeyEvent e) {}
            @Override public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_U) {
                    jo.updateFromDisk();
                    repaint();
                }
            }
            @Override public void keyReleased(KeyEvent e) {}
        });
        frameUI.add(c);
        frameUI.pack();
        Rectangle2D fullBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frameUI.setLocation(
            (int)(fullBounds.getCenterX() - (frameUI.getContentPane().getPreferredSize().width / 2f)),
            (int)(fullBounds.getCenterY() - (frameUI.getContentPane().getPreferredSize().height / 2f))
        );
        colorModelUI = UtilMethods.getColorModelUI(paint, this);
        BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D)img.getGraphics();
        g.setColor(new Color(0x0719D3));
        g.fill(new Ellipse2D.Float(0, 0, img.getWidth(), img.getHeight()));
        g.setColor(new Color(64, 250, 24, 255));
        g.fill(new Ellipse2D.Float(img.getWidth() * 0.2f, img.getHeight() * 0.2f, img.getWidth() * 0.6f, img.getHeight() * 0.6f));
        frameUI.setIconImage(img);
        frameUI.setVisible(true);
    }

    private Container getContainer(int width, int height) {
        Container c = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                if(!getBounds().equals(colorModelUI.getBounds())) {
                    colorModelUI.update(getBounds());
                    buttons.forEach(ButtonUI::updateBounds);
                }
                ((Graphics2D)g).setPaint(colorModelUI.getPaint());
                ((Graphics2D)g).fill(getBounds());
                buttons.forEach(b -> {
                    ((Graphics2D)g).setPaint(b.getColorModelUI().getPaint());
                    ((Graphics2D)g).fill(b.getBounds());
                });
            }
        };
        c.setPreferredSize(new Dimension(width, height));
        return c;
    }

    public Rectangle getBounds() {
        return frameUI.getBounds();
    }

    public void repaint() {
        frameUI.repaint();
    }
}
