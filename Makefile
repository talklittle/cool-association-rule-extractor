JAVAC  = javac
OUTDIR = $(PWD)/bin
LIBDIR = $(PWD)/src/lib

SRCDIR1 = $(PWD)/src/coms6111/proj3
SRCFILES = $(SRCDIR1)/Apriori.java \
	   $(SRCDIR1)/Bits.java \
	   $(SRCDIR1)/FileReader.java \
	   $(SRCDIR1)/Itemset.java \
	   $(SRCDIR1)/Rule.java

build: $(SRCFILES)
	./build.sh

all: build

exec:
	@echo 'make execYahoo or make exec20newsgroups'

execYahoo:
	./run.sh Yahoo

exec20newsgroups:
	./run.sh 20newsgroups

clean:
	-rm -rf $(OUTDIR)
