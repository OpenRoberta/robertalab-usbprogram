#!/bin/bash
# release - Release and installer creation script for the Open Roberta Lab connection program

CURRENT_TAG=$(git describe --tags)


usage() {
    echo "Usage: release <linux/windows/osx>"
    echo ""
    echo "Specify the operating system that a release should be created for."
    exit
}

create_linux() {
    echo "Creating Linux package"
    cp -R linux OpenRobertaUSB
    tar -zcvf OpenRobertaUSBLinux-$CURRENT_TAG.tar.gz OpenRobertaUSB
    rm -rf OpenRobertaUSB
}

create_windows() {
    echo "Creating Windows installers"
    cd windows
    ./build.bat
    mv OpenRobertaUSBSetupDE-$CURRENT_TAG.msi ..
    mv OpenRobertaUSBSetupEN-$CURRENT_TAG.msi ..
}

create_osx() {
    echo "Creating OSX package"
    cd osx && ./appify start.sh OpenRobertaUSB
    mkdir -p package/ORUSB.pkg/Payload/Applications/OpenRobertaUSB.app/
    cp -a resources OpenRobertaUSB.app/Contents/ 
    cp -a OpenRobertaUSB.app package/ORUSB.pkg/Payload/Applications/
    pkgutil --flatten-full package OpenRobertaUSBMacOSX-$CURRENT_TAG.pkg
    mv OpenRobertaUSBMacOSX-$CURRENT_TAG.pkg ..
    rm -rf OpenRobertaUSB.app
}


if [ "$1" == "" ]; then
    usage
else
    case $1 in
        linux|windows|osx)
            echo "Building the project for version $CURRENT_TAG"
            cd .. && mvn clean install && cd installers
            ;;
        *)
            usage
    esac
    case $1 in
        linux) create_linux ;;
        windows) create_windows ;;
        osx) create_osx ;;
    esac
fi
echo "Release finished"
