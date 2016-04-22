package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ndunn on 4/22/16.
 */
public class PreferenceInfoConverter {

    public static PreferenceInfo convertFromJson(JSONObject preferences) {
        PreferenceInfo preferenceInfo = new PreferenceInfo();

        // preferences.organism
        preferenceInfo.setCurrentOrganism(OrganismInfoConverter.convertFromJson(preferences.get(FeatureStringEnum.ORGANISM.getValue()).isObject()));

        // preferences.sequence // this is inferred from the preferences

        // preferences.organisms / sequences
        JSONArray organismPreferencesArray = preferences.get(FeatureStringEnum.ORGANISMS.getValue()).isArray() ;

        List<OrganismPreferenceInfo> organismPreferenceInfoList = new ArrayList<>();

        for(int i =0  ; i < organismPreferencesArray.size() ;i++){
            JSONObject organismInfoObject = organismPreferencesArray.get(i).isObject();
            OrganismPreferenceInfo organismPreferenceInfo = OrganismPreferenceInfoConverter.convertFromJson(organismInfoObject);
//            organismPreferenceInfo.setOrganismInfo(organismPreferenceInfo)

            organismPreferenceInfoList.add(organismPreferenceInfo);
        }
//        preferenceInfo.setOrganismPreferenceInfos();

        return preferenceInfo ;
    }

    public static JSONObject convertToJson(PreferenceInfo preferenceInfo){
        return preferenceInfo.toJSON();
    }
}
