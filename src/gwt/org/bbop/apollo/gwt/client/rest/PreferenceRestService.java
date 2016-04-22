package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.core.client.GWT;
import org.bbop.apollo.gwt.client.dto.PreferenceInfo;

/**
 * Created by ndunn on 4/22/16.
 */
public class PreferenceRestService extends RestService{

    /**
     * Saves preferences for current user
     * @param preferenceInfo
     */
    public static PreferenceInfo savePreferences(PreferenceInfo preferenceInfo){
        GWT.log("Saving preferences: "+preferenceInfo.toJSON().toString());
        return preferenceInfo ;
    }

    /**
     * Saves preferences for current user
     */
    public static PreferenceInfo getPreferences(){
        PreferenceInfo preferenceInfo = new PreferenceInfo();
        return preferenceInfo ;
    }
}
