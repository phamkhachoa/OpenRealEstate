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

/* draw gallery with control buttons */

class AdminViewImagesWidget extends CWidget
{

    public $afterRefresh = false;
    public $objectId;
    public $images;
    public $withMain = true;

    public function getViewPath($checkTheme = true)
    {
        if ($checkTheme && ($theme = Yii::app()->getTheme()) !== null) {
            if (is_dir($theme->getViewPath() . DIRECTORY_SEPARATOR . 'modules' . DIRECTORY_SEPARATOR . 'images' . DIRECTORY_SEPARATOR . 'views'))
                return $theme->getViewPath() . DIRECTORY_SEPARATOR . 'modules' . DIRECTORY_SEPARATOR . 'images' . DIRECTORY_SEPARATOR . 'views';
        }
        return Yii::getPathOfAlias('application.modules.images.views');
    }

    public function run()
    {
        if (!$this->images) {
            $sql = 'SELECT id, file_name, id_object, file_name_modified, is_main FROM {{images}} WHERE id_object=:id ORDER BY sorter';
            $this->images = Images::model()->findAllBySql($sql, array(':id' => $this->objectId));
        }

        $this->render('widgetAdminViewImages', array(
            'images' => $this->images,
            'afterRefresh' => $this->afterRefresh,
        ));
    }
}
