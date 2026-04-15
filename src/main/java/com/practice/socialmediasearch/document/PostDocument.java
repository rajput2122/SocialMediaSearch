package com.practice.socialmediasearch.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.util.List;

@Document(indexName = "posts", createIndex = false)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String caption;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Keyword)
    private String locationName;

    @Field(type = FieldType.Keyword)
    private String userId;
}

