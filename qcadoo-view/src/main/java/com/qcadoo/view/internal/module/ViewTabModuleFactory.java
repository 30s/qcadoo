package com.qcadoo.view.internal.module;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.qcadoo.plugin.api.ModuleFactory;
import com.qcadoo.view.internal.api.InternalViewDefinitionService;
import com.qcadoo.view.internal.xml.ViewDefinitionParser;

public class ViewTabModuleFactory extends ModuleFactory<ViewTabModule> {

    @Autowired
    private InternalViewDefinitionService viewDefinitionService;

    @Autowired
    private ViewDefinitionParser viewDefinitionParser;

    @SuppressWarnings("unchecked")
    @Override
    public ViewTabModule parse(final String pluginIdentifier, final Element element) {
        List<Resource> xmlFiles = new ArrayList<Resource>();
        for (Element resourceElement : (List<Element>) element.getChildren()) {
            String resource = resourceElement.getText();
            if (resource == null) {
                throw new IllegalStateException("Missing resource element of view module");
            }
            xmlFiles.add(new ClassPathResource(pluginIdentifier + "/" + resource));
        }
        return new ViewTabModule(xmlFiles, viewDefinitionService, viewDefinitionParser);
    }

    @Override
    public String getIdentifier() {
        return "viewWindowTabExtension";
    }

}
