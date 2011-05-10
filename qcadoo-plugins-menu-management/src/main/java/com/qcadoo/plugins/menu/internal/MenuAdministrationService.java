package com.qcadoo.plugins.menu.internal;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.CustomRestriction;
import com.qcadoo.model.api.search.SearchCriteriaBuilder;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FormComponent;
import com.qcadoo.view.api.components.GridComponent;
import com.qcadoo.view.api.components.WindowComponent;
import com.qcadoo.view.api.ribbon.Ribbon;
import com.qcadoo.view.api.utils.TranslationUtilsService;
import com.qcadoo.view.constants.QcadooViewConstants;

@Service
public class MenuAdministrationService {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private TranslationUtilsService translationUtilsService;

    private static final List<String[]> hiddenCategories = new ArrayList<String[]>();

    static {
        hiddenCategories.add(new String[] { "qcadooView", "home" });
        hiddenCategories.add(new String[] { "qcadooView", "administration" });
    }

    public void addRestrictionToCategoriesGrid(final ViewDefinitionState viewDefinitionState) {
        GridComponent categoriesGrid = (GridComponent) viewDefinitionState.getComponentByReference("grid");

        categoriesGrid.setCustomRestriction(new CustomRestriction() {

            @Override
            public void addRestriction(final SearchCriteriaBuilder searchCriteriaBuilder) {
                searchCriteriaBuilder.openAnd();

                for (String[] category : hiddenCategories) {
                    searchCriteriaBuilder.openNot().openAnd().isEq("pluginIdentifier", category[0]).isEq("name", category[1])
                            .closeAnd().closeNot();
                }

                searchCriteriaBuilder.closeAnd();
            }

        });
    }

    public void translateCategoriesGrid(final ViewDefinitionState viewDefinitionState) {
        GridComponent categoriesGrid = (GridComponent) viewDefinitionState.getComponentByReference("grid");
        for (Entity categoryEntity : categoriesGrid.getEntities()) {
            if (categoryEntity.getStringField("pluginIdentifier") != null) {
                categoryEntity.setField("name",
                        translationUtilsService.getCategoryTranslation(categoryEntity, viewDefinitionState.getLocale()));
            }
        }
    }

    public void translateCategoryForm(final ViewDefinitionState viewDefinitionState) {
        FormComponent categoryForm = (FormComponent) viewDefinitionState.getComponentByReference("form");
        Entity categoryEntity = null;
        if (categoryForm.getEntity() != null) {
            categoryEntity = dataDefinitionService.get(QcadooViewConstants.PLUGIN_IDENTIFIER, QcadooViewConstants.MODEL_CATEGORY)
                    .get(categoryForm.getEntity().getId());
        }

        if (categoryEntity != null && categoryEntity.getStringField("pluginIdentifier") != null) {
            ComponentState categoryNameField = viewDefinitionState.getComponentByReference("categoryName");
            categoryNameField.setEnabled(false);
            categoryNameField.setFieldValue(translationUtilsService.getCategoryTranslation(categoryEntity,
                    viewDefinitionState.getLocale()));

            disableWinfowButtons(viewDefinitionState);
        }

        GridComponent categoryItemsGrid = (GridComponent) viewDefinitionState.getComponentByReference("itemsGrid");
        for (Entity itemEntity : categoryItemsGrid.getEntities()) {
            if (itemEntity.getStringField("pluginIdentifier") != null) {
                itemEntity.setField("name",
                        translationUtilsService.getItemTranslation(itemEntity, viewDefinitionState.getLocale()));
            }
        }
    }

    public void translateItemForm(final ViewDefinitionState viewDefinitionState) {
        FormComponent itemForm = (FormComponent) viewDefinitionState.getComponentByReference("form");
        Entity itemEntity = null;
        if (itemForm.getEntity() != null) {
            itemEntity = dataDefinitionService.get(QcadooViewConstants.PLUGIN_IDENTIFIER, QcadooViewConstants.MODEL_ITEM).get(
                    itemForm.getEntity().getId());
        }
        if (itemEntity != null && itemEntity.getStringField("pluginIdentifier") != null) {
            ComponentState itemNameField = viewDefinitionState.getComponentByReference("itemName");
            itemNameField.setEnabled(false);
            itemNameField.setFieldValue(translationUtilsService.getItemTranslation(itemEntity, viewDefinitionState.getLocale()));

            viewDefinitionState.getComponentByReference("itemView").setEnabled(false);

            viewDefinitionState.getComponentByReference("itemActive").setEnabled(false);

            disableWinfowButtons(viewDefinitionState);
        }
    }

    private void disableWinfowButtons(final ViewDefinitionState viewDefinitionState) {
        WindowComponent window = (WindowComponent) viewDefinitionState.getComponentByReference("window");
        Ribbon ribbon = window.getRibbon();
        ribbon.getGroupByName("actions").getItemByName("save").setEnabled(false);
        ribbon.getGroupByName("actions").getItemByName("save").requestUpdate(true);
        ribbon.getGroupByName("actions").getItemByName("saveBack").setEnabled(false);
        ribbon.getGroupByName("actions").getItemByName("saveBack").requestUpdate(true);
        ribbon.getGroupByName("actions").getItemByName("cancel").setEnabled(false);
        ribbon.getGroupByName("actions").getItemByName("cancel").requestUpdate(true);
        window.requestRibbonRender();
    }
}
