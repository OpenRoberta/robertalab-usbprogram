#!/bin/bash
cd .. && mvn clean install && cd installers
cp -R linux OpenRobertaUSB
tar -zcvf OpenRobertaUSBLinux.tar.gz OpenRobertaUSB
rm -rf OpenRobertaUSB
