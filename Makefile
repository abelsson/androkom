JAVAC_ARGS = -encoding iso-8859-1
JAVAC	= javac $(JAVAC_ARGS)
JAVADOC	= javadoc
JAR	= jar
DOCDIR	= apidocs

all:	classes

classes:
	$(JAVAC) nu/dll/lyskom/*.java

apps:	classes snarfkom swingkom kombiff komwho kombot

snarfkom:
	$(JAVAC) nu/dll/app/snarfkom/SnarfKom.java

swingkom:
	$(JAVAC) nu/dll/app/swingkom/*.java

kombiff: 
	$(JAVAC) nu/dll/app/kombiff/*.java

kombot: 
	$(JAVAC) nu/dll/app/kombot/*.java

komwho: 
	$(JAVAC) nu/dll/app/komwho/*.java

komtest:
	$(JAVAC) nu/dll/app/test/*.java

dist:
	$(JAR) -cf lattekom.jar nu/dll/lyskom/*.class

komtest-dist: dist
	$(JAR) -cfm t2.jar manifests/t2-manifest nu/dll/app/test/*.class
	$(JAR) -c0fm lattekom-t2.jar manifests/t2-manifest nu/dll/lyskom/*.class nu/dll/app/test/*.class

tabgui-dist: dist
	$(JAR) -cfm t2-tabgui.jar manifests/t2-manifest nu/dll/app/test/*.class
	$(JAR) -c0fm lattekom-t2-tabgui.jar manifests/tabgui-manifest nu/dll/lyskom/*.class nu/dll/app/test/*.class

swingkom-dist:
	$(JAR) -cfm lattekom-swing.jar manifests/swing-manifest nu/dll/lyskom/*.class nu/dll/app/swingkom/*.class

doc:
	mkdir -p $(DOCDIR) && $(JAVADOC) -public -author -version -windowtitle "LatteKOM API" -d $(DOCDIR) nu/dll/lyskom/*.java

clean:
	find nu/dll/ -type f -name '*.class' -exec rm -f {} \;
