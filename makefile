main := Main.java
jar := a4.jar

default:
	javac -sourcepath src src/${main} -d out/src
	cd out/src && jar cmf ../../src/manifest.mf ../${jar} *.class

run: default
	java -jar out/${jar}

clean:
	-@rm -f out/src/*.class
	-@rm -f out/${jar}