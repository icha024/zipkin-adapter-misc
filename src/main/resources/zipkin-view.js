{
   "_id": "_design/search",
   "language": "javascript",
   "views": {
       "traceid-by-time": {
           "map": "function(doc){ emit(doc.timestamp, doc.traceId)}"
       },
       "span-by-traceid": {
           "map": "function(doc){ emit(doc.traceId, doc)}"
       },
       "service-names": {
           "map": "function(doc){ emit(doc.annotations[0].endpoint.serviceName, null)}",
           "reduce": "_count"
       },
       "service-span-names": {
           "map": "function(doc){ emit([doc.annotations[0].endpoint.serviceName, doc.name], null)}",
           "reduce": "_count"
       },
       "trace": {
           "map": "function(doc){ emit(doc.traceId, doc)}"
       }
   }
}