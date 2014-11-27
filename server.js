var express = require('express')
var env = process.env;
var testhost = env.SHAPES_TEST_HOST || '127.0.0.1'
var testport = env.SHAPES_TEST_PORT || 3000

var app = express()

var serveStatic = require('serve-static')

app.use(serveStatic('public'))
app.use(serveStatic('build'))


app.listen(3000)