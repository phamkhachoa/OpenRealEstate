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

class SimilarAds extends CActiveRecord
{

    public $similarAdsModulePath;
    public $assetsPath;

    public function init()
    {
        $this->preparePaths();
        //$this->publishAssets();
    }

    public static function model($className = __CLASS__)
    {
        return parent::model($className);
    }

    public function tableName()
    {
        return '{{apartment}}';
    }

    public function preparePaths()
    {
        $this->similarAdsModulePath = dirname(__FILE__) . '/../';
        $this->assetsPath = $this->similarAdsModulePath . '/assets';
    }

    public function publishAssets()
    {
        $this->assetsPath = Yii::getPathOfAlias('webroot.themes.' . Yii::app()->theme->name . '.views.modules.similarads.assets');

        if (is_dir($this->assetsPath)) {
            $baseUrl = Yii::app()->assetManager->publish($this->assetsPath);

            Yii::app()->clientScript->registerCssFile($baseUrl . '/similarads.css');

            if (file_exists(Yii::app()->theme->basePath . DIRECTORY_SEPARATOR . 'js' . DIRECTORY_SEPARATOR . 'owl-carousel' . DIRECTORY_SEPARATOR . 'owl.carousel.js')) {
                Yii::app()->clientScript->registerScriptFile(Yii::app()->theme->baseUrl . '/js/owl-carousel/owl.carousel.js', CClientScript::POS_BEGIN);
            }
            if (file_exists(Yii::app()->theme->basePath . DIRECTORY_SEPARATOR . 'js' . DIRECTORY_SEPARATOR . 'owl-carousel' . DIRECTORY_SEPARATOR . 'owl.carousel.css')) {
                Yii::app()->clientScript->registerCssFile(Yii::app()->theme->baseUrl . '/js/owl-carousel/owl.carousel.css');
            }
            if (file_exists(Yii::app()->theme->basePath . DIRECTORY_SEPARATOR . 'js' . DIRECTORY_SEPARATOR . 'owl-carousel' . DIRECTORY_SEPARATOR . 'owl.theme.css')) {
                Yii::app()->clientScript->registerCssFile(Yii::app()->theme->baseUrl . '/js/owl-carousel/owl.theme.css');
            }
        }
    }

    public function getSimilarAds($inCriteria = null)
    {
        if ($inCriteria === null) {
            $criteria = new CDbCriteria;
            $criteria->addCondition('active = ' . Apartment::STATUS_ACTIVE);
            if (param('useUserads'))
                $criteria->addCondition('owner_active = ' . Apartment::STATUS_ACTIVE);
            $criteria->order = $this->getTableAlias() . '.id ASC';
        } else {
            $criteria = $inCriteria;
        }

        $similarAds = array();
        $similarAds['apartments'] = HApartment::findAllWithCache($criteria);

        return (is_array($similarAds['apartments']) && !empty($similarAds['apartments'])) ? $similarAds['apartments'] : null;
    }
}
