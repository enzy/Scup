# Scup - Simple screenshot & file uploader #

*Easily upload screenshot or files to FTP server and copy its URL address to clipboard.*

## Known issues ##

* Cannot connect to FTP server on Windows, [explanation on stackoverflow](http://stackoverflow.com/questions/6990663/java-7-prevents-ftp-transfers-on-windows-vista-and-7-if-firewall-is-on-any-idea)
 * Local solution: `netsh advfirewall set global StatefulFTP disable`
 * JVM solution: `-Djava.net.preferIPv4Stack=true`
