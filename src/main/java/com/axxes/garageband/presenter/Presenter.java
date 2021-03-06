package com.axxes.garageband.presenter;

import com.axxes.garageband.Audio.AudioDevice;
import com.axxes.garageband.Audio.effects.*;
import com.axxes.garageband.model.instrument.*;
import com.axxes.garageband.model.loop.Drumloop;
import com.axxes.garageband.model.measures.Beat;
import com.axxes.garageband.model.measures.Measure;
import com.axxes.garageband.util.MusicXmlParser;
import com.axxes.garageband.util.MusicXmlWriter;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Border;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.converter.NumberStringConverter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Controller
public class Presenter {

    @FXML
    private GridPane grid;
    private int gridRow = 1;

    @FXML
    AnchorPane imageKick;
    @FXML
    AnchorPane imageSnare;
    @FXML
    AnchorPane imageHihat;
    @FXML
    AnchorPane imageCymbal;
    @FXML
    TextField bpmTextField;
    @FXML
    Pane root;

    private Rectangle highLighter;
    private int highlighterPosition;

    private Slider slider;
    private boolean programmaticSliderChange;

    private final MusicXmlParser parser;
    private final MusicXmlWriter writer;

    private Timeline loopTimeline;
    private IntegerProperty bpm;
    private int beats;
    private int beatsPerMeasure;

    private final AudioDevice audioDevice;

    private final Drumloop drumloop;
    private final Kick kick;
    private final Cymbal cymbal;
    private final HiHat hiHat;
    private final Snare snare;

    private final Echo echoEffect;
    private final NoEffect noEffect;
    private final Reverb reverbEffect;
    private final RingModulator ringModulatorEffect;
    private final Flanger flangerEffect;
    private final Distortion distortionEffect;

    @Autowired
    public Presenter(Drumloop drumloop, MusicXmlParser parser, MusicXmlWriter writer, AudioDevice audioDevice, Kick kick, Cymbal cymbal, HiHat hiHat, Flanger flangerEffect, Snare snare, Echo echoEffect, NoEffect noEffect, RingModulator ringModulatorEffect, Reverb reverbEffect, Distortion distortionEffect) {
        this.parser = parser;
        this.writer = writer;
        this.audioDevice = audioDevice;
        this.drumloop = drumloop;
        this.kick = kick;
        this.cymbal = cymbal;
        this.hiHat = hiHat;
        this.snare = snare;
        this.flangerEffect = flangerEffect;
        this.echoEffect = echoEffect;
        this.noEffect = noEffect;
        this.ringModulatorEffect = ringModulatorEffect;
        this.reverbEffect = reverbEffect;
        this.distortionEffect = distortionEffect;
    }

    private void createLoop() {
        if (this.loopTimeline != null) {
            this.loopTimeline.stop();
        }
        int timeBetweenBeats = 60000 / this.bpm.get();

        this.loopTimeline = new Timeline(new KeyFrame(
                Duration.millis(timeBetweenBeats),
                ae -> {
                    Logger.getLogger(Presenter.class).info("Drumloop step.");
                    stepHighlighterAndSlider();
                    this.drumloop.step();
                }));
        this.loopTimeline.setCycleCount(Animation.INDEFINITE);
    }

    @FXML
    protected void initialize() {
        this.programmaticSliderChange = false;
        this.beats = drumloop.getMeasures().stream().map(Measure::getBeats).mapToInt(Collection::size).sum();
        this.beatsPerMeasure = drumloop.getBeatsPerMeasure();
        this.bpm = new SimpleIntegerProperty();
        this.bpm.bindBidirectional(drumloop.getBpm());
        Bindings.bindBidirectional(this.bpmTextField.textProperty(), this.bpm, new NumberStringConverter());
        createBaseGrid();
        createHighlighter();
        createSlider();
        createLoop();
    }


    private void createSlider(){
        this.slider = new Slider(0, this.beats - 1, this.highlighterPosition);
        this.slider.setShowTickMarks(true);
        this.slider.setBlockIncrement(1);
        this.slider.setSnapToTicks(true);
        this.slider.setMajorTickUnit(1);
        this.slider.setMinorTickCount(0);
        this.slider.setLayoutX(105);
        this.slider.setLayoutY(40);
        this.slider.setPrefWidth(this.beats*60 - 40);
        this.slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(!programmaticSliderChange){
                changeHighlighterPosition(newValue.intValue());
            }
        });

        root.getChildren().add(slider);
    }

    private void changeHighlighterPosition(int position){
        this.highlighterPosition = position;
        int measureCount = position / this.beatsPerMeasure;
        int beatCount = position % this.beatsPerMeasure;
        drumloop.setCurrentMeasure(measureCount);
        drumloop.getMeasures().get(measureCount).setCurrentBeat(beatCount);
        this.highLighter.setX(85 + (60*this.highlighterPosition));
    }

    private void createBaseGrid() {
        this.grid.add(createLabel("Beat"), 0, 0);
        for (int i = 1; i <= this.beats; i++) {
            int currentBeat = ((i - 1) % this.beatsPerMeasure) + 1;
            this.grid.add(createLabel(String.valueOf(currentBeat)),i, 0);
        }
        this.grid.setBorder(Border.EMPTY);
    }

    private Label createLabel(String text) {
        Label label = new Label(text);
        label.setAlignment(Pos.CENTER);
        label.setPrefWidth(60);
        label.setPrefHeight(30);
        return label;
    }

    private void createHighlighter() {
        this.highLighter = new Rectangle(85, 65, 60, 30);
        this.highlighterPosition = 0;
        this.highLighter.setMouseTransparent(true);
        this.highLighter.setFill(Color.RED);
        this.highLighter.setOpacity(0.5);
        this.root.getChildren().add(highLighter);
    }

    private void stepHighlighterAndSlider(){
        if (this.highlighterPosition == this.beats){
            this.highlighterPosition = 0;
        }
        this.highLighter.setX(85 + (60*this.highlighterPosition));
        programmaticSliderChange = true;
        this.slider.setValue(this.highlighterPosition);
        programmaticSliderChange = false;
        this.highlighterPosition++;
    }

    private void disableAddInstrumentButton(Instrument instrument) {
        if (instrument.getClass().equals(HiHat.class)) {
            this.imageHihat.setDisable(true);
        } else if (instrument.getClass().equals(Snare.class)) {
            this.imageSnare.setDisable(true);
        } else if (instrument.getClass().equals(Kick.class)) {
            this.imageKick.setDisable(true);
        } else if (instrument.getClass().equals(Cymbal.class)) {
            this.imageCymbal.setDisable(true);
        }
    }

    private void enableAddInstrumentButton() {
        this.imageHihat.setDisable(false);
        this.imageSnare.setDisable(false);
        this.imageKick.setDisable(false);
        this.imageCymbal.setDisable(false);
    }

    private void addInstrumentLine(Instrument instrument) {
        this.highLighter.setHeight(this.highLighter.getHeight()+50);
        disableAddInstrumentButton(instrument);
        this.grid.addRow(this.gridRow, createLabel(instrument.getClass().getSimpleName()));

        for (int i = 0; i < this.beats; i++) {
            int measureCount = i / 4;
            int beatCount = i % 4;
            Button button = createToggleInstrumentButton(instrument, measureCount, beatCount);
            this.grid.add(button, i + 1, this.gridRow);

            createEffectsContextMenu(instrument, button, measureCount, beatCount);
        }
        this.gridRow++;
    }

    private Button createToggleInstrumentButton(Instrument instrument, int measureCount, int beatCount) {
        Button button = new Button();
        Image image = new Image(instrument.getImage());
        ImageView imageView = new ImageView();
        imageView.setImage(image);
        imageView.setFitWidth(40);
        imageView.setFitHeight(40);
        button.setGraphic(imageView);

        button.setOnAction(event -> instrumentToggle(instrument, measureCount, beatCount));

        bindBeatToButton(instrument, button, measureCount, beatCount);

        return button;
    }

    private void createEffectsContextMenu(Instrument instrument, Button button, int measureCount, int beatCount) {
        ContextMenu effectsMenu = new ContextMenu();

        MenuItem noEffectItem = new MenuItem("No effect");
        noEffectItem.setOnAction(event -> instrumentAddEffect(instrument, measureCount, beatCount, noEffect));

        MenuItem echoEffectItem = new MenuItem("Echo");
        echoEffectItem.setOnAction(event -> instrumentAddEffect(instrument, measureCount, beatCount, echoEffect));

        MenuItem reverbEffectItem = new MenuItem("Reverb");
        reverbEffectItem.setOnAction(event -> instrumentAddEffect(instrument, measureCount, beatCount, reverbEffect));

        MenuItem ringModulatorEffectItem = new MenuItem("Ring Modulator");
        ringModulatorEffectItem.setOnAction(event -> instrumentAddEffect(instrument, measureCount, beatCount, ringModulatorEffect));

        MenuItem flangerEffectItem = new MenuItem("Flanger");
        flangerEffectItem.setOnAction(event -> instrumentAddEffect(instrument, measureCount, beatCount, flangerEffect));

        MenuItem distortionEffectItem = new MenuItem(("Distortion"));
        distortionEffectItem.setOnAction(event -> instrumentAddEffect(instrument, measureCount, beatCount, distortionEffect));

        effectsMenu.getItems().addAll(noEffectItem, echoEffectItem, reverbEffectItem, ringModulatorEffectItem, flangerEffectItem, distortionEffectItem);
        button.setContextMenu(effectsMenu);
    }

    private void instrumentAddEffect(Instrument instrument, int measureCount, int beatCount, Effect effect) {
        this.drumloop.getMeasures()
                .get(measureCount)
                .getBeats()
                .get(beatCount)
                .setAudioEffect(instrument, effect);
    }


    private void instrumentToggle(Instrument instrument, int measureCount, int beatCount) {
        if (this.drumloop.hasInstrument(instrument, measureCount, beatCount)) {
            this.drumloop.removeInstrument(instrument, measureCount, beatCount);
        } else {
            this.drumloop.addInstrument(instrument, measureCount, beatCount);
        }
    }

    private void bindBeatToButton(Instrument instrument, Button button, int measureCount, int beatCount) {
        Beat beat = this.drumloop.getMeasures().get(measureCount).getBeats().get(beatCount);
        BooleanBinding hasInstrument = Bindings.createBooleanBinding(() -> beat.getInstruments().contains(instrument), beat.getInstruments());
        button.styleProperty().bind(Bindings.when(hasInstrument).then("-fx-background-color: darkgray").otherwise(""));
    }


    public void menuButtonSave() {
        final Stage dialog = new Stage();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File("drumloops"));
        fileChooser.setInitialFileName("drumloop.xml");
        fileChooser.setTitle("Save music file");
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("xml", "*.xml", "*.XML");
        fileChooser.getExtensionFilters().add(filter);
        fileChooser.setSelectedExtensionFilter(filter);
        File file = fileChooser.showSaveDialog(dialog);
        if (file != null) {
            writer.writeXMLFromDrumloop(drumloop, file);
        }
    }

    public void menuButtonLoad() {
        final Stage dialog = new Stage();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File("drumloops"));
        fileChooser.setTitle("Open music file");
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("xml", "*.xml", ".XML");
        fileChooser.getExtensionFilters().addAll(filter);
        fileChooser.setSelectedExtensionFilter(filter);

        File file = fileChooser.showOpenDialog(dialog);

        this.highlighterPosition = 0;
        this.highLighter.setX(85);
        this.highLighter.setHeight(30);
        deleteInstrumentLines();
        if (file != null) {
            parser.parserDrumloopFromXml(file);
            createInstrumentLines();
        }
    }

    private void deleteInstrumentLines() {
        List<Node> deleteNodes = new ArrayList<>();
        for (Node node : this.grid.getChildren()) {
            if (GridPane.getRowIndex(node) > 0) {
                deleteNodes.add(node);
            }
        }
        this.grid.getChildren().removeAll(deleteNodes);
    }

    private void createInstrumentLines() {
        enableAddInstrumentButton();
        Set<Instrument> instrumentSet = this.drumloop.getInstrumentSet();
        for (Instrument i : instrumentSet) {
            addInstrumentLine(i);
        }
    }

    public void exit() {
       kick.shutdownExecutor();
       cymbal.shutdownExecutor();
       hiHat.shutdownExecutor();
       snare.shutdownExecutor();
       audioDevice.destroy();
       Platform.exit();
    }

    public void imageKickPressed() {
        addInstrumentLine(kick);
    }

    public void imageSnarePressed() {
        addInstrumentLine(snare);
    }

    public void imageHihatPressed() {
        addInstrumentLine(hiHat);
    }

    public void imageCymbalPressed() {
        addInstrumentLine(cymbal);
    }

    public void playLoop() {
        createLoop();
        this.loopTimeline.play();
    }

    public void stopLoop() {
        this.loopTimeline.stop();
    }
}
