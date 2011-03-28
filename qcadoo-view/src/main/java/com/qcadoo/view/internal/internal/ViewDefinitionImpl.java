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

package com.qcadoo.view.internal.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.util.StringUtils;

import com.qcadoo.localization.api.TranslationService;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.view.api.ComponentPattern;
import com.qcadoo.view.api.ContainerPattern;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.components.window.WindowComponentPattern;
import com.qcadoo.view.internal.HookDefinition;
import com.qcadoo.view.internal.api.InternalViewDefinition;
import com.qcadoo.view.internal.api.InternalViewDefinitionService;
import com.qcadoo.view.internal.patterns.AbstractComponentPattern;

public final class ViewDefinitionImpl implements InternalViewDefinition {

    private final String name;

    private final String pluginIdentifier;

    private final DataDefinition dataDefinition;

    private final boolean menuAccessible;

    private Integer windowWidth;

    private Integer windowHeight;

    private final List<HookDefinition> postConstructHooks = new ArrayList<HookDefinition>();

    private final List<HookDefinition> postInitializeHooks = new ArrayList<HookDefinition>();

    private final List<HookDefinition> preInitializeHooks = new ArrayList<HookDefinition>();

    private final List<HookDefinition> preRenderHooks = new ArrayList<HookDefinition>();

    private final Set<String> jsFilePaths = new HashSet<String>();

    private final Map<String, ComponentPattern> patterns = new LinkedHashMap<String, ComponentPattern>();

    private final Map<String, ComponentPattern> registry = new LinkedHashMap<String, ComponentPattern>();

    private final TranslationService translationService;

    public ViewDefinitionImpl(final String name, final String pluginIdentifier, final DataDefinition dataDefinition,
            final boolean menuAccessible, final TranslationService translationService) {
        this.name = name;
        this.dataDefinition = dataDefinition;
        this.pluginIdentifier = pluginIdentifier;
        this.menuAccessible = menuAccessible;
        this.translationService = translationService;
    }

    public void setWindowDimmension(final Integer windowWidth, final Integer windowHeight) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
    }

    public void initialize() {
        List<ComponentPattern> list = getPatternsAsList(patterns.values());

        int lastNotInitialized = 0;

        while (true) {
            int notInitialized = 0;

            for (ComponentPattern pattern : list) {
                if (!pattern.initialize()) {
                    notInitialized++;
                }
            }

            if (notInitialized == 0) {
                break;
            }

            if (notInitialized == lastNotInitialized) {
                throw new IllegalStateException("There is cyclic dependency between components");
            }

            lastNotInitialized = notInitialized;
        }
    }

    @Override
    public Map<String, Object> prepareView(final JSONObject jsonObject, final Locale locale) {
        callHooks(postConstructHooks, jsonObject, locale);

        Map<String, Object> model = new HashMap<String, Object>();
        Map<String, Object> childrenModels = new HashMap<String, Object>();

        for (ComponentPattern componentPattern : patterns.values()) {
            childrenModels.put(componentPattern.getName(), componentPattern.prepareView(locale));
        }

        model.put(JSON_COMPONENTS, childrenModels);
        model.put(JSON_JS_FILE_PATHS, getJsFilePaths());

        model.put("hasDataDefinition", getDataDefinition() != null);

        try {
            JSONObject json = new JSONObject();
            JSONObject translations = new JSONObject();
            translations.put("backWithChangesConfirmation",
                    translationService.translate("commons.backWithChangesConfirmation", locale));
            json.put("translations", translations);
            if (windowWidth != null) {
                json.put("windowWidth", windowWidth);
            }
            if (windowHeight != null) {
                json.put("windowHeight", windowHeight);
            }
            model.put("jsOptions", json);
        } catch (JSONException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        return model;
    }

    @Override
    public JSONObject performEvent(final JSONObject jsonObject, final Locale locale) throws JSONException {
        callHooks(postConstructHooks, jsonObject, locale);

        ViewDefinitionState viewDefinitionState = new ViewDefinitionStateImpl();

        for (ComponentPattern cp : patterns.values()) {
            viewDefinitionState.addChild(cp.createComponentState(viewDefinitionState));
        }

        callHooks(preInitializeHooks, viewDefinitionState, locale);

        viewDefinitionState.initialize(jsonObject, locale);

        for (ComponentPattern cp : patterns.values()) {
            ((AbstractComponentPattern) cp).updateComponentStateListeners(viewDefinitionState);
        }

        callHooks(postInitializeHooks, viewDefinitionState, locale);

        JSONObject eventJson = jsonObject.getJSONObject(JSON_EVENT);
        String eventName = eventJson.getString(JSON_EVENT_NAME);
        String eventComponent = eventJson.has(JSON_EVENT_COMPONENT) ? eventJson.getString(JSON_EVENT_COMPONENT) : null;
        JSONArray eventArgsArray = eventJson.has(JSON_EVENT_ARGS) ? eventJson.getJSONArray(JSON_EVENT_ARGS) : new JSONArray();
        String[] eventArgs = new String[eventArgsArray.length()];
        for (int i = 0; i < eventArgsArray.length(); i++) {
            eventArgs[i] = eventArgsArray.getString(i);
        }

        viewDefinitionState.performEvent(eventComponent, eventName, eventArgs);

        callHooks(preRenderHooks, viewDefinitionState, locale);

        return viewDefinitionState.render();
    }

    public void registerViews(final InternalViewDefinitionService viewDefinitionService) {
        for (ComponentPattern cp : patterns.values()) {
            cp.registerViews(viewDefinitionService);
        }
    }

    public void addComponentPattern(final ComponentPattern componentPattern) {
        patterns.put(componentPattern.getName(), componentPattern);
    }

    @Override
    public ComponentPattern getComponentByReference(final String reference) {
        return registry.get(reference);
    }

    @Override
    public void registerComponent(final String reference, final String path, final ComponentPattern pattern) {
        if (registry.containsKey(reference)) {
            throw new IllegalStateException("Duplicated pattern reference : " + reference);
        }
        registry.put(reference, pattern);
    }

    @Override
    public void unregisterComponent(final String reference, final String path) {
        registry.remove(reference);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPluginIdentifier() {
        return pluginIdentifier;
    }

    @Override
    public boolean isMenuAccessible() {
        return menuAccessible;
    }

    @Override
    public DataDefinition getDataDefinition() {
        return dataDefinition;
    };

    public Set<String> getJsFilePaths() {
        return jsFilePaths;
    }

    @Override
    public void addJsFilePath(final String jsFilePath) {
        jsFilePaths.add(jsFilePath);
    }

    @Override
    public void addHook(final HookType type, final HookDefinition hookDefinition) {
        switch (type) {
            case PRE_INITIALIZE:
                addPreInitializeHook(hookDefinition);
                break;
            case PRE_RENDER:
                addPreRenderHook(hookDefinition);
                break;
            case POST_INITIALIZE:
                addPostInitializeHook(hookDefinition);
                break;
            case POST_CONSTRUCT:
                addPostConstructHook(hookDefinition);
                break;
            default:
                throw new IllegalArgumentException("Unknown hook type");
        }
    }

    @Override
    public void removeHook(final HookType type, final HookDefinition hookDefinition) {
        switch (type) {
            case PRE_INITIALIZE:
                preInitializeHooks.remove(hookDefinition);
                break;
            case PRE_RENDER:
                preRenderHooks.remove(hookDefinition);
                break;
            case POST_INITIALIZE:
                postInitializeHooks.remove(hookDefinition);
                break;
            case POST_CONSTRUCT:
                postConstructHooks.remove(hookDefinition);
                break;
            default:
                throw new IllegalArgumentException("Unknown hook type");
        }
    }

    public void addPostInitializeHook(final HookDefinition hookDefinition) {
        postInitializeHooks.add(hookDefinition);
    }

    public void addPreRenderHook(final HookDefinition hookDefinition) {
        preRenderHooks.add(hookDefinition);
    }

    public void addPreInitializeHook(final HookDefinition hookDefinition) {
        preInitializeHooks.add(hookDefinition);
    }

    public void addPostConstructHook(final HookDefinition hookDefinition) {
        postConstructHooks.add(hookDefinition);
    }

    private void callHooks(final List<HookDefinition> hooks, final ViewDefinitionState viewDefinitionState, final Locale locale) {
        for (HookDefinition hook : hooks) {
            hook.callWithViewState(viewDefinitionState, locale);
        }
    }

    private void callHooks(final List<HookDefinition> hooks, final JSONObject jsonObject, final Locale locale) {
        for (HookDefinition hook : hooks) {
            hook.callWithJSONObject(this, jsonObject, locale);
        }
    }

    private List<ComponentPattern> getPatternsAsList(final Collection<ComponentPattern> patterns) {
        List<ComponentPattern> list = new ArrayList<ComponentPattern>();
        list.addAll(patterns);
        for (ComponentPattern pattern : patterns) {
            if (pattern instanceof ContainerPattern) {
                list.addAll(getPatternsAsList(((ContainerPattern) pattern).getChildren().values()));
            }
        }
        return list;
    }

    @Override
    public WindowComponentPattern getRootWindow() {
        if (patterns.size() != 1) {
            return null;
        }
        ComponentPattern rootPattern = patterns.values().iterator().next();
        if (rootPattern instanceof WindowComponentPattern) {
            return (WindowComponentPattern) rootPattern;
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String translateContextReferences(final String context) {
        if (context == null) {
            return null;
        }
        try {
            JSONObject oldContext = new JSONObject(context);
            JSONObject newContext = new JSONObject();
            Iterator<String> paths = oldContext.keys();

            while (paths.hasNext()) {
                String oldPath = paths.next();
                String[] newPath = oldPath.split("\\.");

                ComponentPattern pattern = getComponentByReference(newPath[0]);

                if (pattern == null) {
                    throw new IllegalStateException("Cannot find component for " + getPluginIdentifier() + "." + getName() + ": "
                            + newPath[0]);
                }

                newPath[0] = pattern.getPath();

                newContext.put(StringUtils.arrayToDelimitedString(newPath, "."), oldContext.get(oldPath));
            }

            return newContext.toString();
        } catch (JSONException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
