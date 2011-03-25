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

package com.qcadoo.plugin.internal.dependencymanager;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.plugin.api.Plugin;
import com.qcadoo.plugin.api.PluginAccessor;
import com.qcadoo.plugin.api.PluginDependencyInformation;
import com.qcadoo.plugin.api.PluginState;
import com.qcadoo.plugin.internal.api.PluginDependencyManager;

@Service
public final class DefaultPluginDependencyManager implements PluginDependencyManager {

    @Autowired
    private PluginAccessor pluginAccessor;

    @Override
    public PluginDependencyResult getDependenciesToEnable(final List<Plugin> plugins) {
        return getDependenciesToEnable(plugins, new HashSet<String>());
    }

    private PluginDependencyResult getDependenciesToEnable(final List<Plugin> plugins, final Set<String> markedNodes) {

        Set<PluginDependencyInformation> disabledDependencies = new HashSet<PluginDependencyInformation>();
        Set<PluginDependencyInformation> unsatisfiedDependencies = new HashSet<PluginDependencyInformation>();

        Set<String> argumentPluginIdentifiersSet = getArgumentIdentifiersSet(plugins);

        boolean isCyclic = false;

        for (Plugin plugin : plugins) {

            if (markedNodes.contains(plugin.getIdentifier())) {
                continue;
            }

            markedNodes.add(plugin.getIdentifier());

            for (PluginDependencyInformation dependencyInfo : plugin.getRequiredPlugins()) {
                if (markedNodes.contains(dependencyInfo.getDependencyPluginIdentifier())) {
                    isCyclic = true;
                    continue;
                }

                Plugin requiredPlugin = pluginAccessor.getPlugin(dependencyInfo.getDependencyPluginIdentifier());

                if (requiredPlugin == null) {
                    if (!argumentPluginIdentifiersSet.contains(dependencyInfo.getDependencyPluginIdentifier())) {
                        unsatisfiedDependencies.add(dependencyInfo);
                    }
                    continue;
                }

                if (!isPluginDisabled(requiredPlugin)) {
                    continue;
                }

                if (!dependencyInfo.isVersionSattisfied(requiredPlugin.getVersion())) {
                    unsatisfiedDependencies.add(dependencyInfo);
                } else {
                    disabledDependencies.add(dependencyInfo);

                    PluginDependencyResult nextLevelDependencioesResult = getDependenciesToEnable(
                            Collections.singletonList(requiredPlugin), markedNodes);

                    if (!nextLevelDependencioesResult.getUnsatisfiedDependencies().isEmpty()
                            || nextLevelDependencioesResult.isCyclic()) {
                        return nextLevelDependencioesResult;
                    }

                    disabledDependencies.addAll(nextLevelDependencioesResult.getDependenciesToEnable());
                }

            }
        }

        if (isCyclic) {
            return PluginDependencyResult.cyclicDependencies();
        }

        if (!unsatisfiedDependencies.isEmpty()) {
            return PluginDependencyResult.unsatisfiedDependencies(unsatisfiedDependencies);
        }

        if (disabledDependencies.isEmpty()) {
            return PluginDependencyResult.satisfiedDependencies();
        }

        Iterator<PluginDependencyInformation> dependencyInfoIterator = disabledDependencies.iterator();
        while (dependencyInfoIterator.hasNext()) {
            if (argumentPluginIdentifiersSet.contains(dependencyInfoIterator.next().getDependencyPluginIdentifier())) {
                dependencyInfoIterator.remove();
            }
        }

        return PluginDependencyResult.dependenciesToEnable(disabledDependencies);
    }

    @Override
    public PluginDependencyResult getDependenciesToDisable(final List<Plugin> plugins) {
        return PluginDependencyResult.dependenciesToDisable(getDependentPlugins(plugins, false));
    }

    @Override
    public PluginDependencyResult getDependenciesToUninstall(final List<Plugin> plugins) {
        return PluginDependencyResult.dependenciesToUninstall(getDependentPlugins(plugins, true));
    }

    @Override
    public PluginDependencyResult getDependenciesToUpdate(final Plugin existingPlugin, final Plugin newPlugin) {
        Set<PluginDependencyInformation> dependentPlugins = getDependentPlugins(Collections.singletonList(existingPlugin), false);

        Set<PluginDependencyInformation> dependenciesToDisableUnsatisfiedAfterUpdate = new HashSet<PluginDependencyInformation>();
        for (Plugin plugin : pluginAccessor.getPlugins()) {
            if (PluginState.TEMPORARY.equals(plugin.getState())) {
                continue;
            }
            for (PluginDependencyInformation dependencyInfo : plugin.getRequiredPlugins()) {
                if (dependencyInfo.getDependencyPluginIdentifier().equals(existingPlugin.getIdentifier())) {
                    if (!dependencyInfo.isVersionSattisfied(newPlugin.getVersion())) {
                        dependenciesToDisableUnsatisfiedAfterUpdate.add(new PluginDependencyInformation(plugin.getIdentifier()));
                    }
                    break;
                }
            }
        }

        return PluginDependencyResult.dependenciesToUpdate(dependentPlugins, dependenciesToDisableUnsatisfiedAfterUpdate);
    }

    private static class DependencyComparator implements Comparator<Plugin> {

        @Override
        public int compare(final Plugin o1, final Plugin o2) {
            if (o1.isSystemPlugin() && !o2.isSystemPlugin()) {
                return -1;
            } else if (!o1.isSystemPlugin() && o2.isSystemPlugin()) {
                return 1;
            }
            return 0;
        }
    }

    @Override
    public List<Plugin> sortPluginsInDependencyOrder(final Collection<Plugin> plugins) {

        Map<String, Set<String>> notInitializedPlugins = createPluginsMapWithDependencies(plugins);

        List<String> initializedPlugins = new LinkedList<String>();

        while (!notInitializedPlugins.isEmpty()) {
            Iterator<Map.Entry<String, Set<String>>> pluginsToInitializedIt = notInitializedPlugins.entrySet().iterator();
            while (pluginsToInitializedIt.hasNext()) {
                Map.Entry<String, Set<String>> pluginToInitializeEntry = pluginsToInitializedIt.next();
                if (pluginToInitializeEntry.getValue().isEmpty()) {
                    initializedPlugins.add(pluginToInitializeEntry.getKey());
                    pluginsToInitializedIt.remove();
                    for (Set<String> dependencies : notInitializedPlugins.values()) {
                        dependencies.remove(pluginToInitializeEntry.getKey());
                    }
                }
            }
        }

        List<Plugin> sortedPlugins = convertIdentifiersToPlugins(initializedPlugins);

        Collections.sort(sortedPlugins, new DependencyComparator());

        return sortedPlugins;
    }

    private Set<PluginDependencyInformation> getDependentPlugins(final List<Plugin> plugins, final boolean includeDisabled) {

        List<Plugin> enabledDependencyPlugins = new LinkedList<Plugin>();

        Set<String> argumentPluginIdentifiersSet = getArgumentIdentifiersSet(plugins);

        for (Plugin plugin : pluginAccessor.getPlugins()) {
            if (includeDisabled) {
                if (PluginState.TEMPORARY.equals(plugin.getState())) {
                    continue;
                }
            } else {
                if (!PluginState.ENABLED.equals(plugin.getState())) {
                    continue;
                }
            }
            for (Plugin pluginToDisable : plugins) {
                for (PluginDependencyInformation dependencyInfo : plugin.getRequiredPlugins()) {
                    if (dependencyInfo.getDependencyPluginIdentifier().equals(pluginToDisable.getIdentifier())) {
                        enabledDependencyPlugins.add(plugin);
                        break;
                    }
                }
            }
        }

        Set<PluginDependencyInformation> enabledDependencies = new HashSet<PluginDependencyInformation>();
        for (Plugin plugin : enabledDependencyPlugins) {
            enabledDependencies.add(new PluginDependencyInformation(plugin.getIdentifier()));
        }

        if (!enabledDependencyPlugins.isEmpty()) {
            PluginDependencyResult nextLevelDependencioesResult = getDependenciesToDisable(enabledDependencyPlugins);
            enabledDependencies.addAll(nextLevelDependencioesResult.getDependenciesToDisable());
        }

        Iterator<PluginDependencyInformation> dependencyInfoIterator = enabledDependencies.iterator();
        while (dependencyInfoIterator.hasNext()) {
            if (argumentPluginIdentifiersSet.contains(dependencyInfoIterator.next().getDependencyPluginIdentifier())) {
                dependencyInfoIterator.remove();
            }
        }

        return enabledDependencies;
    }

    private Map<String, Set<String>> createPluginsMapWithDependencies(final Collection<Plugin> plugins) {
        Map<String, Set<String>> resultMap = new HashMap<String, Set<String>>();
        for (Plugin plugin : plugins) {
            resultMap.put(plugin.getIdentifier(), null);
        }
        for (Plugin plugin : plugins) {
            Set<String> dependencyIdentifiers = new HashSet<String>();
            for (PluginDependencyInformation dependency : plugin.getRequiredPlugins()) {
                if (resultMap.containsKey(dependency.getDependencyPluginIdentifier())) {
                    dependencyIdentifiers.add(dependency.getDependencyPluginIdentifier());
                }
            }
            resultMap.put(plugin.getIdentifier(), dependencyIdentifiers);
        }
        return resultMap;
    }

    private List<Plugin> convertIdentifiersToPlugins(final Collection<String> pluginIdetifiers) {
        List<Plugin> resultList = new LinkedList<Plugin>();
        for (String pluginIdentifier : pluginIdetifiers) {
            resultList.add(pluginAccessor.getPlugin(pluginIdentifier));
        }
        return resultList;
    }

    // public List<Plugin> sortPluginsInDependencyOrder(final Collection<Plugin> plugins) {
    // List<Plugin> sortedPlugins = new LinkedList<Plugin>();
    //
    // for (Plugin plugin : plugins) {
    // sortedPlugins.add(plugin);
    // Collection<Plugin> requiredPlugins = getPluginsBasedOnDependencyInfo(plugin.getRequiredPlugins());
    // for (Plugin requiredPlugin : requiredPlugins) {
    // Collection<Plugin> dependencyPlugins = getPluginsBasedOnDependencyInfo(requiredPlugin.getRequiredPlugins());
    // if (!dependencyPlugins.isEmpty()) {
    // List<Plugin> recursivelySortedList = sortPluginsInDependencyOrder(dependencyPlugins);
    // sortedPlugins.addAll(recursivelySortedList);
    // }
    // }
    // }
    //
    // // TODO MADY: this one could be invoked only once at the very end to boost performance
    // removeDuplicateWithOrder(sortedPlugins);
    //
    // return sortedPlugins;
    // }
    //
    // private void removeDuplicateWithOrder(List<Plugin> list) {
    //
    // Collections.reverse(list);
    //
    // Set<Plugin> set = new HashSet<Plugin>();
    // List<Plugin> sortedList = new LinkedList<Plugin>();
    //
    // for (Plugin plugin : list) {
    // if (set.add(plugin)) {
    // sortedList.add(plugin);
    // }
    // }
    //
    // list.clear();
    // list.addAll(sortedList);
    // }
    //
    // private Collection<Plugin> getPluginsBasedOnDependencyInfo(Set<PluginDependencyInformation> requiredPlugins) {
    // List<Plugin> plugins = new ArrayList<Plugin>();
    //
    // for (PluginDependencyInformation dependencyInfo : requiredPlugins) {
    // plugins.add(pluginAccessor.getPlugin(dependencyInfo.getKey()));
    // }
    //
    // return plugins;
    // }

    private Set<String> getArgumentIdentifiersSet(final List<Plugin> plugins) {
        Set<String> argumentPluginInformationsSet = new HashSet<String>();
        for (Plugin plugin : plugins) {
            argumentPluginInformationsSet.add(plugin.getIdentifier());
        }
        return argumentPluginInformationsSet;
    }

    private boolean isPluginDisabled(final Plugin plugin) {
        return PluginState.DISABLED.equals(plugin.getState()) || PluginState.TEMPORARY.equals(plugin.getState());
    }

    void setPluginAccessor(final PluginAccessor pluginAccessor) {
        this.pluginAccessor = pluginAccessor;
    }

}
