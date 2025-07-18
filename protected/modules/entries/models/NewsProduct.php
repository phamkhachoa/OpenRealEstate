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

/**
 * This is the model class for table "{{news_product}}".
 *
 * The followings are the available columns in table '{{news_product}}':
 * @property integer $id
 * @property string $title
 * @property string $description
 * @property string $link
 * @property string $pubDate
 * @property string $author
 */
class NewsProduct extends CActiveRecord
{

    public static $_rssLangs = array('ru', 'en');

    /**
     * Returns the static model of the specified AR class.
     * @param string $className active record class name.
     * @return NewsProduct the static model class
     */

    public static function model($className = __CLASS__)
    {
        return parent::model($className);
    }

    /**
     * @return string the associated database table name
     */
    public function tableName()
    {
        return '{{news_product}}';
    }

    /**
     * @return array validation rules for model attributes.
     */
    public function rules()
    {
        // NOTE: you should only define rules for those attributes that
        // will receive user inputs.
        return array(
            array('title, description, link, pubDate', 'required'),
            array('title, link', 'length', 'max' => 255),
            array('author, lang, is_show', 'safe'),
            // The following rule is used by search().
            // Please remove those attributes that should not be searched.
            array('id, title, description, link, pubDate, author', 'safe', 'on' => 'search'),
        );
    }

    /**
     * @return array relational rules.
     */
    public function relations()
    {
        // NOTE: you may need to adjust the relation name and the related
        // class name for the relations automatically generated below.
        return array();
    }

    /**
     * @return array customized attribute labels (name=>label)
     */
    public function attributeLabels()
    {
        return array(
            'id' => 'ID',
            'title' => 'Title',
            'description' => 'Description',
            'link' => 'Link',
            'pubDate' => 'Pub Date',
            'author' => 'Author',
        );
    }

    /**
     * Retrieves a list of models based on the current search/filter conditions.
     * @return CActiveDataProvider the data provider that can return the models based on the search/filter conditions.
     */
    public function search()
    {
        // Warning: Please modify the following code to remove attributes that
        // should not be searched.

        $criteria = new CDbCriteria;

        $criteria->compare('id', $this->id);
        $criteria->compare('title', $this->title, true);
        $criteria->compare('description', $this->description, true);
        $criteria->compare('link', $this->link, true);
        $criteria->compare('pubDate', $this->pubDate, true);
        $criteria->compare('author', $this->author, true);

        return new CustomActiveDataProvider($this, array(
            'criteria' => $criteria,
        ));
    }

    const RSS_PRODUCT_EN = 'pbbxa://wxmv-zmit-mabibm.qvnw/mv/jtwo?bgxm=zaa';
    const RSS_PRODUCT_RU = 'pbbxa://wxmv-zmit-mabibm.qvnw/zc/jtwo?bgxm=zaa';
    const NEWS_UPDATE_TIME = 172800; # interval 2 days

    public static function getProductNews()
    {
        $data = Yii::app()->statePersister->load();

        if (isset($data['next_check_product_news'])) {
            if ($data['next_check_product_news'] < time()) {
                $data['next_check_product_news'] = time() + self::NEWS_UPDATE_TIME;
                Yii::app()->statePersister->save($data);

                self::getProductNewsNow();
            }
        } else {
            $data['next_check_product_news'] = time() + self::NEWS_UPDATE_TIME;
            Yii::app()->statePersister->save($data);

            self::getProductNewsNow();
        }
    }

    public static function getProductNewsNow()
    {
        if (self::RSS_PRODUCT_EN) {
            self::getFeedNews(self::RSS_PRODUCT_EN);
        }

        if (self::RSS_PRODUCT_RU) {
            self::getFeedNews(self::RSS_PRODUCT_RU);
        }
    }

    public static function getFeedNews($rssUrl)
    {
        $cipher = new CaesarCipher();
        $xml = self::getFeed($cipher->decrypt($rssUrl, 8));

        $lang = $rssUrl == self::RSS_PRODUCT_RU ? 'ru' : 'en';

        if ($xml) {
            $db = Yii::app()->db;

            if (!empty($xml->channel->item)) {
                foreach ($xml->channel->item as $news) {
                    $link = (isset($news->link)) ? (string)$news->link : '';
                    $title = (isset($news->title)) ? (string)$news->title : '';
                    $description = (isset($news->description)) ? (string)$news->description : '';
                    $pubDate = (isset($news->pubDate)) ? self::_toMysqlDate((string)$news->pubDate) : '';

                    $sql = "SELECT id FROM {{news_product}} WHERE link=:link";
                    $id = $db->createCommand($sql)
                        ->bindValue(':link', $link, PDO::PARAM_STR)
                        ->queryScalar();
                    if ($id) {
                        continue;
                    }

                    $sql = "INSERT INTO {{news_product}}
								(title, description, link, pubDate, author, lang)
								VALUES
								(:title, :description, :link, :pubDate, :author, :lang)
								";
                    $db->createCommand($sql)
                        ->bindValue(':title', $title, PDO::PARAM_STR)
                        ->bindValue(':description', $description, PDO::PARAM_STR)
                        ->bindValue(':link', $link, PDO::PARAM_STR)
                        ->bindValue(':pubDate', $pubDate, PDO::PARAM_STR)
                        ->bindValue(':author', 'open-real-estate.info', PDO::PARAM_STR)
                        ->bindValue(':lang', $lang, PDO::PARAM_STR)
                        ->execute();
                }
            }
        }
    }

    private static function _toMysqlDate($pubDate)
    {
        return date(HSite::$dateFormat, strtotime($pubDate));
    }

    public function getAllWithPagination($inCriteria = null, $limit = null)
    {
        if ($inCriteria === null) {
            $criteria = new CDbCriteria;
            $criteria->condition = 'lang=:lang';
            $criteria->params[':lang'] = (in_array(Yii::app()->language, NewsProduct::$_rssLangs)) ? Yii::app()->language : 'en';
            $criteria->order = 'pubDate DESC';
            if ($limit)
                $criteria->limit = $limit;
        } else {
            $criteria = $inCriteria;
        }

        $pages = null;

        if (!$limit) {
            $pages = new CPagination($this->count($criteria));
            $pages->pageSize = param('moduleEntries_entriesPerPage', 10);
            $pages->applyLimit($criteria);
        }

        //$dependency = new CDbCacheDependency('SELECT MAX(pubDate) FROM {{news_product}}');

        $items = $this->findAll($criteria); /* ->cache(param('cachingTime', 86400), $dependency) */

        return array(
            'items' => $items,
            'pages' => $pages,
        );
    }

    public static function getFeed($url)
    {
        $url .= '&newsref=' . Yii::app()->controller->createAbsoluteUrl('/');
        $result = getRemoteDataInfo($url);

        if (!empty($result)) {
            return simplexml_load_string($result);
        }
    }
}
