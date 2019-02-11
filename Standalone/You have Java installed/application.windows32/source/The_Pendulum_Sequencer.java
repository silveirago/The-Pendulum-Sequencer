import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import themidibus.*; 
import java.util.*; 
import controlP5.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class The_Pendulum_Sequencer extends PApplet {

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

 //Import the library
MidiBus myBus; // The MidiBus

 // GUI
ControlP5 cp5;

//Pendulum p;
ArrayList<Pendulum> p;
ArrayList<Float> pLength;
int nPendulum = 24; // number of pendulums in the ArrayList
int nActivePendulums = 15; // number of active pendulums pendulums
int nOfCycles = 16;
float hueOffset = 255 / nPendulum;
float[] hueColors =  new float[nPendulum];
//int[] numbers = new int[3];
boolean isDragging  = false;
int nPendulumOffset;
boolean isDeleting = false;

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
float gravity = 0.8f;
float resistance = 1;
boolean useMouse = false;
int scaleNamesIndex = 1;
int midiPort = 0;
String[] outputs;
boolean isOn = false;


public void setup() {
  
  prepareExitHandler();
  surface.setResizable(true);
  //fullScreen(P2D, SPAN);
  colorMode(HSB, 255, 255, 255, 255);
  

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
  float pi = 3.1415926535898f; // you got that
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
  nPendulumOffset = nPendulum - nActivePendulums;
  nPendulumOffset = nPendulum - nPendulumOffset;
  //println(nPendulumOffset);

  // strings (triggers)
  strings = new ArrayList<SoundObject>();

  // MIDI
  // List all available Midi devices on STDOUT. This will show each device's index and name.
  //MidiBus.list();   
  //myBus = new MidiBus(this, 0, 1); // Create a new MidiBus using the device index to select the Midi input and output devices respectively.
  //myBus = new MidiBus(this, -1, "IAC Bus 1");
  myBus = new MidiBus(this, -1, 1); // sets MIDI port
  //String busName = myBus.getBusName();
  myBus.setBusName("Pendulum Sequencer Bus");
  //println(myBus.getBusName());  
  outputs = MidiBus.availableOutputs(); // stores the MIDI ports

  nTriggered = new ArrayList<ArrayList<Boolean>>(); // stores the if the note was triggered or not
  pNTriggered = new ArrayList<ArrayList<Boolean>>(); // stores the if the note was triggered or not

  // creates a 2d ArrayList to compare each pendumlum with each string
  for (int i=0; i < nPendulum; i++) {
    for (int j=0; j < nPendulum; j++) {
      //myList.get(0).set(1, 17);
      nTriggered.add(new ArrayList<Boolean>());
      pNTriggered.add(new ArrayList<Boolean>());
      nTriggered.get(i).add(j, false);
      pNTriggered.get(i).add(j, false);
    }
  }

  // GUI
  GUIstuff(); // create GUI
}

public void draw() {

  background(30);

  // draws intructions
  textSize(12);
  fill(0, 0, 150);
  textAlign(LEFT);
  text("z: deletes last trigger", 10, height - 10);
  text("a: adds a pendulum", 10, height - 25);
  text("d: deletes the last pendulum", 10, height - 40);
  text("r + click: deletes a pendulum", 10, height - 55);
  text("s + click in the right circle: resizes the circle", 10, height - 70);

  // line where the circles for adjusting the height will be
  stroke(0, 200, 200);
  strokeWeight(1);  
  line(width-p.get(0).gethOffset(), 0, width-p.get(0).gethOffset(), height); 

  // do pendulum stuff
  for (int i=0; i<nActivePendulums; i++) {

    if (isOn == true) {
      p.get(i).update();
      p.get(i).go();
    }
    p.get(i).setGravity(gravity); // changes gravity
    p.get(i).setDamping(resistance); // changes reistance
    //println(gravity);
  }
  // draw strings (triggers)
  for (int i=0; i< strings.size(); i++) {    
    strings.get(i).update();
  }


  // is a circle intersecting with a line?
  for (int i=0; i<nActivePendulums; i++) {
    for (int j=0; j<strings.size(); j++) {                
      PVector initialPos = strings.get(j).getInitialPos();
      PVector finalPos = strings.get(j).getFinalPos();               
      p.get(i).isOnLine(initialPos, finalPos); // checks if they are intersecting        
      isTriggered = p.get(i).getNoteTriggered();        
      if (i < nActivePendulums && j < strings.size()) {
        nTriggered.get(i).set(j, isTriggered);
        changeStringBrightness(j);   
        p.get(i).setBrightness(isTriggered); // changes brightness when intersected
      }

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
public void connectCircles() {  
  noFill();
  PVector pos = p.get(0).getPosition();
  beginShape();  
  vertex(pos.x, pos.y);
  for (int i=0; i < nActivePendulums; i++) {   
    pos = p.get(i).getPosition();  
    curveVertex(pos.x, pos.y);
  }
  pos = p.get(nActivePendulums-1).getPosition();
  strokeWeight(1);
  vertex(pos.x, pos.y);
  endShape();
}

public void mousePressed() {

  if ((mouseX > 170) || (mouseY > 190)) {
    useMouse = true;

    //checks if you clicked in a circle
    for (int i=0; i<nActivePendulums; i++) {
      p.get(i).clicked(mouseX, mouseY);
      p.get(i).hClicked(mouseX, mouseY);

      if (p.get(i).getDragging() == true) {
        isDragging = true;
      }

      if (key == 'r' && keyPressed == true && p.get(i).dragging == true) {

        p.remove(i);
        nActivePendulums--;
        isDeleting = false;
        println("delete");
      }
    }
    //println(isDragging);
    if (isDragging == false) {
      if (keyPressed == false) {
        //adds a "string" that plays a sound when touched by a circle
        strings.add(new SoundObject());
        //println(strings.size());
        strings.get(strings.size()-1).setMousePressed(true);
        //println(strings.size());
      }
    }
  }
}


public void mouseReleased() {

  if (useMouse == true) {
    //stops draggin the cricle when mouse is released
    for (int i=0; i<nActivePendulums; i++) {
      p.get(i).stopDragging();
      p.get(i).stopHDragging();
    }

    if (isDragging == false) {
      if (keyPressed == false) {
        strings.get(strings.size()-1).setMousePressed(false);
      }
    }
    isDragging = false;
  }
  useMouse = false;
}

public void keyPressed() {
  int keyN = PApplet.parseInt(key - '0');

  if (key == 'z') {
    if (strings.size()>0) {
      strings.remove(strings.size()-1); // remove last string
    }
  }

  if (key == '1' || key == '2' || key == '3' || key == '4' || key == '5') { // resets angle of the pendulums
    for (int i=0; i<nActivePendulums; i++) {
      if (nActivePendulums>0) {
        p.get(i).angle = PI/(8-(keyN+1));
        p.get(i).aVelocity = 0;
      }
    }
  }

  if (key == 's') { // resets angle of the pendulums
    for (int i=0; i<nActivePendulums; i++) {
      if (nActivePendulums>0) {
        p.get(i).isChangingRadius(true);
      }
    }
  }

  if (key == 'a') { // adds a pendulum 
    if (nActivePendulums<24) {
      p.add(nActivePendulums, new Pendulum(new PVector(width/2, 0), pLength.get(nActivePendulums), hueColors[nActivePendulums], scales[scaleNamesIndex][nActivePendulums], nActivePendulums, 30)); // pendulum wave
      //nTriggered.add(new ArrayList<Boolean>());
      //pNTriggered.add(new ArrayList<Boolean>());
      //nTriggered.get(nActivePendulums).add(nActivePendulums, false);
      //pNTriggered.get(nActivePendulums).add(nActivePendulums, false);
      nActivePendulums++;
    }
  }
  if (nActivePendulums>0) {
    if (key == 'd') { // adds a pendulum
      p.remove(nActivePendulums-1);
      //nTriggered.remove(nActivePendulums-1);
      //pNTriggered.remove(nActivePendulums-1);
      nActivePendulums--;
    }
  }
}

public void keyReleased() {
}


public void changeStringBrightness(int j) {
  if (isTriggered == true) {
    changeBrightness = true;
  }
  if (changeBrightness ==true) {
    strings.get(j).setBrightness(true); // changes brightness when intersected
  } else {
    strings.get(j).setBrightness(false);
  }// changes brightness when intersected
}

public void changePendulumBrightness(int i, int j) {
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
// changes the musical scale
public void scales(int n) {
  scaleNamesIndex = n;

  for (int i=0; i<nActivePendulums; i++) {
    p.get(i).setPitch(scales[scaleNamesIndex][i]);
    p.get(i).displayNote();
  }
}
// changes the MIDI port
public void MIDI_out(int n) {
  midiPort = n;
  myBus.close();
  myBus = new MidiBus(this, -1, midiPort); // sets MIDI port
}
// on/off
public void on_off(boolean theFlag) {
  isOn = theFlag;
  if (isOn == false) {
    for (int ch=0; ch<16; ch++) {  // sets all midi notes off when app shuts down
      for (int n=0; n<128; n++) {
        myBus.sendNoteOn(ch, n, 0);
      }
    }
  }
  //for (int i=0; i<nActivePendulums; i++) {
  //  if (nActivePendulums>0) {
  //    p.get(i).angle = PI/6;
  //    p.get(i).aVelocity = 0;
  //  }
  //}
}

// STOP
// must add "prepareExitHandler();" in setup() for Processing sketches 
private void prepareExitHandler () {
  Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
    public void run () {
      //System.out.println("SHUTDOWN HOOK");
      myBus = new MidiBus(this, -1, 1); // sets MIDI port
      for (int ch=0; ch<16; ch++) {  // sets all midi notes off when app shuts down
        for (int n=0; n<128; n++) {
          myBus.sendNoteOn(ch, n, 0);
        }
      }
      try {
        stop();
      } 
      catch (Exception ex) {
        ex.printStackTrace(); // not much else to do at this point
      }
    }
  }
  ));
}  

public void GUIstuff() {

  // GUI
  //slider gravity
  int w = 100;
  int h = 15;
  cp5 = new ControlP5(this);
  cp5.addSlider("gravity")
    .setPosition(10, 10)
    .setSize(w, h)
    .setRange(0, 1)  
    .setValue(0.4f)
    ;
  //slider resistance
  cp5 = new ControlP5(this);
  cp5.addSlider("resistance")
    .setPosition(10, 30)
    .setSize(w, h)
    .setRange(0, 1)  
    .setValue(0.01f)    
    ;
  cp5.addToggle("on_off")
    .setPosition(10, 100)
    .setSize(h, h)
    .setValue(true)
    .setState(false) 
    //.setMode(ControlP5.SWITCH)
    ;


  /* add a ScrollableList, by default it behaves like a DropdownList */
  //List output = Arrays.asList(
  cp5.addScrollableList("MIDI_out")
    .setPosition(10, 75)
    .setSize(w, 100)
    .setBarHeight(20)
    .setItemHeight(20)
    .addItems(outputs)
    //.setType(ScrollableList.DROPDOWN) // currently supported DROPDOWN and LIST
    ;
  cp5.get(ScrollableList.class, "MIDI_out").close();

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
}
// Adapted by Gustavo Silveira from The Nature of Code 
// Nature of Code >> Daniel Shiffman
// http://natureofcode.com
// Nerd Musician >> Gustavo Silveira
// http://www.musiconerd.com
// http://www.gustavosilveira.net

// Pendulum

// A Simple Pendulum Class
// Includes functionality for user can click and drag the pendulum and adjust the pendulum's height

class Pendulum {

  PVector position;    // position of pendulum ball
  PVector origin;      // position of arm origin
  float r;             // Length of arm
  float angle;         // Pendulum arm angle
  float aVelocity;     // Angle velocity
  float aAcceleration; // Angle acceleration
  float momentum;      // Momentum when it hits the trigger
  float gravity;   

  float ballr;         // Ball radius
  float damping;       // Arbitary damping amount
  float hue;           // Color of the circle
  float brightness;    // Brightness of the circle
  boolean changeRadius = false;

  boolean dragging = false;
  boolean hDragging = false;

  boolean isPositive = true;  // checks if circle is at one side of the screen
  boolean pIsPositive = true; // previous val

  //PVector hPosition; // position for the cirlcle that indicates the pendulum length
  float hRadius;
  float hy;
  float hOffset = 20;

  // MIDI
  //int[] notes = {0, 2, 4, 5, 7, 9, 11, 12}; 
  String[] noteName = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"}; 
  int velocity = 127;
  int channel = 0;
  int noteIndex = 0;
  int pitch = 0;
  int pPitch = 0; // previous pitch
  int octave = 12 * 4;

  boolean noteTriggered = false;
  boolean pNoteTriggered = false;

  float closeness;


  // This constructor could be improved to allow a greater variety of pendulums
  Pendulum(PVector origin_, float r_, float _hue, int _pitch, int _channel, float _ballr) {
    // Fill all variables
    origin = origin_;
    position = new PVector();
    //position.set(r*sin(angle), r*cos(angle), 0);

    r = r_;
    //angle = 0.261799; //15 degrees
    angle = PI/6;
    hue = _hue;
    brightness = 240;
    hy = r_;
    aVelocity = 0.0f;
    aAcceleration = 0.0f;
    gravity = 0.8f;       // Arbitrary constant
    damping = 0.9999f;   // Arbitrary damping
    ballr = _ballr;      // Arbitrary ball radius
    hRadius = 10;
    closeness = hRadius * 2;
    momentum = abs(aVelocity * ballr);

    //note = notes[int(random(8))] + 48;
    velocity = PApplet.parseInt(random(100, 127));
    pitch = _pitch;
    channel = _channel % 16;
  }

  public void go() {

    drag();    //for user interaction
    if (changeRadius == true) {
      if (hDragging == true) {
        setRadius();
      }
    } else {
      hDrag();
    }
    display();
    displayHCircle();
    displayNote();
  }

  // Function to update position
  public void update() {
    // As long as we aren't dragging the pendulum, let it swing!

    if (!dragging) {
      //gravity = 0.8;                              // Arbitrary constant
      aAcceleration = (-1 * gravity / r) * sin(angle);  // Calculate acceleration (see: http://www.myphysicslab.com/pendulum1.html)
      aVelocity += aAcceleration;                 // Increment velocity
      aVelocity *= damping;                       // Arbitrary damping
      angle += aVelocity;                         // Increment angle
      momentum = abs(aVelocity * ballr);
      //println(momentum);
    }
  }

  public void display() {
    position.set(r*sin(angle), r*cos(angle));         // Polar to cartesian conversion
    position.add(origin);                              // Make sure the position is relative to the pendulum's origin

    stroke(hue, 255, brightness);
    strokeWeight(2);
    // Draw the arm
    line(origin.x, origin.y, position.x, position.y);
    ellipseMode(CENTER);
    fill(hue, 255, brightness);
    if (dragging) fill(hue, 200, 230);
    // Draw the ball
    strokeWeight(1);
    stroke(hue, 255, brightness);
    ellipse(position.x, position.y, ballr, ballr);
  }

  //displays the name of the note
  public void displayNote() {
    String n = noteName[pitch % 12]; // note name
    String o = str((pitch + octave) / 12); // octave
    textAlign(RIGHT);
    text(n + o, width - hOffset - 15, hy + hRadius/4);
  }

  // displays the small circle that indicates the height of the pendulum
  public void displayHCircle() {    
    pushMatrix();
    stroke(hue, 240, 240);
    //noStroke();
    strokeWeight(1);
    ellipseMode(CENTER);
    fill(hue, 200, 255);
    if (dragging) fill(0);
    // Draw the ball        
    ellipse(width-hOffset, hy, hRadius, hRadius);
    popMatrix();
  }


  // The methods below are for mouse interaction

  // This checks to see if we clicked on the pendulum ball
  public void clicked(int mx, int my) {
    float d = dist(mx, my, position.x, position.y);
    if (d < ballr) {
      dragging = true;
      //} else {
      //  dragging = false;
    }
  }

  // This checks to see if we clicked on the height ball
  public void hClicked(int mx, int my) {
    float d = dist(mx, my, width-hOffset, hy); //
    if (d < hRadius) {
      hDragging = true;
    } else {
      hDragging = false;
    }
  }

  // This tells us we are not longer clicking on the ball
  public void stopDragging() {
    if (dragging) {
      aVelocity = 0; // No velocity once you let go
      dragging = false;
    }
  }

  // This tells us we are not longer clicking on the ball
  public void stopHDragging() {
    if (hDragging) {
      aVelocity = 0; // No velocity once you let go
      hDragging = false;
    }
  }

  public void drag() {
    // If we are draging the ball, we calculate the angle between the 
    // pendulum origin and mouse position
    // we assign that angle to the pendulum
    if (dragging) {
      PVector diff = PVector.sub(origin, new PVector(mouseX, mouseY));      // Difference between 2 points
      angle = atan2(-1*diff.y, diff.x) - radians(90);                      // Angle relative to vertical axis
    }
  }

  public void hDrag() {
    // If we are draging the ball, we calculate the angle between the 
    // pendulum origin and mouse position
    // we assign that angle to the pendulum
    if (hDragging) {
      hy = mouseY;      // Difference between 2 points
      r = hy;
    }
  }

  // checks if the phase of the pendum is at a positive or negative phase
  // checks if it is in the right or left
  public void crossedCenter() {    
    if (position.x > width/2) {
      isPositive =  true;
    } else {
      isPositive =  false;
    }

    if (isPositive != pIsPositive) {
      //println(isPositive);
    }
    pIsPositive = isPositive;
  }

  public void setAngle(float _angle) {
    angle = _angle;
  }
  // gets the position
  public PVector getPosition() {
    return position;
  }
  //gets the length of the string
  public float getLength() {
    return r;
  }
  //sets the length of the string
  public void setLength(float _l) {
    r = _l;
  }

  public float gethOffset() {
    return hOffset;
  }

  public float getRadius() {
    return ballr;
  }

  public void setRadius() {
    //ballr = _radius;
    ballr = hy - mouseY;
  }

  public boolean getNoteTriggered() {
    return noteTriggered;
  }

  public boolean getDragging() {
    if ((dragging || hDragging) == true) {      
      return true;
    } else {           
      return false;
    }
  }

  ////////////////////////////////

  public void playMidiNote() {        
    //midi velocity is a product of the mass times the angular velocity (momentum)
    float vel = map(momentum, 0, 1, 0, 127); // scales the momentum to a usable midi value
    if (vel<0) { 
      vel = 0;
    };
    if (vel>127) { 
      vel = 127;
    };
    velocity = PApplet.parseInt(vel);
    //println(velocity);
    myBus.sendNoteOn(channel, pPitch, 0); // Send a Midi noteOff to the previous note
    myBus.sendNoteOn(channel, pitch + octave, velocity); // Send a Midi noteOn
    //println(channel, notes[noteIndex]+octave, velocity);
    //println(channel, pPitch, 0);
    pPitch =  pitch + octave;
  }


  public void isOnLine(PVector v0, PVector v1) {
    PVector vp = new PVector();
    PVector p = position;
    // Return minimum distance between line segment vw and point p
    PVector line = PVector.sub(v1, v0);
    float l2 = line.magSq();  // i.e. |w-v|^2 -  avoid a sqrt
    if (l2 == 0.0f) {
      vp.set(v0);
      noteTriggered = false;
    }
    PVector pv0_line = PVector.sub(p, v0);
    float t = pv0_line.dot(line)/l2;
    pv0_line.normalize();
    vp.set(line);
    vp.mult(t);
    vp.add(v0);
    float d = PVector.dist(p, vp);
    if (t >= 0 && t <= 1 && d <= closeness)
      noteTriggered =  true;
    else
      noteTriggered =  false;
  }

  public void setBrightness(boolean _intersected) {
    if (_intersected == true) {
      brightness = 180;
    } else {
      brightness = 240;
    }
  }

  public void setPitch(int p) {
    pitch = p; 
    //println(pitch);
  }

  public void setGravity (float _g) {    
    gravity = map(_g, 0.0f, 1.0f, 0.0f, 2.0f);
  }

  public void setDamping (float _d) {
    damping = map(_d, 0, 1, 1, 0.99f);
  }

  public void isChangingRadius(boolean _r) {
    changeRadius = _r;
  }
}
class SoundObject {
  PVector initialPos; // sound object position
  PVector finalPos; // sound object position
  PVector aPosition; // actuator position
  boolean isPressed = false;
  ArrayList<PVector> vertex;
  boolean mouseReleased = false;
  boolean mouseIsPressed = false;
  float sHue; //sound object hue
  float brightness;

  int count = 0;
  boolean pVal = false;
  boolean val = false;

  SoundObject() {
    //aPosition = _aPosition;
    initialPos = new PVector(mouseX, mouseY);
    finalPos = new PVector(mouseX, mouseY);

    sHue = random(70, 255);
    brightness = 255;
  }

  public void update() {  
    if (mouseIsPressed == true) {
      finalPos.set(mouseX, mouseY); // finds the last mouse pos before releasing
    }
    stroke(sHue, 180, brightness);
    strokeWeight(3);    
    line(initialPos.x, initialPos.y, finalPos.x, finalPos.y);
  }

  // checks is mouse is pressed to draw a line
  public void setMousePressed(boolean _mouseIsPressed) {
    mouseIsPressed = _mouseIsPressed;
  }

  // gets the mouse value
  public boolean getMousePressed() {
    return mouseIsPressed;
  }

  public PVector getInitialPos() {
    return initialPos;
  }
  public PVector getFinalPos() {
    return finalPos;
  }

  public void setBrightness(boolean _intersected) {
    if (_intersected == true) {
      brightness = 150;
    } else {
      brightness = 255;
    }
  }
}
int scales[][] = {
 {0, 1, 2, 3, 4, 5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23}, // 0: chromatic
 {0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23, 24, 26, 28, 29, 31, 33, 35, 36, 38, 40}, // 1: ionian
 {0, 2, 3, 5, 7, 9, 10, 12, 14, 15, 17, 19, 21, 22, 24, 26, 27, 29, 31, 33, 34, 36, 38, 39}, // 2: dorian
 {0, 1, 3, 5, 7, 8, 10, 12, 13, 15, 17, 19, 20, 22, 24, 25, 27, 29, 31, 32, 34, 36, 37, 39}, // 3: phrygian
 {0, 2, 4, 6, 7, 9, 11, 12, 14, 16, 18, 19, 21, 23, 24, 26, 28, 30, 31, 33, 35, 36, 38, 40}, // 4: lydian
 {0, 2, 4, 5, 7, 9, 10, 12, 14, 16, 17, 19, 21, 22, 24, 26, 28, 29, 31, 33, 34, 36, 38, 40}, // 5: mixolydian
 {0, 2, 3, 5, 7, 8, 10, 12, 14, 15, 17, 19, 20, 22, 24, 26, 27, 29, 31, 32, 34, 36, 38, 39}, // 6: aeolian
 {0, 1, 3, 5, 6, 8, 10, 12, 13, 15, 17, 18, 20, 22, 24, 25, 27, 29, 30, 32, 34, 36, 37, 39}, // 7: locrian
 {0, 2, 4, 6, 8, 10,12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36, 38, 40, 42, 44, 46}, // 8: wholetone
 {0, 3, 7,10,14, 17,21, 24, 27, 31, 34, 38, 41, 45, 48, 51, 55, 58, 62, 65, 69, 72, 77, 80}, // 9: m7 9 11 13
 {0, 3, 6, 9,12, 15,18, 21, 24, 27, 30, 33, 36, 39, 42, 45, 48, 51, 54, 57, 60, 63, 66, 69}, //10: dim7
 {0, 2, 3, 5, 6, 8,  9, 11, 12, 14, 15, 17, 18, 20, 21, 23, 24, 26, 27, 29, 30, 32, 33, 35}, //11: octatonic 2-1
 {0, 1, 3, 4, 6, 7,  9, 10, 12, 13, 15, 16, 18, 19, 21, 22, 24, 25, 27, 28, 30, 31, 33, 34}, //12: octatonic 1-2
 {0, 2, 4, 7, 9,12, 14, 16, 19, 21, 24, 26, 28, 31, 33, 36, 38, 40, 43, 45, 48, 50, 52, 55}, //13: major pentatonic
 {0, 3, 5, 7,10,12, 15, 17, 19, 22, 24, 27, 29, 31, 34, 36, 39, 41, 43, 46, 48, 51, 53, 55}, //14: minor pentatonic 
};
//String[] scaleNames = {"chromatic", "ionian", "dorian", "phrygian", "lydian", "mixolydian", 
//"aeolian", "locrian", "wholetone", "m7 9 11 13", "dim7", "octatonic 2-1", 
//"octatonic 1-2", "major", "pentatonic", "minor pentatonic"};

/*

0, 0 1 2 3 4 5 6  7  8  9  10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50;
1, 0 2 4 5 7 9 11 12 14 16 17 19 21 23 24 26 28 29 31 33 35 36 38 40 41 43 45 47 48 50 52 53 55 57 59 60;
2, 0 2 3 5 7 9 10 12 14 15 17 19 21 22 24 26 27 29 31 33 34 36 38 39 41 43 45 46 48 50 51 53 55 57 58 60 62;
3, 0 1 3 5 7 8 10 12 13 15 17 19 20 22 24 25 27 29 31 32 34 36 37 39 41 43 44 46 48 49 51 53 55 56 58 60 61 63;
4, 0 2 4 6 7 9 11 12 14 16 18 19 21 23 24 26 28 30 31 33 35 36 38 40 42 43 45 47 48 50 52 54 55 57 59 60 62 64;
5, 0 2 4 5 7 9 10 12 14 16 17 19 21 22 24 26 28 29 31 33 34 36 38 40 41 43 45 46 48 50 52 53 55 57 58 60 62 64;
6, 0 2 3 5 7 8 10 12 14 15 17 19 20 22 24 26 27 29 31 32 34 36 38 39 41 43 44 46 48 50 51 53 55 56 58 60 62 63;
7, 0 1 3 5 6 8 10 12 13 15 17 18 20 22 24 25 27 29 30 32 34 36 37 39 41 42 44 46 48 49 51 53 54 56 58 60 61 63;
8,  0 2 4  6  8 10 12 14 16 18 20 22 24 26 28 30 32 34 36 38 40 42 44 46 48 50 52 54 56 58 60 62 64 66 68 70 72 74;
9,  0 3 7 10 14 17 21 24 27 31 34 38 41 45 48 51 55 58 62;
10, 0 3 6 9  12 15 18 21 24 27 30 33 36 39 42 45 48 51 54 57 60 63 66 69 72 75 78 81 84 87 90;
11, 0 2 3 5 6 8 9 11 12 14 15 17 18 20 21 23 24 26 27 29 30 32 33 35 36 38 39 41 42 44 45 47 48 50 51 53 54 56 57 59 60 62 63 65 66 68 69 71 72;
12, 0 1 3 4 6  7  9 10 12 13 15 16 18 19 21 22 24 25 27 28 30 31 33 34 36 37 39 40 42 43 45 46 48 49 51 52 54 55 57 58 60 61 63 64;
13, 0 2 4 7 9  12 14 16 19 21 24 26 28 31 33 36 38 40 43 45 48 50 52 55 57 60;
14, 0 3 5 7 10 12 15 17 19 22 24 27 29 31 34 36 39 41 43 46 48 51 53 55 58 60;
15, 0 12 24 36 48;

*/
  public void settings() {  size(800, 1080, P2D);  smooth(8); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "The_Pendulum_Sequencer" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
