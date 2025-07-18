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

class ApartmentVideo extends ParentModel
{

    public $supportExt = 'mp4';
    public $fileMaxSize = 15728640; /* 1024 * 1024 * 15 - 15 MB */
    public $path = 'webroot.uploads.video';
    public $url = 'uploads/video';

    public function init()
    {
        $fileMaxSize['postSize'] = toBytes(ini_get('post_max_size'));
        $fileMaxSize['uploadSize'] = toBytes(ini_get('upload_max_filesize'));

        $this->fileMaxSize = min($fileMaxSize);

        parent::init();
    }

    public static function model($className = __CLASS__)
    {
        return parent::model($className);
    }

    public function tableName()
    {
        return '{{apartment_video}}';
    }

    public function rules()
    {
        return array(
            array('apartment_id', 'required'),
            array('apartment_id', 'numerical', 'integerOnly' => true),
            array('id, apartment_id', 'safe', 'on' => 'search'),
        );
    }

    public function relations()
    {
        Yii::import('application.modules.apartments.models.Apartment');
        return array(
            'apartment' => array(self::BELONGS_TO, 'Apartment', 'apartment_id'),
        );
    }

    public function behaviors()
    {
        $arr = array();
        $arr['AutoTimestampBehavior'] = array(
            'class' => 'zii.behaviors.CTimestampBehavior',
            'createAttribute' => 'date_updated',
            'updateAttribute' => 'date_updated',
        );
        /* if (issetModule('historyChanges')) {
          $arr['ArLogBehavior'] = array(
          'class' => 'application.modules.historyChanges.components.ArLogBehavior',
          );
          } */

        return $arr;
    }

    public function attributeLabels()
    {
        return array(
            'id' => 'ID',
            'apartment_id' => tt('apartment_id', 'apartments'),
            'video_html' => tt('video_html', 'apartments'),
            'video_file' => tt('video_file', 'apartments'),
        );
    }

    public function search()
    {
        $criteria = new CDbCriteria;

        $criteria->compare('id', $this->id);
        $criteria->compare('apartment_id', $this->apartment_id);

        return new CustomActiveDataProvider($this, array(
            'criteria' => $criteria,
        ));
    }

    public function beforeDelete()
    {
        if ($this->video_file) {
            $pathVideo = Yii::getPathOfAlias($this->path) . DIRECTORY_SEPARATOR . $this->apartment_id . DIRECTORY_SEPARATOR;
            deleteFile($pathVideo, $this->video_file);
        }

        return parent::beforeDelete();
    }

    public function parseVideoHTML($code)
    {
        if (preg_match( "/\s*[a-zA-Z\/\/:\.]*youtu(be.com\/watch\?v=|.be\/)([a-zA-Z0-9\-_]+)([a-zA-Z0-9\/\*\-\_\?\&\;\%\=\.]*)/i", $code, $match) && isset($match[0])) {
            if ($match[0] == $code) {
                return $this->parseVideoLink($match[0]);
            }
        }

        return [false, 'error'];
    }

    public function isFile()
    {
        return $this->video_file ? true : false;
    }

    public function isHtml()
    {
        return $this->video_html ? true : false;
    }

    public function isFileExists()
    {
        $path = Yii::getPathOfAlias($this->path) . DIRECTORY_SEPARATOR . $this->apartment_id . DIRECTORY_SEPARATOR . $this->video_file;
        return file_exists($path);
    }

    public function getFileUrl()
    {
        return Yii::app()->getBaseUrl() . '/' . $this->url . '/' . $this->apartment_id . '/' . $this->video_file;
    }

    function parseVideoEmbed($code)
    {
        $code = str_replace("'", "\"", stripslashes($code));

        $pattObject = '/<object(.*)>(.*)(<embed.*><\/embed>).*<\/object>/Us';
        preg_match($pattObject, $code, $objMatch);

        if (is_array($objMatch) && count($objMatch) > 0) {
            $pattObjectParams = '/(width="\d+"|height="\d+")/Us';
            $objParamsCode = $objMatch[1];
            preg_match_all($pattObjectParams, $objParamsCode, $objectParMatch);

            if (count($objectParMatch[1]) == 2) {
                $objParStr = strip_tags(implode(' ', $objectParMatch[1]));
            } else
                return array(false, "error");

            $pattParam = '/<param.*name="(.*)".*value="(.*)".*>.*<\/param>/Us';
            $paramCode = strip_tags($objMatch[2], '<param>');
            preg_match_all($pattParam, $paramCode, $paramsMatch);

            $paramsNames = $paramsMatch[1];
            $paramsValues = $paramsMatch[2];
            $paramStr = '';
            foreach ($paramsNames as $key => $item) {
                $paramStr .= '<param name="' . $paramsNames[$key] . '" value="' . $paramsValues[$key] . '" ></param>';
            }
            if ($paramStr == '')
                return array(false, "error");

            $embedCode = strip_tags($objMatch[3], '<embed>');
            $embedPatt = '/([a-zA-Z]*=".*")/Us';
            preg_match_all($embedPatt, $embedCode, $embedMatch);
            $embedParams = $embedMatch[1];

            if (!count($embedParams))
                return array(false, "error");
            $embedStr = '<embed ' . implode(' ', $embedParams) . ' ></embed>';

            $objStr = "<object " . $objParStr . " >" . $paramStr . $embedStr . "</object>";
            return array(true, $objStr);
        }
        return array(false, "error");
    }

    public function parseVideoIframe($code)
    {
        $param = array();
        $code = str_replace("'", "\"", stripslashes($code));

        $afsStr = (strpos($code, 'allowfullscreen') === false) ? "" : " allowfullscreen";

        $pattObject = '/<iframe(.*)>/Us';
        preg_match($pattObject, $code, $objMatch);

        if (is_array($objMatch) && count($objMatch) > 0) {
            $pattObjectParams = '/(width|title|height|src|frameborder)="([^\"]*)"/Us';
            $objParamsCode = $objMatch[1];
            preg_match_all($pattObjectParams, $objParamsCode, $objectParMatch, PREG_SET_ORDER);

            if (empty($objectParMatch)) {
                return array(false, "error");
            }

            foreach ($objectParMatch as $paramData) {
                $valueParam = $paramData[2];

                if ($paramData[1] == 'width' && $valueParam > 640) {
                    $valueParam = 615;
                }
                if ($paramData[1] == 'height' && $valueParam > 345) {
                    $valueParam = 345;
                }

                $param[$paramData[1]] = $valueParam;
            }

            if (!isset($param["src"])) {
                return array(false, "error");
            }

            $objStr = '<iframe ';
            foreach ($param as $name => $value) {
                $objStr .= $name . '="' . $value . '" ';
            }
            $objStr .= $afsStr . "></iframe>";
            return array(true, $objStr);
        }
        return array(false, "error");
    }

    public static function parseVideoLink($code)
    {
        return array(true, convertYoutube($code));
    }

    public static function saveVideo(Apartment $ad)
    {
        if(!$ad->validate(['video_file'])){
            return false;
        }

        $className = get_class($ad);
        if ((isset($_FILES[$className]['name']['video_file']) && $_FILES[$className]['name']['video_file'])) {
            $ad->scenario = 'video_file';

            $ad->videoUpload = CUploadedFile::getInstance($ad, 'video_file');
            $videoFile = md5(uniqid()) . '.' . $ad->videoUpload->extensionName;
            $pathVideo = Yii::getPathOfAlias('webroot.uploads.video') . DIRECTORY_SEPARATOR . $ad->id;

            if (newFolder($pathVideo)) {
                $ad->videoUpload->saveAs($pathVideo . '/' . $videoFile);

                $sql = 'INSERT INTO {{apartment_video}} (apartment_id, video_file, 	video_html, date_updated)
                            VALUES ("' . $ad->id . '", "' . $videoFile . '", "", NOW())';
                Yii::app()->db->createCommand($sql)->execute();

                if (issetModule('historyChanges')) {
                    HistoryChanges::addApartmentInfoToHistory('add_video', $ad->id, 'create');
                }

                //return true;
            } else {
                $ad->addError('videoUpload', tt('not_create_folder_to_save.', 'apartments'));
                return false;
            }
        }


        if (isset($_POST[$className]['video_html']) && $_POST[$className]['video_html']) {
            $ad->video_html = $_POST[$className]['video_html'];
            $ad->scenario = 'video_html';
            if ($ad->validate(['video_html'])) {

                $isAutoSave = Yii::app()->request->getPost('is_auto_save');
                $keyAutoSave = md5($ad->video_html.$ad->id);
                if($isAutoSave && Yii::app()->cache->get($keyAutoSave)){
                     return true;
                }

                $sql = 'INSERT INTO {{apartment_video}} (apartment_id, video_file, 	video_html, date_updated)
								VALUES ("' . $ad->id . '", "", "' . CHtml::encode($ad->video_html) . '", NOW())';
                Yii::app()->db->createCommand($sql)->execute();

                if (issetModule('historyChanges')) {
                    HistoryChanges::addApartmentInfoToHistory('add_video', $ad->id, 'create');
                }

                if($isAutoSave){
                    Yii::app()->cache->set($keyAutoSave, 1, 10800);
                }

                $ad->video_html = '';
            } else {
                return false;
            }
        }

        return true;
    }
}
