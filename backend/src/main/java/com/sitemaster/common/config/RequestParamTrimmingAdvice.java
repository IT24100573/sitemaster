package com.sitemaster.common.config;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Trims query and form parameters (multipart forms, filters) and turns blank
 * values into null. JSON bodies are handled by TrimmingStringDeserializer.
 */
@ControllerAdvice
public class RequestParamTrimmingAdvice {

    @InitBinder
    public void trimStrings(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }
}
