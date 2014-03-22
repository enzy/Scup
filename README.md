# Scup - Simple screenshot & file uploader

*Easily upload screenshot or files to server and copy its URL address to clipboard.*

## Current features ##

* Run in system tray
* Monitor system clipboard for changes of image or file content (Print Screen, file copy, etc.)
* Compress multiple files into single .zip
* Crop image with rectangular selection
* Upload files to: FTP, Dropbox or save to disk
* Copy file URL to clipboard
* Check for updates on start

## How to run Scup ##

* Check that you have [Java JRE installed](http://java.com/en/download/installed.jsp?detect=jre&try=1)
* Download [Scup.exe](https://github.com/enzy/Scup/raw/master/dist/Scup.exe) or [Scup.jar](https://github.com/enzy/Scup/raw/master/dist/Scup.jar)
* On Windows simple run `Scup.exe`
* Other platforms are not supported, but you can hack on source code.

## Support and contribution ##

Feel free to [create issue](https://github.com/enzy/Scup/issues/new) with your problem, request, idea or question!
Also fork this repository whenever you like, code some changes and create pull request when done.

**Donations are welcome:**

[![Donate this project](https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=J2XC575WFNHA4&lc=US&item_name=Scup%20%2d%20Simple%20screenshot%20and%20file%20uploader&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donate_SM%2egif%3aNonHosted)
[![Flattr this project](http://api.flattr.com/button/flattr-badge-large.png)](https://flattr.com/submit/auto?user_id=matejsimek&url=https://github.com/enzy/Scup&title=Scup&language=en_GB&tags=github&category=software&description=Scup - Simple screenshot and file uploader)
[![Gittip](http://img.shields.io/gittip/enzy.svg)](https://www.gittip.com/enzy/)

## Known issues ##

* Cannot connect to FTP server on Windows, [explanation on stackoverflow](http://stackoverflow.com/questions/6990663/java-7-prevents-ftp-transfers-on-windows-vista-and-7-if-firewall-is-on-any-idea)
 * Local solution: `netsh advfirewall set global StatefulFTP disable`
 * JVM solution: `-Djava.net.preferIPv4Stack=true` (used in .exe wrapper)
* Big memory consumption (needs optimalization)

## License ##

<span xmlns:dct="http://purl.org/dc/terms/" href="http://purl.org/dc/dcmitype/InteractiveResource" property="dct:title" rel="dct:type">Scup</span> by <a xmlns:cc="http://creativecommons.org/ns#" href="https://github.com/enzy/Scup" property="cc:attributionName" rel="cc:attributionURL">Matěj Šimek</a> is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-nc-sa/3.0/">Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License</a>.

<a rel="license" href="http://creativecommons.org/licenses/by-nc-sa/3.0/"><img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-nc-sa/3.0/88x31.png" /></a>
