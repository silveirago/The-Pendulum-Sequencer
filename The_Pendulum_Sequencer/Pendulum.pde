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
    aVelocity = 0.0;
    aAcceleration = 0.0;
    gravity = 0.8;       // Arbitrary constant
    damping = 0.9999;   // Arbitrary damping
    ballr = _ballr;      // Arbitrary ball radius
    hRadius = 10;
    closeness = hRadius * 2;
    momentum = abs(aVelocity * ballr);

    //note = notes[int(random(8))] + 48;
    velocity = int(random(100, 127));
    pitch = _pitch;
    channel = _channel % 16;
  }

  void go() {
    
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
  void update() {
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

  void display() {
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
  void displayNote() {
    String n = noteName[pitch % 12]; // note name
    String o = str((pitch + octave) / 12); // octave
    textAlign(RIGHT);
    text(n + o, width - hOffset - 15, hy + hRadius/4);
  }

  // displays the small circle that indicates the height of the pendulum
  void displayHCircle() {    

    stroke(hue, 240, 240);
    //noStroke();
    strokeWeight(1);
    ellipseMode(CENTER);
    fill(hue, 200, 255);
    if (dragging) fill(0);
    // Draw the ball        
    ellipse(width-hOffset, hy, hRadius, hRadius);
  }


  // The methods below are for mouse interaction

  // This checks to see if we clicked on the pendulum ball
  void clicked(int mx, int my) {
    float d = dist(mx, my, position.x, position.y);
    if (d < ballr) {
      dragging = true;
      //} else {
      //  dragging = false;
    }
  }

  // This checks to see if we clicked on the height ball
  void hClicked(int mx, int my) {
    float d = dist(mx, my, width-hOffset, hy); //
    if (d < hRadius) {
      hDragging = true;
    } else {
      hDragging = false;
    }
  }

  // This tells us we are not longer clicking on the ball
  void stopDragging() {
    if (dragging) {
      aVelocity = 0; // No velocity once you let go
      dragging = false;
    }
  }

  // This tells us we are not longer clicking on the ball
  void stopHDragging() {
    if (hDragging) {
      aVelocity = 0; // No velocity once you let go
      hDragging = false;
    }
  }

  void drag() {
    // If we are draging the ball, we calculate the angle between the 
    // pendulum origin and mouse position
    // we assign that angle to the pendulum
    if (dragging) {
      PVector diff = PVector.sub(origin, new PVector(mouseX, mouseY));      // Difference between 2 points
      angle = atan2(-1*diff.y, diff.x) - radians(90);                      // Angle relative to vertical axis
    }
  }

  void hDrag() {
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
  void crossedCenter() {    
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

  void setAngle(float _angle) {
    angle = _angle;
  }
  // gets the position
  PVector getPosition() {
    return position;
  }
  //gets the length of the string
  float getLength() {
    return r;
  }
  //sets the length of the string
  void setLength(float _l) {
    r = _l;
  }

  float gethOffset() {
    return hOffset;
  }

  float getRadius() {
    return ballr;
  }

  void setRadius() {
    //ballr = _radius;
    ballr = hy - mouseY;
  }

  boolean getNoteTriggered() {
    return noteTriggered;
  }

  boolean getDragging() {
    if ((dragging || hDragging) == true) {      
      return true;
    } else {           
      return false;
    }
  }

  ////////////////////////////////

  void playMidiNote() {        
    //midi velocity is a product of the mass times the angular velocity (momentum)
    float vel = map(momentum, 0, 1, 0, 127); // scales the momentum to a usable midi value
    if (vel<0) { 
      vel = 0;
    };
    if (vel>127) { 
      vel = 127;
    };
    velocity = int(vel);
    //println(velocity);
    myBus.sendNoteOn(channel, pPitch, 0); // Send a Midi noteOff to the previous note
    myBus.sendNoteOn(channel, pitch + octave, velocity); // Send a Midi noteOn
    //println(channel, notes[noteIndex]+octave, velocity);
    //println(channel, pPitch, 0);
    pPitch =  pitch + octave;
  }


  void isOnLine(PVector v0, PVector v1) {
    PVector vp = new PVector();
    PVector p = position;
    // Return minimum distance between line segment vw and point p
    PVector line = PVector.sub(v1, v0);
    float l2 = line.magSq();  // i.e. |w-v|^2 -  avoid a sqrt
    if (l2 == 0.0) {
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

  void setBrightness(boolean _intersected) {
    if (_intersected == true) {
      brightness = 180;
    } else {
      brightness = 240;
    }
  }
  
  void setPitch(int p) {
   pitch = p; 
   //println(pitch);
  }

  void setGravity (float _g) {    
    gravity = map(_g, 0.0, 1.0, 0.0, 2.0);
  }

  void setDamping (float _d) {
    damping = map(_d, 0, 1, 1, 0.99);     
  }

  void isChangingRadius(boolean _r) {
    changeRadius = _r;
  }
}
