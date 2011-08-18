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
package com.qcadoo.view.internal.states.components;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Locale;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.qcadoo.localization.api.TranslationService;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.FieldDefinition;
import com.qcadoo.model.internal.DefaultEntity;
import com.qcadoo.model.internal.ExpressionServiceImpl;
import com.qcadoo.model.internal.types.StringType;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FormComponent;
import com.qcadoo.view.internal.api.ContainerState;
import com.qcadoo.view.internal.api.InternalComponentState;
import com.qcadoo.view.internal.components.FieldComponentPattern;
import com.qcadoo.view.internal.components.FieldComponentState;
import com.qcadoo.view.internal.components.form.FormComponentState;
import com.qcadoo.view.internal.states.AbstractComponentState;
import com.qcadoo.view.internal.states.AbstractContainerState;
import com.qcadoo.view.internal.states.AbstractStateTest;

public class FormComponentStateTest extends AbstractStateTest {

    private Entity entity;

    private ViewDefinitionState viewDefinitionState;

    private FieldComponentState name;

    private ContainerState form;

    private DataDefinition dataDefinition;

    private FieldDefinition fieldDefinition;

    private TranslationService translationService;

    @Before
    public void init() throws Exception {
        entity = mock(Entity.class);
        given(entity.getField("name")).willReturn("text");

        viewDefinitionState = mock(ViewDefinitionState.class);

        translationService = mock(TranslationService.class);

        fieldDefinition = mock(FieldDefinition.class);
        given(fieldDefinition.getType()).willReturn(new StringType());
        given(fieldDefinition.getName()).willReturn("name");

        dataDefinition = mock(DataDefinition.class, RETURNS_DEEP_STUBS);
        given(dataDefinition.get(12L)).willReturn(null);
        given(dataDefinition.get(13L)).willReturn(entity);
        given(dataDefinition.getPluginIdentifier()).willReturn("plugin");
        given(dataDefinition.getName()).willReturn("name");
        given(dataDefinition.getField("name")).willReturn(fieldDefinition);

        FieldComponentPattern namePattern = mock(FieldComponentPattern.class);
        given(namePattern.isRequired()).willReturn(false);
        name = new FieldComponentState(namePattern);
        ((AbstractComponentState) name).setTranslationService(translationService);
        name.setName("name");
        name.initialize(new JSONObject(), Locale.ENGLISH);

        form = new FormComponentState(null, "'static expression'");
        ((AbstractContainerState) form).setDataDefinition(dataDefinition);
        ((AbstractContainerState) form).setTranslationService(translationService);
        ((AbstractContainerState) form).addFieldEntityIdChangeListener("name", name);
        form.initialize(new JSONObject(ImmutableMap.of("components", new JSONObject())), Locale.ENGLISH);

        new ExpressionServiceImpl().init();
    }

    @Test
    public void shouldInitialeFormWithEntityId() throws Exception {
        // given
        InternalComponentState componentState = new FormComponentState(null, null);

        JSONObject json = new JSONObject();
        JSONObject jsonContent = new JSONObject();
        jsonContent.put(FormComponentState.JSON_ENTITY_ID, 13L);
        json.put(AbstractComponentState.JSON_CONTENT, jsonContent);
        JSONObject jsonChildren = new JSONObject();
        json.put(AbstractComponentState.JSON_CHILDREN, jsonChildren);

        // when
        componentState.initialize(json, Locale.ENGLISH);

        // then
        assertEquals(13L, componentState.getFieldValue());
    }

    @Test
    public void shouldInitialeFormWithNullEntityId() throws Exception {
        // given
        InternalComponentState componentState = new FormComponentState(null, null);

        JSONObject json = new JSONObject();
        JSONObject jsonContent = new JSONObject();
        jsonContent.put(FormComponentState.JSON_ENTITY_ID, (String) null);
        json.put(AbstractComponentState.JSON_CONTENT, jsonContent);
        JSONObject jsonChildren = new JSONObject();
        json.put(AbstractComponentState.JSON_CHILDREN, jsonChildren);

        // when
        componentState.initialize(json, Locale.ENGLISH);

        // then
        assertNull(componentState.getFieldValue());
    }

    @Test
    public void shouldRenderFormEntityId() throws Exception {
        // given
        TranslationService translationService = mock(TranslationService.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        AbstractComponentState componentState = new FormComponentState(null, "2");
        componentState.setTranslationService(translationService);
        componentState.setDataDefinition(dataDefinition);
        componentState.setFieldValue(13L);
        componentState.initialize(new JSONObject(ImmutableMap.of("components", new JSONObject())), Locale.ENGLISH);

        // when
        JSONObject json = componentState.render();

        // then
        assertEquals(13L, json.getJSONObject(AbstractComponentState.JSON_CONTENT).getLong(FormComponentState.JSON_ENTITY_ID));
    }

    @Test
    public void shouldHaveMessageIfEntityIsNotExistsAndEntityIdIsNotNull() throws Exception {
        // given
        form.setFieldValue(12L);
        given(translationService.translate(eq("null.entityNotFound"), eq("qcadooView.message.entityNotFound"), any(Locale.class)))
                .willReturn("translated entityNotFound");

        // when
        form.performEvent(viewDefinitionState, "initialize", new String[0]);

        // then
        assertFalse(((FormComponent) form).isValid());
        assertTrue(form.render().toString().contains("translated entityNotFound"));

    }

    @Test
    public void shouldClearFormIfEntityIsNotExistsAndEntityIdIsNull() throws Exception {
        // given
        form.setFieldValue(null);

        // when
        form.performEvent(viewDefinitionState, "initialize", new String[0]);

        // then
        assertNull(form.getFieldValue());
    }

    @Test
    public void shouldResetForm() throws Exception {
        // given
        form.setFieldValue(13L);

        // when
        form.performEvent(viewDefinitionState, "reset", new String[0]);

        // then
        assertEquals("text", name.getFieldValue());
    }

    @Test
    public void shouldClearFormEntity() throws Exception {
        // given
        form.setFieldValue(13L);

        // when
        form.performEvent(viewDefinitionState, "clear", new String[0]);

        // then
        assertNull(name.getFieldValue());
        assertNull(form.getFieldValue());
    }

    @Test
    public void shouldDeleteFormEntity() throws Exception {
        // given
        form.setFieldValue(13L);

        // when
        form.performEvent(viewDefinitionState, "delete", new String[0]);

        // then
        assertNull(name.getFieldValue());
        verify(dataDefinition).delete(13L);
        assertNull(form.getFieldValue());
    }

    @Test
    public void shouldSaveFormEntity() throws Exception {
        // given
        Entity entity = new DefaultEntity(dataDefinition, null, Collections.singletonMap("name", (Object) "text"));
        Entity savedEntity = new DefaultEntity(dataDefinition, 13L, Collections.singletonMap("name", (Object) "text2"));
        given(dataDefinition.create(null)).willReturn(new DefaultEntity(dataDefinition));
        given(dataDefinition.save(eq(entity))).willReturn(savedEntity);
        name.setFieldValue("text");

        form.setFieldValue(null);

        // when
        form.performEvent(viewDefinitionState, "save", new String[0]);

        // then
        verify(dataDefinition).save(eq(entity));
        assertEquals("text2", name.getFieldValue());
        assertEquals(13L, form.getFieldValue());
        assertTrue(((FormComponent) form).isValid());
    }

    @Test
    public void shouldCopyFormEntity() throws Exception {
        // given
        Entity copiedEntity = new DefaultEntity(dataDefinition, 14L, Collections.singletonMap("name", (Object) "text(1)"));
        given(dataDefinition.copy(13L)).willReturn(Collections.singletonList(copiedEntity));
        given(dataDefinition.get(14L)).willReturn(copiedEntity);
        name.setFieldValue("text");
        form.setFieldValue(13L);

        // when
        form.performEvent(viewDefinitionState, "copy", new String[0]);

        // then
        verify(dataDefinition).copy(13L);
        verify(dataDefinition).get(14L);
        assertEquals("text(1)", name.getFieldValue());
        assertEquals(14L, form.getFieldValue());
    }

    @Test
    public void shouldUseContextWhileSaving() throws Exception {
        // given
        Entity entity = new DefaultEntity(dataDefinition, 13L, Collections.singletonMap("name", (Object) "text2"));
        Entity savedEntity = new DefaultEntity(dataDefinition, 13L, Collections.singletonMap("name", (Object) "text2"));
        given(dataDefinition.create(13L)).willReturn(new DefaultEntity(dataDefinition, 13L));
        given(dataDefinition.save(eq(entity))).willReturn(savedEntity);
        given(dataDefinition.getFields().keySet()).willReturn(Collections.singleton("name"));
        name.setFieldValue("text");

        JSONObject json = new JSONObject();
        JSONObject jsonContext = new JSONObject();
        jsonContext.put("id", 14L);
        jsonContext.put("name", "text2");
        JSONObject jsonContent = new JSONObject();
        jsonContent.put(FormComponentState.JSON_ENTITY_ID, 13L);
        json.put(AbstractComponentState.JSON_CONTEXT, jsonContext);
        json.put(AbstractComponentState.JSON_CONTENT, jsonContent);
        json.put(AbstractComponentState.JSON_CHILDREN, new JSONObject());

        form.initialize(json, Locale.ENGLISH);

        // when
        form.performEvent(viewDefinitionState, "save", new String[0]);

        // then
        verify(dataDefinition).save(eq(entity));
        assertEquals("text2", name.getFieldValue());
        assertEquals(13L, form.getFieldValue());
        assertTrue(((FormComponent) form).isValid());
    }

    @Test
    public void shouldHaveValidationErrors() throws Exception {
        // given
        Entity entity = new DefaultEntity(dataDefinition, null, Collections.singletonMap("name", (Object) "text"));
        Entity savedEntity = new DefaultEntity(dataDefinition, null, Collections.singletonMap("name", (Object) "text2"));
        savedEntity.addGlobalError("global.error");
        savedEntity.addError(fieldDefinition, "field.error");

        given(translationService.translate(eq("global.error"), any(Locale.class))).willReturn("translated global error");
        given(translationService.translate(eq("field.error"), any(Locale.class))).willReturn("translated field error");
        given(dataDefinition.create(null)).willReturn(new DefaultEntity(dataDefinition));
        given(dataDefinition.save(eq(entity))).willReturn(savedEntity);
        name.setFieldValue("text");

        form.setFieldValue(null);

        // when
        form.performEvent(viewDefinitionState, "save", new String[0]);

        // then
        verify(dataDefinition).save(eq(entity));
        assertFalse(((FormComponent) form).isValid());
        assertTrue(form.render().toString().contains("translated global error"));
    }
}
