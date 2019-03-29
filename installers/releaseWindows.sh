#!/bin/bash
cd ../OpenRobertaUSB && mvn clean install && cd ../installers
cd windows
./build.bat
mv OpenRobertaUSBSetupDE.msi ..
mv OpenRobertaUSBSetupEN.msi ..