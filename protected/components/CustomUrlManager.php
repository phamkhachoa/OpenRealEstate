<?php
/* * ********************************************************************************************
 * 								Open Real Estate
 * 								----------------
 * 	version				:	V1.29.1
 * 	copyright			:	(c) 2016 Monoray
 * 							http://monoray.net
 * 							http://monoray.ru
 *
 * 	website				:	http://open-real-estate.info/en
 *
 * 	contact us			:	http://open-real-estate.info/en/contact-us
 *
 * 	license:			:	http://open-real-estate.info/en/license
 * 							http://open-real-estate.info/ru/license
 *
 * This file is part of Open Real Estate
 *
 * ********************************************************************************************* */

class CustomUrlManager extends CUrlManager
{

    private $langRoute;
    private $isInstalled = false;

    public function init()
    {
        $langs = Lang::getActiveLangs();
        $defaultLang = Lang::getDefaultLang();
        $this->isInstalled = oreInstall::isInstalled();

        $keyDefault = array_search($defaultLang, $langs);
        if ($keyDefault !== false && $this->isInstalled) {
            unset($langs[$keyDefault]);
        }

        $this->langRoute = '<lang:' . implode('|', $langs) . '>';

        $rules = array(
            'sitemap.xml' => 'sitemap/main/viewxml',
            'yandex-export-feed.xml' => 'yandexRealty/main/viewfeed',
            'version' => '/site/version',
            'min/serve/g/' => 'min/serve/',
            'rss' => 'quicksearch/main/mainsearch/rss/1',
            //'/property/'=>'quicksearch/main/mainsearch',
            '<module:\w+>/backend/<controller:\w+>/<action:\w+>' => '<module>/backend/<controller>/<action>', // CGridView ajax
        );

        if (issetModule('api')) {
            $apiRulesPath = Yii::getPathOfAlias('application.modules.api.components.rest.config') . DIRECTORY_SEPARATOR . 'routes.php';
            if (file_exists($apiRulesPath)) {
                $apiRules = require_once($apiRulesPath);

                if (is_array($apiRules) && !empty($apiRules)) {
                    $rules = CMap::mergeArray($rules, $apiRules);
                }
                unset($apiRulesPath, $apiRules);
            }
        }

        $rulesLangOne = array(
            '/' => 'site/index',
            '/login' => 'site/login',
            '/soc-login' => 'site/socLogin',
            '/admin' => 'site/login',
            '/administrator' => 'site/login',
            '/register' => 'site/register',
            '/logincode' => 'site/logincode',
            '/recover' => 'site/recover',
            '/site/recover' => '/site/recover',
            '/logout' => 'site/logout',
            '/activationUser' => 'site/activation',
            '/site/activation' => 'site/activation',
            '/map/getMarkersViewAllMap' => 'site/getMarkersViewAllMap',
            '/sell' => 'quicksearch/main/mainsearch/type/2',
            '/rent' => 'quicksearch/main/mainsearch/type/1',
            '/min/serve/g/' => 'min/serve/',
            //'/site/login/vkontakte' => array('site/login', 'service' => 'vkontakte', 'caseSensitive' => false),
            '/site/login/<service:\w+>' => 'site/login',
            '/themes/widget' => 'themes/widget',
            '/callback' => 'site/callback',
        );

        $rulesSeoSummaryInfo = array();
        if ($this->isInstalled && issetModule('seo')) {
            $cityRoute = SeoFriendlyUrl::getActiveCityRoute();
            if (!empty($cityRoute) && is_array($cityRoute)) {
                $routeToCityPage = array();

                foreach ($cityRoute as $arr) {
                    if (is_array($arr) && !empty($arr)) {
                        foreach ($arr as $item) {
                            $routeToCityPage[$item['url']] = $item['lang'];
                        }
                    }
                }

                $objTypesRoute = SeoFriendlyUrl::getActiveObjTypesRoute();

                if (!empty($objTypesRoute) && is_array($objTypesRoute)) {
                    $routeToObjTypePage = array();

                    foreach ($objTypesRoute as $arr) {
                        if (is_array($arr) && !empty($arr)) {
                            foreach ($arr as $item) {
                                $routeToObjTypePage[$item['url']] = $item['lang'];
                            }
                        }
                    }

                    $rulesSeoSummaryInfo['/<cityUrlName:(' . implode('|', array_keys($routeToCityPage)) . ')>/<objTypeUrlName:(' . implode('|', array_keys($routeToObjTypePage)) . ')>'] = 'seo/main/viewsummaryinfo';
                }

                $rulesSeoSummaryInfo['/<cityUrlName:(' . implode('|', array_keys($routeToCityPage)) . ')>'] = 'seo/main/viewsummaryinfo';
            }
        }

        $rulesLangEntries = array();
        if ($this->isInstalled) {
            if (Yii::app()->db->schema->getTable('{{entries_category}}')) {
                $entriesCategoryRoute = EntriesCategory::getEntriesCategoryRoute();

                if (!empty($entriesCategoryRoute) && is_array($entriesCategoryRoute)) {
                    $routeToCategories = array();
                    if (issetModule('seo')) {
                        foreach ($entriesCategoryRoute as $arr) {
                            if (is_array($arr) && !empty($arr)) {
                                foreach ($arr as $item) {
                                    $routeToCategories[$item['url']] = $item['lang'];
                                }
                            }
                        }
                        $rulesLangEntries['/<catUrlName:(' . implode('|', array_keys($routeToCategories)) . ')>'] = 'entries/main/index';
                    } else {
                        $rulesLangEntries['/<catUrlName:(' . implode('|', array_keys($entriesCategoryRoute)) . ')>'] = 'entries/main/index';
                    }

                    # nothing without main category
                    $entriesRoute = Entries::getEntriesRoute();

                    if (!empty($entriesRoute) && is_array($entriesRoute)) {
                        if (issetModule('seo')) {
                            $rulesLangEntries['/<catUrlName:(' . implode('|', array_keys($routeToCategories)) . ')>/<url:([-a-zA-Z0-9_+\.]{1,255})>'] = 'entries/main/view';
                        } else {
                            foreach ($entriesRoute as $idEntry => $catEntry) {
                                $rulesLangEntries['/<catUrlName:(' . $catEntry . ')>/<id:(' . $idEntry . ')>'] = 'entries/main/view';
                            }
                        }
                    }

                    $rulesLangEntries['/<catUrlName:(' . implode('|', array_keys($routeToCategories)) . ')>/<tag:[\w\s,]>'] = 'entries/main/index';
                }
            }
        }

        $rulesLangTwo = array(
            '/favorites' => 'favorite/main/index',
            '/entries' => 'entries/main/index',
            '/entries/<catUrlName:[-a-zA-Z0-9_+\.]{1,255}>' => 'entries/main/index',
            '/entries/<catUrlName:[-a-zA-Z0-9_+\.]{1,255}>/<tag:[\w\s,]' => 'entries/main/index',
            '/entries/<catUrlName:[-a-zA-Z0-9_+\.]{1,255}>/<id:\d+>' => 'entries/main/view',
            '/entries/<catUrlName:[-a-zA-Z0-9_+\.]{1,255}>/<url:[-a-zA-Z0-9_+\.]{1,255}>' => 'entries/main/view',
            '/yandex-export/feed-<id:\d+>.xml' => 'yandexRealty/main/feed',
            '/faq' => 'articles/main/index',
            '/faq/<id:\d+>' => 'articles/main/view',
            '/faq/<url:[-a-zA-Z0-9_+\.]{1,255}>' => 'articles/main/view',
            '/contact-us' => 'contactform/main/index',
            '/specialoffers' => 'specialoffers/main/index',
            '/sitemap' => 'sitemap/main/index',
            '/reviews' => 'reviews/main/index',
            '/reviews/add' => 'reviews/main/add',
            '/guestad/add' => 'guestad/main/create',
            '/page/<id:\d+>' => 'infopages/main/view',
            '/page/<url:[-a-zA-Z0-9_+\.]{1,255}>' => 'infopages/main/view',
            '/search' => 'quicksearch/main/mainsearch',
            '/comparisonList' => 'comparisonList/main/index',
            '/complain/add' => 'apartmentsComplain/main/complain',
            '/booking/add' => 'booking/main/bookingform',
            '/booking/request' => 'booking/main/mainform',
            '/usercpanel' => 'usercpanel/main/index',
            '/usercpanel/data' => 'usercpanel/main/data',
            '/usercpanel/changepwd' => 'usercpanel/main/changepassword',
            '/usercpanel/tariffplans' => 'tariffPlans/main/index',
            '/usercpanel/payments' => 'usercpanel/main/payments',
            '/usercpanel/balance' => 'usercpanel/main/balance',
            '/usercpanel/bookingtable' => 'bookingtable/main/index',
            '/usercpanel/bookingtable/my' => 'bookingtable/main/my',
            '/usercpanel/bookingtable/payforbooking' => 'paidservices/main/payForBooking',
            '/usercpanel/comments' => 'comments/main/commentsuserlist',
            '/userlistings' => 'userads/main/index',
            '/userlistings/drafts' => 'userads/main/drafts',
            '/userlistings/create' => 'userads/main/create',
            '/userlistings/edit' => 'userads/main/update',
            '/userlistings/delete' => 'userads/main/delete',
            '/userlistings/savecoords' => 'userads/main/savecoords',
            '/userlistings/activate' => 'userads/main/activate',
            '/userlistings/clone' => 'userads/main/clone',
            '/users/view' => '/users/main/view',
            '/users/viewall' => 'users/main/search',
            '/users/alllistings' => 'apartments/main/alllistings',
            '/apartments/sendEmail' => 'apartments/main/sendEmail',
            '/apartments/viewDetailsViewsStats' => 'apartments/main/viewDetailsViewsStats',
            '/mailbox' => 'messages/main/index',
            '/mailbox/send' => 'messages/main/sendform',
            '/mailbox/read' => 'messages/main/read',
            '/mailbox/delete' => 'messages/main/delete',
            '/mailbox/deleteMessage' => 'messages/main/deleteMessage',
            '/messages/downloadFile' => 'messages/main/downloadFile',
            '/favorites/list' => 'favorite/main/list',
            '/service-<serviceId:\d+>' => 'quicksearch/main/mainsearch',
            '/property/downloadDocument' => 'apartments/main/downloadDocument',
            '/property/renameDocument' => 'apartments/main/renameDocument',
            '/property/deleteDocument' => 'apartments/main/deleteDocument',
            '/property/<id:\d+>' => 'apartments/main/view',
            '/property/<url:[-a-zA-Z0-9_+\.]{1,255}>' => 'apartments/main/view',
            '/<controller:(quicksearch|specialoffers)>/main/index' => '<controller>/main/index',
            '/<_m>/<_c>/<_a>*' => '<_m>/<_c>/<_a>',
            '/<_c>/<_a>*' => '<_c>/<_a>',
            '/<_c>' => '<_c>',
        );

        $rulesLang = CMap::mergeArray($rulesLangOne, $rulesSeoSummaryInfo);
        $rulesLang = CMap::mergeArray($rulesLang, $rulesLangEntries);
        $rulesLang = CMap::mergeArray($rulesLang, $rulesLangTwo);

        foreach ($rulesLang as $key => $rule) {
            if ($langs && $this->langRoute) {
                $rules[$this->langRoute . $key] = $rule;
            }
            $rules[$key] = array($rule, 'defaultParams' => array('lang' => $defaultLang));
        }

        if ($langs && $this->langRoute) {
            $rules[$this->langRoute] = '';
        }

        $this->addRules($rules);

        if ($this->isInstalled) {
            $modules = Yii::app()->getModules();

            $paramModules = ConfigurationModel::getModulesList();
            foreach ($paramModules as $module) {
                if (isset($modules[$module]) && !param('module_enabled_' . $module)) {
                    $modules[$module]['enabled'] = false;
                }
            }

            Yii::app()->setModules($modules);
        }
        return parent::init();
    }

    private $parseReady = false;

    public function parseUrl($request)
    {
        if (issetModule('seo') && $this->parseReady === false && oreInstall::isInstalled()) {
            if (preg_match('#^([\w-]+)#i', $request->pathInfo, $matches)) {
                $activeLangs = Lang::getActiveLangs();
                $arr = array();
                foreach ($activeLangs as $lang) {
                    $arr[] = 'url_' . $lang . ' = :alias';
                }
                $condition = '(' . implode(' OR ', $arr) . ')';

                $seo = SeoFriendlyUrl::model()->find(array(
                    'condition' => 'direct_url = 1 AND ' . $condition,
                    'params' => array('alias' => $matches[1])
                ));

                if ($seo !== null) {
                    foreach ($activeLangs as $lang) {
                        $field = 'url_' . $lang;

                        if ($seo->$field == $matches[1]) {

                            setLangCookie($lang);
                            Yii::app()->setLanguage($lang);
                            //$_GET['lang'] = $lang;
                        }
                    }
                    $_GET['url'] = $matches[1];
                    //$_GET['id'] = $seo->model_id;
                    $_GET['is_direct_url'] = 1;

                    //Yii::app()->controller->seo = $seo;
                    return 'infopages/main/view';
                }
            }

            $this->parseReady = true;
        }

        return parent::parseUrl($request);
    }

    public function createUrl($route, $params = array(), $ampersand = '&')
    {
        if ($route != 'min/serve') {
            $langs = Lang::getActiveLangs();
            $countLangs = count($langs);
            $defaultLang = Lang::getDefaultLang();

            if (isset($params['lang']) && $params['lang'] == $defaultLang && $this->isInstalled) {
                if (!param('useBootstrap')) {
                    unset($params['lang']);
                }
            } else if (Yii::app()->language != $defaultLang && !isFree() && empty($params['lang']) && $countLangs > 1) {
                $params['lang'] = Yii::app()->language;
            }

            if (!$this->isInstalled && $countLangs == 1 && $route != 'install') {
                $params['lang'] = Yii::app()->language;
            }

            if (!$this->isInstalled && $countLangs > 1 && !isset($params['lang']) && $route != 'install') {
                $params['lang'] = Yii::app()->language;
            }

            # for direct url seo_url_friendly
            if ($route == 'infopages/main/view' && is_array($params) && count($params)) {
                $url = (isset($params['url']) && $params['url']) ? $params['url'] : '';
                $isDirectUrl = (isset($params['is_direct_url']) && $params['is_direct_url']) ? $params['is_direct_url'] : '';

                if ($url && $isDirectUrl) {
                    $stringParams = array();
                    if (isset($params['sort']))
                        $stringParams['sort'] = $params['sort'];

                    if (isset($params['page']))
                        $stringParams['page'] = $params['page'];

                    if ($url) {
                        $anchor = (isset($params['#'])) ? '#' . $params['#'] : '';

                        if (count($stringParams)) {
                            return $this->getBaseUrl() . '/' . $url . '?' . http_build_query($stringParams) . $anchor;
                        } else {
                            return $this->getBaseUrl() . '/' . $url . $anchor;
                        }
                    }
                }
            }
        }

        return parent::createUrl($route, $params, $ampersand);
    }
}
