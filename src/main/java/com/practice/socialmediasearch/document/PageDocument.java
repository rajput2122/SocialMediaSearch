package com.practice.socialmediasearch.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

@Document(indexName = "pages", createIndex = false)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageDocument {

    @Id
    private String id;

    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            }
    )
    private String pageName;

    @Field(type = FieldType.Text)
    private String bio;
}

