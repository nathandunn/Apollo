package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.storage.client.Storage;
import org.bbop.apollo.gwt.client.dto.*;
import org.bbop.apollo.gwt.client.rest.PreferenceRestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

import java.util.Date;
import java.util.Map;

/**
 * Created by ndunn on 4/22/16.
 */
public class PreferenceInfoService {

    private static PreferenceInfoService instance ;

    private static Storage preferenceStore = Storage.getSessionStorageIfSupported();
    private Boolean eventsInQueue = false ;


    private PreferenceInfoService(){
        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if(eventsInQueue){
                    savePreferences();
                }
                return false;
            }
        },30000);
    }

    public static PreferenceInfoService getInstance(){
        if(instance==null){
            instance = new PreferenceInfoService() ;
        }
        return instance ;
    }

//    public PreferenceInfo getPreferenceInfo() {
//        return preferenceInfo;
//    }
//
//    public void setPreferenceInfo(PreferenceInfo preferenceInfo) {
//        setPreferenceInfo(preferenceInfo, false);
//    }
    public void setPreferenceInfo(PreferenceInfo preferenceInfo) {
        setPreferenceInfo(preferenceInfo,false);
    }

    public void setPreferenceInfo(PreferenceInfo preferenceInfo, Boolean saveLocal) {
        setPreference(FeatureStringEnum.PREFERENCES.getValue(), preferenceInfo.toJSON().toString());
        triggerEventAdded();
        if (saveLocal) {
            savePreferences();
        }
    }

    private void savePreferences() {
        PreferenceRestService.savePreferences(getPreference(FeatureStringEnum.PREFERENCES.getValue()));
        eventsInQueue=false ;
    }

    private void triggerEventAdded() {
        eventsInQueue = true ;
    }

    public void setPreference(String key, Object value) {
//        PreferenceInfoService.getInstance().setPreference(key,value);
        if (preferenceStore != null) {
            preferenceStore.setItem(key, value.toString());
        }
    }

    public String getPreference(String key) {
        if (preferenceStore != null) {
            return preferenceStore.getItem(key);
        }
        return null;
    }

    public void setPreference(OrganismInfo organismInfo, SequenceInfo sequenceInfo){
        PreferenceInfo preferenceInfo = PreferenceInfoConverter.convertFromJson(JSONParser.parseStrict(getPreference(FeatureStringEnum.PREFERENCES.getValue())).isObject());
        preferenceInfo.setCurrentOrganism(organismInfo);
        Map<OrganismInfo,OrganismPreferenceInfo> organismPreferenceInfoMap = preferenceInfo.getOrganismPreferenceInfos();
        OrganismPreferenceInfo organismPreferenceInfo = organismPreferenceInfoMap.get(organismInfo);


        organismPreferenceInfoMap.put(organismInfo,organismPreferenceInfo);
    }
}
