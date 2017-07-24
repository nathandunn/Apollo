package org.bbop.apollo

import edu.unc.genomics.io.BigWigFileReader
import grails.converters.JSON
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

import java.nio.file.FileSystems
import java.nio.file.Path

class BigwigController {

    def permissionService
    def preferenceService
    def sequenceService
    def projectionService
    def assemblageService
    def bigwigService
    def trackService

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
    JSONObject features(String sequenceName, Long organismId, Integer start, Integer end) {

        JSONObject data = permissionService.handleInput(request, params)
        Organism organism = Organism.findById(organismId)
        JSONObject returnObject = trackService.getBigWigFromCache(organism, sequenceName, start, end, params.urlTemplate) ?: new JSONObject()
//        JSONObject returnObject = new JSONObject()
        if (returnObject.containsKey(FeatureStringEnum.FEATURES.value)) {
            render returnObject as JSON
        }
        JSONArray featuresArray = new JSONArray()
        returnObject.put(FeatureStringEnum.FEATURES.value, featuresArray)

        BigWigFileReader bigWigFileReader
        Path path
        try {
            File file = new File(organism.directory + "/" + params.urlTemplate)
            path = FileSystems.getDefault().getPath(file.absolutePath)
            // TODO: should cache these if open
            bigWigFileReader = new BigWigFileReader(path)

            MultiSequenceProjection projection = projectionService.getProjection(sequenceName, organism)

            if (projection) {
                bigwigService.processProjection(featuresArray, projection, bigWigFileReader, start, end)
            } else {
                bigwigService.processSequence(featuresArray, sequenceName, bigWigFileReader, start, end)
            }
//            trackService.cacheBam(returnObject,organism,sequenceName,start,end,params.urlTemplate)
            log.debug "end bigwith featrues array size ${featuresArray.size()}"
        } catch (e) {
            log.error "Error retrieving bigwig features ${e} -> ${path}"
        }

        render returnObject as JSON
    }

    JSONObject region(String refSeqName, Long organismId, Integer start, Integer end) {
        render new JSONObject() as JSON
    }

    JSONObject regionFeatureDensities(String refSeqName, Long organismId, Integer start, Integer end, Integer basesPerBin) {
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
        render new JSONObject() as JSON
    }


    JSONObject global(String trackName, Long organismId) {
        JSONObject data = permissionService.handleInput(request, params)
        Organism currentOrganism = Organism.findById(organismId)

        if (!currentOrganism) {
            String clientToken = request.session.getAttribute(FeatureStringEnum.CLIENT_TOKEN.value)
            currentOrganism = preferenceService.getCurrentOrganismForCurrentUser(clientToken)
        }
        JSONObject trackObject = trackService.getTrackObjectForOrganismAndTrack(currentOrganism, trackName)

        JSONObject returnObject = new JSONObject()
        Path path = FileSystems.getDefault().getPath(currentOrganism.directory + "/" + trackObject.urlTemplate)

        BigWigFileReader bigWigFileReader = new BigWigFileReader(path)
        returnObject.put("scoreMin", bigWigFileReader.min())
        returnObject.put("scoreMax", bigWigFileReader.max())
        returnObject.put("scoreMean", bigWigFileReader.mean())
        returnObject.put("scoreStdDev", bigWigFileReader.stdev())
        returnObject.put("featureCount", bigWigFileReader.numBases())
        returnObject.put("featureDensity", 1)
        render returnObject as JSON
    }

}
