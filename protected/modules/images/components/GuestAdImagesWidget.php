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

/* draw area with gallery (with control buttons, inputs for comments) and uploader */

class GuestAdImagesWidget extends CWidget
{

    public $sessionId;

    public function getViewPath($checkTheme = true)
    {
        return Yii::getPathOfAlias('application.modules.images.views');
    }

    public function run()
    {
        $this->registerAssets();

        $guestAdImages = null;
        $filePathName = 'temp__' . $this->sessionId;

        if (is_dir(Yii::getPathOfAlias('webroot.uploads.guestad.' . $filePathName))) {
            $files = getFilesNameArrayInPathWithoutHtml(Yii::getPathOfAlias('webroot.uploads.guestad.' . $filePathName . '.' . Images::ORIGINAL_IMG_DIR));

            if (count($files)) {
                $guestAdImages = array();

                foreach ($files as $file) {
                    $fileNameExplode = explode('__', $file);

                    $model = new Images();
                    $model->id = $fileNameExplode[0];
                    $model->id_object = 0;
                    $model->id_owner = $this->sessionId;
                    $model->file_name = $file;
                    $model->sorter = $fileNameExplode[0];

                    $guestAdImages[] = $model;
                }
            }
        }

        $this->render('widgetGuestAdImages', array(
            'guestAdImages' => $guestAdImages,
        ));
    }

    public function registerAssets()
    {
        $assets = dirname(__FILE__) . '/../assets';
        $baseUrl = Yii::app()->assetManager->publish($assets);

        if (is_dir($assets)) {
            Yii::app()->clientScript->registerCssFile($baseUrl . '/styles.css');
        } else {
            throw new Exception('Image - Error: Couldn\'t find assets folder to publish.');
        }
    }
}
