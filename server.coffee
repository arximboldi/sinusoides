# Minimalistic test server

express = require 'express'
app = express()

app.engine('html', require('ejs').renderFile)
app.use "/build", express.static(__dirname + '/build')
app.use "/css", express.static(__dirname + '/css')
app.use "/data", express.static(__dirname + '/data')
app.use "/ext", express.static(__dirname + '/ext')
app.use "/static", express.static(__dirname + '/static')
app.get '/debug*', (req, res) -> res.render('debug.html')
app.get '/*', (req, res) -> res.render('index.html')


PORT = 8746
app.listen PORT, -> console.log("Listening on port " + PORT)
