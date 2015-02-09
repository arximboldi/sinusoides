# Minimalistic test server

express = require 'express'
app = express()

app.engine('html', require('ejs').renderFile)
app.get '/', (req, res) -> res.render('index.html')
app.get '/debug', (req, res) -> res.render('debug.html')
app.use "/build", express.static(__dirname + '/build')
app.use "/css", express.static(__dirname + '/css')
app.use "/data", express.static(__dirname + '/data')
app.use "/ext", express.static(__dirname + '/ext')
app.use "/old", express.static(__dirname + '/old')

PORT = 8746
app.listen PORT, -> console.log("Listening on port " + PORT)
