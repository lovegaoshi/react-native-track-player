const { getDefaultConfig, mergeConfig } = require('@react-native/metro-config');
/**
 * Metro configuration
 * https://reactnative.dev/docs/metro
 *
 * @type {import('metro-config').MetroConfig}
 */

const path = require('path');
const escape = require('escape-string-regexp');
const pak = require('../package.json');

const root = path.resolve(__dirname, '..');
const modules = Object.keys({
  ...pak.peerDependencies,
});

/** build the blockList **/
const blockList = modules.map(
  (m) => new RegExp(`^${escape(path.join(root, 'node_modules', m))}\\/.*$`),
);
// This stops "react-native run-windows" from causing the metro server to crash if its already running
blockList.push(
  new RegExp(`${path.resolve(__dirname, 'windows').replace(/[/\\]/g, '/')}.*`),
);
// This prevents "react-native run-windows" from hitting: EBUSY: resource busy or locked, open msbuild.ProjectImports.zip
blockList.push(/.*\.ProjectImports\.zip/);

/** build extraNodeModules **/
const extraNodeModules = modules.reduce((acc, name) => {
  acc[name] = path.join(__dirname, 'node_modules', name);
  return acc;
}, {});

const config = {
  projectRoot: __dirname,
  watchFolders: [root],

  // We need to make sure that only one version is loaded for peerDependencies
  // So we exclude them at the root, and alias them to the versions in
  // example's node_modules
  resolver: {
    blockList,
    extraNodeModules,
    nodeModulesPaths: [path.resolve(__dirname, 'node_modules')],
  },
  transformer: {
    getTransformOptions: async () => ({
      transform: {
        experimentalImportSupport: false,
        inlineRequires: true,
      },
    }),
  },
};
module.exports = mergeConfig(getDefaultConfig(__dirname), config);
