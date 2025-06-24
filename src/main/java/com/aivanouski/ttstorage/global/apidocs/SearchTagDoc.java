package com.aivanouski.ttstorage.global.apidocs;

import io.swagger.v3.oas.annotations.Parameter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Parameter(
        name = "search",
        description = """
                        Optional parameter.
                
                        Scenarios:
                        1. Use full tag name or part of the tag name to search the tags containing the particular characters.
                        2. Do not specify the search parameter or set its value as 'null' if filtration is not required.
                """
)
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface SearchTagDoc {
}