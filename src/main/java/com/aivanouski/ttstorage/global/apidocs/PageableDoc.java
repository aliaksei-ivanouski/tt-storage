package com.aivanouski.ttstorage.global.apidocs;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Parameter(
        name = "pageable",
        description = """
                    Pageable request
                
                    Parameters description:
                    - page - page number, starts from 0
                    - size - page size
                    - sort - array of comma separated sequential <field_name>,<sort_direction>
                             it works the same way as SQL "ORDER BY `name` ASC, `created_at` DESC"
                
                    Example:
                    1. A third (starts from 0) page with five elements maximum sorted by filename in ascending order 
                    and then by file size in descending order second
                    {
                      "page": 2,
                      "size": 5,
                      "sort": [
                        "filename,asc",
                        "size,desc"
                      ]
                    }
                    2. A first page with ten elements maximum sorted by created date in descending order
                    {
                      "page": 0,
                      "size": 10,
                      "sort": [
                        "createdAt,desc"
                      ]
                    }
                """
)
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface PageableDoc {
}
