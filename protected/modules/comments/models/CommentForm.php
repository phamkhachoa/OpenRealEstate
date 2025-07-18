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

class CommentForm extends CFormModel
{
    use BadWordsTraitModel;

    public $body;
    public $url;
    public $verifyCode;
    public $enableRating = false;
    public $rating;
    public $user_name;
    public $user_email;
    public $rel;
    public $modelName;
    public $modelId;

    public function attributeLabels()
    {
        return array(
            'body' => Yii::t('module_comments', 'Comment'),
            'rating' => Yii::t('module_comments', 'Rate'),
            'user_name' => Yii::t('module_comments', 'Name'),
            'user_email' => Yii::t('module_comments', 'Email'),
            'verifyCode' => tt('Verification Code', 'contactform'),
        );
    }

    public function rules()
    {
        $return = array(
            array('body', 'required'),
            array('verifyCode', ((Yii::app()->user->isGuest || param('useCaptchaCommentsForRegistered', 1)) && !param('useReCaptcha', 0)) ? 'required' : 'safe'),
            array('verifyCode', 'CustomCaptchaValidatorFactory', 'allowEmpty' => !((Yii::app()->user->isGuest || param('useCaptchaCommentsForRegistered', 1) && param('useReCaptcha', 0)))),
            array('user_name, user_email', 'length', 'max' => 64),
            array('user_email', 'email'),
            array('body', 'checkDisabledWords'),
            array('rating, url, modelName, modelId, rel', 'safe'),
        );

        if (Yii::app()->user->isGuest) {
            $return[] = array('user_name, user_email', 'required');
        }

        return $return;
    }

    public function defineShowRating()
    {
        if ($this->modelName == 'Apartment') {
            $this->enableRating = true;
        }
    }
}
