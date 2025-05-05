# Lucene to Elasticsearch
The goal of this program is to copy documents from Lucene to Elasticsearch.
 
1. The program identifies the first document in Lucene and stores its date in a synchronization file.
2. Documents are copied from Lucene to Elasticsearch day by day, starting from the first day until today.
3. Documents for today are not copied; the program waits until tomorrow.
4. When it becomes tomorrow, the program generates data for both Lucene and Elasticsearch.
5. The documents from the previous day are then copied from Lucene to Elasticsearch.
 
## Software Requirements
- Java 21
- Lucene 2.9.4
- Elasticsearch 8.17.4
 
## Limitations
- It is assumed that yesterday's data can be copied within one day.
