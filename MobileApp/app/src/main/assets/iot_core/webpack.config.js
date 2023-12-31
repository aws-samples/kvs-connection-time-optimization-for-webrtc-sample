const NodePolyfillPlugin = require("node-polyfill-webpack-plugin");
const TerserPlugin = require("terser-webpack-plugin");

module.exports = {
  entry: "./index.ts",
  devtool: "source-map",
  target: "web",
  output: {
    filename: "index.js",
  },
  resolve: {
    extensions: [".ts", ".js"],
  },
  module: {
    rules: [
      // all files with a '.ts' or '.tsx' extension will be handled by 'ts-loader'
      { test: /\.tsx?$/, use: ["ts-loader"], exclude: /node_modules/ },
    ],
  },
  plugins: [new NodePolyfillPlugin()],
  optimization: {
      minimize: true,
      minimizer: [
        new TerserPlugin({
            extractComments: false,
          }
        )
      ],
    }
};
