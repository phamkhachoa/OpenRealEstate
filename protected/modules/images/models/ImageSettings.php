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

class ImageSettings extends CFormModel
{

    public $useWatermark;
    public $watermarkType;

    const WATERMARK_FILE = 1;
    const WATERMARK_TEXT = 2;

    public $watermarkFile;
    public $watermarkContent;
    public $watermarkTextColor;
    public $watermarkTextOpacity;
    public $watermarkTextSize;
    public $watermarkPosition;

    public $thumbQuality;

    const POS_LEFT_TOP = 1;
    const POS_LEFT_MIDDLE = 2;
    const POS_LEFT_BOTTOM = 3;
    const POS_CENTER_TOP = 4;
    const POS_CENTER_MIDDLE = 5;
    const POS_CENTER_BOTTOM = 6;
    const POS_RIGHT_TOP = 7;
    const POS_RIGHT_MIDDLE = 8;
    const POS_RIGHT_BOTTOM = 9;

    public $maxImageWidth;
    public $maxImageHeight;
    public static $previewFilename = 'images/default/no_photo_bigthumb.png';
    public static $settings = array(
        'maxImageWidth',
        'maxImageHeight',
        'watermarkContent',
        'watermarkFile',
        'useWatermark',
        'watermarkType',
        'watermarkTextColor',
        'watermarkTextOpacity',
        'watermarkPosition',
        'watermarkTextSize',
        'thumbQuality'
    );

    public function loadSettings()
    {
        foreach (self::$settings as $item) {
            $this->{$item} = param($item);
        }
    }

    public function init()
    {
        // PHP 5.4 only ;(
        /* array_map(function($item){
          $this->{$item} = param($item);
          }, self::$settings); */

        $this->loadSettings();

        parent::init();
    }

    public function rules()
    {
        return array(
            array('maxImageWidth, maxImageHeight, useWatermark', 'required'),
            array('maxImageWidth, maxImageHeight', 'numerical', 'allowEmpty' => true, 'min' => 0, 'integerOnly' => true),
            array('watermarkFile', 'file', 'allowEmpty' => true, 'types' => param('watermarkFileTypes', 'gif, png, jpg')),
            array('watermarkTextOpacity', 'numerical', 'allowEmpty' => true, 'min' => 0, 'max' => 100, 'integerOnly' => true),
            array('watermarkTextSize', 'numerical', 'allowEmpty' => true, 'min' => 0, 'max' => 48, 'integerOnly' => true),
            array('watermarkContent', 'length', 'max' => 255),
            array('watermarkTextColor', 'colorValidator'),
            array('watermarkPosition, watermarkType, ', 'safe'),
            array('watermarkFile', 'watermarkFileValidator'),
            array('thumbQuality', 'numerical', 'min' => 0, 'max'=>100, 'integerOnly' => true),
        );
    }

    public function watermarkFileValidator($attribute, $params)
    {
        if ($this->useWatermark && $this->watermarkType == self::WATERMARK_FILE) {
            if (!$this->watermarkFile) {
                $this->addError($attribute, tc('Watermark file can\'t be empty.'));
            }
        }
    }

    public function colorValidator($attribute, $params)
    {
        if (!preg_match('/^#[a-f0-9]{6}$/i', $this->{$attribute})) {
            $this->addError($attribute, tc('Invalid format of text color'));
        }
    }

    public function attributeLabels()
    {
        return array(
            'maxImageWidth' => tc('maxImageWidth'),
            'maxImageHeight' => tc('maxImageHeight'),
            'useWatermark' => tc('useWatermark'),
            'watermarkType' => tc('watermarkType'),
            'watermarkFile' => tc('watermarkFile'),
            'watermarkContent' => tc('watermarkContent'),
            'watermarkTextColor' => tc('watermarkTextColor'),
            'watermarkTextOpacity' => tc('watermarkTextOpacity'),
            'watermarkPosition' => tc('watermarkPosition'),
            'watermarkTextSize' => tc('watermarkTextSize'),
            'thumbQuality' => tc('thumbQuality'),
        );
    }

    public function beforeValidate()
    {
        $this->watermarkFile = CUploadedFile::getInstance($this, 'watermarkFile');
        return parent::beforeValidate();
    }

    public static function deleteWatermark()
    {
        $sql = 'SELECT DISTINCT(id_object) FROM {{images}}';
        $ids = Yii::app()->db->createCommand($sql)->queryColumn();
        if ($ids) {
            $names = array(
                'thumb_*',
                'full_*',
            );
            foreach ($ids as $id) {
                foreach ($names as $name) {
                    $mask = Yii::getPathOfAlias('webroot') . DIRECTORY_SEPARATOR .
                        Images::UPLOAD_DIR . DIRECTORY_SEPARATOR .
                        Images::OBJECTS_DIR . DIRECTORY_SEPARATOR .
                        $id . DIRECTORY_SEPARATOR .
                        Images::MODIFIED_IMG_DIR . DIRECTORY_SEPARATOR . $name;
                    @array_map("unlink", glob($mask));
                }
            }
        }

        $sql = 'UPDATE {{images}} SET file_name_modified = ""';
        $ids = Yii::app()->db->createCommand($sql)->execute();


        if (param('watermarkFile')) {
            $file = Yii::getPathOfAlias('webroot.uploads') . DIRECTORY_SEPARATOR . param('watermarkFile');
            if (file_exists($file)) {
                @unlink($file);
            }
            ConfigurationModel::updateValue('watermarkFile', '');
        }
    }

    public function save()
    {
        // delete old watermark
        if (!$this->useWatermark ||
            $this->useWatermark && $this->watermarkType == self::WATERMARK_FILE && $this->watermarkFile ||
            $this->useWatermark && $this->watermarkType == self::WATERMARK_TEXT) {
            self::deleteWatermark();
        }

        // save new from file
        if ($this->useWatermark && $this->watermarkFile) {
            $fileName = md5(file_get_contents($this->watermarkFile->tempName)) . '.' . $this->watermarkFile->extensionName;
            $this->watermarkFile->saveAs(Yii::getPathOfAlias('webroot.uploads') . DIRECTORY_SEPARATOR . $fileName);

            ConfigurationModel::updateValue('watermarkFile', $fileName);
        } else {
            ConfigurationModel::updateValue('watermarkFile', '');
        }

        // generate new watermark from text - moved to Images class
        /* if($this->useWatermark && $this->watermarkType == self::WATERMARK_TEXT && $this->watermarkContent){
          self::deleteWatermark();
          ConfigurationModel::updateValue('watermarkFile', '');
          } */

        foreach (self::$settings as $item) {
            if ($item != 'watermarkFile') {
                ConfigurationModel::updateValue($item, $this->{$item});
            }
        }
    }
}
