/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package com.djrapitops.plan.extension.implementation;

import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.annotation.*;
import com.djrapitops.plan.extension.extractor.ExtensionExtractor;
import com.djrapitops.plan.extension.extractor.MethodAnnotations;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Icon;
import com.djrapitops.plan.extension.implementation.providers.*;
import com.djrapitops.plan.utilities.java.Lists;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Extracts objects that can be used to obtain data to store in the database.
 * <p>
 * Goal of this class is to abstract away DataExtension API annotations so that they will not be needed outside when calling methods.
 *
 * @author Rsl1122
 */
public class DataProviderExtractor {

    private final ExtensionExtractor extractor;
    private final DataProviders providers;

    /**
     * Create a DataProviderExtractor.
     *
     * @param extension DataExtension to extract information from.
     * @throws IllegalArgumentException If something is badly wrong with the specified extension class annotations.
     */
    public DataProviderExtractor(DataExtension extension) {
        extractor = new ExtensionExtractor(extension);

        extractor.extractAnnotationInformation();

        providers = new DataProviders();
        extractProviders();
    }

    public String getPluginName() {
        return extractor.getPluginInfo().name();
    }

    public Icon getPluginIcon() {
        PluginInfo pluginInfo = extractor.getPluginInfo();
        return new Icon(pluginInfo.iconFamily(), pluginInfo.iconName(), pluginInfo.color());
    }

    public Collection<TabInformation> getPluginTabs() {
        Map<String, TabInfo> tabInformation = extractor.getTabInformation()
                .stream().collect(Collectors.toMap(TabInfo::tab, Function.identity(), (one, two) -> one));

        Map<String, Integer> order = getTabOrder().map(this::orderToMap).orElse(new HashMap<>());

        // Extracts PluginTabs
        return extractor.getMethodAnnotations().getAnnotations(Tab.class).stream()
                .map(Tab::value)
                .distinct()
                .map(tabName -> {
                    Optional<TabInfo> tabInfo = Optional.ofNullable(tabInformation.get(tabName));
                    return new TabInformation(
                            tabName,
                            tabInfo.map(info -> new Icon(info.iconFamily(), info.iconName(), Color.NONE)).orElse(null),
                            tabInfo.map(TabInfo::elementOrder).orElse(null),
                            order.getOrDefault(tabName, 100)
                    );
                }).collect(Collectors.toList());
    }

    private Map<String, Integer> orderToMap(String[] order) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < order.length; i++) {
            map.put(order[i], i);
        }
        return map;
    }

    public Optional<String[]> getTabOrder() {
        return extractor.getTabOrder().map(TabOrder::value);
    }

    public Collection<String> getInvalidatedMethods() {
        return Lists.mapUnique(extractor.getInvalidateMethodAnnotations(), InvalidateMethod::value);
    }

    public DataProviders getProviders() {
        return providers;
    }

    private void extractProviders() {
        PluginInfo pluginInfo = extractor.getPluginInfo();

        MethodAnnotations methodAnnotations = extractor.getMethodAnnotations();
        Map<Method, Tab> tabs = methodAnnotations.getMethodAnnotations(Tab.class);
        Map<Method, Conditional> conditions = methodAnnotations.getMethodAnnotations(Conditional.class);

        extractProviders(pluginInfo, tabs, conditions, BooleanProvider.class, BooleanDataProvider::placeToDataProviders);
        extractProviders(pluginInfo, tabs, conditions, DoubleProvider.class, DoubleDataProvider::placeToDataProviders);
        extractProviders(pluginInfo, tabs, conditions, PercentageProvider.class, PercentageDataProvider::placeToDataProviders);
        extractProviders(pluginInfo, tabs, conditions, NumberProvider.class, NumberDataProvider::placeToDataProviders);
        extractProviders(pluginInfo, tabs, conditions, StringProvider.class, StringDataProvider::placeToDataProviders);
        extractProviders(pluginInfo, tabs, conditions, TableProvider.class, TableDataProvider::placeToDataProviders);
        extractProviders(pluginInfo, tabs, conditions, GroupProvider.class, GroupDataProvider::placeToDataProviders);
    }

    private <T extends Annotation> void extractProviders(PluginInfo pluginInfo, Map<Method, Tab> tabs, Map<Method, Conditional> conditions, Class<T> ofKind, DataProviderFactory<T> factory) {
        for (Map.Entry<Method, T> entry : extractor.getMethodAnnotations().getMethodAnnotations(ofKind).entrySet()) {
            Method method = entry.getKey();
            T annotation = entry.getValue();
            Optional<Conditional> conditional = Optional.ofNullable(conditions.get(method));
            Optional<Tab> tab = Optional.ofNullable(tabs.get(method));

            factory.placeToDataProviders(
                    providers, method, annotation,
                    conditional.orElse(null),
                    tab.map(Tab::value).orElse(null),
                    pluginInfo.name()
            );
        }
    }

    public Collection<String> getWarnings() {
        return extractor.getWarnings();
    }

    /**
     * Functional interface for defining a method that places required DataProvider to DataProviders.
     *
     * @param <T> Type of the annotation on the method that is going to be extracted.
     */
    interface DataProviderFactory<T extends Annotation> {
        void placeToDataProviders(
                DataProviders dataProviders,
                Method method, T annotation, Conditional condition, String tab, String pluginName
        );
    }
}