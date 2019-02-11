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

  void update() {  
    if (mouseIsPressed == true) {
      finalPos.set(mouseX, mouseY); // finds the last mouse pos before releasing
    }
    stroke(sHue, 180, brightness);
    strokeWeight(3);    
    line(initialPos.x, initialPos.y, finalPos.x, finalPos.y);
  }

  // checks is mouse is pressed to draw a line
  void setMousePressed(boolean _mouseIsPressed) {
    mouseIsPressed = _mouseIsPressed;
  }

  // gets the mouse value
  boolean getMousePressed() {
    return mouseIsPressed;
  }

  PVector getInitialPos() {
    return initialPos;
  }
  PVector getFinalPos() {
    return finalPos;
  }

  void setBrightness(boolean _intersected) {
    if (_intersected == true) {
      brightness = 150;
    } else {
      brightness = 255;
    }
  }
}
