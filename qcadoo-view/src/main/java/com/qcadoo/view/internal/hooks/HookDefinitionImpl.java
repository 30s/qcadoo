/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo Framework
 * Version: 1.1.7
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
package com.qcadoo.view.internal.hooks;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import org.apache.commons.beanutils.MethodUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qcadoo.plugin.api.PluginUtils;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.internal.HookDefinition;
import com.qcadoo.view.internal.api.ViewDefinition;

public final class HookDefinitionImpl implements HookDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(HookDefinitionImpl.class);

    private final Object bean;

    private final String methodName;

    private final String pluginIdentifier;

    public HookDefinitionImpl(final Object bean, final String methodName, final String pluginIdentifier) {
        this.bean = bean;
        this.methodName = methodName;
        this.pluginIdentifier = pluginIdentifier;
    }

    private Object call(final Object[] params, final Class<?>[] paramClasses) {
        try {
            return MethodUtils.invokeMethod(bean, methodName, params, paramClasses);
        } catch (NoSuchMethodException e) {
            LOG.warn("custom validation method is not exist", e);
        } catch (IllegalAccessException e) {
            LOG.warn("problem while calling custom validation method", e);
        } catch (InvocationTargetException e) {
            LOG.warn("problem while calling custom validation method", e);
        } catch (ClassCastException e) {
            LOG.warn("custom validation method has returned not boolean type", e);
        }
        return null;
    }

    @Override
    public void callWithViewState(final ViewDefinitionState viewDefinitionState) {
        if (pluginIdentifier == null || PluginUtils.isEnabled(pluginIdentifier)) {
            call(new Object[] { viewDefinitionState }, new Class[] { ViewDefinitionState.class });
        }
    }

    @Override
    public void callWithJSONObject(final ViewDefinition viewDefinition, final JSONObject object, final Locale locale) {
        if (pluginIdentifier == null || PluginUtils.isEnabled(pluginIdentifier)) {
            call(new Object[] { viewDefinition, object, locale }, new Class[] { ViewDefinition.class, JSONObject.class,
                    Locale.class });
        }
    }

    public String getMethod() {
        return methodName;
    }

    public Object getObject() {
        return bean;
    }

}
