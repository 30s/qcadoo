/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo Framework
 * Version: 0.4.7
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

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.qcadoo.localization.api.TranslationService;

@Controller
public final class LoginController {

    @Autowired
    private TranslationService translationService;

    @Value("${setAsDemoEnviroment}")
    private boolean setAsDemoEnviroment;

    @Value("${useCompressedStaticResources}")
    private boolean useCompressedStaticResources;

    @RequestMapping(value = "login", method = RequestMethod.GET)
    public ModelAndView getLoginPageView(@RequestParam(required = false) final String loginError,
            @RequestParam(required = false, defaultValue = "false") final Boolean iframe,
            @RequestParam(required = false, defaultValue = "false") final Boolean popup,
            @RequestParam(required = false, defaultValue = "false") final Boolean logout,
            @RequestParam(required = false, defaultValue = "") final String targetUrl,
            @RequestParam(required = false, defaultValue = "false") final Boolean timeout, final Locale locale) {

        ModelAndView mav = new ModelAndView();
        mav.setViewName("qcadooView/login");
        mav.addObject("translation", translationService.getMessagesGroup("security", locale));
        mav.addObject("currentLanguage", locale.getLanguage());
        mav.addObject("locales", translationService.getLocales());

        mav.addObject("iframe", iframe);
        mav.addObject("popup", popup);
        mav.addObject("targetUrl", targetUrl);

        if (logout) {
            mav.addObject("messageType", "success");
            mav.addObject("messageHeader", "security.message.logoutHeader");
            mav.addObject("messageContent", "security.message.logoutContent");
        } else if (timeout || iframe || popup) {
            mav.addObject("messageType", "info");
            mav.addObject("messageHeader", "security.message.timeoutHeader");
            mav.addObject("messageContent", "security.message.timeoutContent");
        } else if (loginError != null) {
            mav.addObject("messageType", "error");
            mav.addObject("messageHeader", "security.message.errorHeader");
            mav.addObject("messageContent", "security.message.errorContent");
        }

        if (setAsDemoEnviroment) {
            mav.addObject("showUserAndPassword", true);
        } else {
            mav.addObject("showUserAndPassword", false);
        }
        mav.addObject("useCompressedStaticResources", useCompressedStaticResources);

        return mav;
    }

    @RequestMapping(value = "accessDenied", method = RequestMethod.GET)
    public ModelAndView getAccessDeniedPageView(final Locale locale) {
        ModelAndView mav = new ModelAndView();
        mav.setViewName("qcadooView/accessDenied");

        mav.addObject("translation", translationService.getMessagesGroup("security", locale));

        return mav;
    }

    @RequestMapping(value = "browserNotSupported", method = RequestMethod.GET)
    public ModelAndView getBrowserNotSupportedView(final Locale locale) {
        ModelAndView mav = new ModelAndView();
        mav.setViewName("qcadooView/browserNotSupported");

        mav.addObject("locales", translationService.getLocales());
        mav.addObject("currentLanguage", locale.getLanguage());
        mav.addObject("translation", translationService.getMessagesGroup("browserNotSupported", locale));

        return mav;
    }
}
