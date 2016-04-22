package org.bbop.apollo

class Preference {

    static constraints = {
        name nullable: true ,blank: false
        domain nullable: true ,blank: false
        preferenceString nullable: true, blank: false
        token nullable: true, blank: false
    }

    String name
    String domain  // if we want to filter for a user / group domain
    String preferenceString // can embed JSONObject
    String token   // this is a general purpose token or JSON string
    Date dateCreated
    Date lastUpdated
}
