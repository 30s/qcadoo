/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 0.3.0
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

package com.qcadoo.view.internal.controllers;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.qcadoo.localization.api.TranslationService;
import com.qcadoo.view.api.crud.CrudService;

@Controller
public final class SystemInfoController {

    @Autowired
    private TranslationService translationService;

    @Autowired
    private CrudService crudController;

    @Value("${buildApplicationName:-}")
    private String buildApplicationName;

    @Value("${buildApplicationVersion:-}")
    private String buildApplicationVersion;

    @Value("${buildFrameworkVersion:-}")
    private String buildFrameworkVersion;

    @Value("${buildTime:-}")
    private String buildTime;

    @Value("${buildNumber:-}")
    private String buildNumber;

    @Value("${buildRevision:-}")
    private String buildRevision;

    @Value("${useCompressedStaticResources}")
    private boolean useCompressedStaticResources;

    @RequestMapping(value = "systemInfo", method = RequestMethod.GET)
    public ModelAndView getSystemInfoView(@RequestParam final Map<String, String> arguments, final Locale locale) {
        ModelAndView mav = crudController.prepareView("qcadooView", "systemInfo", arguments, locale);

        Map<String, String> translationsMap = new HashMap<String, String>();
        translationsMap.put("core.systemInfo.header", translationService.translate("core.systemInfo.header", locale));
        translationsMap.put("core.systemInfo.buildApplicationName.label",
                translationService.translate("core.systemInfo.buildApplicationName.label", locale));
        translationsMap.put("core.systemInfo.buildApplicationVersion.label",
                translationService.translate("core.systemInfo.buildApplicationVersion.label", locale));
        translationsMap.put("core.systemInfo.buildFrameworkVersion.label",
                translationService.translate("core.systemInfo.buildFrameworkVersion.label", locale));
        translationsMap.put("core.systemInfo.buildNumber.label",
                translationService.translate("core.systemInfo.buildNumber.label", locale));
        translationsMap.put("core.systemInfo.buildRevision.label",
                translationService.translate("core.systemInfo.buildRevision.label", locale));
        translationsMap.put("core.systemInfo.buildTime.label",
                translationService.translate("core.systemInfo.buildTime.label", locale));
        mav.addObject("translationsMap", translationsMap);

        mav.addObject("buildApplicationName", buildApplicationName);
        mav.addObject("buildApplicationVersion", buildApplicationVersion);
        mav.addObject("buildFrameworkVersion", buildFrameworkVersion);
        mav.addObject("buildNumber", buildNumber);
        mav.addObject("buildTime", buildTime);
        mav.addObject("buildRevision", buildRevision);
        mav.addObject("useCompressedStaticResources", useCompressedStaticResources);

        return mav;
    }
}
