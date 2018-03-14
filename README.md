# CFRQ Blacklist API


### Introduction
The CFRQ IP Blacklist, implemented as an Apache Camel Route.

### Build
Run the maven packager:

	mvn package

### Run

Deploy the application into Tomcat by copying the `.war` to
`/var/lib/tomcat8/webapps` (for example).

And then navigate to:

	http://localhost:8080/blacklist-camel
<http://localhost:8080/blacklist-camel>

A test route is available at:

	http://localhost:8080/blacklist-camel/say/hello
<http://localhost:8080/blacklist-camel/say/hello>
