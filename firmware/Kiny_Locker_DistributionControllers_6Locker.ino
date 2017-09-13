
/*
Firmware for : Cloud Locker/Distribution Board/6 Locker
Property of  : Kiny.io
Developed by : Rohan Stanley
*/


///////////////////Daisy Chaining Ports (begins)////////////////////////////
#define in_serial Serial    //Daisy In port
#define out_serial Serial1  //Daisy Out port
#define Busy_out 38
#define Int_out 67
#define Busy_in 69
#define Int_in 46  
#define led 13
///////////////////Daisy Chaining Ports (Ends)//////////////////////////////

///////////////////Compartment Pin Allotments (begins)//////////////////////

//////////////////IR Sensor Input Pins (begins)////////////////////
#define ir0 56  //Compartment 1
#define ir1 59  //Compartment 2
#define ir2 24  //Compartment 3
#define ir3 27  //Compartment 4
#define ir4 39  //Compartment 5
#define ir5 32  //Compartment 6
#define ir6 35  //Compartment 7
#define ir7 41  //Compartment 8
#define ir8 44  //Compartment 9
#define ir9 49  //Compartment 10
#define ir10 10 //Compartment 11
#define ir11 7  //Compartment 12
#define ir12 17 //Compartment 13
#define ir13 5  //Compartment 14
#define ir14 61 //Compartment 15
///////////////////IR Sensor Input Pins (Ends)//////////////////////

////////////////////Door Sensor Input Pins (begins)/////////////////
#define d0 55  //Compartment 1
#define d1 58  //Compartment 2
#define d2 23  //Compartment 3
#define d3 26  //Compartment 4
#define d4 29  //Compartment 5
#define d5 31  //Compartment 6
#define d6 34  //Compartment 7
#define d7 40  //Compartment 8
#define d8 43  //Compartment 9
#define d9 48  //Compartment 10
#define d10 11 //Compartment 11
#define d11 8  //Compartment 12
#define d12 16 //Compartment 13
#define d13 2  //Compartment 14
#define d14 60 //Compartment 15
////////////////////Door Sensor Input Pins (Ends)////////////////////

////////////////////Lock Control Pins (begins)///////////////////////
#define m0 54  //Compartment 1
#define m1 57  //Compartment 2
#define m2 22  //Compartment 3
#define m3 25  //Compartment 4
#define m4 28  //Compartment 5
#define m5 30  //Compartment 6
#define m6 33  //Compartment 7
#define m7 37  //Compartment 8
#define m8 42  //Compartment 9                                                    
#define m9 47  //Compartment 10
#define m10 12 //Compartment 11
#define m11 9  //Compartment 12
#define m12 6  //Compartment 13
#define m13 3  //Compartment 14
#define m14 4  //Compartment 15
////////////////////Lock Control Pins (Ends)//////////////////////////

/////////////////////Compartment Pin Allotments (Ends)/////////////////////

//GLOBAL VARIABLES
#define BUSY HIGH
#define Idle LOW
String response,response1, query, forwardQuery,Box = "" , ID = "", commandNext = "";
int slaveNumber, BoxNumber, index = 2;
char commandType, entryType;
boolean myCommand = false;
boolean firstIterationX = true;
boolean DoNothing = false , QueryForwarded = false, NoResponse = false;

uint8_t IRPins[15]= {ir0,ir1,ir2,ir3,ir4,ir5,ir6,ir7,ir8,ir9,ir10,ir11,ir12,ir13,ir14};
uint8_t DPins[15]= {d0,d1,d2,d3,d4,d5,d6,d7,d8,d9,d10,d11,d12,d13,d14};
uint8_t MPins[15]= {m0,m1,m2,m3,m4,m5,m6,m7,m8,m9,m10,m11,m12,m13,m14};
int BoxNumbers[30], noOfBoxes = 1 ,myNOB = 0, extraNOB = 0, num;
long COUNTER = 0, OVERFLOW = 2000000;

void setup() {
 
  for(uint8_t b = 0; b <= 14 ;b++)
  {
    pinMode(MPins[b],OUTPUT);
    digitalWrite(MPins[b],LOW);
    pinMode(DPins[b],INPUT);
    pinMode(IRPins[b],INPUT);
  }
  pinMode(Busy_in,OUTPUT);
  digitalWrite(Busy_in,Idle);
  pinMode(Int_in,OUTPUT);
  digitalWrite(Int_in,LOW);
  
  pinMode(led,OUTPUT);
  digitalWrite(led,HIGH);
  delay(2000);
  digitalWrite(led,LOW);
  delay(1000);
  
  in_serial.begin(9600);
  out_serial.begin(9600);

///////////Random Test code Area (Starts here)/////////////////////////

///////////Random Test code Area (Ends here)///////////////////////////

}

void loop() {

///////////Random Test code Area (Starts here)/////////////////////////
  
///////////Random Test code Area (Ends here)///////////////////////////

    digitalWrite(led,HIGH);
    while(in_serial.available()==0 && out_serial.available()==0);
    digitalWrite(led,LOW);
    if(out_serial.available()){
    digitalWrite(Int_in,HIGH);
    while(out_serial.available()){
      in_serial.write(out_serial.read());
      delay(3);
    }
    } digitalWrite(Int_in,LOW);
 
   if(in_serial.available()){
    while(in_serial.available()){
      char q = in_serial.read();
      query += q;
      delay(3);
    }
    query.trim();
    digitalWrite(Busy_in,BUSY);
    
    slaveNumber = query.substring(0,1).toInt();
    ID = query.substring(1,3);
    commandType = query.charAt(4);
    int queryLength = query.length() - 1;
    index = 5;
    noOfBoxes = (queryLength / 3) - 1;  
      
    if(noOfBoxes > 1){
      BoxNumber = query.substring(5,7).toInt() - 1;
      for(int i =0 ;i <= noOfBoxes ;i++){
        BoxNumbers[i] = query.substring(index,index+2).toInt();
        index = index + 3;
      }
      for (int i = 0; i < noOfBoxes ; ++i){
        for (int j = i + 1; j < noOfBoxes; ++j){
            if (BoxNumbers[i] > BoxNumbers[j]){
                num =  BoxNumbers[i];
                BoxNumbers[i] = BoxNumbers[j];
                BoxNumbers[j] = num;
            }
        }
      }
      Box = query.substring(5,7);
    }
    
    else if(noOfBoxes == 1){
      BoxNumber = query.substring(5,7).toInt() - 1;
      Box = query.substring(5,7);
    }

    else{
      BoxNumber = 0; // Just to pass the MyCommand Switch Case
      Box = "";
    }
    myCommand = false;
    
    switch(slaveNumber){
      case 0 : if(BoxNumber <= 14){
                BoxNumber = BoxNumber - (15 * slaveNumber);
                myCommand = true;
        } break;
      
      case 1 :  if(BoxNumber >= 15 && BoxNumber <= 29){
                BoxNumber = BoxNumber - (15 * slaveNumber);
                myCommand = true;
        } break;
      
      case 2 : if(BoxNumber >= 30 && BoxNumber <= 44){
                BoxNumber = BoxNumber - (15 * slaveNumber);
                myCommand = true;
        } break;
        
      case 3 :  if(BoxNumber >= 45 && BoxNumber <= 59){
                BoxNumber = BoxNumber - (15 * slaveNumber);
                myCommand = true;
        } break;
      
      case 4 :  if(BoxNumber >= 60 && BoxNumber <= 74){
                BoxNumber = BoxNumber - (15 * slaveNumber);
                myCommand = true;
        } break;
      
      case 5 :  if(BoxNumber >= 75 && BoxNumber <= 89){
                BoxNumber = BoxNumber - (15 * slaveNumber);
                myCommand = true;
        } break;
      
      default: break;
    }

    if(!myCommand){
      query = query.substring(1);
      slaveNumber = slaveNumber + 1;
      forwardQuery = String(slaveNumber);
      forwardQuery.concat(query);
      out_serial.println(forwardQuery);   //send the command to the next Dist board
      QueryForwarded = true;
      COUNTER = 0;
      NoResponse = false;
      while(out_serial.available()==0){   // waiting for a response from next slave
        if(COUNTER>OVERFLOW){
          NoResponse = true;              // No response from the next Slave
          break;
        }
        else COUNTER++;
      }
  
      if(!NoResponse){
        while(out_serial.available()){
          in_serial.write(out_serial.read());
          delay(3);
        }        
      }
      else{
        response = "";
        response = ID;
        response += ":X";               //denoting No response from the next slave
        in_serial.println(response);
        response = "";
      }
    }
    
    else{
    query = query.substring(4);
    commandType = query.charAt(0); 

    switch(commandType)
    {
      ////////////////////////////////////////////// Open Command (starts here)///////////////////////////////////////////////////////////////////
      case 'T': if(Box != ""){                       
                        Open(BoxNumber);
                        // POSITIVE ACKNOWLEDGEMENT FROM DS
                        delay(500);
                        if(checkIfOpen(BoxNumber)){
                          //InterruptCC();
                          response = ID;
                          response += ":B";
                          response += Box;
                          response += 'O';
                          in_serial.println(response);    //Acknowledgement for Door Open
                          response = "";
                        }
                        // NEGATIVE ACKNOWLDEGEMENT FROM DS
                        else{
                          response = "";
                          response = ID; 
                          response += ":N";
                          in_serial.println(response);    //Negative Acknowledgement
                        } break;
                }
  
                else{
                  response = "";
                  response = ID;
                  response += ":SBN";
                  in_serial.println(response);     // Nack to Specify Box Number
                } break;
  
      case 'R': if(Box != "")
                {
                  Open(BoxNumber);
                  // POSITIVE ACKNOWLEDGEMENT FROM DS
                  delay(500);
                  if(checkIfOpen(BoxNumber)){
                    //InterruptCC();
                    response = ID;
                    response += ":B";
                    response += Box;
                    response += 'O';
                    in_serial.println(response);    //Acknowledgement for Door Open
                    response = "";
                    //in_serial.print("\n");
                  }
                  // NEGATIVE ACKNOWLDEGEMENT FROM DS
                  else{                        
                    response = "";
                    response = ID; 
                    response += ":N";
                    //response += Box;
                    in_serial.println(response);    //Negative Acknowledgement
                  } break; 
                }
  
                else{
                  response = "";
                  response = ID;
                  response += ":SBN";
                  in_serial.println(response);     // Nack to Specify Box Number
                } break;                    
      ///////////////////////////////////////////////Open Command (Ends here)/////////////////////////////////////////////////////////////////////
      
      ////////////////////////////////////////////Box Status Query (starts Here)//////////////////////////////////////////////////////////////////
      case 'B': if(Box!="")  // for B command with a BoxNumber
                {
                  response = "";
                  response = ID;
                  response += ":B";
                  if(noOfBoxes > 1){
                      boolean firstIteration1 = true;
                      for(int s = 0 ; s < noOfBoxes; s++){
                          if(firstIteration1) firstIteration1 = false;
                          else response += '&';
                          BoxNumber = BoxNumbers[s] - (15 * slaveNumber) - 1;
                          response += 0;
                          response += BoxNumbers[s];
                          
                          if(checkIfOpen(BoxNumber)){
                            response += 'O';
                          }
                          else{ 
                            if(checkIfEmpty(BoxNumber))
                              response += 'F';
                            else
                              response += 'E';
                          }
                      } response.trim();
                    in_serial.println(response);
                  }
                  
                  else{
                    response += Box;
                    if(checkIfOpen(BoxNumber)){
                      response += 'O';
                    }
                    else{ 
                      if(checkIfEmpty(BoxNumber))
                        response += 'F';
                      else
                        response += 'E';
                    }
                  in_serial.println(response);
                } break;
                    
              }
                else{
                    // Checking Empty states of all boxes and generating response                        
                    boolean firstIteration = true;
                    response = "";
                    response = ID;
                    response += ":B";
                    
                    for(int g = 0; g <= 5; g++){
                      if(firstIteration) firstIteration = false;
                      else response += '&';
                      response += 0;
                      response += g+1;
                      if(checkIfOpen(g)){
                        response += 'O';
                      }
                      else{
                        if(checkIfEmpty(g)) response += 'F';
                        else response += 'E';
                      }
                    } response.trim();
                    in_serial.println(response);
                    break;
                } break;
      ////////////////////////////////////////////////////////Box Status Query (Ends Here)/////////////////////////////////////////////////////
      default:  break;
    }
    }
    // clearing variables for next iteration
    Box = "";
    response= "";
    response1= "";
    commandNext = "";
    ID = "";
    query="";
    forwardQuery="";
    commandType="";
    entryType="";
   }
    digitalWrite(Busy_in,Idle);
}

boolean checkIfEmpty(uint8_t box){
  boolean emptyState = digitalRead(IRPins[box]);
  return emptyState;
}

boolean checkIfOpen(uint8_t box){
  boolean boxState = digitalRead(DPins[box]);
  return boxState;
}

void Open(uint8_t box){
  digitalWrite(MPins[box],HIGH);
  delay(800);
  digitalWrite(MPins[box],LOW);
}
