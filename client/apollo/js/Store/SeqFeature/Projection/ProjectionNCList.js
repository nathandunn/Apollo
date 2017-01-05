define([
        'dojo/_base/declare',
        'dojo/_base/lang',
        'dojo/_base/array',
        'JBrowse/Store/SeqFeature/NCList',
        'JBrowse/Model/SimpleFeature'
    ],
    function(
        declare,
        lang,
        array,
        NCList,
        SimpleFeature
    ) {
        return declare(NCList, {

            constructor: function (args) {
                console.log("constructing projection-nclist");
            },

            getApollo: function(){
                return window.parent;
            },
            getFeatures: function(query, origFeatCallback, finishCallback, errorCallback) {

                console.log('RUNNING A PROJECTION NCLIST ');
                // var rev = this.browser.config.reverseComplement;
                // var startBase  = query.start;
                // var endBase    = query.end;
                // var len = this.refSeq.length;
                // if (rev) {
                //     query.start = len - endBase;
                //     query.end = len - startBase;
                // }

                var refSeq = this.refSeq ;


                var projectFeature = function(inputFeature,parent) {
                    // var array = this.getApollo().projectFeatures(JSON.stringify(array),refSeq.name);
                    // TODO: just do the version
                    // TODO: just do the full copy
                    // var newFeature = new SimpleFeature({
                    //     id: inputFeature.id ,
                    //     parent: parent,
                    //     data: {
                    //         start: inputFeature.get('start'),
                    //         end: inputFeature.get('end'),
                    //         strand: inputFeature.get('strand'),
                    //         name: inputFeature.get('name'),
                    //         id: inputFeature.get('id'),
                    //         type: inputFeature.get('type'),
                    //         description: inputFeature.get('description')
                    //     }
                    // });
                    // for(var k in inputFeature){
                    //   newFeature.data[k]=inputFeature[k];
                    // }
                    // Object.keys(inputFeature).forEach(function(key) {
                    //     // console.log('copying key: '+key + ' of type ' + (typeof key));
                    //     if(key instanceof Object || key instanceof String || key instanceof Number || key instanceof Array){
                    //         newFeature.data[key] = inputFeature[key];
                    //     }
                    // });
                    // Object.assign(newFeature.data,inputFeature);


                    // newFeature.data.subfeatures = array.map(inputFeature.get('subfeatures'), function(elt) {
                    //     return projectFeature(elt, newFeature);
                    // });
                    // return newFeature;

                    console.log(JSON.stringify(inputFeature));

                    return inputFeature ;
                };
                var featCallback = function(feature) {
                    return origFeatCallback(projectFeature(feature));
                };

                this.inherited(arguments, [query, featCallback, finishCallback, errorCallback]);
            }
        });
    });


