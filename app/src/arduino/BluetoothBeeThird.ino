// For programming over FTDI, remove the BluetoothBee module

// Thermometer DS1820 with datapin on D2

#include <OneWire.h>
#include <DallasTemperature.h>

// serial port
long DATARATE = 38400;  

const uint8_t dallasPin = 2;

char inChar = 0;
int counter = 0;

OneWire oneWire(dallasPin);
DallasTemperature sensors(&oneWire);

void setup() {

  // Dallas DS1820 setup
  sensors.begin();
  sensors.setResolution(TEMP_12_BIT); // Genauigkeit auf 12-Bit setzen
  sensors.setWaitForConversion(true);

  // bluetooth bee setup
  Serial.begin(38400);  // default data rate for BT Bee
  Serial.print("\r\n+STWMOD=0\r\n");     // set to slave
  delay(1000);
  Serial.print("\r\n+STNA=Fio Bluetooth Bee\r\n");
  delay(1000);
  Serial.print("\r\n+STAUTO=0\r\n");     // don't permit auto-connect
  delay(1000);
  Serial.print("\r\n+STOAUT=1\r\n");     // existing default
  delay(1000);
  Serial.print("\r\n +STPIN=0000\r\n");  // existing default
  delay(2000);  // required

  // initiate BTBee connection
  Serial.print("\r\n+INQ=1\r\n");
  delay(2000);   // wait for pairing

  pinMode(LED_BUILTIN, OUTPUT);
}

void loop() {

  // test app:
  //   wait for character,
  //   a returns message, h=led on, l=led off
  if (Serial.available()) {
    inChar = Serial.read();

    if (inChar == 'a') {
      Serial.print("connected");  // test return connection
    }

    if (inChar == 'h') {
      digitalWrite(LED_BUILTIN, HIGH);   // on
    }

    if (inChar == 'l') {
      digitalWrite(LED_BUILTIN, LOW);    // off
    }

    // Measure temperature
    if (inChar == 't') {
      sensors.requestTemperatures(); // Send the command to get temperatures

      float tempDallas1 = sensors.getTempCByIndex(0);
      Serial.println(tempDallas1);
    }
  }
}

