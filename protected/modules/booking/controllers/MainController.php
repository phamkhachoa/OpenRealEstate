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

class MainController extends ModuleUserController
{

    public $modelName = 'Booking';

    public function actions()
    {
        $return = array();
        if (param('useJQuerySimpleCaptcha', 0)) {
            $return['captcha'] = array(
                'class' => 'jQuerySimpleCCaptchaAction',
                'backColor' => 0xFFFFFF,
            );
        } else {
            $return['captcha'] = array(
                'class' => 'MathCCaptchaAction',
                'backColor' => 0xFFFFFF,
            );
        }

        return $return;
    }

    public function actionBookingform()
    {
        Yii::app()->getModule('apartments');

        $this->modelName = 'Apartment';
        $apartment = $this->loadModel();
        $this->modelName = 'Booking';

        $booking = new Booking;
        $booking->scenario = 'bookingform';

        $user = null;

        $forceStartDay = CHtml::encode(Yii::app()->request->getParam('dateText'));
        if ($forceStartDay) {
            $forceStartDay = strtotime($forceStartDay);
        }

        if (isset($_POST['Booking']) && BlockIp::checkAllowIp(Yii::app()->controller->currentUserIpLong) && !$apartment->deleted) {
            $booking->attributes = $_POST['Booking'];
            $booking->apartment_id = $apartment->id;

            $booking->user_ip = Yii::app()->controller->currentUserIp;
            $booking->user_ip_ip2_long = Yii::app()->controller->currentUserIpLong;

            $paidService = null;
            if (issetModule('paidservices')) {
                $paidService = PaidServices::model()->findByPk(PaidServices::ID_BOOKING_PAY);
            }
            $payImmediately = $paidService && $paidService->isActive() && $paidService->getFromJson('pay_immediately');

            if ($payImmediately) {
                $booking->amount = HBooking::calculateAdvancePayment($booking);
                if (!$booking->amount) {
                    $payImmediately = false;
                }
            }

            if ($booking->validate()) {
                if (Yii::app()->user->isGuest) {
                    if (param('useUserRegistration', 1)) {
                        $userData = User::createUser(array(
                            'email' => $booking->useremail,
                            'username' => $booking->username,
                            'phone' => $booking->phone,
                            'activatekey' => User::generateActivateKey()
                        ));

                        if ($userData) {
                            $user = $userData['userModel'];
                            $user->id = $userData['id'];
                            $user->password = $userData['password'];
                            $user->email = $userData['email'];
                            $user->username = $userData['username'];
                            $user->activatekey = $userData['activatekey'];
                            $user->activateLink = $userData['activateLink'];

                            if ($payImmediately) {
                                $identity = new UserIdentity($userData['email'], $userData['password']);
                                if ($identity->authenticate() && Yii::app()->user->login($identity, 2592000)) {
                                    Yii::app()->user->setState('attempts-login', 0);
                                    User::updateUserSession();
                                    User::updateLatestInfo(Yii::app()->user->id, Yii::app()->controller->currentUserIp);
                                }
                            }

                            $notifier = new Notifier;
                            $notifier->raiseEvent('onNewUser_' . param('user_registrationMode'), $user, array(
                                'user' => $userData['userModel'],
                                'forceSendUser' => true
                            ));
                        } else {
                            $booking->addError('', tt('Error User Registration', 'booking'));
                        }
                    }
                } else {
                    $user = HUser::getModel();
                }

                if (!$booking->hasErrors()) {
                    $booking->time_inVal = $this->getI18nTimeIn($booking->time_in);
                    $booking->time_outVal = $this->getI18nTimeOut($booking->time_out);

                    if (issetModule('bookingtable')) {
                        $status = $payImmediately ? Bookingtable::STATUS_NEED_PAY : Bookingtable::STATUS_NEW;
                        $modelbt = Bookingtable::addRecord($booking, $user, $status);
                    }

                    $types = HApartment::getI18nTypesArray();
                    $booking->type = $types[Apartment::TYPE_RENT];

                    $ownerApartment = User::model()->findByPk($apartment->owner_id);

                    if ($ownerApartment) {
                        $booking->ownerEmail = $ownerApartment->email;

                        $notifier = new Notifier();
                        $notifier->raiseEvent('onNewBooking', $booking, array('user' => $ownerApartment, 'replyTo' => $user ? $user->email : $booking->useremail));

                        Yii::app()->user->setFlash('success', tt('Operation successfully complete. Your order will be reviewed by owner.'));
                    } else {
                        Yii::app()->user->setFlash('success', tt('Operation successfully complete. Your order will be reviewed by admin.'));
                    }

                    if ($payImmediately && $modelbt && Yii::app()->user->id) {
                        Yii::app()->user->setFlash('success', tt('It is necessary to pay', 'booking'));
                        $this->redirect(Yii::app()->createUrl('/paidservices/main/payForBooking', array('id' => $modelbt->id)));
                    }

                    $this->redirect($apartment->getUrl());
                }
            }
        }

        $booking->unsetAttributes(array('verifyCode'));

        if (!Yii::app()->user->isGuest && !$user) {
            $user = User::model()->findByPk(Yii::app()->user->getId());
        }

        if (Yii::app()->request->isAjaxRequest) {
            $this->excludeJs();

            $this->renderPartial('bookingform', array(
                'apartment' => $apartment,
                'model' => $booking,
                'isFancy' => true,
                'user' => $user,
                'forceStartDay' => $forceStartDay,
            ), false, true);
        } else {
            $this->render('bookingform', array(
                'apartment' => $apartment,
                'model' => $booking,
                'isFancy' => false,
                'user' => $user,
                'forceStartDay' => $forceStartDay,
            ));
        }
    }

    public function actionMainform()
    {
        $model = new SimpleformModel;
        $model->scenario = 'forrent';

        $user = null;

        if (isset($_POST['SimpleformModel']) && BlockIp::checkAllowIp(Yii::app()->controller->currentUserIpLong)) {
            $request = Yii::app()->request;
            $isForBuy = $request->getPost('isForBuy', 0);

            $model->attributes = $_POST['SimpleformModel'];

            $model->user_ip = Yii::app()->controller->currentUserIp;
            $model->user_ip_ip2_long = Yii::app()->controller->currentUserIpLong;

            if ($isForBuy) {
                $model->scenario = 'forbuy';
            }

            if ($model->validate()) {
                if (!$isForBuy) {
                    $model->time_inVal = $this->getI18nTimeIn($model->time_in);
                    $model->time_outVal = $this->getI18nTimeOut($model->time_out);
                }

                $types = HApartment::getInvertedI18nTypesArray();
                $saveType = $model->type;
                $model->type = $types[$model->type];

                $notifier = new Notifier;
                if (!$isForBuy)
                    $notifier->raiseEvent('onNewSimpleBookingForRent', $model);
                else
                    $notifier->raiseEvent('onNewSimpleBookingForBuy', $model);

                if (Yii::app()->user->isGuest) {
                    if (param('useUserRegistration', 1)) {
                        $userData = User::createUser(array(
                            'email' => $model->useremail,
                            'username' => $model->username,
                            'phone' => $model->phone,
                            'activatekey' => User::generateActivateKey()
                        ));

                        if ($userData) {
                            $user = $userData['userModel'];
                            $user->id = $userData['id'];
                            $user->password = $userData['password'];
                            $user->email = $userData['email'];
                            $user->username = $userData['username'];
                            $user->activatekey = $userData['activatekey'];
                            $user->activateLink = $userData['activateLink'];


                            $notifier = new Notifier;
                            $notifier->raiseEvent('onNewUser_' . param('user_registrationMode'), $user, array(
                                'user' => $userData['userModel'],
                                'forceSendUser' => true
                            ));
                        } else {
                            $model->addError('', tt('Error User Registration', 'booking'));
                        }
                    }
                } else {
                    $user = HUser::getModel();
                }

                if (issetModule('bookingtable')) {
                    $model->type = $saveType;
                    $model = Bookingtable::addRecord($model, $user, Bookingtable::STATUS_NEW);
                }

                Yii::app()->user->setFlash('success', tt('Operation successfully complete. Your order will be reviewed by administrator.'));
            }
        }


        if (!Yii::app()->user->isGuest && !$user) {
            $user = User::model()->findByPk(Yii::app()->user->getId());
        }

        $type = HApartment::getTypesWantArray();

        if (Yii::app()->request->isAjaxRequest) {
            $this->excludeJs();

            $this->renderPartial('simpleform', array(
                'model' => $model,
                'type' => $type,
                'user' => $user,
                'isFancy' => true,
            ), false, true);
        } else {
            $this->render('simpleform', array(
                'model' => $model,
                'type' => $type,
                'user' => $user,
                'isFancy' => false,
            ));
        }
    }

    public function createUser($email, $username = '', $phone = '', $activateKey = '', $isActive = false)
    {
        $model = new User;
        $model->email = $email;
        if ($username)
            $model->username = $username;
        if ($phone)
            $model->phone = $phone;
        if ($isActive)
            $model->active = 1;
        if ($activateKey)
            $model->activatekey = $activateKey;

        $password = $model->randomString();
        $model->setPassword($password);

        $return = array();

        if ($model->save()) {
            $return = array(
                'email' => $model->email,
                'username' => $model->username,
                'password' => $password,
                'id' => $model->id,
                'active' => $model->active,
                'activateKey' => $activateKey,
                'activateLink' => Yii::app()->createAbsoluteUrl('/site/activation', array('key' => $activateKey))
            );
        }
        return $return;
    }

    public function getI18nTimeIn($time_in)
    {
        $result = array();
        $defaultLang = Lang::getDefaultLang();
        $adminLang = Lang::getAdminMailLang();
        $currentLang = Yii::app()->language;

        if (param('booking_half_day')) {
            $result['default'] = Booking::getTimeList(null, $time_in);
        } else {
            $sql = 'SELECT title_' . $defaultLang . ' as title FROM {{apartment_times_in}} WHERE id=' . $time_in;
            $result['default'] = Yii::app()->db->createCommand($sql)->queryScalar();
        }

        if ($adminLang != $defaultLang) {
            if (param('booking_half_day')) {
                $result['admin'] = Booking::getTimeList($adminLang, $time_in);
            } else {
                $sql = 'SELECT title_' . $adminLang . ' as title FROM {{apartment_times_in}} WHERE id=' . $time_in;
                $result['admin'] = Yii::app()->db->createCommand($sql)->queryScalar();
            }
        } else
            $result['admin'] = $result['default'];

        if ($currentLang != $defaultLang) {
            if (param('booking_half_day')) {
                $result['current'] = Booking::getTimeList($currentLang, $time_in);
            } else {
                $sql = 'SELECT title_' . $currentLang . ' as title FROM {{apartment_times_in}} WHERE id=' . $time_in;
                $result['current'] = Yii::app()->db->createCommand($sql)->queryScalar();
            }
        } else
            $result['current'] = $result['default'];


        return $result;
    }

    public function getI18nTimeOut($time_out)
    {
        $result = array();
        $defaultLang = Lang::getDefaultLang();
        $adminLang = Lang::getAdminMailLang();
        $currentLang = Yii::app()->language;

        if (param('booking_half_day')) {
            $result['default'] = Booking::getTimeList(null, $time_out);
        } else {
            $sql = 'SELECT title_' . $defaultLang . ' as title FROM {{apartment_times_out}} WHERE id=' . $time_out;
            $result['default'] = Yii::app()->db->createCommand($sql)->queryScalar();
        }

        if ($adminLang != $defaultLang) {
            if (param('booking_half_day')) {
                $result['admin'] = Booking::getTimeList($adminLang, $time_out);
            } else {
                $sql = 'SELECT title_' . $adminLang . ' as title FROM {{apartment_times_out}} WHERE id=' . $time_out;
                $result['admin'] = Yii::app()->db->createCommand($sql)->queryScalar();
            }
        } else
            $result['admin'] = $result['default'];

        if ($currentLang != $defaultLang) {
            if (param('booking_half_day')) {
                $result['current'] = Booking::getTimeList($currentLang, $time_out);
            } else {
                $sql = 'SELECT title_' . $currentLang . ' as title FROM {{apartment_times_out}} WHERE id=' . $time_out;
                $result['current'] = Yii::app()->db->createCommand($sql)->queryScalar();
            }
        } else
            $result['current'] = $result['default'];


        return $result;
    }

    public function getExistRooms()
    {
        return Apartment::getExistsRooms();
    }

    public function generateActivateKey()
    {
        return md5(uniqid());
    }
}
