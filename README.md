# robertalab-usbprogram

[![Build Status](https://travis-ci.org/OpenRoberta/robertalab-usbprogram.svg?branch=develop)](https://travis-ci.org/OpenRoberta/robertalab-usbprogram)

Standalone program for connecting robot hardware to the Open Roberta lab using
an usb connection.

### Fast installation with maven

#### Clone the repository and compile
    git clone git://github.com/OpenRoberta/robertalab-usbprogram.git
    cd robertalab-usbprogram
    mvn clean install

### Run USB program
For running the USB program use Java.

    java -jar target/OpenRobertaUSB-*.jar

### Development notes

You can follow the test status on https://travis-ci.org/OpenRoberta/.

Development happens in the `develop` branch. Please sent PRs against that
branch.

    git clone git://github.com/OpenRoberta/robertalab-usbprogram.git
    cd robertalab-usbprogram
    git checkout -b develop origin/develop
    
### Installer creation
Linux:
- run `release.sh linux` in the `installers` directory
- add the version to the resulting file

Windows:
- download [WiX Toolset](https://github.com/wixtoolset/wix3/releases)
- download [WDK 8.1](https://www.microsoft.com/en-us/download/details.aspx?id=42273)
- run `release.sh windows` in the `installers` directory (e.g. in Git Bash)
- add the version to the resulting files

Mac:
- run `release.sh osx` in the `installers` directory
- add the version to the resulting file
