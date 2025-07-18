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

class CustomActiveDataProvider extends CActiveDataProvider
{

    private $_pagination;

    // override to create instance of CustomPagination
    public function getPagination($className = 'CPagination')
    {
        if ($this->_pagination === null) {

            $this->_pagination = new CustomPagination;
            if (($id = $this->getId()) != '')
                $this->_pagination->pageVar = $id . '_page';
        }
        return $this->_pagination;
    }
}
