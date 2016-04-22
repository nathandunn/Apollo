package org.bbop.apollo

import grails.converters.JSON

class PreferenceController {

    def preferenceService
    /**
     *
     */
    def savePreferences(String preferences) {
        render preferenceService.setPreferences(preferences) as JSON
    }
}
