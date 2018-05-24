;This file will be executed next to the application bundle image
;I.e. current directory will contain folder KKDystrack-Core with application files

#define VERSION "0.9.7"

[Setup]
AppId={{fxApplication}}
AppName=KKDystrack-Core
AppVersion={#VERSION}
AppVerName=KKDystrack-Core {#VERSION}
AppPublisher=Dystify
AppComments=KKDystrack2
AppCopyright=Copyright (C) 2018
;AppPublisherURL=http://java.com/
;AppSupportURL=http://java.com/
;AppUpdatesURL=http://java.com/
UsePreviousAppDir=no
DefaultDirName={sd}\KKDystrack-Core
DisableStartupPrompt=Yes
DisableDirPage=Yes
DisableProgramGroupPage=Yes
DisableReadyPage=Yes
DisableFinishedPage=Yes
DisableWelcomePage=Yes
DefaultGroupName=Dystify
;Optional License
LicenseFile=
;WinXP or above
MinVersion=0,5.1 
OutputBaseFilename=KKDystrack-Core-{#VERSION}
Compression=lzma
LZMANumBlockThreads=10
LZMABlockSize=1024
CompressionThreads=2
SolidCompression=yes
PrivilegesRequired=admin
SetupIconFile=KKDystrack-Core\KKDystrack-Core.ico
UninstallDisplayIcon={app}\KKDystrack-Core.ico
UninstallDisplayName=KKDystrack-Core
WizardImageStretch=No
WizardSmallImageFile=KKDystrack-Core-setup-icon.bmp   
ArchitecturesInstallIn64BitMode=x64


[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "KKDystrack-Core\KKDystrack-Core.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "KKDystrack-Core\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\KKDystrack-Core"; Filename: "{app}\KKDystrack-Core.exe"; IconFilename: "{app}\KKDystrack-Core.ico"; Check: returnTrue()
Name: "{commondesktop}\KKDystrack-Core"; Filename: "{app}\KKDystrack-Core.exe";  IconFilename: "{app}\KKDystrack-Core.ico"; Check: returnFalse()


[Run]
Filename: "{app}\KKDystrack-Core.exe"; Parameters: "-Xappcds:generatecache"; Check: returnFalse()
Filename: "{app}\KKDystrack-Core.exe"; Description: "{cm:LaunchProgram,KKDystrack-Core}"; Flags: nowait postinstall skipifsilent; Check: returnTrue()
Filename: "{app}\KKDystrack-Core.exe"; Parameters: "-install -svcName ""KKDystrack-Core"" -svcDesc ""KKDystrack-Core"" -mainExe ""KKDystrack-Core.exe""  "; Check: returnFalse()

[UninstallRun]
Filename: "{app}\KKDystrack-Core.exe "; Parameters: "-uninstall -svcName KKDystrack-Core -stopOnUninstall"; Check: returnFalse()

[Code]
function returnTrue(): Boolean;
begin
  Result := True;
end;

function returnFalse(): Boolean;
begin
  Result := False;
end;

function InitializeSetup(): Boolean;
begin
// Possible future improvements:
//   if version less or same => just launch app
//   if upgrade => check if same app is running and wait for it to exit
//   Add pack200/unpack200 support? 
  Result := True;
end;  
