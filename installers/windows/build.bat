"%WIX%bin\candle.exe" resources.wxs setup.wxs
"%WIX%bin\light.exe" -out OpenRobertaUSBSetupDE.msi -ext WixUIExtension -cultures:de-DE setup.wixobj resources.wixobj -b ./resources
"%WIX%bin\light.exe" -out OpenRobertaUSBSetupEN.msi -ext WixUIExtension -cultures:en-US setup.wixobj resources.wixobj -b ./resources
@pause