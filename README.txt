############################ Mobile VLE OKTech ############################
 
The main class in this project is the VLEHandlerOKTechImpl which implements the VLEHandler class 
from the Mobile VLE Core project. The implementation is a SOAP client for the Moodle OK Tech Web 
Service Plug-in. The VLEHandlerOKTechImpl loads itself into the VLEHandlerFactory in the Mobile
VLE Core project from where it can be accessed by other project such as the Android front end.

This project depends on the Mobile VLE Core project, see https://github.com/johnhunsley/Mobile-VLE-Core
so you will need to either check out that and build it, or download the jar and install it into your local 
maven repository.

This project is build with Maven, see see http://maven.apache.org/

Once you have installed the Mobile VLE Core jar you check this project out into a directory e.g. ~/Mobile-VLE-OKTech

From the command line cd into that directory, the pom.xml and src/ dir should be present in this
directory, then issue the following 

mvn clean install

You'll end up with a target/ directory in the same directory and the MobileVLE_OKTech-1.0.jar which 
will also be installed in your local maven repo.


Now you can build the Android front end app.

email john@vlemobile.com or jphunsley@gmail.com for more help  :-)

