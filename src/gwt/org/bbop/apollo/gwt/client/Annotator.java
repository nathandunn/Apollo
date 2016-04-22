package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.PreferenceInfo;
import org.bbop.apollo.gwt.client.rest.PreferenceRestService;
import org.bbop.apollo.gwt.shared.ClientTokenGenerator;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Annotator implements EntryPoint {

    private static Annotator instance;

    public static Annotator getInstance() {
        if (instance == null) {
            instance = new Annotator();
        }
        return instance;
    }

    public static EventBus eventBus = GWT.create(SimpleEventBus.class);
    private static Storage preferenceStore = Storage.getSessionStorageIfSupported();
    private static PreferenceInfo preferenceInfo;

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        MainPanel mainPanel = MainPanel.getInstance();
        RootLayoutPanel rp = RootLayoutPanel.get();
        rp.add(mainPanel);

        Dictionary optionsDictionary = Dictionary.getDictionary("Options");
//        if (optionsDictionary.keySet().contains(FeatureStringEnum.ORGANISM.getValue())) {
//            String clientToken = optionsDictionary.get(FeatureStringEnum.ORGANISM.getValue());
////            if (ClientTokenGenerator.isValidToken(clientToken)) {
//            setPreference(FeatureStringEnum.ORGANISM.getValue(), clientToken);
////            }
//        }
        Double height = 100d;
        Style.Unit heightUnit = Style.Unit.PCT;
        Double top = 0d;
        Style.Unit topUnit = Style.Unit.PCT;

        if (optionsDictionary.keySet().contains("top")) {
            top = Double.valueOf(optionsDictionary.get("top"));
        }
        if (optionsDictionary.keySet().contains("topUnit")) {
            topUnit = Style.Unit.valueOf(optionsDictionary.get("topUnit").toUpperCase());
        }
        if (optionsDictionary.keySet().contains("height")) {
            height = Double.valueOf(optionsDictionary.get("height"));
        }
        if (optionsDictionary.keySet().contains("heightUnit")) {
            heightUnit = Style.Unit.valueOf(optionsDictionary.get("heightUnit").toUpperCase());
        }
        rp.setWidgetTopHeight(mainPanel, top, topUnit, height, heightUnit);

        exportStaticMethod();
    }

    public static native void exportStaticMethod() /*-{
        $wnd.setPreference = $entry(@org.bbop.apollo.gwt.client.Annotator::setPreference(Ljava/lang/String;Ljava/lang/Object;));
        $wnd.getPreference = $entry(@org.bbop.apollo.gwt.client.Annotator::getPreference(Ljava/lang/String;));
        $wnd.getClientToken = $entry(@org.bbop.apollo.gwt.client.Annotator::getClientToken());
    }-*/;

    public static void setPreference(String key, Object value) {
        if (preferenceStore != null) {
            preferenceStore.setItem(key, value.toString());
        }
    }

    public static String getPreference(String key) {
        if (preferenceStore != null) {
            String returnValue = preferenceStore.getItem(key);
            return returnValue;
        }
        return null;
    }


    public static String getRootUrl() {
        String rootUrl = GWT.getModuleBaseURL().replace("annotator/", "");
        return rootUrl;
    }

    public static String getClientToken() {
        return MainPanel.getInstance().getCurrentOrganism().getId();
//        String token = getPreference(FeatureStringEnum.ORGANISM.getValue());
////        if (!ClientTokenGenerator.isValidToken(token)) {
//        if (preferenceInfo.getCurrentOrganism() != null) {
//            token = preferenceInfo.getCurrentOrganism().getId();
//        } else if (preferenceInfo.getOrganismPreferenceInfos().size() > 0) {
//            OrganismInfo currentOrganism = preferenceInfo.getOrganismPreferenceInfos().keySet().iterator().next();
//            preferenceInfo.setCurrentOrganism(currentOrganism);
//            token = preferenceInfo.getCurrentOrganism().getId();
//        }
////            token = ClientTokenGenerator.generateRandomString();
//        setPreference(FeatureStringEnum.ORGANISM.getValue(), token);
//        }
//        token = getPreference(FeatureStringEnum.ORGANISM.getValue());
//        return token;

    }

    public static PreferenceInfo getPreferenceInfo() {
        return preferenceInfo;
    }

    public static void setPreferenceInfo(PreferenceInfo preferenceInfo) {
        setPreferenceInfo(preferenceInfo, false);
    }

    public static void setPreferenceInfo(PreferenceInfo preferenceInfo, Boolean saveLocal) {
        preferenceInfo = preferenceInfo;
//        setPreference("preference",this.preferenceInfo.toJSON());
        setPreference(FeatureStringEnum.PREFERENCE.getValue(), preferenceInfo.toJSON().toString());
        if (saveLocal) {
            PreferenceRestService.savePreferences(preferenceInfo);
        }
    }
}
