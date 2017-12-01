package org.freedesktop.gstreamer.examples;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Message;
import org.freedesktop.gstreamer.MessageType;
import org.freedesktop.gstreamer.Structure;
import org.freedesktop.gstreamer.elements.AppSink;
import org.freedesktop.gstreamer.elements.PlayBin;
import org.freedesktop.gstreamer.lowlevel.GValueAPI;
import org.freedesktop.gstreamer.lowlevel.GstStructureAPI;

/**
 *
 * An example of playing back a video in multiple sinks - one full size in a
 * native window, one scaled in a Swing window. Use the File... button to choose
 * a local video file.
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class AudioBarExample {

    /**
     * @param args the command line arguments
     */
    private static PlayBin playbin;

    public static void main(String[] args) {

        Gst.init();
        EventQueue.invokeLater(() -> {
            Bin videoBin = Bin.launch(
                    "videoconvert ! videoscale ! capsfilter caps=video/x-raw,width=640,height=480 ! appsink name=appsink",
                    true);

            SimpleVideoComponent vc = new SimpleVideoComponent((AppSink) videoBin.getElementByName("appsink"));

            Bin audioBin = Bin.launch(
                    "level interval=100000000 ! queue ! autoaudiosink",
//                    "level interval=100000000 ! autoaudiosink",
                    true);

            playbin = new PlayBin("playbin");
            playbin.setVideoSink(videoBin);
            playbin.setAudioSink(audioBin);

            // set PlayBin flags to not use internal video scaling otherwise don't get native resolution output
            // no mapping for GstPlayFlags in bindings yet!
            int flags = (int) playbin.get("flags");
            flags |= (1 << 6);
            playbin.set("flags", flags);

            JFrame window = new JFrame("Audio Bars");
            window.add(vc);
            vc.setPreferredSize(new Dimension(640, 480));
            Box buttons = Box.createHorizontalBox();

            AudioBars audioBars = new AudioBars();
            buttons.add(audioBars);

            JButton fileButton = new JButton("File...");
            buttons.add(fileButton);
            window.add(buttons, BorderLayout.SOUTH);
            window.pack();
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            fileButton.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(window);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    playbin.stop();
                    playbin.setURI(fileChooser.getSelectedFile().toURI());

                    playbin.getBus().connect(new Bus.MESSAGE() {

                        @Override
                        public void busMessage(Bus arg0, Message message) {
                            Structure struct = message.getStructure();
                            // TODO read state changes from the bus

                            if (message.getType() == MessageType.ELEMENT
                                    && message.getSource().getName().startsWith("level")) {
                                // We can get either rms or peak
                                List<Double> channelLevels = getLevel("peak", struct);
                                long timeDelay = getTimeOffset(struct);
                                Gst.getScheduledExecutorService().schedule(
                                        () -> EventQueue.invokeLater(() -> audioBars.setCurrentLevels(channelLevels)),
                                        timeDelay, TimeUnit.NANOSECONDS);
                            }
                        }

                        private List<Double> getLevel(String type, Structure struct) {
                            GValueAPI.GValue value = GstStructureAPI.GSTSTRUCTURE_API.gst_structure_get_value(struct, type);
                            GValueAPI.GValueArray array = new GValueAPI.GValueArray(GValueAPI.GVALUE_API.g_value_get_boxed(value));
                            int count = array.getNValues();
                            List<Double> levelsList = new ArrayList<>(count);
                            for (int i = 0; i < count; i++) {
                                double val = (double) array.getValue(i);
                                val = Math.pow(10, val / 20);
                                levelsList.add(val);
                            }
                            array.free();
                            return levelsList;
                        }
                        
                        private long getTimeOffset(Structure struct) {
                            long actualTime = playbin.getClock().getTime().toNanos() -
                                    playbin.getBaseTime().toNanos();
                            long runningTime = ((Long) struct.getValue("running-time"));
                            long duration = ((Long) struct.getValue("duration"));
                            long messageTime = runningTime + (duration / 2);
                            return messageTime - actualTime;
                        }
                    });
                }

                playbin.play();
            });

            window.setVisible(true);
        });
    }

}
