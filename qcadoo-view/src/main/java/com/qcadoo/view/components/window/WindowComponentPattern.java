package com.qcadoo.view.components.window;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ribbon.Ribbon;
import com.qcadoo.view.internal.ComponentDefinition;
import com.qcadoo.view.internal.patterns.AbstractContainerPattern;
import com.qcadoo.view.internal.ribbon.RibbonUtils;
import com.qcadoo.view.internal.xml.ViewDefinitionParser;

public class WindowComponentPattern extends AbstractContainerPattern {

    private static final String JSP_PATH = "containers/window.jsp";

    private static final String JS_OBJECT = "QCD.components.containers.Window";

    private Boolean header;

    private Boolean fixedHeight;

    private Ribbon ribbon;

    private boolean hasRibbon = true;

    private String firstTabName;

    private WindowTabComponentPattern mainTab;

    public WindowComponentPattern(final ComponentDefinition componentDefinition) {
        super(componentDefinition);
    }

    @Override
    public ComponentState getComponentStateInstance() {
        return new WindowComponentState(this);
    }

    @Override
    public void parse(final Node componentNode, final ViewDefinitionParser parser) {
        super.parseWithoutChildren(componentNode, parser);

        Node ribbonNode = null;

        NodeList childNodes = componentNode.getChildNodes();

        Boolean tabMode = null;

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if ("ribbon".equals(child.getNodeName())) {
                if (ribbonNode != null) {
                    throw new IllegalStateException("Window can contain only one ribbon");
                }
                ribbonNode = child;

            } else if ("windowTab".equals(child.getNodeName())) {

                if (tabMode != null && !tabMode) {
                    throw new IllegalStateException("Window cannot have both 'windowTab' and 'component' tags");
                }
                tabMode = true;

                WindowTabComponentPattern tab = new WindowTabComponentPattern(parser.getComponentDefinition(child, this,
                        getViewDefinition()));
                tab.parse(child, parser);
                addChild(tab);
                if (firstTabName == null) {
                    firstTabName = tab.getName();
                }

            } else if ("component".equals(child.getNodeName())) {

                if (tabMode != null && tabMode) {
                    throw new IllegalStateException("Window cannot have both 'windowTab' and 'component' tags");
                }
                tabMode = false;

                if (mainTab == null) {
                    ComponentDefinition componentDefinition = new ComponentDefinition();
                    componentDefinition.setName("mainTab");
                    componentDefinition.setParent(this);
                    componentDefinition.setTranslationService(getTranslationService());
                    componentDefinition.setViewDefinition(getViewDefinition());
                    componentDefinition.setReference("mainTab");
                    componentDefinition.setDataDefinition(null);
                    mainTab = new WindowTabComponentPattern(componentDefinition);
                    addChild(mainTab);
                    firstTabName = mainTab.getName();
                }

                mainTab.addChild(parser.parseComponent(child, mainTab));

            } else if ("option".equals(child.getNodeName())) {

                String type = parser.getStringAttribute(child, "type");
                String value = parser.getStringAttribute(child, "value");
                if ("header".equals(type)) {
                    header = Boolean.parseBoolean(value);
                } else if ("fixedHeight".equals(type)) {
                    fixedHeight = Boolean.parseBoolean(value);
                } else {
                    throw new IllegalStateException("Unknown option for window: " + type);
                }

            } else {
                throw new IllegalStateException("Unknown tag for window: " + child.getNodeName());
            }
        }

        if (header == null) {
            header = parser.getBooleanAttribute(componentNode, "header", true);
        }
        if (fixedHeight == null) {
            fixedHeight = parser.getBooleanAttribute(componentNode, "fixedHeight", false);
        }

        hasRibbon = parser.getBooleanAttribute(componentNode, "ribbon", true);

        if (ribbonNode != null) {
            setRibbon(RibbonUtils.getInstance().parseRibbon(ribbonNode, parser, getViewDefinition()));
        }

    }

    @Override
    protected Map<String, Object> getJspOptions(final Locale locale) {
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("header", header);
        options.put("oneTab", this.getChildren().size() < 2);
        options.put("hasRibbon", hasRibbon);
        return options;
    }

    @Override
    protected JSONObject getJsOptions(final Locale locale) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("fixedHeight", fixedHeight);
        json.put("header", header);
        json.put("oneTab", this.getChildren().size() < 2);
        json.put("hasRibbon", hasRibbon);
        if (ribbon != null) {
            json.put("ribbon", RibbonUtils.getInstance().translateRibbon(ribbon, locale, this));
        }
        json.put("firstTabName", firstTabName);
        JSONObject translations = new JSONObject();
        for (String childName : getChildren().keySet()) {
            List<String> tabNameKeys = new LinkedList<String>();
            tabNameKeys.add(getTranslationPath() + "." + childName + ".tabLabel");
            tabNameKeys.add("core.tabs." + childName + ".tabLabel");
            translations.put("tab." + childName, getTranslationService().translate(tabNameKeys, locale));
        }
        json.put("translations", translations);
        return json;
    }

    public void setRibbon(final Ribbon ribbon) {
        this.ribbon = ribbon;
    }

    public Ribbon getRibbon() {
        return ribbon;
    }

    @Override
    public String getJspFilePath() {
        return JSP_PATH;
    }

    @Override
    public String getJsFilePath() {
        return JS_PATH;
    }

    @Override
    public String getJsObjectName() {
        return JS_OBJECT;
    }
}
