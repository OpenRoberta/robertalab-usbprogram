#!/bin/bash

dir="$( pwd )"

echo "[Desktop Entry]" > "/usr/share/applications/ORUSB.desktop"
echo "Version=${version}" >> "/usr/share/applications/ORUSB.desktop"
echo "Name=Open Roberta USB" >> "/usr/share/applications/ORUSB.desktop"
echo "Exec=java -jar -Dfile.encoding=utf-8 $dir/${artifactId}.jar" >> "/usr/share/applications/ORUSB.desktop"
echo "Path=$dir" >> "/usr/share/applications/ORUSB.desktop"
echo "Icon=$dir/OR.png" >> "/usr/share/applications/ORUSB.desktop"
echo "Terminal=false" >> "/usr/share/applications/ORUSB.desktop"
echo "Type=Application" >> "/usr/share/applications/ORUSB.desktop"
#echo "StartupNotify=True" >> "/usr/share/applications/ORUSB.desktop"
echo "Categories=Application;Development;" >> "/usr/share/applications/ORUSB.desktop"

chmod u+x "/usr/share/applications/ORUSB.desktop"

gpasswd -a ${SUDO_USER:-$USER} dialout
udevadm control --reload-rules

#useradd ${SUDO_USER:-$USER} dialout
exec su -l ${SUDO_USER:-$USER}
