all:
	javac src/*.java
	javac misc/*.java
	javac gui/*.java
	javac Main.java

run:
	java Main

jar:
	jar cfe DotoIP.jar Main Main.class misc/Utils.class src/*.class gui/*.class data/logo.png

documentation:
	javadoc Main.java src/*.java gui/*.java misc/*.java -d docs
