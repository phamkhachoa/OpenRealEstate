<?php
/**
-------------------------
COPYRIGHT NOTICES
-------------------------
This file is part of FlexicaCORE.
You can use this file in any way you want as long as you keep all
the author information below intact.
*/

/**
 * @author FlexicaCMS team <contact@flexicacms.com>
 * @link http://www.flexicacms.com/
 * @copyright Copyright &copy; 2009-2011 Gia Han Online Solutions Ltd.
 */

/**
* Fix CJuiDatePicker binding JS event in case this widget is part of an
* ajax update, i.e. this widget is used as a filter in a CGridView
*/

Yii::import('zii.widgets.jui.CJuiDatePicker');
class FJuiDatePicker extends CJuiDatePicker
{
    /**
    * Range name, specified in case the widget is used with another one
    * to allow user to select from - to dates
    * 
    * @var string
    */
    public $range='';
	public $cssFile =  null;
    
    public function init() {
        parent::init();

        $this->options = CMap::mergeArray(array(
                'dateFormat'=> HDate::getDateFormatShort(), //Yii::app()->getLocale()->getDateFormat('short'),
                'changeMonth'=>true,
                'changeYear'=>true,
        ), $this->options);
    }
    
    public function run()
    {
        /*echo*/ $dateFormat = $this->options['dateFormat'];
        
        list($name,$id)=$this->resolveNameID();

        if(isset($this->htmlOptions['id']))
            $id=$this->htmlOptions['id'];
        else
            $this->htmlOptions['id']=$id;
        if(isset($this->htmlOptions['name']))
            $name=$this->htmlOptions['name'];
        else
            $this->htmlOptions['name']=$name;
            
        if ($this->range != '') {
            $this->options['beforeShow'] = <<<EOD
js:function(input, inst) {
    $('.hasDatepicker.{$this->range}').each(function(index, elm){
        if (index == 0) { from = elm; }
        if (index == 1) { to = elm; }
    });
    
    if (to.id == input.id) { to = null; }
    if (from.id == input.id) { from = null; }
    if (to) {
        //this one is a 'from' date
        maxDate = $(to).val(); //$.datepicker.parseDate('{$dateFormat}', $(to).val());
        if (maxDate) {
            $(inst.input).datepicker("option", "maxDate", maxDate);
		}
    } 
    if (from) {
        //this one is a 'to' date
        minDate = $(from).val(); //$.datepicker.parseDate('{$dateFormat}', $(from).val());
        if (minDate) {
            $(inst.input).datepicker("option", "minDate", minDate);
		}
    }
}
EOD;
            $this->range = ' '.$this->range;
            if (isset($this->htmlOptions['class']))
                $this->htmlOptions['class'].=$this->range;
            else
                $this->htmlOptions['class']=$this->range;
        }

        if($this->hasModel())
            echo CHtml::activeTextField($this->model,$this->attribute,$this->htmlOptions);
        else
            echo CHtml::textField($name,$this->value,$this->htmlOptions);


        $options=CJavaScript::encode($this->options);

        $jsStart = "$.datepicker.setDefaults($.datepicker.regional['']); ".PHP_EOL;
        $js = $jsStart."jQuery('#{$id}').datepicker($options);";

        if (isset($this->language) && $this->language != 'en'){
            $this->registerScriptFile($this->i18nScriptFile);
            $js = $jsStart."jQuery('#{$id}').datepicker(jQuery.extend({showMonthAfterYear:false}, jQuery.datepicker.regional['{$this->language}'], {$options}));";
        }
        $js = $js."\n\$('body').ajaxSuccess(function(){".$js."})";

        $cs = Yii::app()->getClientScript();
        $cs->registerScript(__CLASS__,     $this->defaultOptions?'jQuery.datepicker.setDefaults('.CJavaScript::encode($this->defaultOptions).');':'');
        $cs->registerScript(__CLASS__.'#'.$id, $js);

    }
}