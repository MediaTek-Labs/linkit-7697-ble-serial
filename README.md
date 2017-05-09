# LinkIt 7697 BLE serial
How to use BLE to do serial communication on LinkIt 7697 HDK

Additinal resource can be found at https://docs.labs.mediatek.com/resource/mt7687-mt7697

## Folder Structure

* `android/ble_serial`: Android studio project files.
* `project/linkit7697_hdk/apps/ble_serial`: LinkIt SDK project files.

## How to Build

### Device Side

* Put/Extract the files into SDK root, so that there is `[SDK_root]/project/linkit7697_hdk/apps/ble_serial`
* Execute `./build.sh linkit7697_hdk ble_serial` under Linux enviornment
* Check generated bin at `[SDK_root]/out/linkit7697_hdk/ble_serial/GCC/build/ble_serial.bin`
* Use Flashtool to download `[SDK_root]/out/linkit7697_hdk/ble_serial/GCC/build/flash_download.ini` into LinkIt 7697 HDK

### Mobile Side

* Extract `android/ble_serial` to anywhere you prefer
* Have Android studio open the project
* Build->Make Project
* Run->Run Application

## How to Run

### Phases

* A. Turn on Device, when it is ready, it will start "BLE Advertising" (appear as BLE_SERIAL)
* B. Launch Android App, Scan and connects to BLE_SERIAL Device. After connected, the state should become "Connected"
* C. Type some text and press "Send", the reversed text should be received and displayed below

### Mobile Side

![Mobile](/images/mobile_side.png)

### Device Side

Below are log output from UART port of LinkIt 7697 HDK
![Device_A](/images/device_A.png)
![Device_BC](/images/device_BC.png)

## Message Sequence Chart

![MSC](/images/msc.png)


