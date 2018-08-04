


/* eslint-disable */
console.log("We are here");
// const scaleLinear =  require("d3-scale");
// const axisTop = require( "d3-axis");
// const select = require( "d3-selection");
//import style from 'App.css';
// var d3 = require ("d3");

//module.exports = XMLHttpRequest;

var DrawGenomeView = function(data,svg_target){
    //Some of these were in different places
    //Not sure if we want these as constants or not
    var MAX_ROWS = 10;
    var calculatedHeight = 500;
    //console.log(data);
    //data = getDataApollo('1','1000','2000');
    //console.log(data);

    var UTR_feats= ["UTR","five_prime_UTR","three_prime_UTR"];
    var CDS_feats= ["CDS"];
    var exon_feats= ["exon"];
    var display_feats=["mRNA"];
    var dataRange = findRange(data,display_feats);

    var view_start = dataRange.fmin;
    var view_end = dataRange.fmax;
    //console.log(view_start);
    var exon_height = 10; // will be white / transparent
    var cds_height = 10; // will be colored in
    var isoform_height = 40; // height for each isoform
    var isoform_view_height = 20; // height for each isoform
    var isoform_title_height = 0; // height for each isoform
    var utr_height = 10; // this is the height of the isoform running all of the way through
    var transcript_backbone_height = 4; // this is the height of the isoform running all of the way through
    var arrow_height = 20;
    var arrow_width = 10;
    var arrow_points = '0,0 0,' + arrow_height + ' ' + arrow_width + ',' + arrow_width;


    //need to build a new sortWeight since these can be dynamic
    var sortWeight = {};
    for(var i=0,len = UTR_feats.length; i <len; i++){
        sortWeight[UTR_feats[i]]=200;
    }
    for(var i=0,len = CDS_feats.length; i <len; i++){
        sortWeight[CDS_feats[i]]=1000;
    }
    for(var i=0,len = exon_feats.length; i <len; i++){
        sortWeight[exon_feats[i]]=100;
    }

    /*
    var sortWeight = {
      'exon': 100
      , 'UTR': 200
      , 'five_prime_UTR': 200
      , 'three_prime_UTR': 200
      , 'CDS': 1000
      // , 'intron': 50
    };
    */

    //Testing if the countIsoforms function is broked
    //var numberIsoforms =2;
    var numberIsoforms = countIsoforms(data);
    if (numberIsoforms > MAX_ROWS) {
        calculatedHeight = (MAX_ROWS + 2) * isoform_height;
    }
    else {
        calculatedHeight = (numberIsoforms + 1) * isoform_height;
    }
    var margin = {top: 8, right: 30, bottom: 30, left: 40},
        width = 960 - margin.left - margin.right,
        height = calculatedHeight - margin.top - margin.bottom;

    var x = scaleLinear()
        .domain([view_start, view_end])
        .range([0, width]);



    var viewer = select(svg_target)
        .attr('width', width + margin.left + margin.right)
        .attr('height', height + margin.top + margin.bottom)
        .append('g')
        .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');

    var row_count =0;
    var used_space = [];
    var fmin_display=-1;
    var fmax_display=-1;
    console.log(data.length);
    for (var i in data) {
        console.log(i);
        var feature = data[i];
        console.log(feature);
        var featureChildren = feature.children;
        if (featureChildren) {

            var selected = feature.selected;

            //do I need this?
            var maxRows = MAX_ROWS;
            //var externalUrl = this.props.url;
            //May want to remove this and add an external sort function
            //outside of the render method to put certain features on top.
            featureChildren = featureChildren.sort(function (a, b) {
                if (a.name < b.name) return -1;
                if (a.name > b.name) return 1;
                return a - b;
            });
            featureChildren.forEach(function (featureChild) {
                var featureType = featureChild.type;
                if (display_feats.indexOf(featureType)>=0) {




                    //function to assign row based on available space.
                    var current_row = checkSpace(used_space,x(featureChild.fmin), x(featureChild.fmax));

                    if (current_row < maxRows) {
                        //Will need to remove this... rows not incremented every time now.
                        row_count += 1;



                        viewer.append('polygon')
                            .datum(function(){
                                var y_val;
                                if (feature.strand > 0) {
                                    y_val = Number((isoform_view_height / 2.0) - (arrow_height / 2.0) + (isoform_height * current_row) + isoform_title_height);
                                }
                                else {
                                    y_val = Number((isoform_view_height / 2.0) + (arrow_height / 2.0) + (isoform_height * current_row) + isoform_title_height);
                                }
                                return {fmin: featureChild.fmin,fmax: featureChild.fmax,strand:feature.strand,y_val: y_val};
                            })
                            .attr('class', 'transArrow')
                            .attr('points', arrow_points)
                            .attr('transform', function (d) {
                                if (feature.strand > 0) {
                                    return 'translate('+Number(x(d.fmax))+',' + d.y_val + ')';
                                }
                                else {
                                    return 'translate('+Number(x(d.fmin))+',' + d.y_val + ') rotate(180)';
                                }
                            });

                        viewer.append('rect')
                            .attr('class', 'transcriptBackbone')
                            .attr('x', x(featureChild.fmin))
                            .attr('y', isoform_height * current_row + isoform_title_height)
                            .attr('transform', 'translate(0,' + ( (isoform_view_height / 2.0) - (transcript_backbone_height / 2.0)) + ')')
                            .attr('height', transcript_backbone_height)
                            .attr('width', x(featureChild.fmax) - x(featureChild.fmin))
                            .datum({fmin: featureChild.fmin,fmax: featureChild.fmax});

                        var text_label = viewer.append('text')
                            .attr('class', 'transcriptLabel')
                            .attr('x', x(featureChild.fmin))
                            .attr('y', isoform_height * current_row + isoform_title_height)
                            .attr('fill', selected ? 'sandybrown' : 'gray')
                            .attr('opacity', selected ? 1 : 0.5)
                            .attr('height', isoform_title_height)
                            .text(featureChild.name)
                            .datum({fmin:featureChild.fmin});

                        //Now that the label has been created we can calculate the space that
                        //this new element is taking up making sure to add in the width of
                        //the box.
                        var text_width = text_label.node().getBBox().width;

                        //console.log(featureChild.name);
                        //console.log(text_width+x(featureChild.fmin));
                        //console.log(x(featureChild.fmax));
                        //console.log("Text Width " +text_width);
                        //console.log("Feature width " +Number(x(featureChild.fmax)-x(featureChild.fmin)));



                        var bp_end=0;
                        //First check to see if label goes past the end
                        if (Number(text_width+x(featureChild.fmin))>width){
                            console.log(featureChild.name+" goes over the edge");
                        }
                        var feat_end;
                        if(text_width>x(featureChild.fmax)-x(featureChild.fmin)){
                            feat_end=x(featureChild.fmin)+text_width;
                        }
                        else {
                            feat_end=x(featureChild.fmax);
                        }

                        //This is probably not the most efficent way to do this.
                        //Making an 2d array... each row is the first array (no zer0)
                        //next level is each element taking up space.
                        //Also using colons as spacers seems very perl... maybe change that?
                        if (used_space[current_row]){
                            var temp = used_space[current_row];
                            temp.push(x(featureChild.fmin)+":"+feat_end);
                            used_space[current_row]= temp;
                        }
                        else {
                            used_space[current_row]=[x(featureChild.fmin)+":"+feat_end]
                        }

                        //Now check on bounds since this feature is displayed
                        //The true end of display is converted to bp.
                        if(fmin_display < 0 ||fmin_display > featureChild.fmin){
                            fmin_display = featureChild.fmin;
                        }
                        if(fmax_display<0 || fmax_display < featureChild.fmax){
                            fmax_display = featureChild.fmax;
                        }

                        // have to sort this so we draw the exons BEFORE the CDS
                        featureChild.children = featureChild.children.sort(function (a, b) {

                            var sortAValue = sortWeight[a.type];
                            var sortBValue = sortWeight[b.type];

                            if (typeof sortAValue == 'number' && typeof sortBValue == 'number') {
                                return sortAValue - sortBValue;
                            }
                            if (typeof sortAValue == 'number' && typeof sortBValue != 'number') {
                                return -1;
                            }
                            if (typeof sortAValue != 'number' && typeof sortBValue == 'number') {
                                return 1;
                            }
                            // NOTE: type not found and weighted
                            return a.type - b.type;
                        });


                        featureChild.children.forEach(function (innerChild) {
                            var innerType = innerChild.type;
                            if (exon_feats.indexOf(innerType)>=0) {
                                viewer.append('rect')
                                    .attr('class', 'exon')
                                    .attr('x', x(innerChild.fmin))
                                    .attr('y', isoform_height * current_row + isoform_title_height)
                                    .attr('transform', 'translate(0,' + ( (isoform_view_height / 2.0) - (exon_height / 2.0)) + ')')
                                    .attr('height', exon_height)
                                    .attr('z-index', 10)
                                    .attr('width', x(innerChild.fmax) - x(innerChild.fmin))
                                    .datum({fmin: innerChild.fmin,fmax: innerChild.fmax});
                            }
                            else if (CDS_feats.indexOf(innerType)>=0) {
                                viewer.append('rect')
                                    .attr('class', 'CDS')
                                    .attr('x', x(innerChild.fmin))
                                    .attr('y', isoform_height * current_row + isoform_title_height)
                                    .attr('transform', 'translate(0,' + ( (isoform_view_height / 2.0) - (cds_height / 2.0)) + ')')
                                    .attr('z-index', 20)
                                    .attr('height', cds_height)
                                    .attr('width', x(innerChild.fmax) - x(innerChild.fmin))
                                    .datum({fmin: innerChild.fmin,fmax: innerChild.fmax});
                            }
                            else if (UTR_feats.indexOf(innerType)>=0) {
                                viewer.append('rect')
                                    .attr('class', 'UTR')
                                    .attr('x', x(innerChild.fmin))
                                    .attr('y', isoform_height * current_row + isoform_title_height)
                                    .attr('transform', 'translate(0,' + ( (isoform_view_height / 2.0) - (utr_height / 2.0)) + ')')
                                    .attr('z-index', 20)
                                    .attr('height', utr_height)
                                    .attr('width', x(innerChild.fmax) - x(innerChild.fmin))
                                    .datum({fmin: innerChild.fmin,fmax: innerChild.fmax});
                            }
                        });
                    }
                    else if (current_row == maxRows) {
                        ++current_row;
                        viewer.append('a')
                            .attr('class', 'transcriptLabel')
                            //.attr('xlink:href', externalUrl)
                            .attr('xlink:show', 'new')
                            .append('text')
                            .attr('x', x(feature.fmin) + 30)
                            .attr('y', isoform_height * current_row + isoform_title_height)
                            .attr('fill', 'red')
                            .attr('opacity', 1)
                            .attr('height', isoform_title_height)
                            .text('Maximum features displayed.  See full view for more.');
                    }
                }
            });
        }
    }
    if (row_count == 0) {
        viewer.append('text')
            .attr('x', 30)
            .attr('y', isoform_title_height + 10)
            .attr('fill', 'orange')
            .attr('opacity', 0.6)
            // .attr('height', isoform_title_height)
            .text('Overview of non-coding genome features unavailable at this time.');

    }
    else {

        //Check if resize is necessary
        /*
        if(fmin_display>view_start ||fmax_display < view_end){
            x = d3.scaleLinear()
              .domain([fmin_display, fmax_display])
              .range([0, width]);
            view_start = fmin_display;
            view_end = fmax_display;
          doResize(fmin_display,fmax_display,viewer,width,x);
          console.log(fmin_display,fmax_display);
        }
        */


        // 9 ticks
        var viewLength = view_end - view_start;
        var resolution = Math.round(30 / Math.log(viewLength)) ;
        var resolutionString = '.'+resolution + 's';
        var tickFormat = x.tickFormat(5, resolutionString);

        var xAxis = axisTop(x)
            .ticks(8, 's')
            .tickSize(8)
            .tickFormat(tickFormat);

        viewer.append('g')
            .attr('class', 'axis')
            .attr('width', width)
            .attr('height', 20)
            .attr('transform', 'translate(0,20)')
            .call(xAxis);
    }



}


function doResize(fmin_display, fmax_display, viewer,width,newx){
    console.log("Do resize.");



    viewer.selectAll("rect.transcriptBackbone")
        .attr("x", function(d){return newx(d.fmin);})
        .attr('width', function(d){return newx(d.fmax) - newx(d.fmin);})

    viewer.selectAll("rect.exon")
        .attr("x", function(d){return newx(d.fmin);})
        .attr('width', function(d){return newx(d.fmax) - newx(d.fmin);})

    viewer.selectAll("rect.CDS")
        .attr("x", function(d){return newx(d.fmin);})
        .attr('width', function(d){return newx(d.fmax) - newx(d.fmin);})

    viewer.selectAll("rect.UTR")
        .attr("x", function(d){return newx(d.fmin);})
        .attr('width', function(d){return newx(d.fmax) - newx(d.fmin);})

    viewer.selectAll("polygon.transArrow")
        .attr('transform', function (d) {
            if (d.strand > 0) {
                return 'translate('+Number(newx(d.fmax))+',' + d.y_val + ')';
            }
            else {
                return 'translate('+Number(newx(d.fmin))+',' + d.y_val + ') rotate(180)';
            }
        });

    var text_label = viewer.selectAll("text.transcriptLabel")
        .attr("x", function(d){
            return Number(newx(d.fmin))
        })
        .attr("class",function(d){
            text_width = this.getBBox().width;
            if((newx(d.fmin)+text_width)>width){
                return "REMOVE";
            }
            else {
                return "transcriptLabel";
            }
        });

    viewer.selectAll("text.REMOVE").remove();
}
//Function to find range
//Now with checkSpace function embedded.
//Will only check rows that make it into the final viz.
//Needs to assign the row as well
//Added check for type.... all types were getting included even if
//we had no intention to display them
function findRange(data,display_feats) {
    var fmin = -1;
    var fmax = -1;

    for (var d in data) {
        var feature = data[d];
        console.log(feature.type);
        var featureChildren = feature.children;
        featureChildren.forEach(function (featureChild) {
            if (display_feats.indexOf(featureChild.type)>=0){
                //console.log(featureChild.name,featureChild.fmin,featureChild.fmax);
                if (fmin < 0 || featureChild.fmin < fmin) {
                    fmin = featureChild.fmin;
                }
                if (fmax < 0 || featureChild.fmax > fmax) {
                    fmax = featureChild.fmax;
                }
            }
        });//transcript level
    }//gene level

    return {
        fmin: fmin,
        fmax: fmax
    };
}
//;

function countIsoforms(data) {
    var isoform_count = 0;
    // gene level
    for (var i in data) {
        var feature = data[i];
        if(feature.children){
            feature.children.forEach(function (geneChild) {
                // isoform level
                if (geneChild.type == 'mRNA') {
                    isoform_count += 1;
                }
            });
        }
    }
    return isoform_count;
}

var GenerateGenomeView= function(chr, start, end, organism,svg_target)
{
    //Clear it first maaang
    console.log("generating.... for "+svg_target+"!");
    var svg = select(svg_target);
    svg.selectAll("*").remove();
    //Right now this is Hardcoded
    var externalLocationString = chr + ':' + start + '..' + end;
    var dataUrl ="https://agr-apollo.berkeleybop.io/apollo/track/" +encodeURI(organism)+ "/All%20Genes/" + encodeURI(externalLocationString) + ".json";
    fetch(dataUrl)
        .then((response) => {
        response.json()
        .then(data => {
        //console.log("In the data function maaaan.");
        //console.log(data);
        DrawGenomeView(data,svg_target);

});
})
.catch((error) => {
    console.log(error);
});
    //return data;
};

//Takes in the current entry start/end and the array of used space and assigns a row
function checkSpace(used_space,start,end)
{
    var row;
    var assigned;
    var fits;
    //if empty... this is the first entry... on the first row.

    if(used_space.length==0)
    {row = 1;}
    else{
        //for each row
        for(var i=1; i<used_space.length; i++){
            //for each entry in that row
            for(var z=0; z<used_space[i].length; z++){

                var [used_start,used_end] = used_space[i][z].split(':');

                //check for overlap
                if(end<used_start||start>used_end){
                    fits=1;
                }
                else {
                    fits=0;break;
                }
            }
            if(fits){
                assigned=1;row=i;break;
            }
        }
        //if this is true for 0 rows... use the next row.
        //zero indexed so length is the next one.
        if(!assigned)
        {row =used_space.length;}
    }
    return row;

}
// module.exports = GenerateGenomeView;

