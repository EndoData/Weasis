# Weasis for EndoData

This is a slightly edited version of Weasis that integrates with EndoData (endodata.fr).

Edited files are

```
/Weasis/weasis-dicom/weasis-dicom-codec/src/main/java/org/weasis/dicom/codec/DicomMediaIO.java
/Weasis/weasis-dicom/weasis-dicom-explorer/src/main/java/org/weasis/dicom/explorer/DicomModel.java
/Weasis/weasis-dicom/weasis-dicom-explorer/src/main/java/org/weasis/dicom/explorer/ImportDicomEndoData.java
/Weasis/weasis-dicom/weasis-dicom-explorer/src/main/java/org/weasis/dicom/explorer/LoadLocalDicom.java
/Weasis/weasis-dicom/weasis-dicom-explorer/src/main/java/org/weasis/dicom/explorer/LocalExport.java
/Weasis/weasis-dicom/weasis-dicom-viewer2d/src/main/java/org/weasis/dicom/viewer2d/EndoDataToolBar.java
```

This version adds another method to call weasis using the Weasis web protocol (weasis://endodataimport), which opens a dicom and exports it as a dicom dir to a specified directory.
It also adds a screenshot button that calls back EndoData using the EndoData web protocol (endodata://weasis/screenshot) to inform it of the new screenshot.

Not the prettiest modifications.

# Building Weasis

I tried with no success to build Weasis locally. Maven dependency errors would block the build (unlike in previous versions).
The build-installers.yml GitHub action workflow works well.
It requires some setup for the macos build :

- I disabled notarisation for now
- I created the correct certificates on developer.apple.com (Developer ID Application, Developer ID Installer).
- The certificates need to be available as an identity (I think ?). To do this I imported them in Keychain Access and I moved them to "login". I also changed the name from "Jeremy Dahan" to "Developer ID Application: Patrick DAHAN".
- Then I exported the certificates (clicking on "Developer ID Application: Patrick DAHAN") choosing .p12
- Then `base64 /Users/jd/Desktop/Certificates.p12 | pbcopy`
- Then I imported them in repo secrets
- The certificate "Developer ID Application: Patrick DAHAN (XXXXXXXXXX)" goes to MACOS_CERTIFICATE_DEVELOPMENT
- The certificate "Developer ID Installer: Patrick DAHAN (XXXXXXXXXX)" goes to MACOS_CERTIFICATE_INSTALLER
- The password goes to MACOS_CERTIFICATE_PWD
- Finally, the string "Developer ID Application: Patrick DAHAN (XXXXXXXXXX)" goes into MACOS\_\_DEVELOPER_ID
- Then hit run and get the artifacts from the run summary

# archives

Archives from the previous edited version (based on 3.6.3).

## Building Weasis for EndoData

Installing jdk 16 from OpenJDK https://jdk.java.net/16/

Cloning Weasis from git.

```bash
cd Weasis;
mvn clean install;
mvn clean install -Dportable=true -P compressXZ -f weasis-distributions
```

Next unzip `target/portable-dist/weasis-portable.zip` into `target/portable-dist/weasis-portable`

```bash
unzip ./weasis-distributions/target/portable-dist/weasis-portable.zip -d ./weasis-distributions/target/portable-dist/weasis-portable
```

and then

```bash
cd ./weasis-distributions/script/; ./package-weasis.sh -i ../target/portable-dist/weasis-portable/ -o ../target/installer --jdk /Library/Java/JavaVirtualMachines/jdk-16.jdk/Contents/Home --mac-signing-key-user-name 'Patrick DAHAN (UXNTLMKH4Y)'
```

In doubt, refer to

https://github.com/nroduit/Weasis/blob/58b23fd46b429190e145740761f490b2375ebdd0/.github/workflows/build-installer.yml

or more recent

```bash
mvn clean install; mvn clean install -Dportable=true -P compressXZ -f weasis-distributions; unzip ./weasis-distributions/target/portable-dist/weasis-portable.zip -d ./weasis-distributions/target/portable-dist/weasis-portable; cd ./weasis-distributions/script/; ./package-weasis.sh -i ../target/portable-dist/weasis-portable/ -o ../target/installer --jdk /Library/Java/JavaVirtualMachines/jdk-16.jdk/Contents/Home --mac-signing-key-user-name 'Patrick DAHAN (UXNTLMKH4Y)'; cd ../../
```

My workflow is thus :

- Make edit
- `mvn clean install;` and check for errors
- `mvn clean install -Dportable=true -P compressXZ -f weasis-distributions; unzip ./weasis-distributions/target/portable-dist/weasis-portable.zip -d ./weasis-distributions/target/portable-dist/weasis-portable; cd ./weasis-distributions/script/; ./package-weasis.sh -i ../target/portable-dist/weasis-portable/ -o ../target/installer --jdk /Library/Java/JavaVirtualMachines/jdk-16.jdk/Contents/Home --mac-signing-key-user-name 'Patrick DAHAN (UXNTLMKH4Y)'; cd ../../`
- Launch the resulting app `/Users/jd/Sync/EndoDataSync/dev/Weasis/weasis-distributions/target/installer/Weasis.app/Contents/MacOS/Weasis` to see the logs

## On Windows

Install Cygwin (with menu shortcut)
You also need dos2unix, which can be installed via chocolatey :
choco install dos2unix (from an admin PS)

dos2unix package-weasis.sh
in weasis-distributions/script

Also install Wix and add to path

download jdk 16, and place it in /home in cygwin.

mvn clean install; mvn clean install -Dportable=true -P compressXZ -f weasis-distributions;
mkdir ./weasis-distributions/target/portable-dist/weasis-portable;
/cygdrive/c/Windows/System32/tar.exe -xf ./weasis-distributions/target/portable-dist/weasis-portable.zip -C ./weasis-distributions/target/portable-dist/weasis-portable

You might need to remove the version suffix, leaving only 3.6.3 for instance.
cd ./weasis-distributions/script/
./package-weasis.sh -i ../target/portable-dist/weasis-portable/ -o ../target/installer --jdk /home/jd/jdk-16

I messed with the script to remove the version string errors and the build went fine. modify the script directly to set the version.
I did the last line in bash in a windows terminal (with powershell)
