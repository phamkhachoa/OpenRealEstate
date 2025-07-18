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

class InfoPages extends ParentModel
{
    public $infoImage;

    const STATUS_INACTIVE = 0;
    const STATUS_ACTIVE = 1;
    const MAIN_PAGE_ID = 1;
    const LICENCE_PAGE_ID = 4;
    const PRIVATE_POLICY_PAGE_ID = 5;
    const POSITION_BOTTOM = 1;
    const POSITION_TOP = 2;

    public $seasonalPricesIds = array();
    public $parent_id_autocomplete;
    public $parent_id;
    public $apartmentsSubTitle;
    public $summaryCitiesSubTitle;
    public $entriesSubTitle;
    public $contactformSubTitle;

    public static function getPositionList()
    {
        return array(
            self::POSITION_BOTTOM => tt('Bottom', 'infopages'),
            self::POSITION_TOP => tt('Top', 'infopages'),
        );
    }

    public static function model($className = __CLASS__)
    {
        return parent::model($className);
    }

    public function tableName()
    {
        return '{{infopages}}';
    }

    public function rules()
    {
        return array(
            array('title', 'i18nRequired'),
            array('title', 'i18nLength', 'max' => 255),
            array('parent_id', 'numerical', 'integerOnly' => true),
            array('active, widget, widget_data, widget_position', 'safe'),
            array($this->getI18nFieldSafe(), 'safe'),
            array('active', 'safe', 'on' => 'search'),
            array(
                'infoImage', 'file',
                'types' => ObjectImage::getSupportExt(),
                'maxSize' => ObjectImage::getMaxImageSize(),
                'tooLarge' => Yii::t('module_apartments', 'The file was larger than {size}MB. Please upload a smaller file.', array('{size}' => ObjectImage::getMaxImageSize(true))),
                'allowEmpty' => true,
            ),
        );
    }

    public function relations()
    {
        $relations = array(
            'menuPage' => array(self::HAS_MANY, 'Menu', 'pageId'),
            'menuPageOne' => array(self::HAS_ONE, 'Menu', 'pageId'),
        );

        $relations['image'] = array(self::HAS_ONE, 'ObjectImage', 'model_id', 'on' => 'image.model_name="InfoPages"');
        if (issetModule('seo')) {
            $relations['seo'] = array(self::HAS_ONE, 'SeoFriendlyUrl', 'model_id', 'on' => 'model_name="InfoPages"', 'alias' => 'seo');
        }

        return $relations;
    }

    public function i18nFields()
    {
        return array(
            'title' => 'varchar(255) not null default ""',
            'body' => 'LONGTEXT null',
        );
    }

    public function seoFields()
    {
        return array(
            'fieldTitle' => 'title',
            'fieldDescription' => 'body'
        );
    }

    public function behaviors()
    {
        return array(
            'AutoTimestampBehavior' => array(
                'class' => 'zii.behaviors.CTimestampBehavior',
                'createAttribute' => 'date_created',
                'updateAttribute' => 'date_updated',
            ),
        );
    }

    public function attributeLabels()
    {
        return array(
            'id' => 'ID',
            'active' => tc('Status'),
            'title' => tt('Page title'),
            'body' => tt('Page body'),
            'date_created' => tt('Creation date'),
            'widget' => tt('Widget', 'infopages'),
            'widget_position' => tt("Widget's position", 'infopages'),
            'apartmentsSubTitle' => tc('Title for the apartments list'),
            'summaryCitiesSubTitle' => tc('Title for the listings by categories'),
            'entriesSubTitle' => tc('Title for the articles list'),
            'contactformSubTitle' => tc('Title for the contact form'),
            'infoImage' => tc('Image'),
        );
    }

    public function scopes()
    {
        return array(
            'scopeActive' => array(
                'condition' => $this->getTableAlias() . '.active = '.self::STATUS_ACTIVE,
            ),
        );
    }

    public function getUrl($absolute = true)
    {
        $baseUrl = $absolute ? Yii::app()->getBaseUrl(true) : Yii::app()->getBaseUrl();
        $method = $absolute ? 'createAbsoluteUrl' : 'createUrl';

        if (issetModule('seo')) {
            $seo = SeoFriendlyUrl::getForUrl($this->id, 'InfoPages');

            if ($seo) {
                $field = 'url_' . Yii::app()->language;
                if ($seo->direct_url) {
                    return $baseUrl . '/' . $seo->$field . (param('urlExtension') ? '.html' : '');
                }
                return Yii::app()->{$method}('/infopages/main/view', array(
                    'url' => $seo->$field . (param('urlExtension') ? '.html' : ''),
                ));
            }
        }

        return Yii::app()->{$method}('/infopages/main/view', array(
            'id' => $this->id,
        ));
    }

    public static function getUrlById($id)
    {
        if (issetModule('seo')) {
            $seo = SeoFriendlyUrl::getForUrl($id, 'InfoPages');

            if ($seo) {
                $field = 'url_' . Yii::app()->language;
                if ($seo->$field) {
                    if ($seo->direct_url) {
                        return Yii::app()->getBaseUrl(true) . '/' . $seo->$field . (param('urlExtension') ? '.html' : '');
                    }
                    return Yii::app()->createAbsoluteUrl('/infopages/main/view', array(
                        'url' => $seo->$field . (param('urlExtension') ? '.html' : ''),
                    ));
                }
            }
        }

        return Yii::app()->createAbsoluteUrl('/infopages/main/view', array(
            'id' => $id,
        ));
    }

    public function getUrlForUpdate()
    {
        return Yii::app()->createAbsoluteUrl('/infopages/backend/main/update', array(
            'id' => $this->id,
        ));
    }

    public function search()
    {
        $criteria = new CDbCriteria;

        $titleField = 'title_' . Yii::app()->language;
        $criteria->compare($titleField, $this->$titleField, true);
        $bodyField = 'body_' . Yii::app()->language;
        $criteria->compare($bodyField, $this->$bodyField, true);

        $criteria->compare($this->getTableAlias() . '.active', $this->active);
        $criteria->compare($this->getTableAlias() . '.widget', $this->widget);

        return new CustomActiveDataProvider($this, array(
            'criteria' => $criteria,
            'sort' => array(
                'defaultOrder' => 'id DESC',
            ),
            'pagination' => array(
                'pageSize' => param('adminPaginationPageSize', 20),
            ),
        ));
    }

    public static function getWidgetOptions($widget = null, $withEmpty = true, $returnWidgetsSubTitles = false)
    {
        $arrWidgets = array();

        if ($withEmpty) {
            $arrWidgets = CMap::mergeArray($arrWidgets, array('' => array('title' => tc('No'))));
        }

        if (issetModule('seo')) {
            $arrWidgets = CMap::mergeArray(
                $arrWidgets, array(
                    'seosummaryinfo' => array(
                        'title' => tc('Summary_infopage_listing'),
                        'subTitles' => array(
                            'summaryCitiesSubTitle' => tc('Title for the listings by categories'),
                            'apartmentsSubTitle' => tc('Title for the apartments list'),
                        )
                    )
                )
            );

            $arrWidgets = CMap::mergeArray(
                $arrWidgets, array(
                    'seosummarycities' => array(
                        'title' => tc('Summary_infopage'),
                        'subTitles' => array(
                            'summaryCitiesSubTitle' => tc('Title for the listings by categories'),
                        )
                    )
                )
            );
        }

        $arrWidgets = CMap::mergeArray(
            $arrWidgets, array(
                'apartments' => array(
                    'title' => tt('Apartments list', 'apartments'),
                    'subTitles' => array(
                        'apartmentsSubTitle' => tc('Title for the apartments list'),
                    )
                )
            )
        );

        $arrWidgets = CMap::mergeArray(
            $arrWidgets, array(
                'randomapartments' => array(
                    'title' => tc('Listing (random)'),
                    'subTitles' => array(
                        'apartmentsSubTitle' => tc('Title for the apartments list'),
                    )
                )
            )
        );

        $arrWidgets = CMap::mergeArray(
            $arrWidgets, array(
                'specialoffers' => array(
                    'title' => tc('Special offers'),
                    'subTitles' => array(
                        'apartmentsSubTitle' => tc('Title for the apartments list'),
                    )
                )
            )
        );

        $arrWidgets = CMap::mergeArray(
            $arrWidgets, array(
                'viewallonmap' => array(
                    'title' => tc('Search for listings on the map'),
                    'subTitles' => array(
                        'apartmentsSubTitle' => tc('Title for the apartments list'),
                    )
                )
            )
        );

        $arrWidgets = CMap::mergeArray(
            $arrWidgets, array(
                'entries' => array(
                    'title' => tt('Entries', 'entries'),
                    'subTitles' => array(
                        'entriesSubTitle' => tc('Title for the articles list'),
                    )
                )
            )
        );

        $arrWidgets = CMap::mergeArray(
            $arrWidgets, array(
                'contactform' => array(
                    'title' => tc('The form of the section "Contact Us"'),
                    'subTitles' => array(
                        'contactformSubTitle' => tc('Title for the contact form'),
                    )
                )
            )
        );

        $widgetTitles = $widgetSubTitles = array();

        foreach ($arrWidgets as $key => $values) {
            $widgetTitles[$key] = (isset($values['title'])) ? $values['title'] : '?';
            $widgetSubTitles[$key] = (isset($values['subTitles'])) ? $values['subTitles'] : null;
        }

        if ($returnWidgetsSubTitles) {
            if ($widget) {
                if (array_key_exists($widget, $widgetSubTitles))
                    return $widgetSubTitles[$widget];

                return null;
            }

            return $widgetSubTitles;
        } else {
            if ($widget) {
                if (array_key_exists($widget, $widgetTitles))
                    return $widgetTitles[$widget];

                return '-';
            }

            return $widgetTitles;
        }

        return array();
    }

    public static function getInfoPagesAddList()
    {
        $return = array();

        $criteria = new CDbCriteria;
        $criteria->addCondition('active = ' . self::STATUS_ACTIVE);
        $criteria->order = 'id DESC';

        $result = InfoPages::model()->findAll($criteria);
        if ($result) {
            foreach ($result as $item) {
                $return[$item->id] = $item->getStrByLang('title');
            }
        }

        return $return;
    }

    public function getTitle()
    {
        return CHtml::encode($this->getStrByLang('title'));
    }

    public function getBody()
    {
        return $this->getStrByLang('body');
    }

    public function beforeSave()
    {
        if (($this->widget == 'apartments' || $this->widget == 'seosummaryinfo') && (isset($_POST['filter'])) || isset($_POST['filterSummaryCities'])) {
            if ($this->widget == 'apartments') {
                $this->widget_data = CJSON::encode($_POST['filter']);
            } elseif ($this->widget == 'seosummaryinfo') {
                $summaryFilter = array();

                $summaryFilter['apartmentFilter'] = (isset($_POST['filter'])) ? $_POST['filter'] : array();
                $summaryFilter['summaryCitiesFilter'] = (isset($_POST['filterSummaryCities'])) ? $_POST['filterSummaryCities'] : array();

                $this->widget_data = CJSON::encode($summaryFilter);
                unset($summaryFilter);
            }
        }
        if ($this->widget == 'entries' && isset($_POST['filterEntries'])) {
            $this->widget_data = CJSON::encode($_POST['filterEntries']);
        }
        if ($this->widget == 'seosummarycities' && isset($_POST['filterSummaryCities'])) {
            $summaryFilter = array();

            $summaryFilter['summaryCitiesFilter'] = (isset($_POST['filterSummaryCities'])) ? $_POST['filterSummaryCities'] : array();

            $this->widget_data = CJSON::encode($summaryFilter);
            unset($summaryFilter);
        }

        if (isset($_POST['widgetSubTitle']) && !empty($_POST['widgetSubTitle'])) {
            $this->widget_titles = CJSON::encode($_POST['widgetSubTitle']);
        }

        return parent::beforeSave();
    }

    public function afterSave()
    {
        if (issetModule('seo')) {
            SeoFriendlyUrl::getAndCreateForModel($this);
        }

        if ($this->infoImage) {
            if (isset($this->image) && $this->image) {
                $this->image->delete();
            }
            $image = new ObjectImage();
            $image->date_created = date(HSite::$dateFormat);
            $image->imageInstance = $this->infoImage;
            $image->model_id = $this->id;
            $image->model_name = 'InfoPages';
            $image->save();
        }

        return parent::afterSave();
    }

    public function beforeDelete()
    {
        if (issetModule('seo')) {
            $sql = 'DELETE FROM {{seo_friendly_url}} WHERE model_id="' . $this->id . '" AND model_name = "InfoPages"';
            Yii::app()->db->createCommand($sql)->execute();

            $sql = 'DELETE FROM {{seo_friendly_url_history}} WHERE model_id="' . $this->id . '" AND model_name = "InfoPages"';
            Yii::app()->db->createCommand($sql)->execute();
        }

        return parent::beforeDelete();
    }

    private $_filter;

    public function getCriteriaForAdList()
    {
        $criteria = new CDbCriteria();

        $listExclude = ApartmentObjType::getListExclude('search');
        if ($listExclude) {
            $criteria->addNotInCondition('t.obj_type_id', $listExclude);
        }

        if ($this->widget_data) {
            $arr = CJSON::decode($this->widget_data);

            if ($this->widget == 'seosummaryinfo') {
                $this->_filter = (isset($arr['apartmentFilter'])) ? $arr['apartmentFilter'] : array();
            } else {
                $this->_filter = $arr;
            }

            unset($arr);

            if (issetModule('location')) {
                $this->setForCriteria($criteria, 'country_id', 'loc_country');
                $this->setForCriteria($criteria, 'region_id', 'loc_region');
                $this->setForCriteria($criteria, 'city_id', 'loc_city');

                if (isset($this->_filter['country_id']) && $this->_filter['country_id'])
                    Yii::app()->controller->selectedCountry = $this->_filter['country_id'];
                if (isset($this->_filter['region_id']) && $this->_filter['region_id'])
                    Yii::app()->controller->selectedRegion = $this->_filter['region_id'];
                if (isset($this->_filter['city_id']) && $this->_filter['city_id'])
                    Yii::app()->controller->selectedCity = $this->_filter['city_id'];
            } else {
                $this->setForCriteria($criteria, 'city_id', 'city_id');

                if (isset($this->_filter['city_id']) && $this->_filter['city_id'])
                    Yii::app()->controller->selectedCity = $this->_filter['city_id'];
            }

            if (issetModule('metroStations')) {
                $this->setForCriteria($criteria, 'metro', 'metro');

                if (isset($this->_filter['metro']) && $this->_filter['metro'])
                    Yii::app()->controller->selectedMetroStations = $this->_filter['metro'];
            }

            if (isset($this->_filter['type']) && $this->_filter['type']) {
                if (strpos($this->_filter['type'], '-') !== false) {
                    $typeArr = explode('-', $this->_filter['type']);
                    $type = (int)$typeArr[0];
                    $priceType = (int)$typeArr[1];
                    if (issetModule('seasonalprices')) {
                        $criteria->addCondition('( t.id IN(SELECT apartment_id FROM {{seasonal_prices}} WHERE price_type = :price_type ) OR (is_price_poa = 1) )');
                    } else {
                        $criteria->addCondition('t.price_type = :price_type');
                    }
                    $criteria->params[':price_type'] = $priceType;
                    $criteria->addCondition('t.type = :apType');
                    $criteria->params[':apType'] = $type;
                } else {
                    $criteria->addCondition('t.type = :apType');
                    $criteria->params[':apType'] = (int)$this->_filter['type'];
                }
            }

            $this->setForCriteria($criteria, 'obj_type_id', 't.obj_type_id');
            $this->setForCriteria($criteria, 'parent_id', 't.parent_id');

            $this->setForCriteria($criteria, 'rooms', 't.num_of_rooms');

            $this->setForCriteria($criteria, 'ot', 't.ot');

            $this->setForCriteria($criteria, 'wp', 't.count_img');

            $this->setForCriteria($criteria, 'square_min', 't.square');
            $this->setForCriteria($criteria, 'square_max', 't.square');

            $this->setForCriteria($criteria, 'floor_min', 't.floor');
            $this->setForCriteria($criteria, 'floor_max', 't.floor');

            if (isset($this->_filter['type']) && $this->_filter['type'])
                Yii::app()->controller->apType = $this->_filter['type'];
            if (isset($this->_filter['obj_type_id']) && $this->_filter['obj_type_id'])
                Yii::app()->controller->objType = $this->_filter['obj_type_id'];
            if (isset($this->_filter['rooms']) && $this->_filter['rooms'])
                Yii::app()->controller->roomsCount = $this->_filter['rooms'];
            if (isset($this->_filter['ot']) && $this->_filter['ot'])
                Yii::app()->controller->ot = $this->_filter['ot'];
            if (isset($this->_filter['wp']) && $this->_filter['wp'])
                Yii::app()->controller->wp = $this->_filter['wp'];

            if (isset($this->_filter['square_min']) && $this->_filter['square_min'])
                Yii::app()->controller->squareCountMin = $this->_filter['square_min'];
            if (isset($this->_filter['square_max']) && $this->_filter['square_max'])
                Yii::app()->controller->squareCountMax = $this->_filter['square_max'];

            if (isset($this->_filter['floor_min']) && $this->_filter['floor_min'])
                Yii::app()->controller->floorCountMin = $this->_filter['floor_min'];
            if (isset($this->_filter['floor_max']) && $this->_filter['floor_max'])
                Yii::app()->controller->floorCountMax = $this->_filter['floor_max'];

            # new fields
            $newFieldsAll = InfoPages::getAddedFields();
            if ($newFieldsAll) {
                foreach ($newFieldsAll as $field) {
                    $this->setForCriteria($criteria, $field['field'], 't.' . $field['field'], true, $field);

                    if (isset($this->_filter[$field['field']]) && $this->_filter[$field['field']])
                        Yii::app()->controller->newFields[$field['field']] = $this->_filter[$field['field']];
                }
            }
        }

//      Для чего это ??? Эта критерия итак проходит потом через apartmentsHelper и HApartment::findAllWithCache, зачем 2 раза?
//		$limit = param('countListitng'.User::getModeListShow(), 10);
//		Yii::import('application.modules.apartments.helpers.ApartmentsHelper');
//		$newcriteria = apartmentsHelper::getApartments($limit, 1, 0, $criteria);
//		$result = HApartment::findAllWithCache($newcriteria['criteria']);
//
//		if ($result) {
//			$result = CHtml::listData($result, 'id', 'id');
//
//			if ($result && !empty($this->seasonalPricesIds)) {
//				$ids = array_unique(CMap::mergeArray($result, $this->seasonalPricesIds));
//				$criteria = new CDbCriteria;
//				$criteria->addInCondition('t.id', $ids);
//			}
//		}

        $criteria->limit = param('countListitng' . User::getModeListShow(), 10);

        return $criteria;
    }

    private function setForCriteria($criteria, $key, $field, $isNewField = false, $newFieldArr = array())
    {

        if (isset($this->_filter[$key]) && ($this->_filter[$key] || $key == 'type')) {
            if ($isNewField && count($newFieldArr)) {

                if ($newFieldArr['type'] == FormDesigner::TYPE_MULTY) {
                    $appsLike = array();

                    $value = $this->_filter[$key];
                    if (!$value || !is_array($value))
                        return;

                    Yii::app()->controller->newFields[$field] = $value;
                    foreach ($value as $val) {
                        $appsLike[] = CHtml::listData(Reference::model()->findAllByAttributes(array('reference_value_id' => $val), array('select' => 'apartment_id')), 'apartment_id', 'apartment_id');
                    }

                    if ($appsLike) {
                        if ($newFieldArr['compare_type'] == FormDesigner::COMPARE_LIKE) {
                            $appsLike = (count($appsLike) > 1) ? call_user_func_array('array_merge', $appsLike) : $appsLike[0];
                        } else {
                            $appsLike = (count($appsLike) > 1) ? call_user_func_array('array_intersect', $appsLike) : $appsLike[0];
                        }
                        $criteria->addInCondition('t.id', $appsLike);
                    }
                } else {
                    switch ($newFieldArr['compare_type']) {
                        case FormDesigner::COMPARE_EQUAL:
                            $criteria->compare($field, $this->_filter[$key]);
                            break;

                        case FormDesigner::COMPARE_LIKE:
                            $criteria->compare($field, $this->_filter[$key], true);
                            break;

                        case FormDesigner::COMPARE_FROM:
                            $value = intval($this->_filter[$key]);
                            $criteria->compare($field, ">={$value}");
                            break;

                        case FormDesigner::COMPARE_TO:
                            $value = intval($this->_filter[$key]);
                            $criteria->compare($field, "<={$value}");
                            break;
                    }
                }
            } else {
                if ($key == 'rooms') {
                    if ($this->_filter[$key] == 4) {
                        $criteria->addCondition($field . ' >= ' . $this->_filter[$key]);
                    } else {
                        $criteria->addCondition($field . ' = ' . $this->_filter[$key]);
                    }
                } elseif ($key == 'ot') {
                    $criteria->join = 'INNER JOIN {{users}} AS u ON u.id = t.owner_id';

                    if ($this->_filter[$key] == User::TYPE_PRIVATE_PERSON) {
                        $ownerTypes = array(
                            User::TYPE_PRIVATE_PERSON,
                            User::TYPE_ADMIN
                        );
                    }
                    if ($this->_filter[$key] == User::TYPE_AGENCY) {
                        $ownerTypes = array(
                            User::TYPE_AGENT,
                            User::TYPE_AGENCY
                        );
                    }
                    if (isset($ownerTypes) && $ownerTypes)
                        $criteria->compare('u.type', $ownerTypes);
                } elseif ($key == 'wp') {
                    $criteria->addCondition('t.count_img > 0');
                } elseif ($key == 'metro') {
                    $apartmentIds = MetroStations::getApartmentsListByMetro($this->_filter[$key]);
                    if ($apartmentIds)
                        $criteria->addInCondition('t.id', $apartmentIds);
                } elseif ($key == 'type') {
                    $criteria->addCondition('t.type = :apType');
                    $criteria->params[':apType'] = $this->_filter['type'];
                } elseif ($key == 'square_min' || $key == 'square_max') {
                    if (!empty($this->_filter[$key])) {
                        if ($key == 'square_min') {
                            $criteria->addCondition('square >= ' . $this->_filter[$key]);
                        }
                        if ($key == 'square_max') {
                            $criteria->addCondition('square <= ' . $this->_filter[$key]);
                        }
                    }
                } elseif ($key == 'floor_min' || $key == 'floor_max') {
                    if (!empty($this->_filter[$key])) {
                        if ($key == 'floor_min') {
                            $criteria->addCondition('floor >= ' . $this->_filter[$key]);
                        }
                        if ($key == 'floor_max') {
                            $criteria->addCondition('floor <= ' . $this->_filter[$key]);
                        }
                    }
                } else {
                    $criteria->compare($field, $this->_filter[$key]);
                }
            }
        }
    }

    private $_filterEntries;

    public function getCriteriaForEntriesList()
    {
        $criteria = new CDbCriteria();
        if ($this->widget_data) {
            $this->_filterEntries = CJSON::decode($this->widget_data);

            $this->setForCriteriaEntries($criteria, 'category_id', 't.category_id');
        }

        return $criteria;
    }

    private function setForCriteriaEntries($criteria, $key, $field)
    {
        if (isset($this->_filterEntries[$key]) && $this->_filterEntries[$key]) {
            $criteria->compare($field, $this->_filterEntries[$key]);
        }
    }

    public static function getAddedFields()
    {
        $addedFields = null;

        if (issetModule('formdesigner')) {
            $newFieldsAll = FormDesigner::getNewFields();
            if ($newFieldsAll && count($newFieldsAll)) {
                foreach ($newFieldsAll as $key => $field) {
                    $addedFields[$key]['field'] = $field->field;
                    $addedFields[$key]['type'] = $field->type;
                    $addedFields[$key]['compare_type'] = $field->compare_type;
                    $addedFields[$key]['label'] = $field->getStrByLang('label');

                    if ($field->type == FormDesigner::TYPE_REFERENCE || $field->type == FormDesigner::TYPE_MULTY) {
                        $addedFields[$key]['listData'] = FormDesigner::getListByCategoryID($field->reference_id);
                    }
                }
            }
        }

        return $addedFields;
    }

    public function getParamsForSummaryCitiesList()
    {
        $return = array();

        if ($this->widget_data) {
            $arr = CJSON::decode($this->widget_data);

            if ($arr && !empty($arr) && isset($arr['summaryCitiesFilter'])) {
                $return = $arr['summaryCitiesFilter'];
            }
        }

        return $return;
    }

    public static function getWidgetSubTitleKey($widget = null)
    {
        $return = null;

        if ($widget) {
            $widgetSubTitlesOptions = self::getWidgetOptions($widget, null, true);

            if (is_array($widgetSubTitlesOptions)) {
                return array_keys($widgetSubTitlesOptions);
            }
        }

        return $return;
    }

    public static function getWidgetSubTitle($subTitleKeys = array(), $widgetTitles = null)
    {
        $return = null;

        if (!empty($subTitleKeys) && !empty($widgetTitles)) {
            if (!is_array($widgetTitles)) {
                $widgetTitles = CJSON::decode($widgetTitles);
            }

            foreach ($subTitleKeys as $item) {
                if (isset($widgetTitles[$item]) && isset($widgetTitles[$item][Yii::app()->language])) {
                    $return = $widgetTitles[$item][Yii::app()->language];
                    break;
                }
            }
        }

        return $return;
    }

    public static function generateWidgetTitlesTabs($model = null, $tabName = null)
    {
        if ($tabName && $model) {
            $activeLang = Lang::getActiveLangs(true);

            $countActiveLang = count($activeLang);

            if (count($activeLang) == 1) {
                $first = reset($activeLang);
                $fieldCurrent = 'widgetSubTitle[' . $tabName . '][' . $first['name_iso'] . ']';
                echo self::generateWidgetTitlesContentTab($fieldCurrent, $first['name_iso'], $tabName, $model);
                return;
            }

            $postfix = '_' . $tabName;
            $i = 1;
            $activeI = 1;
            foreach ($activeLang as $lang) {
                $fieldCurrent = 'widgetSubTitle[' . $tabName . '][' . $lang['name_iso'] . ']';
                $tabs['tabs']['tab' . $i . $postfix] = array(
                    'title' => '<img src="' . Yii::app()->getBaseUrl() . Lang::FLAG_DIR . $lang['flag_img'] . '">&nbsp;' . $lang['name'],
                    'content' => self::generateWidgetTitlesContentTab($fieldCurrent, $lang['name_iso'], $tabName, $model)
                );
                if ($lang['name_iso'] == Yii::app()->language) {
                    $activeI = $i;
                }
                $i++;
            }
            $tabs['activeTab'] = 'tab' . $activeI . $postfix;

            Yii::app()->controller->widget('CTabView', $tabs);
        }
    }

    public static function generateWidgetTitlesContentTab($field, $lang, $tabName, $model)
    {
        $isCKEditor = false;

        $activeLang = Lang::getActiveLangs(true);

        $countActiveLang = count($activeLang);

        $fieldId = 'id_' . $tabName . '_' . $lang;

        $value = '';
        if (isset($model->{$tabName}) && !empty($model->{$tabName})) {
            $arr = $model->{$tabName};

            if (isset($arr[$lang])) {
                $value = $arr[$lang];
            }
        }

        $html = '';

        if ($countActiveLang > 1) {
            $html .= '<div class="translate_button" >';
            $html .= '<span class="t_loader_' . $tabName . '" style="display: none;"><img src="' . Yii::app()->theme->baseUrl . '/images/ajax-loader.gif" alt="Переводим"></span>';
            $html .= CHtml::button(tc('Translate'), array(
                'onClick' => "translateField('widgetSubTitle[{$tabName}]', '{$lang}', '{$isCKEditor}', " . CJavaScript::encode($tabName) . ")"
            ));
            $html .= '</div>';

            $html .= '<div class="copylang_button">';
            $html .= CHtml::button(tc('Copy to all languages'), array(
                'onClick' => "copyField('widgetSubTitle[{$tabName}]', '{$lang}', '{$isCKEditor}', " . CJavaScript::encode($tabName) . ")"
            ));
            $html .= '</div>';
        }

        $html .= '<div class="form-group">';
        $html .= CHtml::label($model->getAttributeLabel($tabName), null, array());
        $html .= CHtml::textField($field, $value, array(
            'class' => 'width300 form-control',
            'maxlength' => 255,
            'id' => $fieldId
        ));
        $html .= '</div>';

        return $html;
    }

    public function allowDelete()
    {
        if ($this->special || ($this->id == InfoPages::MAIN_PAGE_ID || $this->id == InfoPages::LICENCE_PAGE_ID)) {
            return false;
        }
        return true;
    }
}
