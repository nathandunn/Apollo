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

                // console.log('RUNNING A PROJECTION NCLIST ');
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
                    var apollo = window.parent ;
                    // TODO: am I dropping properties this way?, do without doing a copy or copy with JSON.parse(JSON.stringify(inputFeature)) to fliter out functions
                    // TODO: project the entire feature to include the STRAND!
                    var newFeature = new SimpleFeature({
                        id: inputFeature.id ,
                        parent: parent,
                        data: {
                            start: apollo.projectValue(refSeq.name,inputFeature.get('start').toString()),
                            end: apollo.projectValue(refSeq.name,inputFeature.get('end').toString()),
                            strand: inputFeature.get('strand'),
                            name: inputFeature.get('name'),
                            id: inputFeature.get('id'),
                            type: inputFeature.get('type'),
                            description: inputFeature.get('description')
                        }
                    });
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


                    newFeature.data.subfeatures = array.map(inputFeature.get('subfeatures'), function(elt) {
                        return projectFeature(elt, newFeature);
                    });

                    // console.log('ABCD: '+JSON.stringify(inputFeature));

                    return inputFeature ;
                };
                var featCallback = function(feature) {
                    return origFeatCallback(projectFeature(feature,parent));
                };

                this.inherited(arguments, [query, featCallback, finishCallback, errorCallback]);
            }
        });
    });


