/*
Firmware for : Cloud Locker/Communication Control Board
Property of  : Kiny.io
Developed by : Rohan Stanley
*/
#include <SoftwareSerial.h>
#define mcBusy A0
#define mcInt 2
#define buzzer 5
#define charging_enable 7
#define CHG_EN 4
#define power_12v A1
#define disco_input A2
boolean charge = false;
SoftwareSerial mcSerial(8, 9); // RX, TX of Daisy Serial Port
String query,response,forwardQuery; 
int slaveNumber = 0;
///////////////////////////////////////////////////////////////////////////////////////////////
// SETUP CODE SECTION
///////////////////////////////////////////////////////////////////////////////////////////////
void setup()
{
  pinMode(power_12v,OUTPUT);
  digitalWrite(power_12v,LOW);
  pinMode(mcBusy,INPUT_PULLUP);
  pinMode(buzzer,OUTPUT);
  digitalWrite(buzzer,LOW);
  pinMode(CHG_EN,OUTPUT);
  digitalWrite(CHG_EN,LOW);
  pinMode(charging_enable, OUTPUT);
  digitalWrite(charging_enable,LOW);
  Serial.begin(9600); // BT serial port
  mcSerial.begin(9600); // Daisy Serial Port
  delay(1000);
  digitalWrite(power_12v , HIGH);
  digitalWrite(charging_enable,HIGH);
  //attachInterrupt(digitalPinToInterrupt(mcInt), mcISR,RISING);
}
///////////////////////////////////////////////////////////////////////////////////////////////
// MAIN SECTION
///////////////////////////////////////////////////////////////////////////////////////////////
void loop()
{
  // Wait for a query to come from Android End
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
  if(query == "L"){
  digitalWrite(charging_enable,LOW);
  digitalWrite(CHG_EN,LOW);
  charge = true;
  query = ""; //Clear for next Response
  }
  else if(query == "H"){
  digitalWrite(charging_enable,HIGH);
  digitalWrite(CHG_EN,HIGH);
  charge = true;
  query = ""; //Clear for next Response
  }
  else
  charge = false;
  // MC Not Busy
  if(!charge){
  if(digitalRead(mcBusy)==HIGH)
  {
     forwardQuery = String(slaveNumber);
     forwardQuery.concat(query);
     forwardQuery.trim();
//     for(int i=0;i <= forwardQuery.length();i++)
//     {  
//      char x = query.charAt(i);
//      mcSerial.write(x);
//     }
  mcSerial.println(forwardQuery);
  }
  // MC Busy, "B" as response to Android to resend query in a few milliseconds
  else
  {
    Serial.println("B");
  }
   query = ""; //Clear for next Response
   forwardQuery = "";
  }
  }
  if(mcSerial.available()){
    while(mcSerial.available()){
    Serial.write(mcSerial.read());
//    char d = mcSerial.read();
//    response += d;
//    delay(3);
    }
  
    //response.trim();
  //Serial.println(response);
  //wait = 10000;
  response = ""; //Clear for next iteration
  }
  
}
///////////////////////////////////////////////////////////////////////////////////////////////
// ISR for Servicing MC's Interrupt
///////////////////////////////////////////////////////////////////////////////////////////////
//void mcISR()
//{
//  int wait = 10000;  
//  while(mcSerial.available()==0 && wait>1)
//  { 
//    wait--;
//    if(wait<=1)
//    return;
//  }
//  
//  while(mcSerial.available()){
//    char c = mcSerial.read();
//    response += c;
//    delay(3);
//  }
//  
//  response.trim();
//  Serial.println(response);
//  wait = 10000;
//  response = ""; //Clear for next iteration
//  return;
//}
