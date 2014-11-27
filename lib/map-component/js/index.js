var d3=require('mbostock~d3@v3.4.13')
d3.geo.tile=function(){function t(){var t=Math.max(Math.log(n)/Math.LN2-8,0),h=Math.round(t+e),o=Math.pow(2,t-h+8),u=[(r[0]-n/2)/o,(r[1]-n/2)/o],l=[],c=d3.range(Math.max(0,Math.floor(-u[0])),Math.max(0,Math.ceil(a[0]/o-u[0]))),M=d3.range(Math.max(0,Math.floor(-u[1])),Math.max(0,Math.ceil(a[1]/o-u[1])));return M.forEach(function(t){c.forEach(function(a){l.push([a,t,h])})}),l.translate=u,l.scale=o,l}var a=[960,500],n=256,r=[a[0]/2,a[1]/2],e=0;return t.size=function(n){return arguments.length?(a=n,t):a},t.scale=function(a){return arguments.length?(n=a,t):n},t.translate=function(a){return arguments.length?(r=a,t):r},t.zoomDelta=function(a){return arguments.length?(e=+a,t):e},t};

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

console.log(center)

//projection([-117.84, 33.6142])

var zoom = d3.behavior.zoom()
    .scale(projection.scale() * 2 * Math.PI)
           .scaleExtent([1 << 19, 1 << 24])
           .translate([width - center[0],
                       height - center[1]])
           .on("zoom", zoomed);

console.log(JSON.stringify({'scale':zoom.scale(),
                            'translate':zoom.translate()}))
console.log(JSON.stringify({'scale':projection.scale(),
                            'translate':projection.translate()}))


// With the center computed, now adjust the projection such that
// it uses the zoom behavior’s translate and scale.

var path = d3.geo.path()
    .projection(projection);

projection
.scale( 1 / 2 / Math.PI)
 .translate([0, 0]);


console.log(JSON.stringify({'scale':zoom.scale(),
                            'translate':zoom.translate()}))
console.log(JSON.stringify({'scale':projection.scale(),
                            'translate':projection.translate()}))

var vectors={}

function zoomed() {
    console.log('zoom')
    var tiles = tiler
                .scale(zoom.scale())
                .translate(zoom.translate())
        ();
//    g      .attr("transform", "scale(" + tiles.scale + ")translate(" + tiles.translate + ")")
    console.log(JSON.stringify({'scales':[zoom.scale(),d3.event.scale,tiles.scale],
                                'translate':tiles.translate}))
//    g.attr("transform", "translate(" + tiles.translate + ")scale(" + tiles.scale + ")");


    g.call(renderTiles, "highroad")
    .call(renderTiles, "buildings");
}

var svg = d3.select("body").append("svg")
    .attr("width", width)
    .attr("height", height)
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

                    });
    });

}


function mousemoved() {
  info.text(formatLocation(projection.invert(d3.mouse(this)), zoom.scale()));
}

function matrix3d(scale, translate) {
  var k = scale / 256, r = scale % 1 ? Number : Math.round;
  return "matrix3d(" + [k, 0, 0, 0, 0, k, 0, 0, 0, 0, k, 0, r(translate[0] * scale), r(translate[1] * scale), 0, 1 ] + ")";
}


d3.select(self.frameElement).style("height", height + "px");
