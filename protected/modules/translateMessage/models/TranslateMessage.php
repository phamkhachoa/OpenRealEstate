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

class TranslateMessage extends ParentModel
{

    private static $_cache;

    const DEFAULT_CATEGORY = 'common';
    const STATUS_NO_ERROR = 0;
    const STATUS_ERROR_NO_TRANSLATE = 1;

    private static $_statusArray;

    public static function model($className = __CLASS__)
    {
        return parent::model($className);
    }

    public function behaviors()
    {
        return array(
            'ERememberFiltersBehavior' => array(
                'class' => 'application.components.behaviors.ERememberFiltersBehavior',
                'defaults' => array(),
                'defaultStickOnClear' => false
            ),
            'AutoTimestampBehavior' => array(
                'class' => 'zii.behaviors.CTimestampBehavior',
                'createAttribute' => null,
                'updateAttribute' => 'date_updated',
                'setUpdateOnCreate' => true,
            ),
        );
    }

    public function tableName()
    {
        return '{{translate_message}}';
    }

    public function rules()
    {
        return array(
            array('category, message', 'required'),
            array('translation', 'i18nRequired'),
            array('category', 'length', 'max' => 150),
            array('message', 'length', 'max' => 255),
            array('id, category, message, date_updated, status', 'safe', 'on' => 'search'),
            array('status', 'safe'),
            array($this->getI18nFieldSafe(), 'safe'),
        );
    }

    public function i18nFields()
    {
        return array(
            'translation' => 'text null'
        );
    }

    public function getTranslation()
    {
        return $this->getStrByLang('translation');
    }

    public function setTranslation($value)
    {
        $this->setStrByLang('translation', $value);
    }

    public function relations()
    {
        return array();
    }

    public function attributeLabels()
    {
        return array(
            'id' => 'ID',
            'category' => tt('category', 'translateMessage'),
            'message' => tt('String constant (defined in code)', 'translateMessage'),
            'language' => tc('Language'),
            'translation' => tt('Constant value (translation)', 'translateMessage'),
            'date_updated' => tc('Last updated on'),
            'status' => tc('Status'),
        );
    }

    public function search()
    {
        $criteria = new CDbCriteria;

        $criteria->compare('id', $this->id);
        $criteria->compare('status', $this->status);
        $criteria->compare('category', $this->category);
        $criteria->compare('message', $this->message, true);
        $tmp = 'translation_' . Yii::app()->language;
        $criteria->compare($tmp, $this->$tmp, true);
        $criteria->compare('date_updated', $this->date_updated, true);

        return new CustomActiveDataProvider($this, array(
            'criteria' => $criteria,
            'sort' => array('defaultOrder' => $this->getTableAlias() . '.id DESC'),
            'pagination' => array(
                'pageSize' => param('adminPaginationPageSizeBig', 60),
            ),
        ));
    }

    public function beforeSave()
    {
        $this->status = self::STATUS_NO_ERROR;
        return parent::beforeSave();
    }

    public function afterSave()
    {
        $langs = Lang::getActiveLangs();
        foreach ($langs as $lang) {
            Yii::app()->cache->delete(DbMessageSource::CACHE_KEY_PREFIX . '.messages.' . $this->category . '.' . $lang);
        }
        return parent::afterSave();
    }

    public static function getCategoryFilter()
    {
        $sql = "SELECT category FROM {{translate_message}} GROUP BY category";
        $all = Yii::app()->db->createCommand($sql)->queryColumn();
        $filter = array();
        foreach ($all as $category) {
            $filter[$category] = $category;
        }
        return $filter;
    }

    public static function missingTranslation($category, $message)
    {
        if ($message == '' || $message == null)
            return;
        // Смотрим есть ли в базе запись
        $sql = "SELECT id, status
                FROM {{translate_message}}
                WHERE category=:category AND message=:message";

        $messageRow = Yii::app()->db->createCommand($sql)
            ->bindValue(':category', $category, PDO::PARAM_STR)
            ->bindValue(':message', $message, PDO::PARAM_STR)
            ->queryRow();

        if (!isset($messageRow['id'])) {
            // Добавляем запись, что есть message без перевода
            $sql = "INSERT INTO {{translate_message}} (status, category, message)
                        VALUES (:status, :category, :message)";
            Yii::app()->db->createCommand($sql)
                ->bindValue(':status', TranslateMessage::STATUS_ERROR_NO_TRANSLATE, PDO::PARAM_INT)
                ->bindValue(':category', $category, PDO::PARAM_STR)
                ->bindValue(':message', $message, PDO::PARAM_STR)
                ->execute();
        } elseif (isset($messageRow['status']) && $messageRow['status'] != TranslateMessage::STATUS_ERROR_NO_TRANSLATE) {
            // Обновляем запись что message без перевода
            $sql = "UPDATE {{translate_message}} SET status=" . TranslateMessage::STATUS_ERROR_NO_TRANSLATE
                . " WHERE id=" . $messageRow['id'];
            Yii::app()->db->createCommand($sql)->execute();
        }
    }

    public static function getStatusArray()
    {
        if (!isset(self::$_statusArray)) {
            self::$_statusArray = array(
                self::STATUS_NO_ERROR => tt('Translated'),
                self::STATUS_ERROR_NO_TRANSLATE => tt('Not translated'),
            );
        }
        return self::$_statusArray;
    }

    public function getStatusHtml()
    {
        if (!isset(self::$_statusArray)) {
            self::getStatusArray();
        }
        if ($this->status == self::STATUS_NO_ERROR) {
            $cssClass = 'status_green'; //'#009933';
        } else {
            $cssClass = 'status_red'; //'#FF3333';
        }
        return "<span class=\"{$cssClass}\">" . self::$_statusArray[$this->status] . "</span>";
    }
}
