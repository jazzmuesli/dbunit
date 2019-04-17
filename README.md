

	wget http://www.datanucleus.org/downloads/maven2/oracle/ojdbc6/11.2.0.3/ojdbc6-11.2.0.3.jar
	mvn install:install-file -Dfile=ojdbc6-11.2.0.3.jar -DgroupId=oracle -DartifactId=ojdbc6 -Dversion=11.2.0.3 -Dpackaging=jar
	mvn package

