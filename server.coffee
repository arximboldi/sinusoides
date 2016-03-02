# Minimalistic test server

express = require 'express'
app = express()

PATH = __dirname + '/resources'
PORT = 8746

app.engine('html', require('ejs').renderFile)

app.get '/', (req, res) -> res.render(PATH + '/views/release.html')
app.get '/index.html', (req, res) -> res.render(PATH + '/views/release.html')

app.use "/", express.static(PATH)
app.get '/debug*', (req, res) -> res.render(PATH + '/views/debug.html')
app.get '/*', (req, res) -> res.render(PATH + '/views/release.html')

app.listen PORT, -> console.log("Listening on port " + PORT)
