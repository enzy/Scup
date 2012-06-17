# Scup - Simple screenshot & file uploader [![Flattr this project](http://api.flattr.com/button/flattr-badge-large.png)](https://flattr.com/submit/auto?user_id=matejsimek&url=https://github.com/enzy/Scup&title=Scup&language=en_GB&tags=github&category=software&description=Scup - Simple screenshot and file uploader) 

*Easily upload screenshot or files to FTP server and copy its URL address to clipboard.* 

## Current features ##

* Run in system tray
* Monitor system clipboard for changes (mostly Print Screen event) with image content
* Crop image
* Upload cropped image to FTP (or save to disk)
* Copy image URL to clipboard

## How to run Scup ##

* Check that you have [Java JRE installed](http://java.com/en/download/installed.jsp?detect=jre&try=1) 
* Download [.jar or .exe from /dist directory](https://github.com/enzy/Scup/tree/master/dist)
* On Windows simple run `Scup.exe` 
* On other platforms run `java -jar Scup-0.1.jar`

## Known issues ##

* Cannot connect to FTP server on Windows, [explanation on stackoverflow](http://stackoverflow.com/questions/6990663/java-7-prevents-ftp-transfers-on-windows-vista-and-7-if-firewall-is-on-any-idea)
 * Local solution: `netsh advfirewall set global StatefulFTP disable`
 * JVM solution: `-Djava.net.preferIPv4Stack=true`
* Big memory consumption (needs optimalization)
