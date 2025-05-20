# Lucene to Elasticsearch
The goal of this program is to copy documents from Lucene to Elasticsearch.

1. It finds the unstored fields value.
2. It searches that from lucene search.
3. It collects that documents.
4. After 10000 documents currently, bulks that documents into the elasticsearch.
5. Stores the last value and continues from there.

## Software Requirements
- Java 21
- Lucene 2.9.4
- Elasticsearch 8.17.4
 
## Limitations
- Only stores documents up until today.
