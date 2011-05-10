package com.qcadoo.plugins.plugins.internal;

import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.plugin.constants.QcadooPluginConstants;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.GridComponent;

@Service
public class PluginManagmentViewHook {

    @Autowired
    private PluginManagmentPerformer pluginManagmentPerformer;

    @Autowired
    private DataDefinitionService dataDefinitionService;

    public void onDownloadButtonClick(final ViewDefinitionState viewDefinitionState, final ComponentState triggerState,
            final String[] args) {
        viewDefinitionState.openModal("../pluginPages/downloadPage.html");
    }

    public void onEnableButtonClick(final ViewDefinitionState viewDefinitionState, final ComponentState triggerState,
            final String[] args) {
        viewDefinitionState.openModal(pluginManagmentPerformer.performEnable(getPluginIdentifiersFromView(viewDefinitionState)));
    }

    public void onDisableButtonClick(final ViewDefinitionState viewDefinitionState, final ComponentState triggerState,
            final String[] args) {
        viewDefinitionState.openModal(pluginManagmentPerformer.performDisable(getPluginIdentifiersFromView(viewDefinitionState)));
    }

    public void onRemoveButtonClick(final ViewDefinitionState viewDefinitionState, final ComponentState triggerState,
            final String[] args) {
        viewDefinitionState.openModal(pluginManagmentPerformer.performRemove(getPluginIdentifiersFromView(viewDefinitionState)));
    }

    private List<String> getPluginIdentifiersFromView(final ViewDefinitionState viewDefinitionState) {

        List<String> pluginIdentifiers = new LinkedList<String>();
        GridComponent grid = (GridComponent) viewDefinitionState.getComponentByReference("grid");

        Preconditions.checkState(grid.getSelectedEntitiesIds().size() > 0, "No record selected");

        DataDefinition pluginDataDefinition = dataDefinitionService.get(QcadooPluginConstants.PLUGIN_IDENTIFIER,
                QcadooPluginConstants.MODEL_PLUGIN);
        for (Long entityId : grid.getSelectedEntitiesIds()) {
            Entity pluginEntity = pluginDataDefinition.get(entityId);
            pluginIdentifiers.add(pluginEntity.getStringField("identifier"));
        }

        return pluginIdentifiers;
    }

}
