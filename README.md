# mongocloud
A simple cloud storage with WebDAV interface.

[Home page and live demo](http://samokhodkin.tk:8080/info/fs.html)

##Building
You need jdk 1.8 and ant. Build script is in the root.
This script produces 2 files:
release/mongogocloud.jar  - the standalone app
release/mongogocloud.war  - the webapp 

##Running
The main class for standalone demo is mongogocloud.server.app.Main.
The classpath should include mongogocloud.jar and all lib/*.jar
To run the webapp demo just deploy mongogocloud.war to the webapp container.

##Architecture
The main classes are:
* mongogocloud.server.fs.MonfoFs - the filesystem implementation
* mongogocloud.server.servlet.BasicAuthFilter - the servlet filer implementing access control
* mongogocloud.server.servlet.FsFilter - the servlet filer implementing a webdav interface to MongoFs

You can combine the above components to implement the needed functionality.
See the Main class above or web.xml as example.