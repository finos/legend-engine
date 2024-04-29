The Java protocol classes are generated from the Pure specification.

These classes generated this way cannot parse or generate the JSON Elasticsearch expects.

To compensate, the Java generator also generates `MixIn` classes layering in the Elasticsearch processing requirements.

These `MixIn` classes are leveraged when configuring Jackson's ObjectMapper, associating the original protocol class with the `MixIn`.

