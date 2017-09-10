/*
Firmware for : Cloud Locker/Communication Control Board
Property of  : Kiny.io
Developed by : Rohan Stanley
*/

#include <SoftwareSerial.h>
#define BUSY HIGH
#define Idle LOW
#define mcBusy A0
#define mcInt 2
#define buzzer 5
#define charging_enable 7
#define CHG_EN 4
#define power_12v A1
#define disco_input A2
boolean charge = false;
SoftwareSerial mcSerial(8, 9); // RX, TX of Daisy Serial Port
String query,response,forwardQuery, command,ID; 
int slaveNumber = 0;

void setup()
{                                                   
  pinMode(power_12v,OUTPUT);
  digitalWrite(power_12v,LOW);
  pinMode(mcBusy,INPUT_PULLUP);
  pinMode(buzzer,OUTPUT);
  digitalWrite(buzzer,LOW);
  pinMode(CHG_EN,OUTPUT);
  digitalWrite(CHG_EN,HIGH);
  pinMode(charging_enable, OUTPUT);
  digitalWrite(charging_enable,LOW);
  Serial.begin(9600); // BT serial port
  mcSerial.begin(9600); // Daisy Serial Port
  delay(1000);
  digitalWrite(power_12v , HIGH);
  digitalWrite(charging_enable,HIGH);
}

void loop()
{
  charge = false;
  while(Serial.available()==0 && mcSerial.available()==0);
  
  //Serial Buffer for acquiring Query String
  if(Serial.available()){
      while(Serial.available())
      {
        char c = Serial.read();
        query += c;
        delay(3);
      }
      query.trim();
    
      ID = query.substring(0,2);
      command = query.substring(3);
      
      if(command == "H"){
        digitalWrite(charging_enable,LOW);
        digitalWrite(CHG_EN,LOW);
        charge = true;
        response = ID;
        response += ":D";
        Serial.println(response);
        query = ""; //Clear for next Response
      }
      else if(command == "L"){
        digitalWrite(charging_enable,HIGH);
        digitalWrite(CHG_EN,HIGH);
        charge = true;
        response = ID;
        response += ":C";
        Serial.println(response);
        query = ""; //Clear for next Response
      }
      else if(command == "S"){  // S = 12v relay On
        digitalWrite(power_12v,HIGH);
        charge = true;
        response = ID;
        response += ":S";
        Serial.println(response);
        query = ""; //Clear for next Response
      }
      else if(command == "P"){  // P = 12v relay Off
        digitalWrite(power_12v,LOW);
        charge = true;
        response = ID;
        response += ":P";
        Serial.println(response);
        query = ""; //Clear for next Response
      }
      else if(command == "Y"){  //Y = Buzzer ON Command
        digitalWrite(buzzer,HIGH);
        charge = true;
        response = ID;
        response += ":Y";
        Serial.println(response);
        query = ""; //Clear for next Response
      }
      else if(command == "Z"){  //Z = Buzzer OFF Command
        digitalWrite(buzzer,LOW);
        charge = true;
        response = ID;
        response += ":Z";
        Serial.println(response);
        query = ""; //Clear for next Response
      } 
      else charge = false;
      
      // If MC is Not Busy
      if(!charge){
          if(digitalRead(mcBusy)==Idle){
            forwardQuery = String(slaveNumber);
            forwardQuery.concat(query);
            forwardQuery.trim();
            mcSerial.println(forwardQuery);
          }
        
          // If MC Busy, "G" as response to Android to resend query in a few milliseconds
          else{
            response ="";
            response += ID;
            response += ":G";
            Serial.println(response);
            response = "";
            ID = "";
          }     
          query = ""; //Clear for next Response
          forwardQuery = ""; 
      }
  }
  if(mcSerial.available()){
    while(mcSerial.available()){
    Serial.write(mcSerial.read());
    }
    response = ""; //Clear for next iteration
  }
}

