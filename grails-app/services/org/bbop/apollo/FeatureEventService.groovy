package org.bbop.apollo

import grails.async.Promise
import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.event.AnnotationEvent
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.history.FeatureOperation
import grails.util.Environment
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import static grails.async.Promises.*


/**
 */
@Transactional
class FeatureEventService {

    def permissionService
    def featureService
    def requestHandlingService

    /**
     *
     * @param featureOperation
     * @param geneName
     * @param transcriptUniqueName
     * @param commandObject
     * @param jsonObject
     * @param user
     * @return
     */
    FeatureEvent addNewFeatureEvent(FeatureOperation featureOperation, String geneName, String transcriptUniqueName, JSONObject commandObject, JSONObject jsonObject, User user) {
        if (Environment.current == Environment.TEST) {
            return addNewFeatureEventWithUser(featureOperation, geneName, transcriptUniqueName, commandObject, jsonObject, (User) null)
        }
        addNewFeatureEventWithUser(featureOperation, geneName, transcriptUniqueName, commandObject, jsonObject, user)
    }

    FeatureEvent addNewFeatureEventWithUser(FeatureOperation featureOperation, String name, String uniqueName, JSONObject commandObject, JSONObject jsonObject, User user) {
        JSONArray newFeatureArray = new JSONArray()
        newFeatureArray.add(jsonObject)
        return addNewFeatureEvent(featureOperation, name, uniqueName, commandObject, new JSONArray(), newFeatureArray, user)

    }

    /**
     * Convention is that 1 is the parent and is returned first in the array.
     * Because we are tracking the split in the actual object blocks, the newJSONArray is also split
     * @param name1
     * @param uniqueName1
     * @param name2
     * @param uniqueName2
     * @param commandObject
     * @param oldFeatureObject
     * @param newFeatureArray
     * @param user
     * @return
     */
    List<FeatureEvent> addSplitFeatureEvent(String name1, String uniqueName1, String name2, String uniqueName2
                                            , JSONObject commandObject, JSONObject oldFeatureObject
                                            , JSONArray newFeatureArray
                                            , User user) {
        List<FeatureEvent> featureEventList = new ArrayList<>()
        JSONArray oldFeatureArray = new JSONArray()
        oldFeatureArray.add(oldFeatureObject)

        List<FeatureEvent> lastFeatureEventList = findCurrentFeatureEvent(uniqueName1)
        if (lastFeatureEventList.size() != 1) {
            throw new AnnotationException("Not one current feature event being split for: " + uniqueName1)
        }
        if (!lastFeatureEventList) {
            throw new AnnotationException("Can not find original feature event to split for " + uniqueName1)
        }
        FeatureEvent lastFeatureEvent = lastFeatureEventList[0]
        lastFeatureEvent.current = false;
        lastFeatureEvent.save()
        deleteFutureHistoryEvents(lastFeatureEvent)

        Date addDate = new Date()

        JSONArray newFeatureArray1 = new JSONArray()
        JSONArray newFeatureArray2 = new JSONArray()

        newFeatureArray1.add(newFeatureArray.getJSONObject(0))
        newFeatureArray2.add(newFeatureArray.getJSONObject(1))

        FeatureEvent featureEvent1 = new FeatureEvent(
                editor: user
                , name: name1
                , uniqueName: uniqueName1
                , operation: FeatureOperation.SPLIT_TRANSCRIPT
                , current: true
                , originalJsonCommand: commandObject.toString()
                , newFeaturesJsonArray: newFeatureArray1.toString()
                , oldFeaturesJsonArray: oldFeatureArray.toString()
                , dateCreated: addDate
                , lastUpdated: addDate
        ).save()

        FeatureEvent featureEvent2 = new FeatureEvent(
                editor: user
                , name: name2
                , uniqueName: uniqueName2
                , operation: FeatureOperation.SPLIT_TRANSCRIPT
                , current: true
                , originalJsonCommand: commandObject.toString()
                , newFeaturesJsonArray: newFeatureArray2.toString()
                , oldFeaturesJsonArray: oldFeatureArray.toString()
                , dateCreated: addDate
                , lastUpdated: addDate
        ).save()

        lastFeatureEvent.childId = featureEvent1.id
        lastFeatureEvent.childSplitId = featureEvent2.id
        featureEvent2.parentId = lastFeatureEvent.id
        featureEvent1.parentId = lastFeatureEvent.id

        featureEvent1.save()
        featureEvent2.save()
        lastFeatureEvent.save()

        featureEventList.add(featureEvent1)
        featureEventList.add(featureEvent2)


        return featureEventList
    }

    /**
     * Convention is that 1 becomes the child and is returned.
     * Because we are tracking the merge in the actual object blocks, the newJSONArray is also split
     * @param geneName1
     * @param uniqueName1
     * @param geneName2
     * @param uniqueName2
     * @param commandObject
     * @param oldFeatureArray
     * @param newFeatureObject
     * @param user
     * @return
     */
    List<FeatureEvent> addMergeFeatureEvent(String geneName1, String uniqueName1, String geneName2, String uniqueName2, JSONObject commandObject, JSONArray oldFeatureArray, JSONObject newFeatureObject,
                                            User user) {
        List<FeatureEvent> featureEventList = new ArrayList<>()

        List<FeatureEvent> lastFeatureEventLeftList = findCurrentFeatureEvent(uniqueName1)
        if (lastFeatureEventLeftList.size() != 1) {
            throw new AnnotationException("Not one current feature event being merged for: " + uniqueName1)
        }
        if (!lastFeatureEventLeftList) {
            throw new AnnotationException("Can not find original feature event to split for " + uniqueName1)
        }
        List<FeatureEvent> lastFeatureEventRightList = findCurrentFeatureEvent(uniqueName2)
        if (lastFeatureEventRightList.size() != 1) {
            throw new AnnotationException("Not one current feature event being merged for: " + uniqueName2)
        }
        if (!lastFeatureEventRightList) {
            throw new AnnotationException("Can not find original feature event to split for " + uniqueName2)
        }


        FeatureEvent lastFeatureEventLeft = lastFeatureEventLeftList[0]
        FeatureEvent lastFeatureEventRight = lastFeatureEventRightList[0]
        lastFeatureEventLeft.current = false;
        lastFeatureEventRight.current = false;
        lastFeatureEventLeft.save()
        lastFeatureEventRight.save()
        deleteFutureHistoryEvents(lastFeatureEventLeft)
        deleteFutureHistoryEvents(lastFeatureEventRight)

        Date addDate = new Date()

        JSONArray newFeatureArray1 = new JSONArray()

        newFeatureArray1.add(newFeatureObject)

        FeatureEvent featureEvent1 = new FeatureEvent(
                editor: user
                , name: geneName1
                , uniqueName: uniqueName1
                , operation: FeatureOperation.MERGE_TRANSCRIPTS
                , current: true
                , originalJsonCommand: commandObject.toString()
                , newFeaturesJsonArray: newFeatureArray1.toString()
                , oldFeaturesJsonArray: oldFeatureArray.toString()
                , dateCreated: addDate
                , lastUpdated: addDate
        ).save()


        lastFeatureEventLeft.childId = featureEvent1.id
        lastFeatureEventRight.childId = featureEvent1.id
        featureEvent1.parentId = lastFeatureEventLeft.id
        featureEvent1.parentMergeId = lastFeatureEventRight.id

        featureEvent1.save()
        lastFeatureEventLeft.save()
        lastFeatureEventRight.save()

        featureEventList.add(featureEvent1)


        return featureEventList
    }

    /**
     * For non-split , non-merge operations
     */
    def addNewFeatureEvent(FeatureOperation featureOperation, String name, String uniqueName, JSONObject inputCommand, JSONArray oldFeatureArray, JSONArray newFeatureArray, User user) {
//        int updated = FeatureEvent.executeUpdate("update FeatureEvent  fe set fe.current = false where fe.uniqueName = :uniqueName", [uniqueName: uniqueName])
        List<FeatureEvent> lastFeatureEventList = findCurrentFeatureEvent(uniqueName)
        FeatureEvent lastFeatureEvent = null
        lastFeatureEventList?.each { a ->
            if (a.uniqueName == uniqueName) {
                lastFeatureEvent = a
            }
        }
        if (lastFeatureEvent) {
            lastFeatureEvent.current = false;
            lastFeatureEvent.save()
            deleteFutureHistoryEvents(lastFeatureEvent)
        }

        FeatureEvent featureEvent = new FeatureEvent(
                editor: user
                , name: name
                , uniqueName: uniqueName
                , operation: featureOperation.name()
                , current: true
                , parentId: lastFeatureEvent?.id
//                , parentMergeId: lastFeatureEventList && lastFeatureEventList.size() > 1 ? lastFeatureEventList[1].id : null
                , originalJsonCommand: inputCommand.toString()
                , newFeaturesJsonArray: newFeatureArray.toString()
                , oldFeaturesJsonArray: oldFeatureArray.toString()
                , dateCreated: new Date()
                , lastUpdated: new Date()
        ).save()

        // set the children here properly
        if (lastFeatureEvent) {
            lastFeatureEvent.childId = featureEvent.id
            lastFeatureEvent.save()
        }

        return featureEvent
    }

    def setNotPreviousFutureHistoryEvents(FeatureEvent featureEvent) {
        List<List<FeatureEvent>> featureEventList = findAllPreviousFeatureEvents(featureEvent)
        featureEventList.each { array ->
            array.each {
                if (it.current) {
                    it.current = false
                    it.save()
                }
            }
        }
    }

    def setNotCurrentFutureHistoryEvents(FeatureEvent featureEvent) {
        List<List<FeatureEvent>> featureEventList = findAllFutureFeatureEvents(featureEvent)
        featureEventList.each { array ->
            array.each {
                if (it.current) {
                    it.current = false
                    it.save()
                }
            }
        }
    }

    def deleteFutureHistoryEvents(FeatureEvent featureEvent) {
        List<List<FeatureEvent>> featureEventList = findAllFutureFeatureEvents(featureEvent)
        int count = 0
        featureEventList.each { it.each { it.delete(); ++count } }
        return count
//        return FeatureEvent.deleteAll(featureEventList.find().eac)
    }

    List<List<FeatureEvent>> findAllPreviousFeatureEvents(FeatureEvent featureEvent) {
        List<List<FeatureEvent>> featureEventList = new ArrayList<>()
        Long parentId = featureEvent.parentId
        FeatureEvent parentFeatureEvent = parentId ? FeatureEvent.findById(parentId) : null

        while (parentFeatureEvent) {

            List<FeatureEvent> featureArrayList = new ArrayList<>()
            featureArrayList.add(parentFeatureEvent)


            FeatureEvent parentMergeFeatureEvent = featureEvent.parentMergeId ? FeatureEvent.findById(featureEvent.parentMergeId) : null
            if (parentMergeFeatureEvent) {
                featureArrayList.add(parentMergeFeatureEvent)
                featureEventList.addAll(findAllPreviousFeatureEvents(parentMergeFeatureEvent))
            }


            featureEventList.add(featureArrayList)
            featureEventList.addAll(findAllPreviousFeatureEvents(parentFeatureEvent))


            parentId = parentFeatureEvent.parentId
            parentFeatureEvent = parentId ? FeatureEvent.findById(parentId) : null
        }

        return featureEventList.sort(true) { a, b ->
            a[0].dateCreated <=> b[0].dateCreated
        }.unique(true) { a, b ->
            a[0].id <=> b[0].id
        }
    }

    /**
     * Ordered from 0 is first N is last (most recent)
     * @param featureEvent
     * @return
     */
    List<List<FeatureEvent>> findAllFutureFeatureEvents(FeatureEvent featureEvent) {
        List<List<FeatureEvent>> featureEventList = new ArrayList<>()

        Long childId = featureEvent.childId
        FeatureEvent childFeatureEvent = childId ? FeatureEvent.findById(childId) : null
        while (childFeatureEvent) {
            List<FeatureEvent> featureArrayList = new ArrayList<>()
            featureArrayList.add(childFeatureEvent)

            FeatureEvent childSplitFeatureEvent = featureEvent.childSplitId ? FeatureEvent.findById(featureEvent.childSplitId) : null
            if (childSplitFeatureEvent) {
                featureArrayList.add(childSplitFeatureEvent)
                featureEventList.addAll(findAllFutureFeatureEvents(childSplitFeatureEvent))
            }

            // if there is a parent merge . .  we just include that parent in the history (not everything)
            // we have to assume that there is a previous feature event (a merge can never be first)
            FeatureEvent parentMergeFeatureEvent = featureEvent.parentMergeId ? FeatureEvent.findById(featureEvent.parentMergeId) : null
            if (parentMergeFeatureEvent && featureEventList) {
                featureEventList.get(featureEventList.size() - 1).add(parentMergeFeatureEvent)
            }

            featureEventList.addAll(findAllFutureFeatureEvents(childFeatureEvent))
            featureEventList.add(featureArrayList)

            childId = childFeatureEvent.childId
            childFeatureEvent = childId ? FeatureEvent.findById(childId) : null
        }

        return featureEventList.sort(true) { a, b ->
            a[0].dateCreated <=> b[0].dateCreated
        }.unique(true) { a, b ->
            a[0].id <=> b[0].id
        }
    }


    def addNewFeatureEvent(FeatureOperation featureOperation, String name, String uniqueName, JSONObject inputCommand, JSONObject oldJsonObject, JSONObject newJsonObject, User user) {
        JSONArray newFeatureArray = new JSONArray()
        newFeatureArray.add(newJsonObject)
        JSONArray oldFeatureArray = new JSONArray()
        oldFeatureArray.add(oldJsonObject)

        return addNewFeatureEvent(featureOperation, name, uniqueName, inputCommand, oldFeatureArray, newFeatureArray, user)
    }

    /**
     * @deprecated
     */
    FeatureEvent addNewFeatureEventWithUser(FeatureOperation featureOperation, Feature feature, JSONObject inputCommand, User user) {
        return addNewFeatureEventWithUser(featureOperation, feature.name, feature.uniqueName, inputCommand, featureService.convertFeatureToJSON(feature), user)
    }

//    def deleteHistoryAsync(String uniqueName) {
//        Promise memberDeleteDeltas = task {
//            deleteHistory(uniqueName)
//        }
//    }


    def deleteHistory(String uniqueName) {
        int count = 0
        getHistory(uniqueName).each { array ->
            array.each {
                it.delete()
                ++count
            }
        }
        return count
//        FeatureEvent.deleteAll(FeatureEvent.findAllByUniqueName(uniqueName))
    }

    /**
     * CurrentIndex of 0 is the oldest.  Highest number is the most recent
     * This returns an array.  We could have any number of splits going forward, so we have to return an array here.
     * @param uniqueName
     * @param currentIndex
     * @return
     */
    List<FeatureEvent> setTransactionForFeature(String uniqueName, int currentIndex) {
        log.info "setting previous transaction for feature ${uniqueName} -> ${currentIndex}"
        log.info "unique values: ${FeatureEvent.countByUniqueName(uniqueName)} -> ${currentIndex}"
        List<List<FeatureEvent>> featureEventList = getHistory(uniqueName)
        FeatureEvent currentFeatureEvent = null
        for (int i = 0; i < featureEventList.size(); i++) {
            List<FeatureEvent> featureEventArray = featureEventList.get(i)
            for (FeatureEvent featureEvent in featureEventArray) {
                if (i <= currentIndex) {
                    featureEvent.current = true
                    featureEvent.save()
                    currentFeatureEvent = featureEvent
                    if (i > 0) {
                        if (featureEventList.get(i).size() < featureEventList.get(i - 1).size()) {
                            featureEventList.get(i - 1).find() { it.uniqueName == uniqueName }.each() {
                                it.current = false
                                it.save()
                            }
                        } else {
                            featureEventList.get(i - 1).each() {
                                it.current = false
                                it.save()
                            }
                        }
                    }
                }
            }
        }

        if (!currentFeatureEvent) {
            log.warn "Did we forget to change the feature event?"
            findCurrentFeatureEvent(uniqueName)
        }
        setNotPreviousFutureHistoryEvents(currentFeatureEvent)
        setNotCurrentFutureHistoryEvents(currentFeatureEvent)

//        log.debug "updated is ${updated}"
        def returnEvent = findCurrentFeatureEvent(currentFeatureEvent.uniqueName)
        return returnEvent
    }

    def setHistoryState(JSONObject inputObject, int count, boolean confirm) {

        String uniqueName = inputObject.getString(FeatureStringEnum.UNIQUENAME.value)
        log.debug "undo count ${count}"
        if (count < 0) {
            log.warn("Can not undo any further")
            return
        }

//        int total = FeatureEvent.countByUniqueName(uniqueName)
        int total = getHistory(uniqueName).size()
        if (count >= total) {
            log.warn("Can not redo any further")
            return
        }

        Sequence sequence = Feature.findByUniqueName(uniqueName).featureLocation.sequence
        println "sequence: ${sequence}"


        def newUniqueNames = getHistory(uniqueName)[count].collect() {
            it.uniqueName
        }

        deleteCurrentState(inputObject, uniqueName, newUniqueNames, sequence)

        List<FeatureEvent> featureEventArray = setTransactionForFeature(uniqueName, count)
//        log.debug "final feature event: ${featureEvent} ->${featureEvent.operation}"
//        log.debug "current feature events for unique name ${FeatureEvent.countByUniqueNameAndCurrent(uniqueName, true)}"

        featureEventArray.each { featureEvent ->

            JSONArray jsonArray = (JSONArray) JSON.parse(featureEvent.newFeaturesJsonArray)
            JSONObject originalCommandObject = (JSONObject) JSON.parse(featureEvent.originalJsonCommand)
            log.debug "array to add size: ${jsonArray.size()} "
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonFeature = jsonArray.getJSONObject(i)

                JSONObject addCommandObject = new JSONObject()
                JSONArray featuresToAddArray = new JSONArray()
                featuresToAddArray.add(jsonFeature)
                addCommandObject.put(FeatureStringEnum.FEATURES.value, featuresToAddArray)

                // we have to explicitly set the track (if we have features ... which we should)
                if (!addCommandObject.containsKey(FeatureStringEnum.TRACK.value) && featuresToAddArray.size() > 0) {
                    addCommandObject.put(FeatureStringEnum.TRACK.value, featuresToAddArray.getJSONObject(0).getString(FeatureStringEnum.SEQUENCE.value))
                }

                addCommandObject = permissionService.copyUserName(inputObject, addCommandObject)

                addCommandObject.put(FeatureStringEnum.SUPPRESS_HISTORY.value, true)
                addCommandObject.put(FeatureStringEnum.SUPPRESS_EVENTS.value, true)

                JSONObject returnObject
                if (featureService.isJsonTranscript(jsonFeature)) {
                    // set the original gene name
//                if (originalCommandObject.containsKey(FeatureStringEnum.NAME.value)) {
////                    addCommandObject.put(FeatureStringEnum.GENE_NAME.value, originalCommandObject.getString(FeatureStringEnum.NAME.value))
                    for (int k = 0; k < featuresToAddArray.size(); k++) {
                        JSONObject featureObject = featuresToAddArray.getJSONObject(k)
//                        featureObject.put(FeatureStringEnum.GENE_NAME.value, originalCommandObject.getString(FeatureStringEnum.NAME.value))
                        featureObject.put(FeatureStringEnum.GENE_NAME.value, featureEvent.name)
                    }
//                }
                    println "original command object = ${originalCommandObject as JSON}"
                    println "final command object = ${addCommandObject as JSON}"

                    returnObject = requestHandlingService.addTranscript(addCommandObject)
                } else {
                    returnObject = requestHandlingService.addFeature(addCommandObject)
                }

                AnnotationEvent annotationEvent = new AnnotationEvent(
                        features: returnObject
                        , sequence: sequence
                        , operation: AnnotationEvent.Operation.UPDATE
                )

                requestHandlingService.fireAnnotationEvent(annotationEvent)
            }
        }

        return featureEventArray

    }

    def deleteCurrentState(JSONObject inputObject, String uniqueName, List<String> newUniqueNames, Sequence sequence) {

        // need to get uniqueNames for EACH current featureEvent
        for (FeatureEvent deleteFeatureEvent in findCurrentFeatureEvent(uniqueName)) {
            JSONObject deleteCommandObject = new JSONObject()
            JSONArray featuresArray = new JSONArray()
            println "delete feature event uniqueNAe: ${deleteFeatureEvent.uniqueName}"
            JSONObject featureToDelete = new JSONObject()
            featureToDelete.put(FeatureStringEnum.UNIQUENAME.value, deleteFeatureEvent.uniqueName)
            featuresArray.add(featureToDelete)

            println "inputObject ${inputObject as JSON}"
            println "deleteCommandObject ${deleteCommandObject as JSON}"

            if (!deleteCommandObject.containsKey(FeatureStringEnum.TRACK.value)) {
//            for(int i = 0 ; i < featuresArray.size() ; i++){
                deleteCommandObject.put(FeatureStringEnum.TRACK.value, sequence.name)
//            }
            }
            deleteCommandObject.put(FeatureStringEnum.SUPPRESS_HISTORY,true)
            println "final deleteCommandObject ${deleteCommandObject as JSON}"

            deleteCommandObject.put(FeatureStringEnum.FEATURES.value, featuresArray)
            deleteCommandObject = permissionService.copyUserName(inputObject, deleteCommandObject)

            println " final delete JSON ${deleteCommandObject as JSON}"
//            FeatureEvent.withNewTransaction {
            // suppress any events that are not part of the new state
            println "newUniqueNames ${newUniqueNames} vs uniqueName ${uniqueName} vs df-uniqueName ${deleteFeatureEvent.uniqueName}"
            requestHandlingService.deleteFeature(deleteCommandObject)
//            }
            println "deletion sucess . .  "
        }

    }

    /**
     * Count back from most recent
     * @param inputObject
     * @param count
     * @param confirm
     * @return
     */
    def redo(JSONObject inputObject, int countForward, boolean confirm) {
        log.info "redoing ${countForward}"
        if (countForward == 0) {
            log.warn "Redo to the same state"
            return
        }
        // count = current - countBackwards
        String uniqueName = inputObject.get(FeatureStringEnum.UNIQUENAME.value)
        int currentIndex = getCurrentFeatureEventIndex(uniqueName)
        int count = currentIndex + countForward
        log.info "current Index ${currentIndex}"
        log.info "${count} = ${currentIndex}-${countForward}"

        setHistoryState(inputObject, count, confirm)
    }

    /**
     * We count backwards in order to get the correct count.
     * @param uniqueName
     * @return
     */
    int getCurrentFeatureEventIndex(String uniqueName) {
        List<FeatureEvent> currentFeatureEventList = FeatureEvent.findAllByUniqueNameAndCurrent(uniqueName, true, [sort: "dateCreated", order: "asc"])
        if (currentFeatureEventList.size() != 1) {
            throw new AnnotationException("Feature event list is the wrong size ${currentFeatureEventList?.size()}")
        }
        FeatureEvent currentFeatureEvent = currentFeatureEventList.iterator().next()
//        List<FeatureEvent> featureEventList = FeatureEvent.findAllByUniqueName(uniqueName, [sort: "dateCreated", order: "asc"])

        int index = -1
        while (currentFeatureEvent) {
            ++index
            currentFeatureEvent = currentFeatureEvent.parentId ? FeatureEvent.findById(currentFeatureEvent.parentId) : null
        }
        return index
    }

    def undo(JSONObject inputObject, int countBackwards, boolean confirm) {
        log.info "undoing ${countBackwards}"
        if (countBackwards == 0) {
            log.warn "Undo to the same state"
            return
        }

        String uniqueName = inputObject.get(FeatureStringEnum.UNIQUENAME.value)
        int currentIndex = getCurrentFeatureEventIndex(uniqueName)
        int count = currentIndex - countBackwards
        log.info "${count} = ${currentIndex}-${countBackwards}"
        setHistoryState(inputObject, count, confirm)
    }

    /**
     * We find the current one for the uniqueName and its index
     * We then count forward by the same amount and return that.
     * @param uniqueName
     * @return
     */
    List<FeatureEvent> findCurrentFeatureEvent(String uniqueName) {
        List<FeatureEvent> featureEventList = FeatureEvent.findAllByUniqueNameAndCurrent(uniqueName, true)
        if (featureEventList.size() != 1) {
            log.debug("No current feature events for ${uniqueName}: " + featureEventList.size())
            return null
        }

        FeatureEvent currentFeatureEvent = featureEventList.first()
        int index = getCurrentFeatureEventIndex(uniqueName)

        // its okay if we grab either side of this array
        // just arbitrarily get the first one
        List<List<FeatureEvent>> previousFeatureEvents = findAllPreviousFeatureEvents(currentFeatureEvent)
        if (!previousFeatureEvents) {
            def futureEvents = findAllFutureFeatureEvents(featureEventList[0])
            // if we have a future event and it is a merge, then we have multiple "current"
            if (futureEvents && futureEvents.get(0).get(0).parentMergeId) {
                if (futureEvents.get(0).get(0).parentMergeId != currentFeatureEvent.id) {
                    return [currentFeatureEvent, FeatureEvent.findById(futureEvents.get(0).get(0).parentMergeId)]
                } else {
                    return [currentFeatureEvent, FeatureEvent.findById(futureEvents.get(0).get(0).parentId)]
                }
            } else {
                return [currentFeatureEvent]
            }
        }

        // its possible that neither one has a matching uniqueName .
        FeatureEvent firstFeatureEvent = previousFeatureEvents[0].find() {
            it.uniqueName == uniqueName
        }
        // example we reverting backwards
        if (!firstFeatureEvent) {
            firstFeatureEvent = previousFeatureEvents[0][0]
        }

        // or index== 0
        if (currentFeatureEvent.id == firstFeatureEvent.id) {
            return [currentFeatureEvent]
        }

        // an index of 1 is 1 in the future.  This returns exclusive future, so we need to
        // substract 1 from the index
        def futureEvents = findAllFutureFeatureEvents(firstFeatureEvent)[index - 1]
        return futureEvents
    }

    /**
     * This is the root uniqueName
     * Should returned sorted most recent at 0, latest at end
     *
     * If splits occur we need to show them
     * @param uniqueName
     * @return
     */
    List<List<FeatureEvent>> getHistory(String uniqueName) {
        List<FeatureEvent> currentFeatureEvent = findCurrentFeatureEvent(uniqueName)

        // if we revert a split or do a merge
        if (!currentFeatureEvent) return []

        List<List<FeatureEvent>> featureEvents = new ArrayList<>()

        for (FeatureEvent featureEvent in currentFeatureEvent) {
            featureEvents.addAll(findAllPreviousFeatureEvents(featureEvent))
        }
        featureEvents.add(currentFeatureEvent)
        // finding future events handles splits correctly, so we only need to manage this branch
        for (FeatureEvent featureEvent in currentFeatureEvent) {
            if (featureEvent.uniqueName == uniqueName) {
                featureEvents.addAll(findAllFutureFeatureEvents(featureEvent))
            }
        }

        // if we have a split, it will pick up the same values twice
        // so we need to filter those out
        return featureEvents.sort(true) { a, b ->
            a[0].dateCreated <=> b[0].dateCreated
        }
        .unique(true) { a, b ->
            a[0].id <=> b[0].id
        }
    }

}
