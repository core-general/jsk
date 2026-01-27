## To build all modules:

if java_home.txt exists, then:
export JAVA_HOME=$(cat java_home.txt); mvn -T 16 clean verify;
else:
mvn -T 16 clean verify;

## To deploy test server with features

In z-module-4-experiments-only check pom file, that inside the build section <build> everything is uncommented. If commented -
uncomment it. Then run "build all modules" as described above.

Then from root "cd z-module-4-experiments-only/target", and then "java -Xmx500m -Dspring.profiles.active=default -jar
z-module-test.jar", it will start test server.
When it starts successfully you will see something like:
"""
2026-01-27 11:04:39.043 INFO [           main] org.eclipse.jetty.server.Server     : Started @9148ms
2026-01-27 11:04:39.043 INFO [           main] s.w.s.spark.WebJettyServerStarter   : Jetty started on port:8088
2026-01-27 11:04:39.044 INFO [           main] sk.spring.SpringApp                 :
Hello!
"""

Now you could interact in browser by this port with the web server using the endponts available through interfaces mentioned in
the log
2026-01-27 11:04:39.036 INFO [           main] sk.web.server.WebServerCore         :
API: jsk.spark.TestApi1
...

You could investigate how web server uses the interfaces of API classes in WebServerCore.class.