JAVAC	= javac
JAVADOC	= javadoc
DOCDIR	= apidocs

all:	classes

classes:
	$(JAVAC) nu/dll/lyskom/*.java

apps:	classes snarfkom swingkom kombiff komwho

snarfkom:
	$(JAVAC) nu/dll/app/snarfkom/SnarfKom.java

swingkom:
	$(JAVAC) nu/dll/app/swingkom/*.java

kombiff:
	$(JAVAC) nu/dll/app/kombiff/*.java

komwho:
	$(JAVAC) nu/dll/app/komwho/*.java


doc:
	mkdir -p $(DOCDIR) && $(JAVADOC) -d $(DOCDIR) nu/dll/lyskom/*.java

