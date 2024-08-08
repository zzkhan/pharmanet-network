import app from "./app";
const PORT = process.env.PORT || 8080;
const express = require("express");
const path = require("path");

const STATIC_ASSETS_PATH = path.resolve(`${__dirname}/../../static`);

// Serve front end assets which have been built by webpack
app.use("/static", express.static(STATIC_ASSETS_PATH));

app.get("/", (request, response) => {
    response.sendFile('../client/index.js', {root: __dirname});
});

app.listen(PORT, () => {
    console.log('listening on port ' + PORT);
})
