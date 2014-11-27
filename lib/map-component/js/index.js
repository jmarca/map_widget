var d3=require('mbostock~d3@v3.4.13')
d3.geo.tile=function(){function t(){var t=Math.max(Math.log(n)/Math.LN2-8,0),h=Math.round(t+e),o=Math.pow(2,t-h+8),u=[(r[0]-n/2)/o,(r[1]-n/2)/o],l=[],c=d3.range(Math.max(0,Math.floor(-u[0])),Math.max(0,Math.ceil(a[0]/o-u[0]))),M=d3.range(Math.max(0,Math.floor(-u[1])),Math.max(0,Math.ceil(a[1]/o-u[1])));return M.forEach(function(t){c.forEach(function(a){l.push([a,t,h])})}),l.translate=u,l.scale=o,l}var a=[960,500],n=256,r=[a[0]/2,a[1]/2],e=0;return t.size=function(n){return arguments.length?(a=n,t):a},t.scale=function(a){return arguments.length?(n=a,t):n},t.translate=function(a){return arguments.length?(r=a,t):r},t.zoomDelta=function(a){return arguments.length?(e=+a,t):e},t};

var tileContainer

var width = Math.max(960, window.innerWidth),
    height = Math.max(500, window.innerHeight),
    transform = ["", "-webkit-", "-moz-", "-ms-", "-o-"].reduce(function(p, v) { return v + "transform" in document.body.style ? v : p; }) + "transform";


var projection = d3.geo.mercator()
    .scale((1 << 21) / 2 / Math.PI)
    .translate([-width / 2, -height / 2]); // just temporary

var tileProjection = d3.geo.mercator();


var tilePath = d3.geo.path()
    .projection(tileProjection);

var zoom = d3.behavior.zoom()
           .scale(projection.scale() * 2 * Math.PI)
           .scaleExtent([1 << 20, 1 << 23])
           .translate(projection([-117.84, 33.6142]).map(function(x) {
                          return -x;
                      }))
           .on("zoom", zoomed);

var tiler = d3.geo.tile()
            .size([width, height]);



    function runterTiles(d) {
        var svg = d3.select(this);
        this._xhr = d3.json("http://" + ["a", "b", "c"][(d[0] * 31 + d[1]) % 3] + ".tile.openstreetmap.us/vectiles-highroad/" + d[2] + "/" + d[0] + "/" + d[1] + ".json",
                function(error, json) {
                    var k = Math.pow(2, d[2]) * 256; // size of the world in pixels

                    tilePath.projection()
                    .translate([k / 2 - d[0] * 256, k / 2 - d[1] * 256]) // [0°,0°] in pixels
                    .scale(k / 2 / Math.PI);

                    svg.selectAll("path")
                    .data(json.features.sort(function(a, b) { return a.properties.sort_key - b.properties.sort_key; }))
                    .enter().append("path")
                    .attr("class", function(d) { return d.properties.kind; })
                    .attr("d", tilePath);
                });
    }

function zoomed() {
    var tiles = tiler
                .scale(zoom.scale())
                .translate(zoom.translate())
        ();


    projection
    .scale(zoom.scale() / 2 / Math.PI)
    .translate(zoom.translate());

    var tile = tileContainer
               .style(transform, matrix3d(tiles.scale, tiles.translate))
               .selectAll(".tile")
               .data(tiles, function(d) { return d; });

    tile.exit()
    .each(function(d) { this._xhr.abort(); })
    .remove();

    tile.enter().append("svg")
    .attr("class", "tile")
    .style("left", function(d) { return d[0] * 256 + "px"; })
    .style("top", function(d) { return d[1] * 256 + "px"; })

    .each(runterTiles);
}





function matrix3d(scale, translate) {
  var k = scale / 256, r = scale % 1 ? Number : Math.round;
  return "matrix3d(" + [k, 0, 0, 0, 0, k, 0, 0, 0, 0, k, 0, r(translate[0] * scale), r(translate[1] * scale), 0, 1] + ")";
}

function prefixMatch(p) {
  var i = -1, n = p.length, s = document.body.style;
  while (++i < n) if (p[i] + "Transform" in s) return "-" + p[i].toLowerCase() + "-";
  return "";
}

function formatLocation(p, k) {
  var format = d3.format("." + Math.floor(Math.log(k) / 2 - 2) + "f");
  return (p[1] < 0 ? format(-p[1]) + "°S" : format(p[1]) + "°N") + " "
       + (p[0] < 0 ? format(-p[0]) + "°W" : format(p[0]) + "°E");
}


function doit(){

    var map = d3.select("body").append("div")
              .attr("class", "map")
              .style("width", width + "px")
              .style("height", height + "px")
              .call(zoom)
              .on("mousemove", mousemoved)

    tileContainer = map.append("div")
                        .attr("class", "tiles")
    //.attr("class", "layer");

    var info = map.append("div")
               .attr("class", "info");

    function mousemoved() {
        info.text(formatLocation(projection.invert(d3.mouse(this)), zoom.scale()));
    }

    zoomed();

}

console.log('here i am')
module.exports=doit