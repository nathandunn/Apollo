package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.Window;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

import java.util.List;

/**
 * Created by ndunn on 3/31/15.
 */
public class AppInfoConverter {

    public static AppStateInfo convertFromJson(JSONObject object){
        AppStateInfo appStateInfo = new AppStateInfo() ;

        JSONObject preferences = object.get(FeatureStringEnum.PREFERENCES.getValue()).isObject();

        if(preferences.get(FeatureStringEnum.ORGANISM.getValue())!=null) {
            appStateInfo.setCurrentOrganism(OrganismInfoConverter.convertFromJson(preferences.get(FeatureStringEnum.ORGANISM.getValue()).isObject()));
        }

        if(preferences.get(FeatureStringEnum.SEQUENCE.getValue())!=null ){
            SequenceInfo sequenceInfo = SequenceInfoConverter.convertFromJson(preferences.get(FeatureStringEnum.SEQUENCE.getValue()).isObject());
            appStateInfo.setCurrentSequence(sequenceInfo);
            appStateInfo.setCurrentStartBp(sequenceInfo.getStart());
            appStateInfo.setCurrentEndBp(sequenceInfo.getEnd());
        }

        appStateInfo.setOrganismList(OrganismInfoConverter.convertFromJsonArray(object.get("organismList").isArray()));
        appStateInfo.setPreferenceInfo(PreferenceInfoConverter.convertFromJson(preferences));

        return appStateInfo ;
    }
}
