var d3=require('mbostock~d3@v3.4.13')
d3.geo.tile=function(){function t(){var t=Math.max(Math.log(n)/Math.LN2-8,0),h=Math.round(t+e),o=Math.pow(2,t-h+8),u=[(r[0]-n/2)/o,(r[1]-n/2)/o],l=[],c=d3.range(Math.max(0,Math.floor(-u[0])),Math.max(0,Math.ceil(a[0]/o-u[0]))),M=d3.range(Math.max(0,Math.floor(-u[1])),Math.max(0,Math.ceil(a[1]/o-u[1])));return M.forEach(function(t){c.forEach(function(a){l.push([a,t,h])})}),l.translate=u,l.scale=o,l}var a=[960,500],n=256,r=[a[0]/2,a[1]/2],e=0;return t.size=function(n){return arguments.length?(a=n,t):a},t.scale=function(a){return arguments.length?(n=a,t):n},t.translate=function(a){return arguments.length?(r=a,t):r},t.zoomDelta=function(a){return arguments.length?(e=+a,t):e},t};

var hpmssums = require('./lib/map-component/js/hpms2009.json')

var plotter  = require('./data.js')


var hpms_map = {}
hpmssums.forEach(function(row){
    hpms_map[row.cell_i + '_'+ row.cell_j] = row
})

//var topology = require('./lib/map-component/js/topology4326.json')
var grids = require('./lib/map-component/js/grids.json')

// hacking has now begun in earnest!
var idfixer = /\.0*/g
var gridhash = {}
grids.features.forEach(function(f){
    var fixid = f.properties.id.replace(idfixer,'')
    gridhash[fixid]=f
    return null
})


var color = d3.scale.pow()
            .exponent(1/2)
    .domain([ 0, 1744422])
    .range(["white", "green"]);


var tileContainer


var width = Math.max(960, window.innerWidth),
    height = Math.max(500, window.innerHeight);

var tiler = d3.geo.tile()
    .size([width, height]);

var projection = d3.geo.mercator()
    .scale((1 << 21) / 2 / Math.PI)
    .translate([width / 2, height / 2]);

var tileProjection = d3.geo.mercator();
var tilePath = d3.geo.path()
    .projection(tileProjection);

var center = projection([-117.84, 33.6142]);


var zoom = d3.behavior.zoom()
    .scale(projection.scale() * 2 * Math.PI)
           .scaleExtent([1 << 12, 1 << 24])
           .translate([width - center[0],
                       height - center[1]])
           .on("zoom", zoomed);


// With the center computed, now adjust the projection such that
// it uses the zoom behavior’s translate and scale.

var path = d3.geo.path()
    .projection(projection);

projection
.scale( 1 / 2 / Math.PI)
 .translate([0, 0]);


var vectors={}

function zoomed() {
    var tiles = tiler
                .scale(zoom.scale())
                .translate(zoom.translate())
        ();

    //g.call(renderTiles, "highroad")
    //g.call(renderTiles, "buildings")
    //g.call(renderTiles2,"counties")
    g.call(renderTiles2,"hpms/links")
    g.call(renderTiles2,"grid4k")

    //g.call(rendertopo)
    g.call(renderplottiles,[
        //'201_98',
        '202_98',
        '203_98',
        // '201_99',
        '202_99',
        '203_99'
        // '201_97',
        //'202_97',
        //'203_97'
    ])
}

var svg = d3.select("body").append("svg")
    .attr("width", width)
    .attr("height", height)
          .attr('transform','translate(0,'+height/3+')')
  .append("g");

var g = svg.append("g");

svg.append("rect")
    .attr("class", "overlay")
    .attr("width", width)
    .attr("height", height);

svg
     .call(zoom)
     .call(zoom.event)


function renderTiles(svg, type) {
    // get the list of cells to grab for this zoom, translation
    var tiles = tiler
                .scale(zoom.scale())
                .translate(zoom.translate())
        ();

    projection
    .scale(zoom.scale() / 2 / Math.PI)
    .translate(zoom.translate());

    if(vectors[type] === undefined ){
        vectors[type] = svg.append("g").attr("class", type)
    }
    var layer = vectors[type]



    //.style("stroke-width", 1 / zoom.scale());


    var cells = layer
                 .selectAll("g")
                .data(tiles,function(d){
                    return d;
                });

    //console.log(cells[0].length)

    //.style("stroke-width", 1 / zoom.scale());

    cells.exit()
    .each(function(d) { this._xhr.abort(); })
    .remove();


    //reproject
    cells.selectAll("path").attr("d",path)

    // I can't get SVG transform to work...I don't know what to transform to

    cells.enter()
    .append("g")
    .each(function(d) {
        var g = d3.select(this);
        this._xhr = d3.json("http://" + ["a", "b", "c"][(d[0] * 31 + d[1]) % 3] + ".tile.openstreetmap.us/vectiles-" + type + "/" + d[2] + "/" + d[0] + "/" + d[1] + ".json", function(error, json) {
                        if(json.features !== undefined){
                            g.selectAll("path")
                            .data(json.features.sort(function(a, b) {
                                      return a.properties.sort_key -
                                          b.properties.sort_key; }))
                            .enter().append("path")
                            .attr("class", function(d) {
                                return d.properties.kind;
                            })
                            //.attr("transform", "translate(" + projection.translate() + ")scale(" + projection.scale() + ")")
                            //.attr("d", tilePath)
                            .attr("d", path)
                        }
                    });
    });

}

function renderTiles2(svg, type) {
    // get the list of cells to grab for this zoom, translation
    var tiles = tiler
                .scale(zoom.scale())
                .translate(zoom.translate())
        ();

    projection
    .scale(zoom.scale() / 2 / Math.PI)
    .translate(zoom.translate());

    if(vectors[type] === undefined ){
        vectors[type] = svg.append("g").attr("class", type)
    }
    var layer = vectors[type]



    //.style("stroke-width", 1 / zoom.scale());


    var cells = layer
                 .selectAll("g")
                .data(tiles,function(d){
                    return d;
                });

    //console.log(cells[0].length)

    //.style("stroke-width", 1 / zoom.scale());

    cells.exit()
    .each(function(d) { this._xhr.abort(); })
    .remove();


    //reproject
    cells.selectAll("path").attr("d",path)


    cells.enter()
    .append("g")
    .each(function(d) {
        var g = d3.select(this);
        this._xhr = d3.json("/" + type + "/"+d[2]+'/'+d[0]+'/'+d[1] + ".json", function(error, json) {
            if(json.features === undefined) return null
            g.selectAll("path")
                .data(json.features.sort(function(a, b) {
                    return a.properties.sort_key -
                        b.properties.sort_key; }))
                .enter().append("path")
            // assign the appropriate class to the new "path" element
                        .attr("class",function(d) {
                            var props = d.properties
                            // if an airdistrict, make class a string
                            // prepended by 'dis'
                            if(props.dis !== undefined){
                                return "dis"+ props.dis
                            }
                            // if a county (fips defined) make class a
                            // string prepended by fips
                            if(props.fips !== undefined){
                                return "fips"+ props.fips
                            }
                            // if an hpms link make class plain old
                            // "hpmslink", although I could do more
                            // interesting things here with different
                            // types of roadway, for example,
                            // hpms_major, hpms_minor, etc etc.  Or
                            // class the road based on both roadway
                            // type and AADT, so that I can style
                            // based on both, perhaps width by class,
                            // color by aadt
                            if(props.hpms_id !== undefined){
                                return "hpmslink"
                            }
                            // if a grid tile, assign two classes,
                            // hpms and another defining the grid cell
                            // based on the grid cell id (stripping
                            // out any extraneous decimal stupidness
                            // that are always .000), for easy
                            // fetching later I think.  because in
                            // this case I color the grid cell based
                            // on the volume in the grid cell, so that
                            // means I have to later pair up the
                            // actual grid cell with its volume to
                            // give it a color.  by setting the class
                            // equal to the grid cell id, css
                            // selectors will work to get the grid
                            // cells and set the color (in some way,
                            // likely by updating a class rather than
                            // actually setting the element by element
                            // color.  Not sure, haven't looked
                            // through my code enough yet to know.
                            // Realize that I'm wriging this in August
                            // 2016, but the code was written Dec
                            // 2014)
                            if(type==='grid4k'){
                                // // 4 cases, odd odd, odd even, even odd. even even
                                // var row = 'even'
                                // var col = 'even'
                                // if(props.i_cell % 2) row='odd'
                                // if(props.j_cell % 2) col='odd'
                                //return row+'_'+col

                                return 'hpmsdata '+ props.id.replace(/\.0*/g,'')

                            }
                            if(props.id !== undefined){
                                return props.id
                            }
                            return type
                        })
            // ah, no, sadly I actually set the fill right here,
            // rather than using a class selector to do it.
                        .style("fill",function(d){
                            // need to make key for hpms_map
                            var props = d.properties
                            //console.log(props)
                            var key = Number(props.i_cell).toFixed() + '_' + Number(props.j_cell).toFixed()
                            //console.log(key)
                            var value = hpms_map[key]
                            if(value) return color(value.sum_vmt)
                            return color(1)
                        })
                        .attr("d", path)

                    });
    });

}


function rendertopo(svg){


    var type = 'grid-boundaries'
    if(vectors[type] === undefined ){
        vectors[type] = svg.append("g").attr("class", type)
    }
    var layer = vectors[type]

    var mesh = topojson.mesh(topology,topology.objects.grids)
    var gg = layer.selectAll("path").data([mesh], function(d) { return 1; })

    // reproject if already drawn
    gg.attr("d",path)

    // draw if not here yet
    gg.enter()
    .append("path")
    .attr("d", path);

}


function rendergrid(svg){


    var type = 'grid-boundaries'
    if(vectors[type] === undefined ){
        vectors[type] = svg.append("g").attr("class", type)
    }
    var layer = vectors[type]

    var gg = layer.selectAll("path").data(grids.features)

    // reproject if already drawn
    gg.attr("d",path)

    // draw if not here yet
    gg.enter()
    .append("path")
    .attr("d", path);

}

function renderplottiles(svg,tileids){


    var type = 'plot-tiles'
    if(vectors[type] === undefined ){
        vectors[type] = svg.append("g").attr("class", type)
    }
    var drawn = []
    tileids.forEach(function(id){
        drawn.push(gridhash[id])})

    var layer = vectors[type]
    var gg = layer.selectAll("path").data(drawn
                                         ,function(d,i){
                                              return d.properties.id
                                          })

    // reproject if already drawn
    gg.attr("d",path)

    // draw if not here yet
    gg.enter()
    .append("path")
    .attr("d", path);

}


// looks like a debugging only type of thing.  Hopefully not running
// in production as all this does is spit out the position of the
// mouse to info.text()
function mousemoved() {
  info.text(formatLocation(projection.invert(d3.mouse(this)), zoom.scale()));
}

// looks like something I copied, because I am not so facile with
// matrix transformations.  I mean, I've used them before, but mostly
// when doing server-side rendering or when playing with SVG and
// PostScript directly.  This looks to me like something that was in
// the d3 demo I undoubtedly started this code from.
function matrix3d(scale, translate) {
  var k = scale / 256, r = scale % 1 ? Number : Math.round;
  return "matrix3d(" + [k, 0, 0, 0, 0, k, 0, 0, 0, 0, k, 0, r(translate[0] * scale), r(translate[1] * scale), 0, 1 ] + ")";
}


d3.select(self.frameElement).style("height", height + "px");

//plotter()
