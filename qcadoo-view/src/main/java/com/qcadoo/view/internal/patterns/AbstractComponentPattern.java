/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 0.3.0
 *
 * This file is part of Qcadoo.
 *
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */

package com.qcadoo.view.internal.patterns;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.springframework.util.StringUtils.hasText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.qcadoo.localization.api.TranslationService;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.FieldDefinition;
import com.qcadoo.model.api.types.BelongsToType;
import com.qcadoo.model.api.types.HasManyType;
import com.qcadoo.model.api.types.TreeType;
import com.qcadoo.plugin.api.PluginUtils;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.internal.ComponentDefinition;
import com.qcadoo.view.internal.ComponentOption;
import com.qcadoo.view.internal.FieldEntityIdChangeListener;
import com.qcadoo.view.internal.ScopeEntityIdChangeListener;
import com.qcadoo.view.internal.api.ComponentCustomEvent;
import com.qcadoo.view.internal.api.ComponentPattern;
import com.qcadoo.view.internal.api.InternalComponentState;
import com.qcadoo.view.internal.api.InternalViewDefinition;
import com.qcadoo.view.internal.api.InternalViewDefinitionService;
import com.qcadoo.view.internal.api.InternalViewDefinitionState;
import com.qcadoo.view.internal.api.ViewDefinition;
import com.qcadoo.view.internal.states.AbstractComponentState;
import com.qcadoo.view.internal.xml.ViewDefinitionParser;
import com.qcadoo.view.internal.xml.ViewDefinitionParserImpl;

public abstract class AbstractComponentPattern implements ComponentPattern {

    protected static final String JSP_PATH = "AbstractJspPath";

    protected static final String JS_PATH = null;

    protected static final String JS_OBJECT = "AbstractJavascriptObject";

    private final String name;

    private String extensionPluginIdentifier;

    private final String fieldPath;

    private final String scopeFieldPath;

    private final ComponentPattern parent;

    private final boolean defaultEnabled;

    private final boolean defaultVisible;

    private final boolean hasDescription;

    private final boolean hasLabel;

    private final String reference;

    private final TranslationService translationService;

    private final InternalViewDefinition viewDefinition;

    private final Map<String, ComponentPattern> fieldEntityIdChangeListeners = new HashMap<String, ComponentPattern>();

    private final Map<String, ComponentPattern> scopeEntityIdChangeListeners = new HashMap<String, ComponentPattern>();

    private final List<ComponentOption> options = new ArrayList<ComponentOption>();

    private final List<ComponentCustomEvent> customEvents = new ArrayList<ComponentCustomEvent>();

    private String script;

    private FieldDefinition fieldDefinition;

    private FieldDefinition scopeFieldDefinition;

    private DataDefinition dataDefinition;

    private boolean initialized;

    private int indexOrder;

    private AbstractComponentPattern fieldComponent;

    private AbstractComponentPattern scopeFieldComponent;

    public AbstractComponentPattern(final ComponentDefinition componentDefinition) {
        checkArgument(hasText(componentDefinition.getName()), "Component name must be specified");
        this.name = componentDefinition.getName();
        this.extensionPluginIdentifier = componentDefinition.getExtensionPluginIdentifier();
        this.fieldPath = componentDefinition.getFieldPath();
        this.scopeFieldPath = componentDefinition.getSourceFieldPath();
        this.parent = componentDefinition.getParent();
        this.reference = componentDefinition.getReference();
        this.hasDescription = componentDefinition.isHasDescription();
        this.hasLabel = componentDefinition.isHasLabel();
        this.defaultEnabled = componentDefinition.isDefaultEnabled();
        this.defaultVisible = componentDefinition.isDefaultVisible();
        this.translationService = componentDefinition.getTranslationService();
        this.dataDefinition = componentDefinition.getDataDefinition();
        this.viewDefinition = (InternalViewDefinition) componentDefinition.getViewDefinition();
        this.viewDefinition.registerComponent(getReference(), getPath(), this);
    }

    protected abstract String getJspFilePath();

    protected abstract String getJsFilePath();

    protected abstract String getJsObjectName();

    protected abstract ComponentState getComponentStateInstance();

    protected JSONObject getJsOptions(final Locale locale) throws JSONException {
        // reimplement me if you want
        return new JSONObject();
    }

    protected Map<String, Object> getJspOptions(final Locale locale) {
        // reimplement me if you want
        return new HashMap<String, Object>();
    }

    protected void initializeComponent() throws JSONException {
        // implement me if you want
    }

    protected void registerComponentViews(final InternalViewDefinitionService viewDefinitionService) {
        // implement me if you want
    }

    protected void unregisterComponentViews(final InternalViewDefinitionService viewDefinitionService) {
        // implement me if you want
    }

    protected ComponentPattern getParent() {
        return parent;
    }

    @Override
    public String getExtensionPluginIdentifier() {
        return extensionPluginIdentifier;
    }

    @Override
    public void registerViews(final InternalViewDefinitionService viewDefinitionService) {
        registerComponentViews(viewDefinitionService);
    }

    @Override
    public void unregisterComponent(final InternalViewDefinitionService viewDefinitionService) {
        viewDefinition.unregisterComponent(getReference(), getPath());
        unregisterComponentViews(viewDefinitionService);

        if (fieldComponent != null) {
            fieldComponent.removeFieldEntityIdChangeListener(fieldDefinition.getName());
        }

        if (scopeFieldComponent != null) {
            scopeFieldComponent.removeScopeEntityIdChangeListener(scopeFieldDefinition.getName());
        }
    }

    @Override
    public InternalComponentState createComponentState(final InternalViewDefinitionState viewDefinitionState) {
        AbstractComponentState state = (AbstractComponentState) getComponentStateInstance();
        state.setDataDefinition(dataDefinition);
        state.setName(name);
        state.setEnabled(isDefaultEnabled());
        state.setVisible(isDefaultVisible());
        state.setTranslationService(translationService);
        state.setTranslationPath(getTranslationPath());
        for (ComponentCustomEvent customEvent : customEvents) {
            state.registerCustomEvent(customEvent.getEvent(), customEvent.getObject(), customEvent.getMethod(),
                    customEvent.getPluginIdentifier());
        }
        if (viewDefinitionState != null) {
            viewDefinitionState.registerComponent(getReference(), state);
        }
        return state;
    }

    @Override
    public Map<String, Object> prepareView(final Locale locale) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("name", getName());
        map.put("path", getPath());
        map.put("indexOrder", indexOrder);
        map.put("jspFilePath", getJspFilePath());
        map.put("jsFilePath", getJsFilePath());
        map.put("jsObjectName", getJsObjectName());
        map.put("hasDescription", isHasDescription());
        map.put("hasLabel", isHasLabel());

        Map<String, Object> jspOptions = getJspOptions(locale);
        jspOptions.put("defaultEnabled", isDefaultEnabled());
        jspOptions.put("defaultRequired", isDefaultRequired());
        jspOptions.put("defaultVisible", isDefaultVisible());
        map.put("jspOptions", jspOptions);

        try {
            JSONObject jsOptions = getJsOptions(locale);
            addListenersToJsOptions(jsOptions);
            jsOptions.put("defaultEnabled", isDefaultEnabled());
            jsOptions.put("defaultRequired", isDefaultRequired());
            jsOptions.put("defaultVisible", isDefaultVisible());
            jsOptions.put("referenceName", reference);
            if (script != null) {
                jsOptions.put("script", prepareScript(script, locale));
            }
            map.put("jsOptions", jsOptions);
        } catch (JSONException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        return map;
    }

    public String prepareScript(final String scriptBody, final Locale locale) {
        Pattern p = Pattern.compile("#\\{translate\\(.*?\\)\\}");
        Matcher m = p.matcher(scriptBody);
        int lastEnd = 0;
        StringBuilder result = new StringBuilder();
        while (m.find()) {
            String expression = scriptBody.substring(m.start() + 12, m.end() - 2);
            result.append(scriptBody.substring(lastEnd, m.start()));
            if (expression.contains(".")) {
                result.append(translationService.translate(expression, locale));
            } else {
                result.append(translationService.translate("core.message." + expression, locale));
            }
            lastEnd = m.end();
        }
        if (lastEnd > 0) {
            result.append(scriptBody.substring(lastEnd));
            return result.toString();
        } else {
            return scriptBody;
        }
    }

    protected void prepareComponentView(final Map<String, Object> map, final Locale locale) throws JSONException {
        // implement me if you want
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final String getPath() {
        if (parent == null) {
            return name;
        } else {
            return parent.getPath() + "." + name;
        }
    }

    @Override
    public String getFunctionalPath() {
        if (parent == null) {
            return name;
        } else {
            return parent.getFunctionalPath() + "." + name;
        }
    }

    @Override
    public void initializeAll() {
        initialize();
    }

    @Override
    public boolean initialize() {
        if (initialized) {
            return true;
        }

        viewDefinition.addJsFilePath(getJsFilePath());

        String[] field = null;
        String[] scopeField = null;

        if (dataDefinition == null) {
            if (fieldPath != null) {
                field = getComponentAndField(fieldPath);
                fieldComponent = (AbstractComponentPattern) (field[0] == null ? parent : viewDefinition
                        .getComponentByReference(field[0]));
                checkNotNull(fieldComponent, "Cannot find field component for " + getPath() + ": " + fieldPath);
                fieldComponent.addFieldEntityIdChangeListener(field[1], this);
            }

            if (scopeFieldPath != null) {
                scopeField = getComponentAndField(scopeFieldPath);
                scopeFieldComponent = (AbstractComponentPattern) (scopeField[0] == null ? parent : viewDefinition
                        .getComponentByReference(scopeField[0]));
                checkNotNull(scopeFieldComponent, "Cannot find sourceField component for " + getPath() + ": " + scopeFieldPath);
                scopeFieldComponent.addScopeEntityIdChangeListener(scopeField[1], this);
            }

            if (isComponentInitialized(fieldComponent) && isComponentInitialized(scopeFieldComponent)) {
                initialized = true;
            } else {
                return false;
            }
        }

        getDataDefinition(viewDefinition, fieldComponent, scopeFieldComponent, dataDefinition);

        getFieldAndScopeFieldDefinitions(field, fieldComponent, scopeField, scopeFieldComponent);

        getDataDefinitionFromFieldDefinition();

        try {
            initializeComponent();
        } catch (JSONException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        return true;
    }

    public void addFieldEntityIdChangeListener(final String field, final ComponentPattern listener) {
        fieldEntityIdChangeListeners.put(field, listener);
    }

    public void addScopeEntityIdChangeListener(final String field, final ComponentPattern listener) {
        scopeEntityIdChangeListeners.put(field, listener);
    }

    public void removeFieldEntityIdChangeListener(final String field) {
        fieldEntityIdChangeListeners.remove(field);
    }

    public void removeScopeEntityIdChangeListener(final String field) {
        scopeEntityIdChangeListeners.remove(field);
    }

    public void updateComponentStateListeners(final ViewDefinitionState viewDefinitionState) {
        if (fieldEntityIdChangeListeners.size() > 0) {
            AbstractComponentState thisComponentState = (AbstractComponentState) viewDefinitionState
                    .getComponentByReference(getReference());
            for (Map.Entry<String, ComponentPattern> listenerPattern : fieldEntityIdChangeListeners.entrySet()) {
                if (isComponentEnabled(listenerPattern.getValue())) {
                    ComponentState listenerState = viewDefinitionState.getComponentByReference(listenerPattern.getValue()
                            .getReference());
                    if (listenerState != null) {
                        thisComponentState.addFieldEntityIdChangeListener(listenerPattern.getKey(),
                                (FieldEntityIdChangeListener) listenerState);
                    }
                }
            }
        }
        if (scopeEntityIdChangeListeners.size() > 0) {
            AbstractComponentState thisComponentState = (AbstractComponentState) viewDefinitionState
                    .getComponentByReference(getReference());
            for (Map.Entry<String, ComponentPattern> listenerPattern : scopeEntityIdChangeListeners.entrySet()) {
                if (isComponentEnabled(listenerPattern.getValue())) {
                    ComponentState listenerState = viewDefinitionState.getComponentByReference(listenerPattern.getValue()
                            .getReference());
                    if (listenerState != null) {
                        thisComponentState.addScopeEntityIdChangeListener(listenerPattern.getKey(),
                                (ScopeEntityIdChangeListener) listenerState);
                    }
                }
            }
        }
    }

    protected boolean isComponentEnabled(final ComponentPattern componentPattern) {
        return componentPattern.getExtensionPluginIdentifier() == null
                || PluginUtils.isEnabled(componentPattern.getExtensionPluginIdentifier());
    }

    protected final Map<String, ComponentPattern> getFieldEntityIdChangeListeners() {
        return fieldEntityIdChangeListeners;
    }

    protected final Map<String, ComponentPattern> getScopeEntityIdChangeListeners() {
        return scopeEntityIdChangeListeners;
    }

    protected final boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    protected final boolean isDefaultRequired() {
        if (getFieldDefinition() != null) {
            return getFieldDefinition().isRequired();
        }

        return false;
    }

    protected final boolean isDefaultVisible() {
        return defaultVisible;
    }

    protected final boolean isHasDescription() {
        return hasDescription;
    }

    protected final boolean isHasLabel() {
        return hasLabel;
    }

    public final void addOption(final ComponentOption option) {
        options.add(option);
    }

    protected final List<ComponentOption> getOptions() {
        return options;
    }

    @Override
    public final String getReference() {
        return reference != null ? reference : getPath();
    }

    protected final FieldDefinition getFieldDefinition() {
        return fieldDefinition;
    }

    protected final FieldDefinition getScopeFieldDefinition() {
        return scopeFieldDefinition;
    }

    protected final DataDefinition getDataDefinition() {
        return dataDefinition;
    }

    public final TranslationService getTranslationService() {
        return translationService;
    }

    public final ViewDefinition getViewDefinition() {
        return viewDefinition;
    }

    public void setExtensionPluginIdentifier(final String extensionPluginIdentifier) {
        this.extensionPluginIdentifier = extensionPluginIdentifier;
    }

    public final String getTranslationPath() {
        return getViewDefinition().getPluginIdentifier() + "." + getViewDefinition().getName() + "." + getFunctionalPath();
    }

    private void addListenersToJsOptions(final JSONObject jsOptions) throws JSONException {
        JSONArray listeners = new JSONArray();
        if (fieldEntityIdChangeListeners.size() > 0 || scopeEntityIdChangeListeners.size() > 0) {
            for (ComponentPattern listener : fieldEntityIdChangeListeners.values()) {
                if (isComponentEnabled(listener)) {
                    listeners.put(listener.getPath());
                }
            }
            for (ComponentPattern listener : scopeEntityIdChangeListeners.values()) {
                if (isComponentEnabled(listener)) {
                    listeners.put(listener.getPath());
                }
            }
        }
        if (customEvents.size() > 0) {
            listeners.put(getPath());
        }
        jsOptions.put("listeners", listeners);
    }

    private boolean isComponentInitialized(final AbstractComponentPattern fieldComponent) {
        return fieldComponent == null || fieldComponent.initialized;
    }

    private void getDataDefinitionFromFieldDefinition() {
        if (fieldDefinition != null) {
            getDataDefinitionFromFieldDefinition(fieldDefinition);
        } else if (scopeFieldDefinition != null) {
            getDataDefinitionFromFieldDefinition(scopeFieldDefinition);
        }
    }

    private void getFieldAndScopeFieldDefinitions(final String[] field, final AbstractComponentPattern fieldComponent,
            final String[] scopeField, final AbstractComponentPattern scopeFieldComponent) {
        if (dataDefinition != null) {
            if (fieldPath != null && field[1] != null) {
                fieldDefinition = fieldComponent.getDataDefinition().getField(field[1]);
                checkNotNull(fieldDefinition, "Cannot find field definition for " + getPath() + ": " + fieldPath);
            }

            if (scopeFieldPath != null && scopeField[1] != null) {
                scopeFieldDefinition = scopeFieldComponent.getDataDefinition().getField(scopeField[1]);
                checkNotNull(scopeFieldDefinition, "Cannot find sourceField definition for " + getPath() + ": " + scopeFieldPath);
            }
        }
    }

    private void getDataDefinition(final ViewDefinition viewDefinition, final AbstractComponentPattern fieldComponent,
            final AbstractComponentPattern scopeFieldComponent, final DataDefinition localDataDefinition) {
        if (fieldPath != null && fieldComponent != null) {
            dataDefinition = fieldComponent.getDataDefinition();
        } else if (scopeFieldPath != null && scopeFieldComponent != null) {
            dataDefinition = scopeFieldComponent.getDataDefinition();
        } else if (localDataDefinition != null) {
            dataDefinition = localDataDefinition;
        } else if (parent != null) {
            dataDefinition = ((AbstractComponentPattern) parent).getDataDefinition();
        } else {
            dataDefinition = viewDefinition.getDataDefinition();
        }
    }

    private void getDataDefinitionFromFieldDefinition(final FieldDefinition fieldDefinition) {
        if (fieldDefinition.getType() instanceof HasManyType) {
            dataDefinition = ((HasManyType) fieldDefinition.getType()).getDataDefinition();
        } else if (fieldDefinition.getType() instanceof TreeType) {
            dataDefinition = ((TreeType) fieldDefinition.getType()).getDataDefinition();
        } else if (fieldDefinition.getType() instanceof BelongsToType) {
            dataDefinition = ((BelongsToType) fieldDefinition.getType()).getDataDefinition();
        }
    }

    private String[] getComponentAndField(final String path) {
        Pattern pField = Pattern.compile("^#\\{(.+)\\}(\\.(\\w+))?");
        Matcher mField = pField.matcher(path);
        if (mField.find()) {
            return new String[] { mField.group(1), mField.group(3) };
        } else {
            return new String[] { null, path };
        }
    }

    @Override
    public void addCustomEvent(final ComponentCustomEvent customEvent) {
        customEvents.add(customEvent);
    }

    @Override
    public void removeCustomEvent(final ComponentCustomEvent customEvent) {
        customEvents.remove(customEvent);
    }

    @Override
    public void parse(final Node componentNode, final ViewDefinitionParser parser) {
        indexOrder = ((ViewDefinitionParserImpl) parser).getCurrentIndexOrder();

        NodeList childNodes = componentNode.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);

            if ("option".equals(child.getNodeName())) {
                addOption(parser.parseOption(child));
            } else if ("listener".equals(child.getNodeName())) {
                addCustomEvent(parser.parseCustomEvent(child));
            } else if ("script".equals(child.getNodeName())) {
                if (script == null) {
                    script = "";
                }
                script += parser.getStringNodeContent(child) + ";";
            }
        }
    }

}