<?php
/* * ********************************************************************************************
 * 								Open Real Estate
 * 								----------------
 * 	version				:	V1.39.0
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

class CustomPagination extends CPagination
{

    public function createPageUrl($controller, $page)
    {
        $params = $this->params === null ? $_GET : $this->params;
        //      if($page>0) // page 0 is the default
        $params[$this->pageVar] = $page + 1;
        //      else
        //              unset($params[$this->pageVar]);
        return $controller->createUrl($this->route, $params);
    }
}
