package org.bbop.apollo

import grails.converters.JSON
import htsjdk.samtools.BAMFileReader
import htsjdk.samtools.BAMIndex
import htsjdk.samtools.BAMIndexMetaData
import htsjdk.samtools.DefaultSAMRecordFactory
import htsjdk.samtools.SAMSequenceRecord
import htsjdk.samtools.SamInputResource
import htsjdk.samtools.SamReader
import htsjdk.samtools.SamReaderFactory
import htsjdk.samtools.ValidationStringency
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

import java.nio.file.FileSystems
import java.nio.file.Path

class BamController {

    def permissionService
    def preferenceService
    def sequenceService
    def projectionService
    def assemblageService
    def trackService
    def bamService

    /**
     *{"features": [

     // minimal required data{ "start": 123, "end": 456 },

     // typical quantitative data{ "start": 123, "end": 456, "score": 42 },

     // Expected format of the single feature expected when the track is a sequence data track.{"seq": "gattacagattaca", "start": 0, "end": 14},

     // typical processed transcript with subfeatures{ "type": "mRNA", "start": 5975, "end": 9744, "score": 0.84, "strand": 1,
     "name": "au9.g1002.t1", "uniqueID": "globallyUniqueString3",
     "subfeatures": [{ "type": "five_prime_UTR", "start": 5975, "end": 6109, "score": 0.98, "strand": 1 },{ "type": "start_codon", "start": 6110, "end": 6112, "strand": 1, "phase": 0 },{ "type": "CDS",         "start": 6110, "end": 6148, "score": 1, "strand": 1, "phase": 0 },

     * @param refSeqName The sequence name
     * @param start The request view start
     * @param end The request view end
     * @return
     */
    JSONObject features(String sequenceName,Long organismId, Integer start, Integer end) {

        JSONObject returnObject = new JSONObject()
        JSONArray featuresArray = new JSONArray()
        returnObject.put(FeatureStringEnum.FEATURES.value, featuresArray)

        Organism organism = Organism.findById(organismId)

        String referer = request.getHeader("Referer")
        String refererLoc = trackService.extractLocation(referer)

        File file
        try {
            file = new File(organism.directory + "/" + params.urlTemplate)
            println "BAM file to read ${file.absolutePath}"
            println "BAM file to read exists ${file.exists()}"
            final SamReader samReader = SamReaderFactory.makeDefault().open(SamInputResource.of(file))

            MultiSequenceProjection projection = projectionService.getProjection(refererLoc, organism)

            if (projection) {
                println "is projectin ${projection}"
                bamService.processProjection(featuresArray, projection, samReader, start, end)
            } else {
                println "NO projectin ${refererLoc}"
                bamService.processSequence(featuresArray, sequenceName, samReader, start, end)
            }
            println "end array ${featuresArray.size()}"
        } catch (e) {
            println "baddness ${e} -> ${file}"
        }

        render returnObject as JSON
    }

    JSONObject regionFeatureDensities(String refSeqName,Long organismId,  Integer start, Integer end, Integer basesPerBin) {

        JSONObject data = permissionService.handleInput(request, params)
        Organism currentOrganism = Organism.findById(organismId)
        if(!currentOrganism){
            String clientToken = request.session.getAttribute(FeatureStringEnum.CLIENT_TOKEN.value)
            currentOrganism = preferenceService.getCurrentOrganismForCurrentUser(clientToken)
        }
        JSONObject trackObject = trackService.getTrackObjectForOrganismAndTrack(currentOrganism, trackName)
        println "params ${params}"

        File file = new File(currentOrganism.directory + "/" + trackObject.urlTemplate)
        File baiFile = new File(currentOrganism.directory + "/" + trackObject.urlTemplate+".bai")

        BAMFileReader bamFileReader = new BAMFileReader(file,baiFile,false, false, ValidationStringency.SILENT, new DefaultSAMRecordFactory())
        JSONObject returnObject = new JSONObject()
//        BAMIndexMetaData metaData = (bamFileReader.index


//        {
//            "bins":  [ 51, 50, 58, 63, 57, 57, 65, 66, 63, 61,
//                       56, 49, 50, 47, 39, 38, 54, 41, 50, 71,
//                       61, 44, 64, 60, 42
//        ],
//            "stats": {
//            "basesPerBin": 200,
//            "max": 88
//        }
//        }
        render returnObject as JSON
    }

    JSONObject global(String trackName, Long organismId) {

        JSONObject data = permissionService.handleInput(request, params)
        Organism currentOrganism = Organism.findById(organismId)
        if(!currentOrganism){
            String clientToken = request.session.getAttribute(FeatureStringEnum.CLIENT_TOKEN.value)
            currentOrganism = preferenceService.getCurrentOrganismForCurrentUser(clientToken)
        }
        JSONObject trackObject = trackService.getTrackObjectForOrganismAndTrack(currentOrganism, trackName)
        println "params ${params}"

        File file = new File(currentOrganism.directory + "/" + trackObject.urlTemplate)
        File baiFile = new File(currentOrganism.directory + "/" + trackObject.urlTemplate+".bai")
        BAMFileReader bamFileReader = new BAMFileReader(file,baiFile,false, false, ValidationStringency.SILENT, new DefaultSAMRecordFactory())
        BAMIndexMetaData[] metaData = BAMIndexMetaData.getIndexStats(bamFileReader)
        println "metadata length: " + metaData.length

//        final SamReader reader = SamReaderFactory.makeDefault().open(file)
        JSONObject returnObject = new JSONObject()

        Long featureCount = 0
        // obviously not
        Long totalLength = Organism.executeQuery("select sum(s.length) from Sequence s where s.organism = :organism",[organism:currentOrganism]).first()
        println "total length ${totalLength}"
//        Integer scoreMin = 0
//        Integer scoreMax = 0
//        Integer scoreMean = 0
//        Double scoreStdEv = 0
        metaData.each {
           featureCount += it.alignedRecordCount + it.unalignedRecordCount
        }
//        {
//
//            "featureDensity": 0.02,
//
//            "featureCount": 234235,
//
//            "scoreMin": 87,
//            "scoreMax": 87,
//            "scoreMean": 42,
//            "scoreStdDev": 2.1
//        }
        returnObject.featureCount = featureCount
        returnObject.featureDensity = (double) featureCount  / (double) totalLength
        println "global BAM ${returnObject as JSON}"

        render returnObject as JSON
    }


    JSONObject region(String refSeqName,Long organismId, Integer start, Integer end) {
        JSONObject data = permissionService.handleInput(request, params)
        Organism currentOrganism = Organism.findById(organismId)
        if(!currentOrganism){
            String clientToken = request.session.getAttribute(FeatureStringEnum.CLIENT_TOKEN.value)
            currentOrganism = preferenceService.getCurrentOrganismForCurrentUser(clientToken)
        }
        Sequence sequence = Sequence.findByNameAndOrganism(refSeqName,currentOrganism)
        JSONObject trackObject = trackService.getTrackObjectForOrganismAndTrack(currentOrganism, trackName)
        println "params ${params}"

        File file = new File(currentOrganism.directory + "/" + trackObject.urlTemplate)
        File baiFile = new File(currentOrganism.directory + "/" + trackObject.urlTemplate+".bai")

        BAMFileReader bamFileReader = new BAMFileReader(file,baiFile,false, false, ValidationStringency.SILENT, new DefaultSAMRecordFactory())
        Long featureCount = bamFileReader.queryAlignmentStart(refSeqName,0).size()
        JSONObject returnObject = new JSONObject()
        returnObject.featureCount = featureCount
        returnObject.featureDensity = (double) featureCount /  (double) sequence.length

//        {
//
//            "featureDensity": 0.02,
//
//            "featureCount": 234235,
//
//            "scoreMin": 87,
//            "scoreMax": 87,
//            "scoreMean": 42,
//            "scoreStdDev": 2.1
//        }
        println "region BAM ${returnObject as JSON}"
        render returnObject as JSON
    }


}
