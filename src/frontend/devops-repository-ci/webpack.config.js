const path = require('path')
const HtmlWebpackPlugin = require('html-webpack-plugin')
const CopyWebpackPlugin = require('copy-webpack-plugin')
const webpackBaseConfig = require('../webpack.base')

module.exports = (env, argv) => {
    const isProd = argv.mode === 'production'
    const urlPrefix = env && env.name ? `${env.name}` : ''
    const envDist = env && env.dist ? env.dist : 'frontend'
    const extUrlPrefix = env && env.name ? `${env.name}-` : ''
    const dist = path.join(__dirname, `../${envDist}/ui-ci`)
    const config = webpackBaseConfig({
        env,
        argv,
        entry: {
            repository: './src/main.js'
        },
        publicPath: '/ui/',
        dist: '/ui-ci',
        port: 8086
    })
    config.plugins.pop()
    config.plugins = [
        ...config.plugins,
        new HtmlWebpackPlugin({
            filename: isProd ? `${dist}/frontend#ui#index.html` : `${dist}/index.html`,
            template: 'index.html',
            inject: true,
            urlPrefix,
            extUrlPrefix,
            timeStamp: +new Date()
        }),
        new CopyWebpackPlugin([{ from: path.join(__dirname, './static'), to: dist }])
    ]

    config.devServer.historyApiFallback = {
        rewrites: [
            { from: /^\/ui/, to: '/ui/index.html' }
        ]
    }
    return config
}
