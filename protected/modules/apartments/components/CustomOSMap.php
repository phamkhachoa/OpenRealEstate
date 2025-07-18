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

class CustomOSMap
{

    private static $_instance;
    private static $jsVars;
    private static $jsCode;
    private static $jsCodeListeners;
    private static $jsCodeAllCenter;
    protected static $icon = array();

    public static function init()
    {
        self::$icon['href'] = Yii::app()->theme->baseUrl . "/images/house.png";
        self::$icon['size'] = array('x' => 32, 'y' => 37);
        self::$icon['offset'] = array('x' => -16, 'y' => -35);

        if (!isset(self::$_instance)) {
            $className = __CLASS__;
            self::$_instance = new $className;
        }
        return self::$_instance;
    }

    public static function createMap($isAppartment = false, $scrollWheel = true, $draggable = true, $viewMany = false, $lazyLoadMarkers = false)
    {
        $zoom = ($viewMany) ? param('module_apartments_osmapsZoomManyApartments', 11) : param('module_apartments_osmapsZoomApartment', 15);

        $baseUrl = Yii::app()->request->baseUrl;
        //Yii::app()->clientScript->registerScriptFile('http://cdn.leafletjs.com/leaflet-0.7/leaflet.js', CClientScript::POS_END);
        //Yii::app()->clientScript->registerCssFile('http://cdn.leafletjs.com/leaflet-0.7/leaflet.css');

        Yii::app()->clientScript->registerScriptFile($baseUrl . '/common/js/leaflet/leaflet-0.7.2/leaflet.js', CClientScript::POS_BEGIN);
        Yii::app()->clientScript->registerCssFile($baseUrl . '/common/js/leaflet/leaflet-0.7.2/leaflet.css');

        Yii::app()->clientScript->registerScriptFile($baseUrl . '/common/js/leaflet/leaflet-0.7.2/dist/leaflet.markercluster-src.js', CClientScript::POS_BEGIN);
        Yii::app()->clientScript->registerCssFile($baseUrl . '/common/js/leaflet/leaflet-0.7.2/dist/MarkerCluster.css');
        Yii::app()->clientScript->registerCssFile($baseUrl . '/common/js/leaflet/leaflet-0.7.2/dist/MarkerCluster.Default.css');

        self::$jsVars = '
		var mapOSMap;
		var markerClusterOSMap;
		var markersOSMap = [];
		var markersForClasterOSMap = [];
        var allMarkersForClasterOSMap = [];
		var latLngList = [];
		';

        self::$jsCode = '
		var initScrollWheel = "' . ($scrollWheel) . '";
		var initDraggable = "' . ($draggable) . '";
		var zoomOSMap = ' . $zoom . ';
		';
        if ($viewMany) {
            if (!$lazyLoadMarkers) {
                self::$jsCode .= 'mapOSMap = L.map("osmap", {scrollWheelZoom: initScrollWheel, dragging: initDraggable}); mapOSMap.setView([' . param('module_apartments_osmapsCenterY', 55.75411314653655) . ', ' . param('module_apartments_osmapsCenterX', 37.620717508911184) . '], zoomOSMap);';
            } else {
                self::$jsCode .= 'mapOSMap = L.map("osmap", {scrollWheelZoom: initScrollWheel, dragging: initDraggable}); mapOSMap.setView([' . param('module_apartments_osmapsCenterY', 55.75411314653655) . ', ' . param('module_apartments_osmapsCenterX', 37.620717508911184) . '], zoomOSMap); mapOSMap.on("load", osMapOnBoundsChanged(mapOSMap));';
            }
        } else {
            self::$jsCode .= 'mapOSMap = L.map("osmap", {scrollWheelZoom: initScrollWheel, dragging: initDraggable}).setView([' . param('module_apartments_osmapsCenterY', 55.75411314653655) . ', ' . param('module_apartments_osmapsCenterX', 37.620717508911184) . '], zoomOSMap);';
        }

        /* self::$jsCode .= 'L.tileLayer("http://{s}.tile.osm.org/{z}/{x}/{y}.png", {
          attribution: "&copy; <a href=\'http://osm.org/copyright\'>OpenStreetMap</a> contributors"
          }).addTo(mapOSMap);
          '; */

        // https
        self::$jsCode .= 'L.tileLayer("https://a.tile.openstreetmap.org/{z}/{x}/{y}.png", {
		attribution: "&copy; <a href=\'https://osm.org/copyright\'>OpenStreetMap</a> contributors"
		}).addTo(mapOSMap);
		';
    }

    public static function addMarker($model, $content, $draggable = 'false', $return = false)
    {
        if (is_object($model)) {
            $id = $model->id;
            $lat = $model->lat;
            $lng = $model->lng;
            $iconFile = $model->getMapIconUrl();
        } elseif (is_array($model)) {
            $id = $model['id'];
            $lat = $model['lat'];
            $lng = $model['lng'];
            $iconFile = ($model['objTypeIconFile']) ? Yii::app()->getBaseUrl() . '/' . ApartmentObjType::model()->iconsMapPath . '/' . $model['objTypeIconFile'] : Yii::app()->theme->baseUrl . "/images/house.png";
        } else
            return false;

        if ($lat && $lng) {
            if (!$content) {
                if (is_object($model)) {
                    $id = $model->id;
                    $title = $model->getStrByLang('title');
                    $address = $model->getStrByLang('address');
                    $url = $model->getUrl();
                } elseif (is_array($model)) {
                    $id = $model['id'];
                    $title = $model['title_' . Yii::app()->language];
                    $address = $model['address_' . Yii::app()->language];
                    $url = (isset($model['seoUrl']) && $model['seoUrl']) ? Yii::app()->createAbsoluteUrl('/apartments/main/view', array('url' => $model['seoUrl'] . (param('urlExtension') ? '.html' : ''))) : Yii::app()->createAbsoluteUrl('/apartments/main/view', array('id' => $id));
                }

                $content = Yii::app()->controller->renderPartial('//../modules/apartments/views/backend/_marker', array('model' => $model), true);
            }

            self::setIconType($model);

            if ($return) {
                return array(
                    'id' => $id,
                    'lat' => $lat,
                    'lng' => $lng,
                    'title' => $title,
                    'address' => $address,
                    'url' => $url,
                    'iconFile' => $iconFile,
                    'content' => $content,
                    'draggable' => false,
                );
            }

            self::$jsCode .= '
				var markerIcon = L.icon({
					iconUrl: "' . self::$icon['href'] . '",
					iconSize: [' . self::$icon['size']['x'] . ', ' . self::$icon['size']['y'] . '],
					className : "marker-icon-class"
				});
				markersOSMap[' . $id . '] = L.marker([' . $lat . ', ' . $lng . '], {icon: markerIcon, draggable : ' . $draggable . '})
					.addTo(mapOSMap)
					.bindPopup("' . CJavaScript::quote($content) . '");

				latLngList.push([' . $lat . ', ' . $lng . ']);
				markersForClasterOSMap.push(markersOSMap[' . $id . ']);
			';
        }
    }

    public static function clusterMarkers()
    {
        self::$jsCode .= '
			if(markersForClasterOSMap.length > 0){
				var markersCluster = L.markerClusterGroup({spiderfyOnMaxZoom: true, showCoverageOnHover: true, zoomToBoundsOnClick: true, removeOutsideVisibleBounds: true, maxClusterRadius: 30});
				for (var i = 0, markerCluster = markersForClasterOSMap.length; i < markerCluster; i++) {
					markersCluster.addLayer(markersForClasterOSMap[i]);
				}
				mapOSMap.addLayer(markersCluster);
				/*markersCluster.on("clusterclick", function (a) {
					a.layer.zoomToBounds();
				});*/
				mapOSMap.fitBounds(latLngList, {reset: true});
				//mapOSMap.fitBounds(new L.LatLngBounds(latLngList), {padding: [50, 50]});
			}
		';
    }

    public static function setCenter()
    {
        self::$jsCode .= '
			if(latLngList.length > 0){
				if (latLngList.length == 1) {
					mapOSMap.setView(latLngList[0]);
				}
				else {
					mapOSMap.fitBounds(latLngList,{reset: true});
					//mapOSMap.fitBounds(new L.LatLngBounds(latLngList), {reset: true});
				}
			}
		';
    }

    public static function setAllCenter($criteria = null)
    {
        $findIds = $apartments = array();

        if (!empty($criteria)) {
            $apartments = HApartment::findAllWithCache($criteria, false);
        }
        if (!empty($apartments)) {
            foreach ($apartments as $item) {
                $findIds[$item->id] = array('lat' => $item->lat, 'lng' => $item->lng);
            }
        }

        if (!empty($findIds)) {
            foreach ($findIds as $item) {
                if ($item['lat'] && $item['lng']) {
                    $lat = $item['lat'];
                    $lng = $item['lng'];

                    self::$jsCodeAllCenter .= '
                        allMarkersForClasterOSMap.push([' . $lat . ', ' . $lng . ']);
                    ';
                }
            }
            self::$jsCodeAllCenter .= '                
                if(allMarkersForClasterOSMap.length > 0){
                    if (allMarkersForClasterOSMap.length == 1) {
                        mapOSMap.setView(allMarkersForClasterOSMap[0]);
                    }
                    else {
                        mapOSMap.fitBounds(allMarkersForClasterOSMap, {reset: true, padding: [10, 10]});
                        //mapOSMap.fitBounds(new L.LatLngBounds(allMarkersForClasterOSMap), {reset: true, padding: [10, 10]});
                    }
                }
            ';

            echo CHtml::script(PHP_EOL . '$(function(){' . self::$jsCodeAllCenter . '});');
        }
    }

    public static function render()
    {
        //echo CHtml::tag('div', array('id' => 'OSMMap', 'style' => 'width: 100%; height: 586px;'), '', true);
        echo CHtml::script(self::$jsVars);
        echo CHtml::script(PHP_EOL . '$(function(){' . self::$jsCode . '});');
    }

    public static function actionOSmap($id, $model, $inMarker)
    {
        self::init();

        $isOwner = self::isOwner($model);

        // If we have already created marker - show it

        if ($model->lat && $model->lng) {
            self::createMap(true);
            self::$jsCode .= '

			';

            $draggable = $isOwner ? 'true' : 'false';

            self::addMarker($model, $inMarker, $draggable);

            if ($isOwner) {
                self::$jsCode .= '
					markersOSMap[' . $model->id . '].on("dragend", function(event) {
						var marker = event.target;
						var result = marker.getLatLng();

						if (result) {
							$.ajax({
								type:"POST",
								url:"' . Yii::app()->controller->createUrl('savecoords', array('id' => $model->id)) . '",
								data:({lat: result.lat, lng: result.lng}),
								cache:false
							});
						}
					});
				';
            }
        } else {
            if (!$isOwner) {
                return '';
            }

            $coordinates = NULL;

            if ($coordinates) {
                $model->lat = $coordinates->lat;
                $model->lng = $coordinates->lng;
            } else {
                $model->lat = param('module_apartments_osmapsCenterY', 55.75411314653655);
                $model->lng = param('module_apartments_osmapsCenterX', 37.620717508911184);
            }

            self::actionOSmap($id, $model, $inMarker);
            return false;
        }

        self::setCenter();
        self::render();
    }

    private static function isOwner($model)
    {
        return Yii::app()->user->checkAccess('backend_access') || param('useUserads', 1) && !Yii::app()->user->isGuest && $model->isOwner();
    }

    public static function setIconType($model = null)
    {
        // каждому типу свой значок
        if ($model) {
            if (is_object($model)) {
                if (isset($model->objType->icon_file) && $model->objType->icon_file) {
                    self::$icon['href'] = Yii::app()->getBaseUrl() . '/' . $model->objType->iconsMapPath . '/' . $model->objType->icon_file;
                    self::$icon['size'] = array('x' => ApartmentObjType::MAP_ICON_MAX_WIDTH, 'y' => ApartmentObjType::MAP_ICON_MAX_HEIGHT);
                    /* $icon['offset'] = array('x' => -16, 'y' => -2); */
                    self::$icon['offset'] = array('x' => -16, 'y' => -35);
                }
            } elseif (is_array($model)) {
                if ($model['objTypeIconFile']) {
                    self::$icon['href'] = Yii::app()->getBaseUrl() . '/' . ApartmentObjType::model()->iconsMapPath . '/' . $model['objTypeIconFile'];
                    self::$icon['size'] = array('x' => ApartmentObjType::MAP_ICON_MAX_WIDTH, 'y' => ApartmentObjType::MAP_ICON_MAX_HEIGHT);
                    /* $icon['offset'] = array('x' => -16, 'y' => -2); */
                    self::$icon['offset'] = array('x' => -16, 'y' => -35);
                }
            }
        }
    }

    public static function setLazyLoadListeners()
    {
        self::$jsCodeListeners = '
			var fetchedAreasBounds;
			var jqXHR;
			var markerClusterOSMap;
			var markersOSMap = [];
			var markersForClasterOSMap = [];
			var latLngList = [];
			
			function osMapRefreshPointsIfNessecary(mapObject){
				mapOS = mapObject || mapOSMap;
				
                var mapBounds = mapOS.getBounds();
                var sw = mapBounds.getSouthWest();
                var ne = mapBounds.getNorthEast();

                if(!fetchedAreasBounds 
                    || !fetchedAreasBounds.contains(sw)
                    || !fetchedAreasBounds.contains(ne)
                    ){
                    //get available areas to display on map
                    osMapFetch({
                        southWest: {
                            lat: sw.lat,
                            lng: sw.lng
                        },
                        northEast: {
                            lat: ne.lat,
                            lng: ne.lng
                        }
                    });
                }
            }
			
			function osMapOnBoundsChanged(mapObject){
				osMapRefreshPointsIfNessecary(mapObject);
            }

            function osMapFetch(data){
                $("#mapLoadingBox").show();
                $("#mapWarningBox").hide();
                
				data.get = ' . CJavaScript::encode($_GET) . ';
					
                if(jqXHR){
                    jqXHR.abort();
                    jqXHR = null;
                }
                jqXHR = $.post(
					"' . Yii::app()->controller->createUrl('/site/getMarkersViewAllMap', array(Yii::app()->request->csrfTokenName => Yii::app()->request->csrfToken)) . '",
					data,
					osMapOnDataFetched
				);
            }
			
            function osMapOnDataFetched(data){
                $("#mapLoadingBox").hide();

				var jsonAnswer = $.parseJSON(data);
								
                if(jsonAnswer.needZoom){
					$("#mapWarningBox").show();
				}
				else {
					$("#mapWarningBox").hide();
				}
				
				if(jsonAnswer.markers) {
					if (mapOSMap.hasLayer(markersCluster)){
						mapOSMap.removeLayer(markersCluster);
					}

					$.each( jsonAnswer.markers, function( key, value ) {							
						if (typeof markersOSMap[value.id] == "undefined") {
							var markerIcon = L.icon({
								iconUrl: value.iconFile,
								iconSize: [32, 37],
								className : "marker-icon-class"
							});

							markersOSMap[value.id] = L.marker([value.lat, value.lng], {icon: markerIcon, draggable : false})
								.addTo(mapOSMap)
								.bindPopup(value.content);

							markersForClasterOSMap.push(markersOSMap[value.id]);
						}
					});

					if(markersForClasterOSMap.length > 0){
						var markersCluster = L.markerClusterGroup({spiderfyOnMaxZoom: true, showCoverageOnHover: true, zoomToBoundsOnClick: true, removeOutsideVisibleBounds: true, maxClusterRadius: 30});
						for (var i = 0, markerCluster = markersForClasterOSMap.length; i < markerCluster; i++) {
							markersCluster.addLayer(markersForClasterOSMap[i]);
						}
						mapOSMap.addLayer(markersCluster);
					}
				}
            }     
		';

        echo CHtml::script(PHP_EOL . self::$jsCodeListeners);

        self::$jsCode .= '			
			mapOSMap.on("dragend", function(e) {
				osMapRefreshPointsIfNessecary();
			});
			
			mapOSMap.on("zoomend", function(e) {
				osMapRefreshPointsIfNessecary();
			});
		';
    }
}
