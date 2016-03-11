var express = require('express')
var env = process.env;
var testhost = env.SHAPES_TEST_HOST || '127.0.0.1'
var testport = env.SHAPES_TEST_PORT || 3000
var puser = process.env.PSQL_USER ;
var ppass = process.env.PSQL_PASS ;
var phost = process.env.PSQL_HOST ;
var pport = process.env.PSQL_PORT || 5432;

var config_okay = require('config_okay')
var path = require('path')
var rootdir = path.normalize(__dirname)
var config_file = rootdir+'/test.config.json'


var serveStatic = require('serve-static')

// routes for geometry geojson stuffs
var spatial_routes = require('spatial_routes')
var carb_areas = spatial_routes.carb_areas
var detectors  = spatial_routes.detectors
var hpms_routes = require('hpms_locator').hpms_routes
var queue = require("queue-async")

var grid_records= require('calvad_areas').grid_records

var queries = require('calvad_grid_merge_sqlquery')
var hpms_data_route = queries.hpms_data_route
var hpms_data_nodetectors_route = queries.hpms_data_nodetectors_route


// for forEach on objects. also for flatten
var _ = require('lodash')

var all_hpms_handler = function(config,app){
    var ij_regex = /(\d*)_(\d*)/;
    app.get('/hpms/data/:yr.:format?'
           ,function(req,res,next){
                // build one task for each grid cell
                var year = req.params.yr
                var tasks=
                    _.map(grid_records,function(membership,cell_id){
                        var re_result = ij_regex.exec(cell_id)
                        return {'cell_id':cell_id
                               ,'cell_i':re_result[1]
                               ,'cell_j':re_result[2]
                               ,'year':year
                               ,'options':config
                               }
                    });
                var q = queue(4);
                res.setTimeout(0)
                tasks.forEach(function(t) {
                    q.defer(queries.hpms_data_handler,t);
                });
                q.awaitAll(function(error, results) {
                    console.log("all done!");
                    // spit out results stitched together
                    results = _.flatten(results)
                    // results = _.filter(results,function(r){
                    //           return r.f_system=='totals'
                    //           })
                    res.json(results)
                    return null
                })
                return null
            })
    return null
}


config_okay(config_file,function(err,c){

    var config ={'postgresql':c.postgresql
                ,'couchdb':c.couchdb}

    var app = express()
    app.use(serveStatic('public'))
    app.use(serveStatic('build'))
    queue()
    .defer(hourly_handler,config,hpmsfiles,app)
    .await(function(e){
        if(e) throw new Error(e)
        carb_areas(config.postgresql,app)
        hpms_routes(config.postgresql,app)
        hpms_data_route(config,app)
        hpms_data_nodetectors_route(config,app)
        all_hpms_handler(config,app)
        app.listen(3000,function(){
            console.log('hup!')
        })
        return null
    })

})
