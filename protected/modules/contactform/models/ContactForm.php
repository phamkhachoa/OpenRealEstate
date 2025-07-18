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

class ContactForm extends CFormModel
{

    public $name;
    public $email;
    public $body;
    public $verifyCode;
    public $phone;
    public $useremail;
    public $username;

    public function rules()
    {
        return array(
            array('name, email, body', 'required'),
            array('email', 'email'),
            array('verifyCode', 'CustomCaptchaValidatorFactory', 'allowEmpty' => (!Yii::app()->user->isGuest || isTest())),
            array('phone', 'safe'),
            array('name, email', 'length', 'max' => 128),
            array('phone', 'length', 'max' => 16, 'min' => 5),
            array('body', 'length', 'max' => 1024),
        );
    }

    public function attributeLabels()
    {
        return array(
            'name' => tt('Name', 'contactform'),
            'email' => tt('Email', 'contactform'),
            'phone' => tt('Phone', 'contactform'),
            'body' => tt('Body', 'contactform'),
            'verifyCode' => tt('Verification Code', 'contactform'),
        );
    }
}
