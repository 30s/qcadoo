/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo Framework
 * Version: 0.4.0
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

package com.qcadoo.model.internal;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.FieldDefinition;
import com.qcadoo.model.api.search.SearchCriteriaBuilder;
import com.qcadoo.model.api.search.SearchResult;
import com.qcadoo.model.internal.api.DataAccessService;
import com.qcadoo.model.internal.api.EntityHookDefinition;
import com.qcadoo.model.internal.api.InternalDataDefinition;
import com.qcadoo.model.internal.search.SearchCriteria;
import com.qcadoo.model.internal.search.SearchCriteriaImpl;
import com.qcadoo.model.internal.types.PriorityType;

public final class DataDefinitionImpl implements InternalDataDefinition {

    private final DataAccessService dataAccessService;

    private final String pluginIdentifier;

    private final String name;

    private String fullyQualifiedClassName;

    private final Map<String, FieldDefinition> fields = new LinkedHashMap<String, FieldDefinition>();

    private FieldDefinition priorityField;

    private final Map<String, EntityHookDefinition> hooks = new HashMap<String, EntityHookDefinition>();

    private final List<EntityHookDefinition> validators = new ArrayList<EntityHookDefinition>();

    private final List<EntityHookDefinition> viewHooks = new ArrayList<EntityHookDefinition>();

    private final List<EntityHookDefinition> createHooks = new ArrayList<EntityHookDefinition>();

    private final List<EntityHookDefinition> updateHooks = new ArrayList<EntityHookDefinition>();

    private final List<EntityHookDefinition> saveHooks = new ArrayList<EntityHookDefinition>();

    private final List<EntityHookDefinition> copyHooks = new ArrayList<EntityHookDefinition>();

    private boolean deletable = true;

    private boolean creatable = true;

    private boolean updatable = true;

    private boolean enabled = true;

    private String identifierExpression = "#id";

    private Class<?> classForEntity;

    public DataDefinitionImpl(final String pluginIdentifier, final String name, final DataAccessService dataAccessService) {
        this.pluginIdentifier = pluginIdentifier;
        this.name = name;
        this.dataAccessService = dataAccessService;
    }

    @Override
    public Entity get(final Long id) {
        return dataAccessService.get(this, id);
    }

    @Override
    public List<Entity> copy(final Long... id) {
        return dataAccessService.copy(this, id);
    }

    @Override
    public void delete(final Long... id) {
        dataAccessService.delete(this, id);
    }

    @Override
    public Entity save(final Entity entity) {
        if (!this.equals(entity.getDataDefinition())) {
            throw new IllegalStateException("Incompatible types");
        }
        return dataAccessService.save(this, entity);
    }

    @Override
    public SearchCriteriaBuilder find() {
        return new SearchCriteriaImpl(this);
    }

    @Override
    public SearchResult find(final SearchCriteria searchCriteria) {
        return dataAccessService.find(searchCriteria);
    }

    @Override
    public void move(final Long id, final int offset) {
        dataAccessService.move(this, id, offset);

    }

    @Override
    public void moveTo(final Long id, final int position) {
        dataAccessService.moveTo(this, id, position);
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
    public String getFullyQualifiedClassName() {
        return fullyQualifiedClassName;
    }

    public void setFullyQualifiedClassName(final String fullyQualifiedClassName) {
        this.fullyQualifiedClassName = fullyQualifiedClassName;
        this.classForEntity = loadClassForEntity();
    }

    @Override
    public Map<String, FieldDefinition> getFields() {
        return fields;
    }

    public void withField(final FieldDefinition field) {
        fields.put(field.getName(), field);
    }

    @Override
    public FieldDefinition getField(final String fieldName) {
        if (fields.containsKey(fieldName)) {
            return fields.get(fieldName);
        } else if (priorityField != null && priorityField.getName().equals(fieldName)) {
            return priorityField;
        } else {
            return null;
        }
    }

    @Override
    public EntityHookDefinition getHook(final String type, final String className, final String methodName) {
        EntityHookDefinition hook = hooks.get(type.toUpperCase(Locale.ENGLISH) + "." + className + "." + methodName);
        checkNotNull(hook, "Cannot find hook " + type.toUpperCase(Locale.ENGLISH) + "." + className + "." + methodName
                + " for dataDefinition " + this);
        return hook;
    }

    public List<EntityHookDefinition> getValidators() {
        return validators;
    }

    public List<EntityHookDefinition> getViewHooks() {
        return viewHooks;
    }

    public List<EntityHookDefinition> getCopyHooks() {
        return copyHooks;
    }

    public List<EntityHookDefinition> getCreateHooks() {
        return createHooks;
    }

    public List<EntityHookDefinition> getSaveHooks() {
        return saveHooks;
    }

    public List<EntityHookDefinition> getUpdateHooks() {
        return updateHooks;
    }

    public void addValidatorHook(final EntityHookDefinition validator) {
        hooks.put(AbstractModelXmlConverter.HooksTag.VALIDATESWITH.toString() + "." + validator.getName(), validator);
        validators.add(validator);
    }

    public void addViewHook(final EntityHookDefinition viewHook) {
        hooks.put(AbstractModelXmlConverter.HooksTag.ONVIEW.toString() + "." + viewHook.getName(), viewHook);
        viewHooks.add(viewHook);
    }

    public void addCreateHook(final EntityHookDefinition createHook) {
        hooks.put(AbstractModelXmlConverter.HooksTag.ONCREATE.toString() + "." + createHook.getName(), createHook);
        createHooks.add(createHook);
    }

    public void addUpdateHook(final EntityHookDefinition updateHook) {
        hooks.put(AbstractModelXmlConverter.HooksTag.ONUPDATE.toString() + "." + updateHook.getName(), updateHook);
        updateHooks.add(updateHook);
    }

    public void addSaveHook(final EntityHookDefinition saveHook) {
        hooks.put(AbstractModelXmlConverter.HooksTag.ONSAVE.toString() + "." + saveHook.getName(), saveHook);
        saveHooks.add(saveHook);
    }

    public void addCopyHook(final EntityHookDefinition copyHook) {
        hooks.put(AbstractModelXmlConverter.HooksTag.ONCOPY.toString() + "." + copyHook.getName(), copyHook);
        copyHooks.add(copyHook);
    }

    public void setIdentifierExpression(final String identifierExpression) {
        this.identifierExpression = identifierExpression;
    }

    @Override
    public String getIdentifierExpression() {
        return identifierExpression;
    }

    @Override
    public boolean callViewHook(final Entity entity) {
        for (EntityHookDefinition hook : viewHooks) {
            if (hook.isEnabled()) {
                hook.call(entity);
            }
        }
        return true;
    }

    @Override
    public boolean callCreateHook(final Entity entity) {
        for (EntityHookDefinition hook : createHooks) {
            if (hook.isEnabled() && !hook.call(entity)) {
                return false;
            }
        }
        for (EntityHookDefinition hook : saveHooks) {
            if (hook.isEnabled() && !hook.call(entity)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean callUpdateHook(final Entity entity) {
        for (EntityHookDefinition hook : updateHooks) {
            if (hook.isEnabled() && !hook.call(entity)) {
                return false;
            }
        }
        for (EntityHookDefinition hook : saveHooks) {
            if (hook.isEnabled() && !hook.call(entity)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean callCopyHook(final Entity entity) {
        for (EntityHookDefinition hook : copyHooks) {
            if (hook.isEnabled() && !hook.call(entity)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean callValidators(final Entity entity) {
        for (EntityHookDefinition hook : validators) {
            if (hook.isEnabled() && !hook.call(entity)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Class<?> getClassForEntity() {
        return classForEntity;
    }

    @Override
    public Object getInstanceForEntity() {
        Class<?> entityClass = getClassForEntity();
        try {
            return entityClass.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalStateException("cannot instantiate class: " + getFullyQualifiedClassName(), e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("cannot instantiate class: " + getFullyQualifiedClassName(), e);
        }
    }

    @Override
    public boolean isPrioritizable() {
        return priorityField != null;
    }

    public void addPriorityField(final FieldDefinition priorityField) {
        checkState(priorityField.getType() instanceof PriorityType, "priority field has wrong type");
        this.priorityField = priorityField;
    }

    @Override
    public FieldDefinition getPriorityField() {
        return priorityField;
    }

    @Override
    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(final boolean deletable) {
        this.deletable = deletable;
    }

    @Override
    public boolean isUpdatable() {
        return updatable;
    }

    @Override
    public boolean isInstertable() {
        return creatable;
    }

    public void setInsertable(final boolean creatable) {
        this.creatable = creatable;
    }

    public void setUpdatable(final boolean updatable) {
        this.updatable = updatable;
    }

    private Class<?> loadClassForEntity() {
        try {
            return getClass().getClassLoader().loadClass(getFullyQualifiedClassName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("cannot find mapping class for definition: " + getFullyQualifiedClassName(), e);
        }
    }

    @Override
    public Entity create() {
        return new DefaultEntity(this);
    }

    @Override
    public Entity create(final Long id) {
        return new DefaultEntity(this, id);
    }

    @Override
    public String toString() {
        return getPluginIdentifier() + "." + getName();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(23, 41).append(name).append(pluginIdentifier).toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DataDefinitionImpl)) {
            return false;
        }
        DataDefinitionImpl other = (DataDefinitionImpl) obj;
        return new EqualsBuilder().append(name, other.name).append(pluginIdentifier, other.pluginIdentifier).isEquals();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        enabled = true;
    }

    @Override
    public void disable() {
        enabled = false;
    }

}
