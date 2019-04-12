#!/bin/bash
cd .. && mvn clean install && cd installers
cd windows
./build.bat
mv OpenRobertaUSBSetupDE.msi ..
mv OpenRobertaUSBSetupEN.msi ..
