/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo Framework
 * Version: 0.4.6
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
package com.qcadoo.model.api;

import java.util.List;
import java.util.Map;

import com.qcadoo.model.api.validators.ErrorMessage;

/**
 * Object represents data from the database tables. All fields are aggregated into key-value map. The key is the name of the field
 * from its definition - {@link com.qcadoo.model.api.FieldDefinition#getName()}.
 * 
 * @since 0.4.0
 */
public interface Entity {

    /**
     * Set the entity's id.
     * 
     * @param id
     *            the entity's name
     */
    void setId(Long id);

    /**
     * Return the entity's id.
     * 
     * @return the entity's id
     */
    Long getId();

    /**
     * Return the entity's dataDefinition.
     * 
     * @return the entity's dataDefinition
     */
    DataDefinition getDataDefinition();

    /**
     * Return the value of the field with given name.
     * 
     * @param fieldName
     *            field's name
     * @return the field's value
     */
    Object getField(String fieldName);

    /**
     * Return the value, casted to string, of the field with given name.
     * 
     * @param fieldName
     *            field's name
     * @return the field's value
     */
    String getStringField(String fieldName);

    /**
     * Return the value, casted to entity, of the field with given name.
     * 
     * @param fieldName
     *            field's name
     * @return the field's value
     */
    Entity getBelongsToField(String fieldName);

    /**
     * Return the value, casted to list of entities, of the field with given name.
     * 
     * @param fieldName
     *            field's name
     * @return the field's value
     */
    EntityList getHasManyField(String fieldName);

    /**
     * Return the value, casted to tree, of the field with given name.
     * 
     * @param fieldName
     *            field's name
     * @return the field's value
     */
    EntityTree getTreeField(String fieldName);

    /**
     * Set the value of the field with given name.
     * 
     * @param fieldName
     *            field's name
     * @param fieldValue
     *            field'value
     */
    void setField(String fieldName, Object fieldValue);

    /**
     * Return all field's values.
     * 
     * @return field's values - name - value pairs
     */
    Map<String, Object> getFields();

    /**
     * Set global error, not related with fields.
     * 
     * @param message
     *            message
     * @param vars
     *            message's vars
     */
    void addGlobalError(String message, String... vars);

    /**
     * Set error for given field.
     * 
     * @param fieldDefinition
     *            field's definition
     * @param message
     *            message
     * @param vars
     *            message's vars
     */
    void addError(FieldDefinition fieldDefinition, String message, String... vars);

    /**
     * Return all global errors.
     * 
     * @return errors
     */
    List<ErrorMessage> getGlobalErrors();

    /**
     * Return all field's errors.
     * 
     * @return fields' errors
     */
    Map<String, ErrorMessage> getErrors();

    /**
     * Return error for given field.
     * 
     * @param fieldName
     *            field's name
     * @return field's error
     */
    ErrorMessage getError(String fieldName);

    /**
     * Return true if there is no global and field's errors.
     * 
     * @return true if entity is valid
     */
    boolean isValid();

    /**
     * Set validation status as not valid
     */
    void setNotValid();

    /**
     * Return true if there is no field's errors for given field.
     * 
     * @param fieldName
     *            field's name
     * @return true if field is valid
     */
    boolean isFieldValid(String fieldName);

    /**
     * Create new entity and copy fields values.
     * 
     * @return copied entity
     */
    Entity copy();

    /**
     * Set if entity is active.
     * 
     * @param active
     *            is active
     * @since 0.4.2
     */
    void setActive(boolean active);

    /**
     * Returns true if entity is active.
     * 
     * @return true if active
     * @since 0.4.2
     */
    boolean isActive();

}
