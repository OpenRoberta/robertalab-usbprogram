#!/bin/bash
cd ../OpenRobertaUSB && mvn clean install && cd ../installers
./build.bat
mv OpenRobertaUSBSetupDE.msi OpenRobertaUSBSetupDE.msi
mv OpenRobertaUSBSetupEN.msi OpenRobertaUSBSetupEN.msi