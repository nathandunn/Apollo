package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

import java.util.List;
import java.util.Map;

/**
 * Created by ndunn on 4/22/16.
 */
public class OrganismPreferenceInfoConverter {
    public static OrganismPreferenceInfo convertFromJson(JSONObject organismInfoObject) {
        OrganismPreferenceInfo organismPreferenceInfo = new OrganismPreferenceInfo();

        OrganismInfo organismInfo = OrganismInfoConverter.convertFromJson(organismInfoObject.get(FeatureStringEnum.ORGANISM.getValue()).isObject());
        organismPreferenceInfo.setOrganismInfo(organismInfo);


        JSONArray sequenceInfoArray = organismInfoObject.get(FeatureStringEnum.SEQUENCES.getValue()).isArray();
        List<SequenceInfo> sequencePreferenceInfoList = SequenceInfoConverter.convertFromJsonArray(sequenceInfoArray);

        organismPreferenceInfo.setSequenceInfoList(sequencePreferenceInfoList);



        return organismPreferenceInfo ;
    }
}
