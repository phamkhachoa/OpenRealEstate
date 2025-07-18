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

class SimilarAdsWidget extends CWidget
{
    public $numBlocks = 4;

    public function getViewPath($checkTheme = true)
    {
        if ($checkTheme && ($theme = Yii::app()->getTheme()) !== null) {
            if (is_dir($theme->getViewPath() . DIRECTORY_SEPARATOR . 'modules' . DIRECTORY_SEPARATOR . 'similarads' . DIRECTORY_SEPARATOR . 'views'))
                return $theme->getViewPath() . DIRECTORY_SEPARATOR . 'modules' . DIRECTORY_SEPARATOR . 'similarads' . DIRECTORY_SEPARATOR . 'views';
        }
        return Yii::getPathOfAlias('application.modules.similarads.views');
    }

    public function viewSimilarAds(Apartment $data)
    {
        if (!empty($data->paidActiveDisableSimilarListings)) {
            return null;
        }

        $similarAds = new SimilarAds;

        $criteria = new CDbCriteria;
        $criteria->addCondition('t.active = ' . Apartment::STATUS_ACTIVE);
        $criteria->addCondition('t.deleted = 0');

        $useIndex = ' FORCE INDEX (type_priceType_halfActive) ';
        if (param('useUserads')) {
            $useIndex = ' FORCE INDEX (type_priceType_fullActive) ';
            $criteria->addCondition('t.owner_active = ' . Apartment::STATUS_ACTIVE);
        }

        // hack
        $criteria->join = $useIndex;

        if ($data->id) {
            $criteria->addCondition('t.id != :id');
            $criteria->params[':id'] = $data->id;
        }

        if (issetModule('location')) {
            if ($data->loc_city) {
                $criteria->addCondition('t.loc_city = :loc_city');
                $criteria->params[':loc_city'] = $data->loc_city;
            }
        } else {
            if ($data->city_id) {
                $criteria->addCondition('t.city_id = :city_id');
                $criteria->params[':city_id'] = $data->city_id;
            }
        }

        if ($data->obj_type_id) {
            $criteria->addCondition('t.obj_type_id = :obj_type_id');
            $criteria->params[':obj_type_id'] = $data->obj_type_id;
        }
        if ($data->type) {
            $criteria->addCondition('t.type = :type');
            $criteria->params[':type'] = $data->type;
        }
        if ($data->price_type) {
            $criteria->addCondition('t.price_type = :price_type');
            $criteria->params[':price_type'] = $data->price_type;
        }

        /* if ($data->lat && $data->lng) {
          #http://stackoverflow.com/questions/574691/mysql-great-circle-distance-haversine-formula
          $tmp[] = '*';
          $tmp[] = new CDbExpression('( 6371 * acos( cos( radians('.$data->lat.') ) * cos( radians( lat ) ) * cos( radians( lng ) - radians('.$data->lng.') ) + sin( radians('.$data->lat.') ) * sin( radians( lat ) ) ) ) AS distance');
          $criteria->select = $tmp;
          $criteria->having = 'distance < 50';
          } */

        $criteria->limit = 10;
        $criteria->order = 't.id ASC';

        $ads = $similarAds->getSimilarAds($criteria);

        if (!empty($ads)) {
            $similarAds->publishAssets();

            // sort by distance
            foreach ($ads as $ad) {
                $ad->distanseFromViewSimilarInMeters = $ad->distanseFromViewSimilar = 0;
                if (($ad->lat && $ad->lng) && ($data->lat && $data->lng)) {
                    if ($data->lat != $ad->lat && $data->lng != $ad->lng) {
                        $ad->distanseFromViewSimilarInMeters = calculateTheDistance($data->lat, $data->lng, $ad->lat, $ad->lng, false, false);
                        $ad->distanseFromViewSimilar = addMeasureToDistanse($ad->distanseFromViewSimilarInMeters, $ad->distanseFromViewSimilarInMeters);
                    }
                }
            }

            usort($ads, function ($a, $b) {
                if ($a->distanseFromViewSimilarInMeters > 0 && $b->distanseFromViewSimilarInMeters > 0) {
                    return ($a->distanseFromViewSimilarInMeters > $b->distanseFromViewSimilarInMeters) ? 1 : 0;
                }
                return 0;
            });
        }

        $this->render('widgetSimilarAds_list', array(
            'ads' => $ads,
            'model' => $data,
        ));
    }
}
