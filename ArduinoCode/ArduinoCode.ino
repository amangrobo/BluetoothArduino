#include <SoftwareSerial.h>

SoftwareSerial bluetooth(10, 11); // RX, TX   pin 11: RX of HC05; pin 10: TX of HC05;
int LED = 13; // the on-board LED
char data; // the data received

enum Mode {Manual, LF, Encoder} mode;

void setup() {
  bluetooth.begin(9600);
  Serial.begin(9600);
  Serial.println("Waiting for command...");
  bluetooth.println("Send 'turn on' to turn on the LED. Send 'turn off' to turn Off");
  pinMode(LED, OUTPUT);

  mode = Manual;
}

void loop() {
  String command;
  if (bluetooth.available()) { //wait for data received
    while (true) {
      data = bluetooth.read();
      if (data == '#') break;
      command = command + data;
    }

    Serial.println(command);

    if (command == "MN") {

      mode = Manual;
      Serial.println("Mode Manual !");
      bluetooth.println("Mode Manual !");

    } else if (command == "LF") {

      mode = LF;
      Serial.println("Mode Line Follower !");
      bluetooth.println("Mode Line Follower !");

    } else if (command == "EC") {

      mode = Encoder;
      Serial.println("Mode Encoder !");
      bluetooth.println("Mode Encoder !");

    } else {
      if (mode == Manual) {
        checkString(command);
      }
    }
  }
  delay(100);
}

void checkString(String str) {

  String dirn = str.substring(0, 1);
  int inpPwm = str.substring(2).toInt();

  int pwm = map(inpPwm, 0, 100, 0, 255);

  Serial.print("String:");
  Serial.println(str);
  Serial.print("dirn: ");
  Serial.println(dirn);
  Serial.print("pwm: ");
  Serial.println(pwm);
}
