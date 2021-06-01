const path = require('path')
const webpack = require('webpack')
const HtmlWebpackPlugin = require('html-webpack-plugin')
const CopyWebpackPlugin = require('copy-webpack-plugin')
const webpackBaseConfig = require('../webpack.base')

module.exports = (env, argv) => {
    const isProd = argv.mode === 'production'
    const urlPrefix = env && env.name ? `${env.name}` : ''
    const envDist = env && env.dist ? env.dist : 'frontend'
    const extUrlPrefix = env && env.name ? `${env.name}-` : ''
    const dist = path.join(__dirname, `../${envDist}/bksoftware`)
    const config = webpackBaseConfig({
        env,
        argv,
        entry: {
            repository: './src/main.js'
        },
        publicPath: '/bksoftware/',
        dist: '/bksoftware',
        port: 8086
    })
    config.plugins.pop()
    config.plugins = [
        ...config.plugins,
        new HtmlWebpackPlugin({
            filename: isProd ? `${dist}/frontend#bksoftware#index.html` : `${dist}/index.html`,
            template: 'index.html',
            inject: true,
            VENDOR_LIBS: `/bksoftware/main.dll.js?v=${Math.random()}`,
            urlPrefix,
            extUrlPrefix,
            timeStamp: +new Date()
        }),
        new webpack.DllReferencePlugin({
            context: __dirname,
            manifest: require('./dist/manifest.json')
        }),
        new CopyWebpackPlugin([{ from: path.join(__dirname, './static'), to: dist }]),
        new CopyWebpackPlugin([{ from: path.join(__dirname, './dist'), to: dist }])
    ]

    config.devServer.historyApiFallback = {
        rewrites: [
            { from: /^\/bksoftware/, to: '/bksoftware/index.html' }
        ]
    }
    return config
}
