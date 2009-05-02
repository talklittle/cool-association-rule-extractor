
JAVAC  = javac
OUTDIR = $(PWD)/bin
LIBDIR = $(PWD)/src/lib

SRCDIR1 = $(PWD)/src/coms6111/proj3
SRCFILES = $(SRCDIR1)/Apriori.java$(SRCDIR1)/Bits.java \
	   $(SRCDIR1)/FileReader.java$(SRCDIR1)/IndexFile.java \
	   $(SRCDIR1)/Itemset.java\

build: $(SRCFILES)
	./build.sh $(OUTDIR)

all: build

exec:
	./run.sh $(LIBDIR)

clean:
	-rm -rf $(OUTDIR)
