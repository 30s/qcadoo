<%--

    ***************************************************************************
    Copyright (c) 2010 Qcadoo Limited
    Project: Qcadoo Framework
    Version: 0.4.7

    This file is part of Qcadoo.

    Qcadoo is free software; you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation; either version 3 of the License,
    or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty
    of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
    ***************************************************************************

--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core_rt" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>
<head>

	<title>QCADOO MES</title>
	
	<c:choose>
		<c:when test="${useCompressedStaticResources}">
			<link rel="stylesheet" href="${pageContext.request.contextPath}/qcadooView/public/qcadoo-min.css" type="text/css" />
			<script type="text/javascript" src="${pageContext.request.contextPath}/qcadooView/public/js/core/lib/_jquery-1.4.2.min.js"></script>
			<script type="text/javascript" src="${pageContext.request.contextPath}/qcadooView/public/js/core/lib/jquery-ui-1.8.5.custom.min.js"></script>
			<script type="text/javascript" src="${pageContext.request.contextPath}/qcadooView/public/js/core/lib/jquery.jqGrid.min.js"></script>
			<script type="text/javascript" src="${pageContext.request.contextPath}/qcadooView/public/qcadoo-min.js"></script>
		</c:when>
		<c:otherwise>
			<link rel="stylesheet" href="${pageContext.request.contextPath}/qcadooView/public/css/core/qcd.css" type="text/css" />
			<link rel="stylesheet" href="${pageContext.request.contextPath}/qcadooView/public/css/core/mainPage.css" type="text/css" />
			<link rel="stylesheet" href="${pageContext.request.contextPath}/qcadooView/public/css/core/menuTopLevel.css" type="text/css" />
			<link rel="stylesheet" href="${pageContext.request.contextPath}/qcadooView/public/css/core/menu/style.css" type="text/css" />
			<link rel="stylesheet" href="${pageContext.request.contextPath}/qcadooView/public/css/core/notification.css" type="text/css" />
			<link rel="stylesheet" href="${pageContext.request.contextPath}/qcadooView/public/css/core/jqModal.css" type="text/css" />
		
			<script type="text/javascript" src="${pageContext.request.contextPath}/qcadooView/public/js/core/lib/_jquery-1.4.2.min.js"></script>
			<script type="text/javascript" src="${pageContext.request.contextPath}/qcadooView/public/js/core/lib/jquery.pnotify.js"></script>
			<script type="text/javascript" src="${pageContext.request.contextPath}/qcadooView/public/js/core/lib/jquery.blockUI.js"></script>
			<script type="text/javascript" src="${pageContext.request.contextPath}/qcadooView/public/js/core/lib/jqModal.js"></script>
			<script type="text/javascript" src="${pageContext.request.contextPath}/qcadooView/public/js/core/qcd/utils/logger.js"></script>
			<script type="text/javascript" src="${pageContext.request.contextPath}/qcadooView/public/js/core/qcd/utils/modal.js"></script>
			<script type="text/javascript" src="${pageContext.request.contextPath}/qcadooView/public/js/core/qcd/utils/connector.js"></script>
			<script type="text/javascript" src="${pageContext.request.contextPath}/qcadooView/public/js/core/qcd/menu/model.js"></script>
			<script type="text/javascript" src="${pageContext.request.contextPath}/qcadooView/public/js/core/qcd/menu/menuController.js"></script>
			<script type="text/javascript" src="${pageContext.request.contextPath}/qcadooView/public/js/core/qcd/core/windowController.js"></script>
			<script type="text/javascript" src="${pageContext.request.contextPath}/qcadooView/public/js/core/qcd/core/messagesController.js"></script>
			<script type="text/javascript" src="${pageContext.request.contextPath}/qcadooView/public/js/crud/qcd/components/elements/utils/loadingIndicator.js"></script>
			
			<script type="text/javascript" src="${pageContext.request.contextPath}/qcadooView/public/js/core/qcd/utils/snow.js"></script>
		</c:otherwise>
	</c:choose>
	
	<link rel="shortcut icon" href="/qcadooView/public/img/core/icons/favicon.png">
	
	<script type="text/javascript">

		var menuStructure = ${menuStructure}

		var windowController;
		
		jQuery(document).ready(function(){
			
			windowController = new QCD.WindowController(menuStructure);
			
			$("#mainPageIframe").load(function() {
				try {
					el = $('body', $('iframe').contents());
					el.click(function() {windowController.restoreMenuState()});
				} catch(e) {
				}
			});
		});

		window.goToPage = function(url, serializationObject, isPage) {
			windowController.goToPage(url, serializationObject, isPage);
		}

		window.openModal = function(id, url, serializationObject, onCloseListener) {
			windowController.openModal(id, url, serializationObject, onCloseListener);
		}

		window.changeModalSize = function(width, height) {
			windowController.changeModalSize(width, height);
		}

		window.goBack = function(pageController) {
			windowController.goBack(pageController);
		}

		window.closeThisModalWindow = function(status) {
			windowController.closeThisModalWindow(status);
		}

		window.getLastPageController = function() {
			return windowController.getLastPageController();
		}

		window.goToLastPage = function() {
			windowController.goToLastPage();
		}

		window.onSessionExpired = function(serializationObject, isModal) {
			windowController.onSessionExpired(serializationObject, isModal);
		}

		window.addMessage = function(type, content) {
			windowController.addMessage(type, content);
		}

		window.onLoginSuccess = function() {
			windowController.onLoginSuccess();
		}
		
		window.goToMenuPosition = function(position) {
			windowController.goToMenuPosition(position);
		}

		window.activateMenuPosition = function(position) {
 			windowController.activateMenuPosition(position);
 		}

		window.hasMenuPosition = function(position) {
			return windowController.hasMenuPosition(position);
		}

		window.updateMenu = function() {
			windowController.updateMenu();
		}

		window.getCurrentUserLogin = function() {
			return "${userLogin}";
		}
	
		window.translationsMap = new Object();
		<c:forEach items="${commonTranslations}" var="translation">
			window.translationsMap["${translation.key}"] = "${translation.value}";
		</c:forEach>
	
		
	</script>

</head>
<body>

	<div id="mainTopMenu">
		<div id="topLevelMenu">
			<img id="logoImage" src="/qcadooView/public/css/core/images/logo_small.png" onclick="windowController.goToDashboard()"></img>
			<div id="topRightPanel">
				<span id="userInfo">${userLogin}</span>
				<a href='#' id="profileButton" onclick="windowController.goToMenuPosition('administration.profile')">${commonTranslations["qcadooView.button.userProfile"] }</a>
				<a href='#' onclick="windowController.performLogout()">${commonTranslations["qcadooView.button.logout"] }</a>
			</div>
		</div>
		<div id="firstLevelMenu">
		</div>
		<div id="secondLevelMenuWrapper">
			<div id="secondLevelMenu">
			</div>
			</div>
	</div>
	<div id="mainPageIframeWrapper"><iframe id="mainPageIframe" frameborder="0"></iframe></div>
</body>
</html>