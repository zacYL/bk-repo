const path = require('path')
const webpack = require('webpack')
const MiniCssExtractPlugin = require('mini-css-extract-plugin')
const VueLoaderPlugin = require('vue-loader/lib/plugin')
const CopyWebpackPlugin = require('copy-webpack-plugin')

module.exports = ({ entry, publicPath, dist, port = 8080, argv, env }) => {
    const isDev = argv.mode === 'development'
    const envDist = env && env.dist ? env.dist : 'frontend'
    const buildDist = path.join(__dirname, envDist, dist)
    return {
        devtool: isDev ? 'source-map' : 'none',
        entry,
        output: {
            publicPath,
            chunkFilename: !isDev ? '[name].[chunkhash].js' : '[name].js',
            filename: !isDev ? '[name].[contentHash].min.js' : '[name].js',
            path: buildDist
        },
        module: {
            rules: [
                {
                    test: /\.vue$/,
                    include: [path.resolve(__dirname, 'devops-repository/src'), path.resolve('src')],
                    loader: 'vue-loader'
                },
                {
                    test: /\.js$/,
                    include: [path.resolve(__dirname, 'devops-repository/src'), path.resolve('src')],
                    use: [
                        {
                            loader: 'babel-loader'
                        }
                    ]
                },
                {
                    test: /\.css$/,
                    use: [MiniCssExtractPlugin.loader, 'css-loader']
                },
                {
                    test: /\.scss$/,
                    use: [MiniCssExtractPlugin.loader, 'css-loader', 'sass-loader']
                },
                {
                    test: /\.svg$/,
                    loader: 'svg-sprite-loader',
                    include: [
                        path.resolve(__dirname, 'devops-repository/src/images'),
                        path.resolve('src/images')
                    ]
                },
                {
                    test: /\.(png|jpe?g|gif|svg|webp|cur)(\?.*)?$/,
                    loader: 'url-loader',
                    exclude: [
                        path.resolve(__dirname, 'devops-repository/src/images'),
                        path.resolve('src/images')
                    ],
                    options: {
                        limit: 10000,
                        name: '[name].[ext]?[hash]'
                    }
                },
                {
                    test: /\.(js|vue)$/,
                    loader: 'eslint-loader',
                    enforce: 'pre',
                    include: [path.resolve(__dirname, 'devops-repository/src'), path.resolve('src')],
                    exclude: /node_modules/,
                    options: {
                        fix: true,
                        formatter: require('eslint-friendly-formatter')
                    }
                },
                {
                    test: /\.(woff2?|eot|ttf|otf)(\?.*)?$/,
                    loader: 'url-loader',
                    options: {
                        esModule: false
                    }
                }
            ]
        },
        plugins: [
            new VueLoaderPlugin(),
            new webpack.optimize.LimitChunkCountPlugin({
                minChunkSize: 1000
            }),
            new webpack.HashedModuleIdsPlugin(),
            new MiniCssExtractPlugin({
                filename: !isDev ? '[name].[chunkHash].css' : '[name].css',
                chunkName: '[id].css'
            }),
            new CopyWebpackPlugin([{ from: path.join(__dirname, 'locale', dist), to: buildDist }])
        ],
        optimization: {
            namedChunks: true,
            minimize: !isDev
        },
        resolve: {
            extensions: ['.js', '.vue', '.json', '.ts', '.scss', '.css'],
            alias: {
                '@': path.resolve('src'),
                '@repository': path.resolve(__dirname, 'devops-repository/src'),
                '@locale': path.resolve(__dirname, 'locale'),
                'vue$': 'vue/dist/vue.esm.js'
            }
        },
        devServer: {
            contentBase: path.join(__dirname, envDist),
            historyApiFallback: true,
            noInfo: false,
            disableHostCheck: true,
            port
        }
    }
}
