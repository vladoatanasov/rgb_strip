#include "RGBdriver.h"
#include <SoftwareSerial.h>

#define CLK 2//pins definitions for the driver        
#define DIO 3
RGBdriver Driver(CLK, DIO);
SoftwareSerial BT(10, 11);

int black[3]  = { 0, 0, 0 };
int white[3]  = { 100, 100, 100 };
int red[3]    = { 100, 0, 0 };
int green[3]  = { 0, 100, 0 };
int blue[3]   = { 0, 0, 100 };
int yellow[3] = { 40, 95, 0 };

int redVal = black[0];
int grnVal = black[1];
int bluVal = black[2];

int wait = 10;      // 10ms internal crossFade delay; increase for slower fades
int hold = 0;       // Optional hold when a color is complete, before the next crossFade
int repeat = 3;     // How many times should we loop before stopping? (0 for no stop)
int j = 0;          // Loop counter for repeat

// Initialize color variables
int prevR = redVal;
int prevG = grnVal;
int prevB = bluVal;

bool inAutoMode = true;

void setup()
{
  BT.begin(9600);
  Serial.begin(9600);
}

void loop()
{
  if(inAutoMode) {
    crossFade(red);
    crossFade(green);
    crossFade(blue);
    crossFade(yellow);
    crossFade(white);
  } else {
    readBT();
  }

  //if you want to set the name of the BT device, send "AT+NAMEmy name" via serial
  //  if (BT.available())
  //    Serial.write(BT.read());
  //
  //  if (Serial.available())
  //    BT.write(Serial.read());

}

int calculateStep(int prevValue, int endValue) {
  int step = endValue - prevValue; // What's the overall gap?
  if (step) {                      // If its non-zero,
    step = 1020 / step;            //   divide by 1020
  }
  return step;
}

int calculateVal(int step, int val, int i) {

  if ((step) && i % step == 0) { // If step is non-zero and its time to change a value,
    if (step > 0) {              //   increment the value if step is positive...
      val += 1;
    }
    else if (step < 0) {         //   ...or decrement it if step is negative
      val -= 1;
    }
  }
  // Defensive driving: make sure val stays in the range 0-255
  if (val > 255) {
    val = 255;
  }
  else if (val < 0) {
    val = 0;
  }
  return val;
}

void crossFade(int color[3]) {
  if(!inAutoMode) return;
  
  // Convert to 0-255
  int R = (color[0] * 255) / 100;
  int G = (color[1] * 255) / 100;
  int B = (color[2] * 255) / 100;

  int stepR = calculateStep(prevR, R);
  int stepG = calculateStep(prevG, G);
  int stepB = calculateStep(prevB, B);

  for (int i = 0; i <= 1020; i++) {
    redVal = calculateVal(stepR, redVal, i);
    grnVal = calculateVal(stepG, grnVal, i);
    bluVal = calculateVal(stepB, bluVal, i);

    setColor(redVal, grnVal, bluVal);

    readBT();

    if(!inAutoMode) return;

    delay(wait); // Pause for 'wait' milliseconds before resuming the loop
  }

  // Update current values for next loop
  prevR = redVal;
  prevG = grnVal;
  prevB = bluVal;
  
  delay(hold); // Pause for optional 'wait' milliseconds before resuming the loop
}

void setColor(int r, int g, int b) {
  Driver.begin();
  Driver.SetColor(r, g, b);
  Driver.end();
}

void readBT() {
  if (BT.available()) {
    char colors[BT.available()] = {};
    int i = 0;
    
    while (BT.available()) {
      char c = BT.read();

      if (c == 'a') {
        Serial.println("Auto mode");
        inAutoMode = true;
        return;
      }

      // send the color to the client
      if (c == 'r') {
        Serial.println("Read color");
        sendColor();
        return;
      }

      colors[i] = c;
      i++;
    }

    Serial.println(colors);

    char *tok;
    int j = 0;

    tok = strtok(colors, ",");
    while (tok != NULL) {
      if (j == 0) {
        Serial.print("Red: ");
        redVal = atoi(tok);
      } else if (j == 1) {
        Serial.print("Green: ");
        grnVal = atoi(tok);
      } else if (j == 2) {
        Serial.print("Blue: ");
        bluVal = atoi(tok);
      }

      Serial.println(atoi(tok));

      tok = strtok (NULL, ",");
      j++;
    }

    setColor(redVal, grnVal, bluVal);
    inAutoMode = false;
  }
}

void sendColor() {
  BT.print(redVal);
  BT.print(",");
  BT.print(grnVal);
  BT.print(",");
  BT.println(bluVal);
}

