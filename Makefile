JAVA=java
JAVAC=javac

# School Server Makefile Configurations
JFLEX=jflex
CLASSPATH=-cp /usr/share/java/cup.jar:.
CUP=cup

all: CM.class

CM.class: absyn/*.java parser.java sym.java Lexer.java ShowTreeVisitor.java Scanner.java CM.java SemanticAnalyzer.java CodeGenerator.java

%.class: %.java
	$(JAVAC) $(CLASSPATH) $^

Lexer.java: CM.flex
	$(JFLEX) CM.flex

parser.java: CM.cup
	#$(CUP) -dump -expect 3 CM.cup
	$(CUP) -expect 3 CM.cup

clean:
	rm -f parser.java Lexer.java sym.java *.class absyn/*.class test/*.abs test/*.sym test/*.tm *~