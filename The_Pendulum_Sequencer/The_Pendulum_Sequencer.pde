// Adapted by Gustavo Silveira from The Nature of Code 
// Nature of Code >> Daniel Shiffman
// http://natureofcode.com
// Nerd Musician >> Gustavo Silveira
// http://www.musiconerd.com
// http://www.gustavosilveira.net

// Pendulum

// A simple pendulum simulation
// Given a pendulum with an angle theta (0 being the pendulum at rest) and a radius r
// we can use sine to calculate the angular component of the gravitational force.

// Gravity Force = Mass * Gravitational Constant;
// Pendulum Force = Gravity Force * sine(theta)
// Angular Acceleration = Pendulum Force / Mass = gravitational acceleration * sine(theta);

// Note this is an ideal world scenario with no tension in the 
// pendulum arm, a more realistic formula might be:
// Angular Acceleration = (g / R) * sine(theta)

// For a more substantial explanation, visit:
// http://www.myphysicslab.com/pendulum1.html 

import themidibus.*; //Import the library
MidiBus myBus; // The MidiBus
import java.util.*;
import controlP5.*; // GUI
ControlP5 cp5;

//Pendulum p;
ArrayList<Pendulum> p;
ArrayList<Float> pLength;
int nPendulum = 15; // number of pendulums
int nOfCycles = 16;
float hueOffset = 255 / nPendulum;
float[] hueColors =  new float[nPendulum];
//int[] numbers = new int[3];
boolean isDragging  = false;

// Sound Object
ArrayList<SoundObject> strings;
int sCounter = 0;
boolean changeBrightness = false;

// MIDI
//ArrayList<ArrayList<Integer>> twoDArrayList = new ArrayList<ArrayList<Integer>>();
ArrayList<ArrayList<Boolean>> nTriggered; // store the state of the note
ArrayList<ArrayList<Boolean>> pNTriggered; // previous state

boolean isTriggered = false;

// GUI
float gravity = 0.8;
float resistance = 1;
boolean useMouse = false;
int scaleNamesIndex = 1;
int midiPort = 0;

void setup() {
  size(800, 1080, P2D);
  surface.setResizable(true);
  //fullScreen(P2D, SPAN);
  colorMode(HSB, 255, 255, 255, 255);
  smooth(8);

  // Make a new Pendulum with an origin position and armlength
  p = new ArrayList<Pendulum>(); // creates an array with pendulums
  pLength = new ArrayList<Float>(); // creates an array with pendulums

  // set the color of the pendulums
  for (int i=0; i<nPendulum; i++) {
    hueColors[i] = hueOffset * i;
  }

  // calculates the length of the pendulums
  // Formula on:
  // https://www.education.com/science-fair/article/pendulum-waves/
  // l = (( tMax / (2PI / (k + n + 1) ) * ( tMax / (2PI / (k + n + 1) ))

  float l = 600; // length of the longest pendulum
  //float g = 1;
  float k = 12; // number of cycles of the longest pendulum to complete one full cycle
  float n = 1; // the index of the pendulum
  float pi = 3.1415926535898; // you got that
  // pendulum formula (in parts) to discover the total time that it will take to complete one full cycle
  //float a = 2*pi*(k+n+1);
  //float b = sqrt(l);
  float Tmax = sqrt(l) * (2*pi*(k+n+1));

  for (int i=0; i<nPendulum; i++) {       
    n = i;     
    l = pow((Tmax / (2 * pi *(k+n+1))), 2);
    pLength.add(i, new Float(l));
    //println(pLength.get(i));
  }

  // add the pendulums
  for (int i=0; i<nPendulum; i++) {  
    //Pendulum(PVector origin_, float r_, float _hue, int _pitch, float _ballr)
    p.add(i, new Pendulum(new PVector(width/2, 0), pLength.get(i), hueColors[i], scales[scaleNamesIndex][i], i, 30)); // pendulum wave
    //p.add(i, new Pendulum(new PVector(width/2, 0), longPendulum, hueColors[i])); // golden ratio
    //longPendulum*=1.61803398875; // golden ratio
    //p.add(i, new Pendulum(new PVector(width/2, 0), longPendulum, hueColors[i], int(random(8)), 30));// golden ratio
    //longPendulum *= 0.666666; //
  }

  // strings (triggers)
  strings = new ArrayList<SoundObject>();

  // MIDI
  // List all available Midi devices on STDOUT. This will show each device's index and name.
  //MidiBus.list(); 
  String[] outputs;


  //myBus = new MidiBus(this, 0, 1); // Create a new MidiBus using the device index to select the Midi input and output devices respectively.
  //myBus = new MidiBus(this, -1, "IAC Bus 1");
  myBus = new MidiBus(this, -1, 1); // sets MIDI port
  //String busName = myBus.getBusName();
  myBus.setBusName("Pendulum Sequencer Bus");
  //println(myBus.getBusName());
  outputs = MidiBus.availableOutputs(); 

  nTriggered = new ArrayList<ArrayList<Boolean>>(); // stores the if the note was triggered or not
  pNTriggered = new ArrayList<ArrayList<Boolean>>(); // stores the if the note was triggered or not

  // creates a 2d ArrayList to compare each pendumlum with each string
  for (int i=0; i<nPendulum; i++) {
    for (int j=0; j<nPendulum; j++) {
      //myList.get(0).set(1, 17);
      nTriggered.add(new ArrayList<Boolean>());
      pNTriggered.add(new ArrayList<Boolean>());
      nTriggered.get(i).add(j, false);
      pNTriggered.get(i).add(j, false);
    }
  }

  // GUI
  //slider gravity
  int w = 100;
  int h = 15;
  cp5 = new ControlP5(this);
  cp5.addSlider("gravity")
    .setPosition(10, 10)
    .setSize(w, h)
    .setRange(0, 1)  
    .setValue(0.4)
    ;
  //slider resistance
  cp5 = new ControlP5(this);
  cp5.addSlider("resistance")
    .setPosition(10, 30)
    .setSize(w, h)
    .setRange(0, 1)  
    .setValue(0.01)    
    ;
  // list scales

  /* add a ScrollableList, by default it behaves like a DropdownList */
  //List output = Arrays.asList(
  cp5.addScrollableList("MIDI out")
    .setPosition(10, 75)
    .setSize(w, 100)
    .setBarHeight(20)
    .setItemHeight(20)
    .addItems(outputs)
    //.setType(ScrollableList.DROPDOWN) // currently supported DROPDOWN and LIST
    ;
    cp5.get(ScrollableList.class, "MIDI out").close();
    
  List scalesN = Arrays.asList("chromatic", "ionian", "dorian", "phrygian", "lydian", "mixolydian", 
    "aeolian", "locrian", "wholetone", "m7 9 11 13", "dim7", "octatonic 2-1", 
    "octatonic 1-2", " major pentatonic", "minor pentatonic");
  cp5.addScrollableList("scales")
    .setPosition(10, 50)
    .setSize(w, 100)
    .setBarHeight(20)
    .setItemHeight(20)
    .addItems(scalesN)    
    //.setType(ScrollableList.DROPDOWN) // currently supported DROPDOWN and LIST
    ;
  cp5.get(ScrollableList.class, "scales").close();

  // list scales
}

void draw() {

  background(30);

  // line where the circles for adjusting the height will be
  stroke(0, 200, 200);
  strokeWeight(1);  
  line(width-p.get(0).gethOffset(), 0, width-p.get(0).gethOffset(), height); 

  // do pendulum stuff
  for (int i=0; i<nPendulum; i++) {
    p.get(i).go();  
    p.get(i).setGravity(gravity); // changes gravity
    p.get(i).setDamping(resistance); // changes reistance
    //println(gravity);
  }
  // draw strings (triggers)
  for (int i=0; i< strings.size(); i++) {    
    strings.get(i).update();
  }


  // is a circle intersecting with a line?
  for (int i=0; i<p.size(); i++) {
    for (int j=0; j<strings.size(); j++) {                
      PVector initialPos = strings.get(j).getInitialPos();
      PVector finalPos = strings.get(j).getFinalPos();               
      p.get(i).isOnLine(initialPos, finalPos); // checks if they are intersecting        
      isTriggered = p.get(i).getNoteTriggered();        

      nTriggered.get(i).set(j, isTriggered);      

      changeStringBrightness(j);   
      p.get(i).setBrightness(isTriggered); // changes brightness when intersected

      // checks every pendulum with every string 
      //and sends a midi note if they are intersecting 
      if (nTriggered.get(i).get(j) == true) { // if they are intersecting do something        
        if (nTriggered.get(i).get(j) != pNTriggered.get(i).get(j)) { // see if prev val is different from actual val                              
          p.get(i).playMidiNote(); // play midi note when intersected
        }
      }
      pNTriggered.get(i).set(j, nTriggered.get(i).get(j)); // stores actual val in previous val
    }
  }
  changeBrightness = false;
  //println(mouseX, mouseY);

  //connectCircles(); // connect the circles with a line

  //saveFrame("output/pendulumWave####.png");
}



// connects circles with a line
void connectCircles() {  
  noFill();
  PVector pos = p.get(0).getPosition();
  beginShape();  
  vertex(pos.x, pos.y);
  for (int i=0; i < nPendulum; i++) {   
    pos = p.get(i).getPosition();  
    curveVertex(pos.x, pos.y);
  }
  pos = p.get(nPendulum-1).getPosition();
  strokeWeight(1);
  vertex(pos.x, pos.y);
  endShape();
}

void mousePressed() {

  if ((mouseX > 155) || (mouseY > 55)) {
    useMouse = true;

    //checks if you clicked in a circle
    for (Pendulum pendulum : p) {
      pendulum.clicked(mouseX, mouseY);
      pendulum.hClicked(mouseX, mouseY);
    }

    for (int i=0; i<p.size(); i++) {    
      if (p.get(i).getDragging() == true) {
        isDragging = true;
      }
    }
    //println(isDragging);
    if (isDragging == false) {
      //adds a "string" that plays a sound when touched by a circle
      strings.add(new SoundObject());
      //println(strings.size());
      strings.get(strings.size()-1).setMousePressed(true);
      //println(strings.size());
    }
  }
}

void mouseReleased() {

  if (useMouse == true) {
    //stops draggin the cricle when mouse is released
    for (Pendulum pendulum : p) {
      pendulum.stopDragging();
      pendulum.stopHDragging();
    }

    if (isDragging == false) {
      strings.get(strings.size()-1).setMousePressed(false);
    }
    isDragging = false;
  }
  useMouse = false;
}

void keyPressed() {
  if (key == 'z') {
    if (strings.size()>0) {
      strings.remove(strings.size()-1); // remove last string
    }
  }
  if (key == '1') { // resets angle of the pendulums
    for (int i=0; i<p.size(); i++) {
      if (p.size()>0) {
        p.get(i).setAngle(PI/6);
      }
    }
  }
  if (key == 's') { // resets angle of the pendulums
    for (int i=0; i<p.size(); i++) {
      if (p.size()>0) {
        p.get(i).isChangingRadius(true);
      }
    }
  }
}

void keyReleased() {
  if (key == 's') { // resets angle of the pendulums
    for (int i=0; i<p.size(); i++) {
      if (p.size()>0) {
        p.get(i).isChangingRadius(false);
      }
    }
  }
}


void changeStringBrightness(int j) {
  if (isTriggered == true) {
    changeBrightness = true;
  }
  if (changeBrightness ==true) {
    strings.get(j).setBrightness(true); // changes brightness when intersected
  } else {
    strings.get(j).setBrightness(false);
  }// changes brightness when intersected
}

void changePendulumBrightness(int i, int j) {
  if (nTriggered.get(i).get(j) == true) {
    changeBrightness = true;
  }
  if (changeBrightness == true) {
    p.get(i).setBrightness(true); // changes brightness when intersected
  } else {
    p.get(i).setBrightness(false); // changes brightness when intersected
  }
}

// GUI
void scales(int n) {
  //println(n);
  scaleNamesIndex = n;

  for (int i=0; i<nPendulum; i++) {
    p.get(i).setPitch(scales[scaleNamesIndex][i]);
    p.get(i).displayNote();
  }
}

void midiOut(int n) {
  //println(n);
  midiPort = n;
}
