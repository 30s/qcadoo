/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo Framework
 * Version: 1.2.0
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
package com.qcadoo.view.internal.xml;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.util.ReflectionTestUtils.getField;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import junit.framework.Assert;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;

import com.google.common.collect.ImmutableMap;
import com.qcadoo.localization.api.TranslationService;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.FieldDefinition;
import com.qcadoo.model.api.types.BelongsToType;
import com.qcadoo.model.api.types.HasManyType;
import com.qcadoo.model.beans.sample.CustomEntityService;
import com.qcadoo.model.internal.types.StringType;
import com.qcadoo.security.api.SecurityRole;
import com.qcadoo.security.api.SecurityRolesService;
import com.qcadoo.view.api.ribbon.RibbonActionItem;
import com.qcadoo.view.internal.HookDefinition;
import com.qcadoo.view.internal.ViewDefinitionServiceImpl;
import com.qcadoo.view.internal.api.*;
import com.qcadoo.view.internal.components.ButtonComponentPattern;
import com.qcadoo.view.internal.components.CheckBoxComponentPattern;
import com.qcadoo.view.internal.components.TextAreaComponentPattern;
import com.qcadoo.view.internal.components.TextInputComponentPattern;
import com.qcadoo.view.internal.components.form.FormComponentPattern;
import com.qcadoo.view.internal.components.grid.GridComponentPattern;
import com.qcadoo.view.internal.components.window.WindowComponentPattern;
import com.qcadoo.view.internal.hooks.HookFactory;
import com.qcadoo.view.internal.internal.ViewComponentsResolverImpl;
import com.qcadoo.view.internal.ribbon.RibbonParserService;

public class ViewDefinitionParserImplTest {

    private ViewDefinitionParserImpl viewDefinitionParser;

    private DataDefinitionService dataDefinitionService;

    private ViewDefinitionService viewDefinitionService;

    private HookFactory hookFactory;

    private ApplicationContext applicationContext;

    private String xml1;

    private String xml2;

    private DataDefinition dataDefinitionA;

    private DataDefinition dataDefinitionB;

    private TranslationService translationService;

    private ContextualHelpService contextualHelpService;

    private SecurityRolesService securityRolesService;

    private SecurityRole userRoleMock, adminRoleMock;

    private static ViewComponentsResolverImpl viewComponentsResolver;

    @BeforeClass
    public static void initClass() throws Exception {
        viewComponentsResolver = new ViewComponentsResolverImpl();
        viewComponentsResolver.register("window", WindowComponentPattern.class);
        viewComponentsResolver.register("form", FormComponentPattern.class);
        viewComponentsResolver.register("checkbox", CheckBoxComponentPattern.class);
        viewComponentsResolver.register("input", TextInputComponentPattern.class);
        viewComponentsResolver.register("textarea", TextAreaComponentPattern.class);
        viewComponentsResolver.register("grid", GridComponentPattern.class);
        viewComponentsResolver.register("button", ButtonComponentPattern.class);
    }

    @Before
    public void init() throws Exception {
        applicationContext = mock(ApplicationContext.class);
        dataDefinitionService = mock(DataDefinitionService.class);

        translationService = mock(TranslationService.class);
        RibbonParserService ribbonService = new RibbonParserService();

        contextualHelpService = mock(ContextualHelpService.class);

        securityRolesService = mock(SecurityRolesService.class);

        viewDefinitionService = new ViewDefinitionServiceImpl();

        hookFactory = new HookFactory();
        setField(hookFactory, "applicationContext", applicationContext);

        viewDefinitionParser = new ViewDefinitionParserImpl();
        setField(viewDefinitionParser, "dataDefinitionService", dataDefinitionService);
        setField(viewDefinitionParser, "viewDefinitionService", viewDefinitionService);
        setField(viewDefinitionParser, "hookFactory", hookFactory);
        setField(viewDefinitionParser, "translationService", translationService);
        setField(viewDefinitionParser, "contextualHelpService", contextualHelpService);
        setField(viewDefinitionParser, "viewComponentsResolver", viewComponentsResolver);
        setField(viewDefinitionParser, "ribbonService", ribbonService);
        setField(viewDefinitionParser, "securityRolesService", securityRolesService);

        userRoleMock = mock(SecurityRole.class);
        given(securityRolesService.getRoleByIdentifier("ROLE_USER")).willReturn(userRoleMock);

        adminRoleMock = mock(SecurityRole.class);
        given(securityRolesService.getRoleByIdentifier("ROLE_ADMIN")).willReturn(adminRoleMock);

        xml1 = "view/test1.xml";
        xml2 = "view/test2.xml";

        given(applicationContext.getBean(CustomEntityService.class)).willReturn(new CustomEntityService());

        dataDefinitionA = mock(DataDefinition.class);
        dataDefinitionB = mock(DataDefinition.class);
        FieldDefinition nameA = mock(FieldDefinition.class, "nameA");
        FieldDefinition nameB = mock(FieldDefinition.class, "nameB");
        FieldDefinition hasManyB = mock(FieldDefinition.class, "hasManyB");
        FieldDefinition belongToA = mock(FieldDefinition.class, "belongsToA");
        HasManyType hasManyBType = mock(HasManyType.class);
        BelongsToType belongToAType = mock(BelongsToType.class);

        given(nameA.getDataDefinition()).willReturn(dataDefinitionA);
        given(nameA.getType()).willReturn(new StringType());
        given(nameB.getType()).willReturn(new StringType());
        given(nameB.getDataDefinition()).willReturn(dataDefinitionA);
        given(hasManyB.getType()).willReturn(hasManyBType);
        given(hasManyB.getDataDefinition()).willReturn(dataDefinitionB);
        given(belongToA.getType()).willReturn(belongToAType);
        given(belongToA.getDataDefinition()).willReturn(dataDefinitionB);
        given(hasManyBType.getDataDefinition()).willReturn(dataDefinitionB);
        given(belongToAType.getDataDefinition()).willReturn(dataDefinitionA);
        given(dataDefinitionA.getField("beansB")).willReturn(hasManyB);
        given(dataDefinitionA.getField("name")).willReturn(nameA);
        given(dataDefinitionB.getField("activeA")).willReturn(nameA);
        given(dataDefinitionB.getField("activeB")).willReturn(nameA);
        given(dataDefinitionB.getField("activeC")).willReturn(nameA);
        given(dataDefinitionB.getField("beanA")).willReturn(belongToA);
        given(dataDefinitionB.getField("beanM")).willReturn(belongToA);
        given(dataDefinitionB.getField("beanB")).willReturn(belongToA);
        given(dataDefinitionB.getField("name")).willReturn(nameB);
        given(dataDefinitionA.getName()).willReturn("beanA");
        given(dataDefinitionB.getName()).willReturn("beanB");
        given(dataDefinitionA.getFields()).willReturn(ImmutableMap.of("name", nameA, "beansB", hasManyB));
        given(dataDefinitionB.getFields()).willReturn(ImmutableMap.of("name", nameB, "beanA", belongToA));
        given(dataDefinitionService.get("sample", "beanA")).willReturn(dataDefinitionA);
        given(dataDefinitionService.get("sample", "beanB")).willReturn(dataDefinitionB);
    }

    @Test
    public void shouldParseXml() {
        // when
        List<InternalViewDefinition> viewDefinitions = parseAndGetViewDefinitions();

        // then
        assertEquals(2, viewDefinitions.size());
        assertNotNull(viewDefinitions.get(0));
        assertNotNull(viewDefinitions.get(1));
    }

    @Test
    public void shouldSetViewDefinitionAttributes() {
        // when
        InternalViewDefinition viewDefinition = parseAndGetViewDefinition();

        // then
        assertEquals("simpleView", viewDefinition.getName());
        assertEquals("sample", viewDefinition.getPluginIdentifier());
        assertEquals(userRoleMock, viewDefinition.getAuthorizationRole());
        assertThat(viewDefinition.getComponentByReference("mainWindow"), instanceOf(WindowComponentPattern.class));
    }

    @Test
    public void shouldSetCustomAuthorizationRole() {
        // when
        InternalViewDefinition viewDefinition = parseAndGetViewDefinition(xml2);

        // then
        assertEquals(adminRoleMock, viewDefinition.getAuthorizationRole());
    }

    @Test
    public void shouldShouldThrowExceptionIfCustomAuthorizationRoleCannotBeFound() {
        // given
        given(securityRolesService.getRoleByIdentifier("ROLE_ADMIN")).willReturn(null);

        // when & then
        try {
            parseAndGetViewDefinition(xml2);
            Assert.fail();
        } catch (ViewDefinitionParserException ignored) {
            // Success
        } catch (Exception ignored) {
            Assert.fail();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldHasRibbon() throws Exception {
        // given
        InternalViewDefinition viewDefinition = parseAndGetViewDefinition();
        TranslationService translationService = mock(TranslationService.class);
        setField(viewDefinition, "translationService", translationService);

        JSONObject jsOptions = (JSONObject) ((Map<String, Map<String, Object>>) viewDefinition.prepareView(new JSONObject(),
                Locale.ENGLISH).get("components")).get("mainWindow").get("jsOptions");

        JSONObject ribbon = jsOptions.getJSONObject("ribbon");

        // then
        assertNotNull(ribbon);
        assertEquals(2, ribbon.getJSONArray("groups").length());
        assertEquals("first", ribbon.getJSONArray("groups").getJSONObject(0).getString("name"));
        assertEquals(2, ribbon.getJSONArray("groups").getJSONObject(0).getJSONArray("items").length());
        assertEquals("second", ribbon.getJSONArray("groups").getJSONObject(1).getString("name"));
        assertEquals(2, ribbon.getJSONArray("groups").getJSONObject(1).getJSONArray("items").length());

        JSONObject item11 = ribbon.getJSONArray("groups").getJSONObject(0).getJSONArray("items").getJSONObject(0);

        assertEquals("test", item11.getString("name"));
        assertFalse(item11.has("icon"));
        assertEquals(RibbonActionItem.Type.BIG_BUTTON.toString(), item11.getString("type"));
        // assertEquals("#{mainWindow.beanBForm}.save,#{mainWindow}.back", item11.getString("clickAction"));

        JSONObject item12 = ribbon.getJSONArray("groups").getJSONObject(0).getJSONArray("items").getJSONObject(1);

        assertEquals("test2", item12.getString("name"));
        assertEquals("icon2", item12.getString("icon"));
        assertEquals(RibbonActionItem.Type.SMALL_BUTTON.toString(), item12.getString("type"));
        assertEquals("xxx", item12.getString("clickAction"));

        JSONObject item21 = ribbon.getJSONArray("groups").getJSONObject(1).getJSONArray("items").getJSONObject(0);

        assertEquals("test2", item21.getString("name"));
        assertFalse(item21.has("icon"));
        assertEquals(RibbonActionItem.Type.BIG_BUTTON.toString(), item21.getString("type"));
        assertFalse(item21.has("clickAction"));

        JSONObject item22 = ribbon.getJSONArray("groups").getJSONObject(1).getJSONArray("items").getJSONObject(1);

        assertEquals("combo1", item22.getString("name"));
        assertFalse(item22.has("icon"));
        assertEquals(RibbonActionItem.Type.BIG_BUTTON.toString(), item22.getString("type"));
        assertEquals("yyy3", item22.getString("clickAction"));
        assertEquals(2, item22.getJSONArray("items").length());

        JSONObject item221 = item22.getJSONArray("items").getJSONObject(0);

        assertEquals("test1", item221.getString("name"));
        assertFalse(item221.has("icon"));
        assertEquals(RibbonActionItem.Type.BIG_BUTTON.toString(), item221.getString("type"));
        assertEquals("yyy1", item221.getString("clickAction"));

        JSONObject item222 = item22.getJSONArray("items").getJSONObject(1);

        assertEquals("test2", item222.getString("name"));
        assertEquals("icon2", item222.getString("icon"));
        assertEquals(RibbonActionItem.Type.BIG_BUTTON.toString(), item222.getString("type"));
        assertEquals("yyy2", item222.getString("clickAction"));
    }

    @Test
    public void shouldSetFields() {
        // when
        InternalViewDefinition vd = parseAndGetViewDefinition();

        // then
        checkComponent(vd.getComponentByReference("mainWindow"), WindowComponentPattern.class, "mainWindow", "beanB");

        checkComponent(vd.getComponentByReference("beanBForm"), FormComponentPattern.class, "beanBForm", "beanB");

        checkComponent(vd.getComponentByReference("referenceToTextarea"), TextAreaComponentPattern.class, "name", "beanB");

        checkComponent(vd.getComponentByReference("active"), CheckBoxComponentPattern.class, "active", "beanB");

        checkComponent(vd.getComponentByReference("neverEnabledCheckbox"), CheckBoxComponentPattern.class,
                "neverEnabledCheckbox", "beanB");

        checkComponent(vd.getComponentByReference("enabledCheckbox"), CheckBoxComponentPattern.class, "enabledCheckbox", "beanB");

        checkComponent(vd.getComponentByReference("selectBeanA"), TextInputComponentPattern.class, "selectBeanA", "beanA");

        checkComponent(vd.getComponentByReference("beanM"), TextAreaComponentPattern.class, "beanM", "beanB");

        checkComponent(vd.getComponentByReference("beansBInnerGrig"), GridComponentPattern.class, "beansBGrig", "beanB");

        checkComponent(vd.getComponentByReference("beanAForm"), FormComponentPattern.class, "beanAForm", "beanA");

        checkComponent(vd.getComponentByReference("beanAFormName"), TextInputComponentPattern.class, "name", "beanA");

        checkComponent(vd.getComponentByReference("beansBGrig"), GridComponentPattern.class, "beansBGrig", "beanB");

        checkComponent(vd.getComponentByReference("link"), ButtonComponentPattern.class, "link", "beanB");

        checkFieldListener(vd.getComponentByReference("beanBForm"), vd.getComponentByReference("referenceToTextarea"), "name");
        checkFieldListener(vd.getComponentByReference("beanBForm"), vd.getComponentByReference("active"), "activeA");
        checkFieldListener(vd.getComponentByReference("beanBForm"), vd.getComponentByReference("beanAForm"), "beanA");
        checkFieldListener(vd.getComponentByReference("beanBForm"), vd.getComponentByReference("beanM"), "beanM");

    }

    @Test
    public final void shouldMarkComponentAsPermanentlyDisabled() {
        // when
        final InternalViewDefinition vd = parseAndGetViewDefinition();
        final ComponentPattern component = vd.getComponentByReference("neverEnabledCheckbox");

        // then
        assertTrue(component.isPermanentlyDisabled());
        assertFalse(component.isDefaultEnabled());
    }

    @Test
    public final void shouldMarkComponentAsNotEnabled() {
        // when
        final InternalViewDefinition vd = parseAndGetViewDefinition();
        final ComponentPattern component = vd.getComponentByReference("active");

        // then
        assertFalse(component.isPermanentlyDisabled());
        assertFalse(component.isDefaultEnabled());
    }

    @Test
    public final void shouldMarkComponentAsEnabled() {
        // when
        final InternalViewDefinition vd = parseAndGetViewDefinition();
        final ComponentPattern component = vd.getComponentByReference("enabledCheckbox");

        // then
        assertFalse(component.isPermanentlyDisabled());
        assertTrue(component.isDefaultEnabled());
    }

    @SuppressWarnings("unchecked")
    private void checkFieldListener(final ComponentPattern component, final ComponentPattern listener, final String field) {
        Map<String, ComponentPattern> listeners = (Map<String, ComponentPattern>) getField(component,
                "fieldEntityIdChangeListeners");
        ComponentPattern actualListener = listeners.get(field);
        assertEquals(listener, actualListener);
    }

    private void checkComponent(final ComponentPattern component, final Class<?> clazz, final String name, final String model) {
        assertNotNull(component);
        assertThat(component, instanceOf(clazz));
        assertEquals(name, component.getName());
    }

    @Test
    public void shouldSetHooks() {
        // when
        ViewDefinition viewDefinition = parseAndGetViewDefinition();

        // then
        testHookDefinition(viewDefinition, "beforeInitializeHooks", 0, CustomEntityService.class, "onView");
        testHookDefinition(viewDefinition, "afterInitializeHooks", 0, CustomEntityService.class, "onView");
        testHookDefinition(viewDefinition, "beforeRenderHooks", 0, CustomEntityService.class, "onView");
    }

    private void testHookDefinition(final Object object, final String hookFieldName, final int hookPosition,
            final Class<?> hookBeanClass, final String hookMethodName) {
        @SuppressWarnings("unchecked")
        HookDefinition hook = ((List<HookDefinition>) getField(object, hookFieldName)).get(hookPosition);

        assertNotNull(hook);
        assertThat(getField(hook, "bean"), instanceOf(hookBeanClass));
        assertEquals(hookMethodName, getField(hook, "methodName"));
    }

    private List<InternalViewDefinition> parseAndGetViewDefinitions() {
        List<InternalViewDefinition> views = new LinkedList<InternalViewDefinition>();
        views.add(viewDefinitionParser.parseViewXml(new ClassPathResource(xml1), "sample"));
        views.add(viewDefinitionParser.parseViewXml(new ClassPathResource(xml2), "sample"));
        return views;
    }

    private InternalViewDefinition parseAndGetViewDefinition() {
        return parseAndGetViewDefinition(xml1);
    }

    private InternalViewDefinition parseAndGetViewDefinition(final String xmlPath) {
        return viewDefinitionParser.parseViewXml(new ClassPathResource(xmlPath), "sample");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSetListeners() throws Exception {
        // when
        ComponentPattern component = parseAndGetViewDefinition().getComponentByReference("beanBForm");

        // then
        List<ComponentCustomEvent> customEvents = (List<ComponentCustomEvent>) getField(component, "customEvents");

        assertEquals("save", customEvents.get(0).getEvent());
        assertThat(customEvents.get(0).getObject(), instanceOf(CustomEntityService.class));
        assertEquals("saveForm", customEvents.get(0).getMethod());

        assertEquals("generate", customEvents.get(1).getEvent());
        assertThat(customEvents.get(1).getObject(), instanceOf(CustomEntityService.class));
        assertEquals("generate", customEvents.get(1).getMethod());
    }

}
