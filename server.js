var express = require('express')
var env = process.env;
var testhost = env.SHAPES_TEST_HOST || '127.0.0.1'
var testport = env.SHAPES_TEST_PORT || 3000
var puser = process.env.PSQL_USER ;
var ppass = process.env.PSQL_PASS ;
var phost = process.env.PSQL_HOST ;
var pport = process.env.PSQL_PORT || 5432;

var app = express()

var serveStatic = require('serve-static')

// routes for geometry geojson stuffs
var spatial_routes = require('spatial_routes')
var carb_areas = spatial_routes.carb_areas
var detectors  = spatial_routes.detectors

app.use(serveStatic('public'))
app.use(serveStatic('build'))
carb_areas({},app)


app.listen(3000)