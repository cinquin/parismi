.DEFAULT_GOAL := all

#For now, prevent removal of target files whose build was not explicitly requested,
#in case they are needed in the downstream analysis.
.SECONDARY :

include ../Makefile_includes
include makefile_includes/*

all : $(automaticSeeding) $(djikstraDistance) test

test : output/backbone+volume+topLayer+fluorescenceQuantification+pearsonEdU+segmentation.proto
	$(shell (((${PIPELINE_SINGLE} check_output.xml) && echo SUCCESS) || (echo FAILURE)) >&2) 

noAutomaticSeeding : $(djikstraDistance)

cleanTables : 
	-rm -f tables/*.xml
	-rm -f tables/userAdjusted/*.xml

cleanMakeFile :
	-rm -f Makefile

cleanIndividualSegmentations : 
	-rm -f output/individualSegmentations/*

clean : cleanIndividualSegmentations
	-rm -f output/*.tif
	-rm -f output/*.proto
	-rm -f output/*.txt
	-rm -f pipeline_log.txt
