#!/bin/bash
cd ../OpenRobertaUSB && mvn clean install && cd ../installers
cd osx && ./appify start.sh OpenRobertaUSB
mkdir -p package/ORUSB.pkg/Payload/Applications/OpenRobertaUSB.app/
cp -a resources OpenRobertaUSB.app/Contents/ 
cp -a OpenRobertaUSB.app package/ORUSB.pkg/Payload/Applications/
pkgutil --flatten-full package OpenRobertaUSBMacOSX.pkg
mv OpenRobertaUSBMacOSX.pkg ..
rm -rf OpenRobertaUSB.app
rm -rf package