JAVAC	= jikes
JAVADOC	= javadoc
DOCDIR	= apidocs

all:	classes

classes:
	$(JAVAC) com/micoco/lyskom/*.java

apps:	classes snarfkom swingkom kombiff komwho

snarfkom:
	$(JAVAC) com/micoco/app/snarfkom/SnarfKom.java

swingkom:
	$(JAVAC) com/micoco/app/swingkom/*.java

kombiff:
	$(JAVAC) com/micoco/app/kombiff/*.java

komwho:
	$(JAVAC) com/micoco/app/komwho/*.java


doc:
	mkdir -p $(DOCDIR) && $(JAVADOC) -d $(DOCDIR) com/micoco/lyskom/*.java

